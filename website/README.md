# Reelbox — marketing site

Single-file static site (`index.html`) for the Reelbox Android app. No build step, no
dependencies, no JavaScript framework — just HTML, CSS and ~250 lines of vanilla JS for
the mobile menu, scrollspy, reveal animations, gallery and FAQ.

## Files

| File | Purpose |
|---|---|
| `index.html` | The entire site: markup, CSS and JS inline |
| `reelbox-1.1.apk` | The signed release APK the Download buttons serve (2.2 MB) |
| `version.json` | Latest release (version code, name, APK URL, notes) — the app reads this at launch to offer updates |
| `home_real.webp` | Home screen — hero mockup + gallery |
| `detail_real.webp` | Detail screen — gallery |
| `providers_real.webp` | "Where to watch" — spotlight + gallery |
| `search_real.webp` | Search results — gallery |
| `watchlist_real.webp` | Watchlist — gallery |
| `og-image.png` | 1200×630 social card |
| `app-icon.png` | 512×512 icon (download card, JSON-LD) |
| `apple-touch-icon.png` | 180×180 iOS home screen icon |
| `favicon.svg`, `favicon-32.png` | Browser tab icons |
| `.capture/final_*.png` | Full-resolution device captures these webp files came from |

Screenshots are 540×1204 WebP, captured on a Samsung device at 1080×2408 and downscaled.
`.capture/` is source material only — exclude it from a deploy if you want a leaner upload.

## Deploy

Netlify drop:

```bash
cd website
npx netlify deploy --prod --dir .
```

Vercel:

```bash
cd website
npx vercel --prod
```

GitHub Pages: push the folder contents to a `gh-pages` branch, then
Settings → Pages → Deploy from branch.

Anything that serves static files works (Cloudflare Pages, Firebase Hosting, S3 + CloudFront,
nginx). The only runtime requirement is HTTPS — Google Fonts and the TMDB links are external.

## Update the download

The APK lives next to `index.html`, so the buttons just point at it:

```html
<a href="reelbox-1.1.apk" download id="download-btn" data-apk-url="reelbox-1.1.apk">
```

Rebuild with `./gradlew :app:assembleRelease` (needs `keystore.properties` and the release
keystore in the repo root — both gitignored), copy `app/build/outputs/apk/release/app-release.apk`
into this folder under a versioned name, then update `href`, `data-apk-url`, and the
`Version` / `Size` cells in `.download-meta` (search for `reelbox-1.0`).

If you'd rather link a GitHub release than host the binary, point `href` and `data-apk-url`
at the release URL and drop the local APK. With `data-apk-url` empty, the button shows an
inline hint in `#download-status` instead of doing nothing.

## Ship an app update

Installed copies of Reelbox (1.1 and later) fetch `version.json` at launch and show an
"Update available" prompt when its `versionCode` is higher than their own.

One command does the whole release (run it from the repo root):

```bash
python3 scripts/release.py 1.2 "What changed, in a sentence or two." --push
```

It bumps `versionCode` / `versionName` in `app/build.gradle.kts`, builds the signed release
APK, swaps it into this folder as `reelbox-<version>.apk`, updates `version.json` and the
download link, version and size in `index.html`, commits, and (with `--push`) pushes so
Vercel redeploys. Users are prompted on their next launch.

- Add `--dry-run` to preview the changes without touching anything.
- Leave off `--push` to review the commit first, then `git push` yourself.
- The notes text is what users read in the update dialog.
- It needs `keystore.properties` and the release keystore in the repo root; a build signed
  with any other key cannot install over the existing app.

## Update the repo link

Two places carry a placeholder:

- `#source-btn` (the "View source" button) — `data-repo-url="…"`
- the footer "Source code" link — `data-repo-url="…"`

Set both to the repository URL and the script lets the links through.

## Structure of the page

`#top` hero → `#where-to-watch` spotlight → `#features` → how it works →
`#screenshots` gallery → FAQ → `#download` → footer.

Anchors in the header and the mobile menu are derived from these ids, and the scrollspy
marks the active one with `aria-current`. If you rename a section, update the `href`s and
the `sections` list in the scrollspy script (step 2).

## Design rules

The page follows `../DESIGN.md`: dark only, accent `#FF4B4B`, gold `#FFC94A`, violet
`#8B5CF6`, Space Grotesk for display type, Inter for body, 8px spacing scale, 1160px
container, no box-shadows (borders and elevation by background only). Keep new sections on
that scale rather than inventing values.

`../PRODUCT.md` bans gradient text, glassmorphism, hero-metric templates and marketing
fluff — that's why the copy is specific (12 regions, 2.2 MB, no account) instead of
superlative.

## Before you ship

- [ ] `data-apk-url` / `data-repo-url` point at real URLs
- [ ] `og:image` / `twitter:image` resolve absolutely — social scrapers need a full
      `https://…` URL, so change the relative `og-image.png` to your domain
- [ ] Footer says "© 2026" — bump it once a year
- [ ] Test the menu, gallery arrows and FAQ with a keyboard only
