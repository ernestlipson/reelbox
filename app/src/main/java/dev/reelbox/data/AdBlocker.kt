package dev.reelbox.data

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

object AdBlocker {

    private val BLOCKED_HOST_KEYWORDS = listOf(
        "bollingpatties",
        "animizecribber",
        "chiveyfewmets",
        "gogtib",
        "adsterra",
        "monetag",
        "propellerads",
        "popads",
        "popcash",
        "exoclick",
        "juicyads",
        "trafficjunky",
        "adsco",
        "adnxs",
        "histats",
        "creativecdn",
        "betting",
        "1xbet",
        "doubleclick",
        "googlesyndication",
        "google-analytics",
        "onclick",
        "trafficstars",
        "hilltopads",
        "clickadu",
        "admaven",
    )

    private val BLOCKED_PATH_KEYWORDS = listOf(
        "/popunder",
        "/onclick",
        "zoneid=",
        "/advert",
        "/banners/",
        "direct-link",
        "click.php",
    )

    fun isAd(url: String): Boolean {
        val lower = url.lowercase()
        return BLOCKED_HOST_KEYWORDS.any { lower.contains(it) } ||
            BLOCKED_PATH_KEYWORDS.any { lower.contains(it) }
    }

    fun emptyResource(): WebResourceResponse =
        WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))

    /**
     * Injected into the WebView to neutralize common popup / redirect tricks,
     * click-jacking overlays, and ensure the HTML5 video plays cleanly.
     */
    val INJECTION_SCRIPT: String = """
        (function() {
            // 1. Disable window.open to block popups and popunders
            window.open = function() {
                console.log('[AdBlock] Blocked window.open');
                return null;
            };

            // 2. Suppress intrusive popups
            window.alert = function() {};
            window.confirm = function() { return true; };
            window.prompt = function() { return null; };

            // 3. Remove known floating ad overlays
            var style = document.createElement('style');
            style.innerHTML = `
                iframe[src*="about:blank"],
                div[id*="ad-"], div[class*="ad-"],
                div[id*="pop-"], div[class*="pop-"],
                div[class*="banner"], div[id*="banner"] {
                    display: none !important;
                    visibility: hidden !important;
                    pointer-events: none !important;
                }
            `;
            (document.head || document.documentElement).appendChild(style);

            // 4. Auto-trigger HTML5 video playback
            function tryPlay() {
                var v = document.querySelector('video');
                if (v && v.paused) {
                    var p = v.play();
                    if (p !== undefined) {
                        p.catch(function() {
                            v.muted = true;
                            v.play();
                        });
                    }
                }
            }

            tryPlay();
            document.addEventListener('DOMContentLoaded', tryPlay);
            setInterval(tryPlay, 1500);
        })();
    """.trimIndent()
}
