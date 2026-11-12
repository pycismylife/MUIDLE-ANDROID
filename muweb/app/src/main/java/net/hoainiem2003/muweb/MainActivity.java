package net.hoainiem2003.muweb;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final String HOME = "https://hoainiem2003.net/MuWeb/";
    private static final String PREFS = "mu_web_auto_route";
    private static final String ROUTE_KEY = "route_json";
    private WebView webView;
    private FrameLayout root;
    private LinearLayout controlPanel;
    private TextView statusView;
    private final Handler routeHandler = new Handler(Looper.getMainLooper());
    private final JSONArray recordedRoute = new JSONArray();
    private JSONArray playbackRoute = new JSONArray();
    private boolean recording = false;
    private boolean playing = false;
    private long lastRecordedAt = 0L;
    private float downRawX, downRawY;
    private boolean touchMoved = false;

    private static final int IMMERSIVE_FLAGS =
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
        View.SYSTEM_UI_FLAG_FULLSCREEN |
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

    private void applyImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(IMMERSIVE_FLAGS);
        if (root != null) root.setSystemUiVisibility(IMMERSIVE_FLAGS);
    }

    private void startKeepAlive() {
        Intent service = new Intent(this, KeepAliveService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(service);
        else startService(service);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView makeButton(String label, int background) {
        TextView button = new TextView(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(12);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(10), dp(8), dp(10), dp(8));
        button.setBackgroundColor(background);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, dp(38));
        lp.setMargins(dp(2), dp(2), dp(2), dp(2));
        controlPanel.addView(button, lp);
        return button;
    }

    private void createAutoRoutePanel() {
        controlPanel = new LinearLayout(this);
        controlPanel.setOrientation(LinearLayout.VERTICAL);
        controlPanel.setPadding(dp(4), dp(4), dp(4), dp(4));
        controlPanel.setBackgroundColor(0xCC151A22);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        controlPanel.addView(buttons);

        TextView record = panelButton(buttons, "REC", 0xFFB23A48);
        TextView save = panelButton(buttons, "SAVE", 0xFF455A64);
        TextView play = panelButton(buttons, "LOOP", 0xFF287A52);
        TextView stop = panelButton(buttons, "STOP", 0xFF8A5A24);
        TextView clear = panelButton(buttons, "CLEAR", 0xFF555555);

        statusView = new TextView(this);
        statusView.setText("AUTO ROUTE: Chưa có lộ trình");
        statusView.setTextColor(Color.WHITE);
        statusView.setTextSize(10);
        statusView.setPadding(dp(6), dp(2), dp(6), dp(2));
        controlPanel.addView(statusView);

        record.setOnClickListener(v -> startRecording());
        save.setOnClickListener(v -> saveRecording());
        play.setOnClickListener(v -> startPlayback());
        stop.setOnClickListener(v -> stopPlayback());
        clear.setOnClickListener(v -> clearRoute());

        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP | Gravity.LEFT);
        panelParams.setMargins(dp(8), dp(8), 0, 0);
        root.addView(controlPanel, panelParams);
        controlPanel.bringToFront();
    }

    private TextView panelButton(LinearLayout parent, String label, int color) {
        TextView button = new TextView(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(11);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(8), dp(7), dp(8), dp(7));
        button.setBackgroundColor(color);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, dp(34));
        lp.setMargins(dp(2), dp(2), dp(2), dp(2));
        parent.addView(button, lp);
        return button;
    }

    private void setStatus(String text) {
        if (statusView != null) statusView.setText("AUTO ROUTE: " + text);
    }

    private void startRecording() {
        stopPlayback();
        recordedRoute.put(new JSONObject());
        recordedRoute.put(new JSONObject());
        // Start a fresh route; existing route remains saved until SAVE is pressed.
        while (recordedRoute.length() > 0) recordedRoute.remove(recordedRoute.length() - 1);
        recording = true;
        lastRecordedAt = SystemClock.elapsedRealtime();
        setStatus("ĐANG GHI - thao tác map/farm như bình thường");
    }

    private void saveRecording() {
        if (!recording && recordedRoute.length() == 0) {
            setStatus("Chưa ghi thao tác nào");
            return;
        }
        recording = false;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putString(ROUTE_KEY, recordedRoute.toString()).apply();
        setStatus("Đã lưu " + recordedRoute.length() + " thao tác");
    }

    private void clearRoute() {
        stopPlayback();
        recording = false;
        while (recordedRoute.length() > 0) recordedRoute.remove(recordedRoute.length() - 1);
        playbackRoute = new JSONArray();
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(ROUTE_KEY).apply();
        setStatus("Đã xóa lộ trình");
    }

    private void loadSavedRoute() {
        String json = getSharedPreferences(PREFS, MODE_PRIVATE).getString(ROUTE_KEY, "[]");
        try { playbackRoute = new JSONArray(json); }
        catch (Exception e) { playbackRoute = new JSONArray(); }
    }

    private void startPlayback() {
        if (recording) saveRecording();
        loadSavedRoute();
        if (playbackRoute.length() == 0) {
            setStatus("Chưa có lộ trình - nhấn REC để ghi");
            return;
        }
        playing = true;
        setStatus("ĐANG CHẠY " + playbackRoute.length() + " thao tác - lặp liên tục");
        schedulePlaybackStep(0);
    }

    private void stopPlayback() {
        playing = false;
        routeHandler.removeCallbacksAndMessages(null);
        if (statusView != null && !recording) setStatus("Đã dừng");
    }

    private void schedulePlaybackStep(int index) {
        if (!playing || webView == null || playbackRoute.length() == 0) return;
        try {
            JSONObject step = playbackRoute.getJSONObject(index);
            long delay = Math.max(80L, step.optLong("delay", 250L));
            routeHandler.postDelayed(() -> {
                if (!playing || webView == null) return;
                float x = (float) step.optDouble("x", 0.5);
                float y = (float) step.optDouble("y", 0.5);
                dispatchRecordedTap(x, y);
                int next = index + 1;
                if (next >= playbackRoute.length()) next = 0;
                schedulePlaybackStep(next);
            }, delay);
        } catch (Exception e) {
            stopPlayback();
            setStatus("Lỗi dữ liệu lộ trình");
        }
    }

    private void dispatchRecordedTap(float normalizedX, float normalizedY) {
        if (webView == null || webView.getWidth() <= 0 || webView.getHeight() <= 0) return;
        float x = Math.max(1f, Math.min(webView.getWidth() - 1f, normalizedX * webView.getWidth()));
        float y = Math.max(1f, Math.min(webView.getHeight() - 1f, normalizedY * webView.getHeight()));
        long now = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0);
        MotionEvent up = MotionEvent.obtain(now, now + 55, MotionEvent.ACTION_UP, x, y, 0);
        webView.dispatchTouchEvent(down);
        webView.dispatchTouchEvent(up);
        down.recycle();
        up.recycle();
    }

    private boolean pointInsidePanel(float rawX, float rawY) {
        if (controlPanel == null || controlPanel.getVisibility() != View.VISIBLE) return false;
        int[] location = new int[2];
        controlPanel.getLocationOnScreen(location);
        return rawX >= location[0] && rawX <= location[0] + controlPanel.getWidth()
            && rawY >= location[1] && rawY <= location[1] + controlPanel.getHeight();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (recording && webView != null) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                touchMoved = false;
            } else if (action == MotionEvent.ACTION_MOVE) {
                if (Math.hypot(event.getRawX() - downRawX, event.getRawY() - downRawY) > dp(18)) touchMoved = true;
            } else if (action == MotionEvent.ACTION_UP && !touchMoved
                && !pointInsidePanel(event.getRawX(), event.getRawY())) {
                int[] location = new int[2];
                webView.getLocationOnScreen(location);
                float localX = event.getRawX() - location[0];
                float localY = event.getRawY() - location[1];
                if (localX >= 0 && localY >= 0 && localX < webView.getWidth() && localY < webView.getHeight()) {
                    long now = SystemClock.elapsedRealtime();
                    long delay = recordedRoute.length() == 0 ? 0 : Math.max(80, now - lastRecordedAt);
                    JSONObject step = new JSONObject();
                    try {
                        step.put("x", localX / webView.getWidth());
                        step.put("y", localY / webView.getHeight());
                        step.put("delay", delay);
                        recordedRoute.put(step);
                        lastRecordedAt = now;
                        setStatus("Đang ghi " + recordedRoute.length() + " thao tác");
                    } catch (Exception ignored) {}
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyImmersiveMode();
        startKeepAlive();

        root = new FrameLayout(this);
        root.setFitsSystemWindows(false);
        webView = new WebView(this);
        webView.setBackgroundColor(0xFF000000);
        webView.setFitsSystemWindows(false);
        root.addView(webView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);
        createAutoRoutePanel();
        applyImmersiveMode();

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
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setTextZoom(100);
        webView.setInitialScale(100);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost();
                if ("hoainiem2003.net".equalsIgnoreCase(host) || "www.hoainiem2003.net".equalsIgnoreCase(host)) return false;
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

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersiveMode();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) webView.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (playing) stopPlayback();
        else if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        stopPlayback();
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}