package net.hoainiem2003.muidle

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var errorView: TextView
    private val homeUrl = "https://hoainiem2003.net/muidle"

    inner class AndroidBridge {
        @JavascriptInterface
        fun copyReport(report: String) {
            runOnUiThread {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("MU IDLE Diagnostic", report))
                Toast.makeText(this@MainActivity, "Đã sao chép báo cáo. Hãy dán vào ChatGPT.", Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this)
        webView = WebView(this)
        errorView = TextView(this).apply {
            text = "Không thể kết nối mạng.\nChạm để thử lại"
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
            setBackgroundColor(Color.WHITE)
            visibility = View.GONE
            setOnClickListener { loadHome() }
        }
        root.addView(webView, FrameLayout.LayoutParams(-1, -1))
        root.addView(errorView, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadsImagesAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
        }
        webView.addJavascriptInterface(AndroidBridge(), "MuIdleAndroid")
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                errorView.visibility = View.GONE
                webView.visibility = View.VISIBLE
                CookieManager.getInstance().flush()
                installDiagnosticPanel()
            }
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true) {
                    webView.visibility = View.GONE
                    errorView.visibility = View.VISIBLE
                }
            }
        }
        if (savedInstanceState != null) webView.restoreState(savedInstanceState) else loadHome()
    }

    private fun installDiagnosticPanel() {
        val js = """
        (function() {
          if (window.__muIdleDiagInstalled) return;
          window.__muIdleDiagInstalled = true;
          var style = document.createElement('style');
          style.textContent = '#mu-idle-diag{position:fixed;z-index:2147483647;top:12px;left:12px;font:14px Arial,sans-serif;color:#fff;max-width:calc(100vw - 24px)}#mu-idle-diag button{border:1px solid #777;background:rgba(18,18,18,.95);color:#fff;border-radius:5px;padding:9px 12px;font:inherit}#mu-idle-diag .gear{width:42px;height:42px;font-size:22px;padding:0}#mu-idle-diag .panel{display:none;margin-top:6px;padding:9px;background:rgba(10,10,10,.96);border:1px solid #777;max-height:72vh;overflow:auto}#mu-idle-diag .line{margin:7px 0}#mu-idle-diag .note{font-size:12px;color:#ffdf8a;line-height:1.4}#mu-idle-diag .scan{width:100%;margin-top:9px;background:#344b32}';
          document.head.appendChild(style);
          var box=document.createElement('div'); box.id='mu-idle-diag';
          box.innerHTML='<button class="gear">⚙</button><div class="panel"><div class="line">Ẩn minimap: <b>Chờ phân tích</b></div><div class="line">Contain: <b>Chờ phân tích</b></div><div class="line">Layer: <b>Chờ phân tích</b></div><div class="line">Render map 1s: <b>Chờ phân tích</b></div><div class="line">Xóa xác quái: <b>Chờ phân tích</b></div><div class="line">Ẩn skill: <b>Chờ phân tích</b></div><div class="line">Ẩn nền map: <b>Chờ phân tích</b></div><div class="note">Các công tắc đang chờ phân tích engine game. Chưa áp dụng thay đổi đồ họa.</div><button class="scan">Quét cấu trúc game và sao chép báo cáo</button></div>';
          document.documentElement.appendChild(box);
          var panel=box.querySelector('.panel');
          box.querySelector('.gear').onclick=function(){panel.style.display=panel.style.display==='block'?'none':'block';};
          box.querySelector('.scan').onclick=function(){
            var canvases=Array.from(document.querySelectorAll('canvas')).map(function(c,i){var r=c.getBoundingClientRect();return {index:i,id:c.id||'',className:String(c.className||''),pixelWidth:c.width,pixelHeight:c.height,cssWidth:Math.round(r.width),cssHeight:Math.round(r.height)};});
            var report={url:location.href,title:document.title,userAgent:navigator.userAgent,viewport:{width:innerWidth,height:innerHeight,dpr:devicePixelRatio},canvasCount:canvases.length,canvases:canvases,imageCount:document.images.length,videoCount:document.querySelectorAll('video').length,iframes:Array.from(document.querySelectorAll('iframe')).map(function(x){return {src:x.src,id:x.id,className:x.className};}),scripts:Array.from(document.scripts).map(function(x){return x.src;}).filter(Boolean),visibleElements:Array.from(document.querySelectorAll('img,canvas,video,iframe,[id],[class]')).slice(0,250).map(function(e){var r=e.getBoundingClientRect();return {tag:e.tagName,id:e.id||'',className:typeof e.className==='string'?e.className:'',width:Math.round(r.width),height:Math.round(r.height)};}).filter(function(e){return e.width>0&&e.height>0;}).slice(0,150),memory:performance.memory?{usedJSHeapSize:performance.memory.usedJSHeapSize,totalJSHeapSize:performance.memory.totalJSHeapSize}:'not exposed'};
            var output=JSON.stringify(report,null,2);
            if(window.MuIdleAndroid) window.MuIdleAndroid.copyReport(output); else prompt('Sao chép báo cáo:',output);
          };
        })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun loadHome() {
        errorView.visibility = View.GONE
        webView.visibility = View.VISIBLE
        webView.loadUrl(homeUrl)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }
}
