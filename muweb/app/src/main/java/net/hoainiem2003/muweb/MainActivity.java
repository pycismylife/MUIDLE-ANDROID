package net.hoainiem2003.muweb;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class MainActivity extends Activity {
    private static final String HOME = "https://hoainiem2003.net/MuWeb/";

    private static final int IMMERSIVE_FLAGS =
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

    private WebView webView;
    private FrameLayout root;

    private void applyImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(IMMERSIVE_FLAGS);
        if (root != null) {
            root.setSystemUiVisibility(IMMERSIVE_FLAGS);
        }
    }

    private void startKeepAliveService() {
        Intent serviceIntent = new Intent(this, KeepAliveService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        root = new FrameLayout(this);
        root.setFitsSystemWindows(false);

        webView = new WebView(this);
        webView.setBackgroundColor(0xFF000000);
        webView.setFitsSystemWindows(false);
        root.addView(webView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        setContentView(root);
        applyImmersiveMode();
        startKeepAliveService();
        configureWebView();

        if (savedInstanceState == null) {
            webView.loadUrl(HOME);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        // Keep the WebView's initial page and text scale consistent.
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setTextZoom(100);
        webView.setInitialScale(100);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url != null && (url.startsWith("https://hoainiem2003.net/")
                    || url.startsWith("https://www.hoainiem2003.net/"))) {
                    installConnectionRecovery();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                WebView view,
                WebResourceRequest request
            ) {
                Uri uri = request.getUrl();
                String host = uri.getHost();

                if ("hoainiem2003.net".equalsIgnoreCase(host)
                    || "www.hoainiem2003.net".equalsIgnoreCase(host)) {
                    return false;
                }

                if ("https".equalsIgnoreCase(uri.getScheme())
                    || "http".equalsIgnoreCase(uri.getScheme())) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (Exception ignored) {
                        // Ignore external links when no compatible app is available.
                    }
                }
                return true;
            }
        });
    }

    private void installConnectionRecovery() {
        String script =
            "(function() {" +
            " if (window.__muConnectionRecoveryInstalled) return;" +
            " window.__muConnectionRecoveryInstalled = true;" +
            " var failures = 0, checking = false, recovering = false, lastCheck = 0;" +
            " var intervalMs = 45000, timeoutMs = 12000;" +
            " function recover(reason) {" +
            "   if (recovering || !navigator.onLine) return;" +
            "   recovering = true;" +
            "   setTimeout(function() {" +
            "     if (navigator.onLine) {" +
            "       try { window.location.reload(); } catch(e) {}" +
            "     } else { recovering = false; }" +
            "   }, 1500);" +
            " }" +
            " async function heartbeat() {" +
            "   if (checking || recovering || !navigator.onLine) return;" +
            "   checking = true; lastCheck = Date.now();" +
            "   var controller = new AbortController();" +
            "   var timer = setTimeout(function(){ controller.abort(); }, timeoutMs);" +
            "   try {" +
            "     var response = await fetch('" + HOME + "', {" +
            "       method:'GET', cache:'no-store', credentials:'include'," +
            "       headers:{'Range':'bytes=0-0'}, signal:controller.signal" +
            "     });" +
            "     if (response.ok || response.status === 206 || response.status === 304) {" +
            "       failures = 0; recovering = false;" +
            "     } else { failures++; }" +
            "   } catch(e) { failures++; }" +
            "   finally { clearTimeout(timer); checking = false; }" +
            "   if (failures >= 2) recover('heartbeat-failed');" +
            " }" +
            " window.addEventListener('online', function(){ failures = 0; recover('network-restored'); });" +
            " window.addEventListener('pageshow', function(){ if (navigator.onLine && Date.now()-lastCheck > intervalMs) heartbeat(); });" +
            " document.addEventListener('visibilitychange', function(){ if (!document.hidden && navigator.onLine) heartbeat(); });" +
            " setInterval(heartbeat, intervalMs);" +
            " setTimeout(heartbeat, 10000);" +
            " window.__muReconnectNow = function(){ if (navigator.onLine) heartbeat(); };" +
            "})();";
        webView.evaluateJavascript(script, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
            webView.evaluateJavascript(
                "if(window.__muReconnectNow && navigator.onLine) window.__muReconnectNow();",
                null
            );
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            applyImmersiveMode();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) {
            webView.saveState(outState);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}