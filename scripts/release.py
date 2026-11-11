#!/usr/bin/env python3
"""Cut a Reelbox release and publish it on the website.

    python3 scripts/release.py 1.2 "Faster search and a new region picker."
    python3 scripts/release.py 1.2 "…" --push      # also push, which deploys via Vercel
    python3 scripts/release.py 1.2 "…" --dry-run   # show what would change, touch nothing

What it does, in order:
  1. Bumps versionCode (+1) and versionName in app/build.gradle.kts.
  2. Builds the signed release APK (./gradlew :app:assembleRelease).
  3. Copies it to website/reelbox-<version>.apk and removes the previous APK.
  4. Updates website/version.json, plus the download link, version and size in
     website/index.html and website/README.md.
  5. Commits. With --push it also pushes; Vercel then redeploys and installed
     apps (1.1+) offer the update on their next launch.

If the build fails, the Gradle file is restored and nothing else is touched.
"""

from __future__ import annotations

import argparse
import json
import re
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GRADLE = ROOT / "app" / "build.gradle.kts"
WEBSITE = ROOT / "website"
VERSION_JSON = WEBSITE / "version.json"
INDEX = WEBSITE / "index.html"
SITE_README = WEBSITE / "README.md"
BUILT_APK = ROOT / "app" / "build" / "outputs" / "apk" / "release" / "app-release.apk"

APK_NAME = re.compile(r"reelbox-\d+(?:\.\d+)*\.apk")


def fail(message: str) -> "NoReturn":
    sys.exit(f"release: {message}")


def git(*args: str) -> str:
    return subprocess.run(
        ["git", *args], cwd=ROOT, check=True, capture_output=True, text=True
    ).stdout


def replace(text: str, pattern: str, repl: str, where: str) -> str:
    """re.sub that refuses to silently match nothing."""
    new, count = re.subn(pattern, repl, text)
    if count == 0:
        fail(f"could not find /{pattern}/ in {where}; update scripts/release.py to match.")
    return new


def main() -> None:
    parser = argparse.ArgumentParser(description="Cut and publish a Reelbox release.")
    parser.add_argument("version", help='new version name, e.g. "1.2"')
    parser.add_argument("notes", help="one or two sentences shown in the in-app update prompt")
    parser.add_argument("--push", action="store_true", help="push after committing (deploys the site)")
    parser.add_argument("--dry-run", action="store_true", help="print the plan and change nothing")
    args = parser.parse_args()

    version = args.version.lstrip("v")
    notes = args.notes.strip()
    if not re.fullmatch(r"\d+(\.\d+){1,2}", version):
        fail(f'"{args.version}" is not a version like 1.2 or 1.2.1.')
    if not notes:
        fail("release notes cannot be empty.")

    # ---- Preconditions -------------------------------------------------
    keystore_props = ROOT / "keystore.properties"
    if not keystore_props.exists():
        fail("keystore.properties is missing, so the build would be signed with the debug "
             "key and could not update existing installs. Restore it and the .jks first.")
    store_file = next(
        (line.split("=", 1)[1].strip() for line in keystore_props.read_text().splitlines()
         if line.startswith("storeFile=")),
        "",
    )
    if not store_file or not (ROOT / store_file).exists():
        fail(f"the keystore named in keystore.properties ({store_file or 'none'}) is missing.")

    dirty = [l for l in git("status", "--porcelain").splitlines() if not l.startswith("??")]
    if dirty:
        fail("commit or stash your changes first:\n  " + "\n  ".join(dirty))

    gradle_before = GRADLE.read_text(encoding="utf-8")
    code_match = re.search(r"versionCode = (\d+)", gradle_before)
    name_match = re.search(r'versionName = "([^"]+)"', gradle_before)
    if not code_match or not name_match:
        fail("could not read versionCode / versionName from app/build.gradle.kts.")
    old_code, old_name = int(code_match.group(1)), name_match.group(1)
    new_code = old_code + 1
    if version == old_name:
        fail(f"the app is already at {old_name}; pick a new version name.")

    info = json.loads(VERSION_JSON.read_text(encoding="utf-8"))
    site = info["apkUrl"].rsplit("/", 1)[0]
    old_apks = sorted(p for p in WEBSITE.glob("reelbox-*.apk"))
    new_apk = WEBSITE / f"reelbox-{version}.apk"

    print(f"Reelbox {old_name} (code {old_code})  ->  {version} (code {new_code})")
    print(f"  APK:    {', '.join(p.name for p in old_apks) or 'none'}  ->  {new_apk.name}")
    print(f"  URL:    {site}/{new_apk.name}")
    print(f"  Notes:  {notes}")
    print(f"  Push:   {'yes' if args.push else 'no (commit only)'}")
    if args.dry_run:
        print("Dry run: nothing was changed.")
        return

    # ---- 1 + 2. Bump and build ----------------------------------------
    gradle_after = gradle_before.replace(code_match.group(0), f"versionCode = {new_code}", 1)
    gradle_after = gradle_after.replace(name_match.group(0), f'versionName = "{version}"', 1)
    GRADLE.write_text(gradle_after, encoding="utf-8")

    print("\nBuilding release APK…")
    build = subprocess.run(["./gradlew", ":app:assembleRelease", "-q"], cwd=ROOT)
    if build.returncode != 0 or not BUILT_APK.exists():
        GRADLE.write_text(gradle_before, encoding="utf-8")
        fail("the build failed; app/build.gradle.kts was restored and nothing was published.")

    # ---- 3. Swap the APK ----------------------------------------------
    for old in old_apks:
        if old != new_apk:
            git("rm", "-q", "--", str(old.relative_to(ROOT)))
    shutil.copyfile(BUILT_APK, new_apk)
    size = f"{new_apk.stat().st_size / 1_048_576:.1f}"

    # ---- 4. Website ----------------------------------------------------
    info.update(versionCode=new_code, versionName=version,
                apkUrl=f"{site}/{new_apk.name}", notes=notes)
    VERSION_JSON.write_text(json.dumps(info, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    html = INDEX.read_text(encoding="utf-8")
    html = replace(html, APK_NAME.pattern, new_apk.name, "index.html")
    html = replace(html, r"dev\.reelbox · [\d.]+", f"dev.reelbox · {version}", "index.html")
    html = replace(html, r"(<dt>Version</dt><dd>)[^<]*", rf"\g<1>{version}", "index.html")
    html = replace(html, r"(<dt>Size</dt><dd>)[^<]*", rf"\g<1>{size} MB", "index.html")
    html = replace(html, r'(<span class="fact-value">)[\d.]+&nbsp;MB', rf"\g<1>{size}&nbsp;MB", "index.html")
    INDEX.write_text(html, encoding="utf-8")

    readme = SITE_README.read_text(encoding="utf-8")
    readme = replace(readme, APK_NAME.pattern, new_apk.name, "website/README.md")
    readme = re.sub(r"\b[\d.]+ MB\b", f"{size} MB", readme)
    SITE_README.write_text(readme, encoding="utf-8")

    # ---- 5. Commit (and push) -----------------------------------------
    git("add", "--", "app/build.gradle.kts", str(new_apk.relative_to(ROOT)),
        "website/version.json", "website/index.html", "website/README.md")
    git("commit", "-q", "-m", f"Release {version}\n\n{notes}")
    print(f"Committed release {version} ({size} MB).")

    if args.push:
        subprocess.run(["git", "push"], cwd=ROOT, check=True)
        print(f"Pushed. Vercel will redeploy; check {site}/version.json in a minute.")
    else:
        print("Not pushed. Run `git push` to publish it.")


if __name__ == "__main__":
    main()
