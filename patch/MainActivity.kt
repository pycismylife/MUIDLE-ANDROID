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
            text = "Không thể kết nối mạng.\\nChạm để thử lại"
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
                installControlPanel()
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

    private fun installControlPanel() {
        val js = """
        (function() {
          if (window.__muIdleControlInstalled) return;
          window.__muIdleControlInstalled = true;
          var style = document.createElement('style');
          style.textContent = '#mu-idle-tools{position:fixed;z-index:2147483647;top:12px;left:12px;font:14px Arial,sans-serif;color:#fff;max-width:calc(100vw - 24px)}#mu-idle-tools button{border:1px solid #777;background:rgba(18,18,18,.96);color:#fff;border-radius:6px;padding:9px 12px;font:inherit}#mu-idle-tools .gear{width:42px;height:42px;font-size:22px;padding:0}#mu-idle-tools .panel{display:none;margin-top:6px;padding:10px;background:rgba(10,10,10,.97);border:1px solid #777;border-radius:8px;max-height:72vh;overflow:auto}#mu-idle-tools .line{display:flex;align-items:center;justify-content:space-between;gap:12px;margin:9px 0}#mu-idle-tools .switch{min-width:76px;background:#444}#mu-idle-tools .switch.on{background:#286b37}#mu-idle-tools .note{font-size:12px;color:#ffdf8a;line-height:1.45;margin-top:9px}#mu-idle-tools .scan{width:100%;margin-top:10px;background:#344b32}';
          document.head.appendChild(style);
          var box = document.createElement('div');
          box.id = 'mu-idle-tools';
          box.innerHTML = '<button class="gear">⚙</button><div class="panel"><b>MU IDLE – Giảm tải</b><div class="line"><span>Đồ họa 3D nhân vật / quái</span><button class="switch" id="mu-native">Bật</button></div><div class="line"><span>Hiệu ứng kỹ năng</span><button class="switch" id="mu-effects">Bật</button></div><div class="line"><span>Mật độ hiệu ứng thấp</span><button class="switch" id="mu-quality">Tắt</button></div><div class="line"><span>Ẩn minimap</span><button class="switch" id="mu-minimap">Tắt</button></div><div class="note" id="mu-status">Điều khiển qua trạng thái đồ họa gốc của MU IDLE.</div><button class="scan">Quét cấu trúc game và sao chép báo cáo</button></div>';
          document.documentElement.appendChild(box);
          var panel = box.querySelector('.panel');
          box.querySelector('.gear').onclick = function() { panel.style.display = panel.style.display === 'block' ? 'none' : 'block'; };
          var status = box.querySelector('#mu-status');
          function gameState() { try { return window.PCReview && window.PCReview.state ? window.PCReview.state : null; } catch(e) { return null; } }
          function saveVisual(state,key) { try { var saved=JSON.parse(localStorage.getItem('muidle_visual')||'{}'); saved[key]=state[key]; localStorage.setItem('muidle_visual',JSON.stringify(saved)); } catch(e) {} }
          function setupBoolean(id,key) {
            var b=box.querySelector('#'+id), s=gameState();
            if(s) { b.textContent=s[key]?'Bật':'Tắt'; b.classList.toggle('on',!!s[key]); }
            b.onclick=function() {
              var st=gameState();
              if(!st || typeof st[key]==='undefined') { status.textContent='Game chưa sẵn sàng hoặc không có tuỳ chọn '+key+'.'; return; }
              st[key]=!st[key]; saveVisual(st,key);
              b.textContent=st[key]?'Bật':'Tắt'; b.classList.toggle('on',!!st[key]);
              status.textContent='Đã cập nhật '+key+' trong cài đặt đồ họa game.';
            };
          }
          setupBoolean('mu-native','native');
          setupBoolean('mu-effects','effects');
          var qb=box.querySelector('#mu-quality');
          var st0=gameState();
          if(st0) { var q0=st0.quality==='low'; qb.textContent=q0?'Bật':'Tắt'; qb.classList.toggle('on',q0); }
          qb.onclick=function() {
            var st=gameState(); if(!st) { status.textContent='Game chưa sẵn sàng.'; return; }
            st.quality=st.quality==='low'?'high':'low'; saveVisual(st,'quality');
            var on=st.quality==='low'; qb.textContent=on?'Bật':'Tắt'; qb.classList.toggle('on',on);
            status.textContent=on?'Đã chọn mật độ hiệu ứng thấp.':'Đã khôi phục mật độ hiệu ứng cao.';
          };
          var miniHidden=false, mb=box.querySelector('#mu-minimap');
          mb.onclick=function() {
            miniHidden=!miniHidden;
            ['mmap','mmxy'].forEach(function(id){var el=document.getElementById(id);if(el)el.style.display=miniHidden?'none':'';});
            mb.textContent=miniHidden?'Bật':'Tắt'; mb.classList.toggle('on',miniHidden);
          };
          box.querySelector('.scan').onclick = function() {
            var canvases = Array.from(document.querySelectorAll('canvas')).map(function(c,i) { var r=c.getBoundingClientRect(); return {index:i,id:c.id||'',className:String(c.className||''),pixelWidth:c.width,pixelHeight:c.height,cssWidth:Math.round(r.width),cssHeight:Math.round(r.height)}; });
            var report = {url:location.href,title:document.title,userAgent:navigator.userAgent,viewport:{width:innerWidth,height:innerHeight,dpr:devicePixelRatio},canvasCount:canvases.length,canvases:canvases,imageCount:document.images.length,videoCount:document.querySelectorAll('video').length,iframes:Array.from(document.querySelectorAll('iframe')).map(function(x){return {src:x.src,id:x.id,className:x.className};}),scripts:Array.from(document.scripts).map(function(x){return x.src;}).filter(Boolean),visibleElements:Array.from(document.querySelectorAll('img,canvas,video,iframe,[id],[class]')).slice(0,250).map(function(e){var r=e.getBoundingClientRect();return {tag:e.tagName,id:e.id||'',className:typeof e.className==='string'?e.className:'',width:Math.round(r.width),height:Math.round(r.height)};}).filter(function(e){return e.width>0&&e.height>0;}).slice(0,150),memory:performance.memory?{usedJSHeapSize:performance.memory.usedJSHeapSize,totalJSHeapSize:performance.memory.totalJSHeapSize}:'not exposed'};
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
