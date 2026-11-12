package net.hoainiem2003.muweb;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String HOME = "https://hoainiem2003.net/MuWeb/";
    private WebView webView;
    private FrameLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        root = new FrameLayout(this);
        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        root.addView(webView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));
        setContentView(root);

        TextView diagnosticButton = new TextView(this);
        diagnosticButton.setText("DIAG");
        diagnosticButton.setTextColor(Color.WHITE);
        diagnosticButton.setTextSize(12);
        diagnosticButton.setGravity(Gravity.CENTER);
        diagnosticButton.setBackgroundColor(0xCC202020);
        diagnosticButton.setPadding(18, 12, 18, 12);
        FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP | Gravity.END
        );
        buttonParams.setMargins(0, 28, 12, 0);
        root.addView(diagnosticButton, buttonParams);
        diagnosticButton.setOnClickListener(v -> collectDiagnostic());

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

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost();
                if ("hoainiem2003.net".equalsIgnoreCase(host) || "www.hoainiem2003.net".equalsIgnoreCase(host)) {
                    return false;
                }
                if ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) {}
                    return true;
                }
                return true;
            }
        });

        if (savedInstanceState == null) webView.loadUrl(HOME);
        else webView.restoreState(savedInstanceState);
    }

    private void collectDiagnostic() {
        String js = "(function(){"
            + "function info(e){var s=getComputedStyle(e),r=e.getBoundingClientRect();"
            + "return {tag:e.tagName,id:e.id||'',cls:(typeof e.className==='string'?e.className:''),"
            + "w:Math.round(r.width),h:Math.round(r.height),x:Math.round(r.x),y:Math.round(r.y),"
            + "transform:s.transform,rotate:s.rotate,writingMode:s.writingMode,position:s.position};}"
            + "var all=[].slice.call(document.querySelectorAll('canvas,iframe,body,html,[style*=\"transform\"],[style*=\"rotate\"]'));"
            + "var seen=[];all.forEach(function(e){var q=info(e);if(q.w>0&&q.h>0&&(e.tagName==='CANVAS'||e.tagName==='IFRAME'||e===document.body||e===document.documentElement||q.transform!=='none'||q.rotate!=='none'))seen.push(q);});"
            + "return JSON.stringify({url:location.href,title:document.title,ua:navigator.userAgent,"
            + "innerWidth:innerWidth,innerHeight:innerHeight,outerWidth:outerWidth,outerHeight:outerHeight,"
            + "devicePixelRatio:devicePixelRatio,screenWidth:screen.width,screenHeight:screen.height,"
            + "availWidth:screen.availWidth,availHeight:screen.availHeight,"
            + "orientation:(screen.orientation?screen.orientation.type+' '+screen.orientation.angle:'unsupported'),"
            + "visualViewport:(window.visualViewport?{width:visualViewport.width,height:visualViewport.height,scale:visualViewport.scale}:null),"
            + "elements:seen.slice(0,35)});})()";
        webView.evaluateJavascript(js, value -> {
            String page = value == null ? "Không lấy được dữ liệu trang." : value;
            DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getRealMetrics(dm);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            String rotationText = rotation == Surface.ROTATION_0 ? "0°" :
                rotation == Surface.ROTATION_90 ? "90°" :
                rotation == Surface.ROTATION_180 ? "180°" : "270°";
            String report = "ANDROID\n"
                + "pixel=" + dm.widthPixels + "x" + dm.heightPixels
                + "\ndensity=" + dm.density
                + "\nrotation=" + rotationText
                + "\nconfiguration=" + (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? "LANDSCAPE" : "PORTRAIT")
                + "\nWebView=" + webView.getWidth() + "x" + webView.getHeight()
                + "\n\nWEB PAGE (JSON)\n" + page;
            new AlertDialog.Builder(this)
                .setTitle("MU Web Diagnostic")
                .setMessage(report)
                .setPositiveButton("Sao chép", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("MU Web Diagnostic", report));
                    Toast.makeText(this, "Đã sao chép. Hãy gửi nội dung cho ChatGPT.", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Đóng", null)
                .show();
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) webView.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
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