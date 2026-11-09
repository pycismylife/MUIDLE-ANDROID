package net.hoainiem2003.muidle

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
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
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): android.webkit.WebResourceResponse? {
                val uri = request?.url ?: return null
                if (!uri.path.orEmpty().endsWith("/g.js")) return null
                return try {
                    val connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 10000
                        readTimeout = 15000
                        instanceFollowRedirects = true
                        setRequestProperty("Accept-Encoding", "identity")
                        request.requestHeaders["User-Agent"]?.let { setRequestProperty("User-Agent", it) }
                        request.requestHeaders["Referer"]?.let { setRequestProperty("Referer", it) }
                        CookieManager.getInstance().getCookie(uri.toString())?.let { setRequestProperty("Cookie", it) }
                    }
                    val status = connection.responseCode
                    if (status !in 200..299) {
                        connection.disconnect()
                        return null
                    }
                    val original = connection.inputStream.use { it.readBytes() }
                    val source = String(original, Charsets.UTF_8)
                    val target = "const n=\"walk\"===Xe.motion?2.7:4.4,e="
                    val replacement = "const n=(\"walk\"===Xe.motion?2.7:4.4)*(Xe.demo?Math.max(0.25,Math.min(5,Number(window.__muIdleOfflineSpeed)||1)):1),e="
                    if (!source.contains(target)) {
                        connection.disconnect()
                        return null
                    }
                    val patched = source.replace(target, replacement).toByteArray(Charsets.UTF_8)
                    val headers = mutableMapOf(
                        "Access-Control-Allow-Origin" to "*",
                        "Cache-Control" to "no-store"
                    )
                    connection.headerFields.forEach { (key, values) ->
                        if (key != null && values != null && key.lowercase() !in setOf("content-length", "content-encoding", "transfer-encoding", "content-type")) {
                            headers[key] = values.joinToString(",")
                        }
                    }
                    connection.disconnect()
                    android.webkit.WebResourceResponse(
                        "application/javascript",
                        "UTF-8",
                        200,
                        "OK",
                        headers,
                        ByteArrayInputStream(patched)
                    )
                } catch (_: Exception) {
                    null
                }
            }
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
          box.innerHTML = '<button class="gear">⚙</button><div class="panel"><b>MU IDLE – Công cụ bổ sung</b><div class="line"><span>Ẩn quái</span><button class="switch" id="mu-hide-monsters">Tắt</button></div><div class="line"><span>Ẩn minimap</span><button class="switch" id="mu-minimap">Tắt</button></div><div class="line" id="mu-offline-speed-row" style="display:none"><span>Tốc độ xem thử offline</span><select id="mu-offline-speed" style="min-width:125px;background:#222;color:#fff;border:1px solid #777;padding:8px;border-radius:5px"><option value="1">1x · Gốc</option><option value="1.5">1.5x</option><option value="2">2x</option><option value="3">3x</option><option value="5">5x</option></select></div><div class="line"><span>Giới hạn FPS bổ sung</span><select id="mu-fps" style="min-width:125px;background:#222;color:#fff;border:1px solid #777;padding:8px;border-radius:5px"><option value="0">Theo game (30/60)</option><option value="5">5 FPS</option><option value="10">10 FPS</option><option value="20">20 FPS</option></select></div><div class="note" id="mu-status">Các mục đồ họa 3D, hiệu ứng kỹ năng, chất lượng hình và ẩn người chơi đã có sẵn trong Cài đặt game.</div><button class="scan">Quét cấu trúc game và sao chép báo cáo</button></div>';
          document.documentElement.appendChild(box);
          var panel = box.querySelector('.panel');
          box.querySelector('.gear').onclick = function() { panel.style.display = panel.style.display === 'block' ? 'none' : 'block'; };
          var status = box.querySelector('#mu-status');
          var offlineSpeedRow=box.querySelector('#mu-offline-speed-row');
          var offlineSpeedSelect=box.querySelector('#mu-offline-speed');
          try {
            var savedOfflineSpeed=localStorage.getItem('mu_idle_offline_speed')||'1';
            offlineSpeedSelect.value=['1','1.5','2','3','5'].includes(savedOfflineSpeed)?savedOfflineSpeed:'1';
            window.__muIdleOfflineSpeed=Number(offlineSpeedSelect.value);
          } catch(e) { window.__muIdleOfflineSpeed=1; }
          function refreshOfflineSpeedControl() {
            var isOfflinePreview=!!(window.PC && window.PC.demo);
            offlineSpeedRow.style.display=isOfflinePreview?'flex':'none';
          }
          refreshOfflineSpeedControl();
          offlineSpeedSelect.onchange=function() {
            var value=Number(offlineSpeedSelect.value)||1;
            window.__muIdleOfflineSpeed=value;
            try { localStorage.setItem('mu_idle_offline_speed',String(value)); } catch(e) {}
            status.textContent='Tốc độ di chuyển bản xem thử offline: '+value+'x. Không ảnh hưởng chế độ online.';
          };
          var monstersHidden=false, monsterButton=box.querySelector('#mu-hide-monsters');
          var originalPaint=null, drawImagePatched=false, originalDrawImage=null;
          function installMonsterFilter() {
            if (window.PC3D && typeof window.PC3D.paint === 'function' && !window.__muOriginalPC3DPaint) {
              window.__muOriginalPC3DPaint = window.PC3D.paint;
            }
            if (!drawImagePatched && window.CanvasRenderingContext2D) {
              var proto=CanvasRenderingContext2D.prototype;
              originalDrawImage=proto.drawImage;
              proto.drawImage=function(source) {
                if (monstersHidden && this.canvas && this.canvas.id==='cv' && source) {
                  var src='';
                  try { src=source.currentSrc || source.src || ''; } catch(e) {}
                  if (/\/spr\/m\//i.test(src)) return;
                }
                return originalDrawImage.apply(this,arguments);
              };
              drawImagePatched=true;
            }
          }
          installMonsterFilter();
          monsterButton.onclick=function() {
            monstersHidden=!monstersHidden;
            installMonsterFilter();
            var pc3d=window.PC3D;
            if (pc3d && typeof pc3d.paint==='function') {
              if (monstersHidden) {
                if (!window.__muOriginalPC3DPaint) window.__muOriginalPC3DPaint=pc3d.paint;
                pc3d.paint=function(actor) {
                  if (monstersHidden && actor && actor.kind==='m') return true;
                  return window.__muOriginalPC3DPaint.apply(this,arguments);
                };
              } else if (window.__muOriginalPC3DPaint) {
                pc3d.paint=window.__muOriginalPC3DPaint;
              }
            }
            monsterButton.textContent=monstersHidden?'Bật':'Tắt';
            monsterButton.classList.toggle('on',monstersHidden);
            status.textContent=monstersHidden?'Đã ẩn phần hình ảnh quái trên màn hình. Quái vẫn tồn tại để game tiếp tục tính chiến đấu.':'Đã hiện lại quái.';
          };
          var fpsSelect=box.querySelector('#mu-fps');
          var fpsCap=0, rafQueue=[], rafCallbacks={}, rafId=0, rafScheduled=false, lastFrameAt=0;
          var nativeRaf=window.requestAnimationFrame.bind(window);
          var nativeCancel=window.cancelAnimationFrame.bind(window);
          function loadFpsCap() {
            try { fpsCap=Math.max(0,Math.min(20,Number(localStorage.getItem('mu_idle_fps_cap')||0))); } catch(e) { fpsCap=0; }
            fpsSelect.value=String(fpsCap);
          }
          function runRafQueue(ts) {
            rafScheduled=false;
            if (fpsCap>0 && lastFrameAt && ts-lastFrameAt < 1000/fpsCap) {
              rafScheduled=true; nativeRaf(runRafQueue); return;
            }
            lastFrameAt=ts;
            var batch=rafQueue.splice(0);
            batch.forEach(function(id) {
              var cb=rafCallbacks[id]; delete rafCallbacks[id];
              if (cb) { try { cb(ts); } catch(e) { setTimeout(function(){throw e;},0); } }
            });
            if (rafQueue.length && !rafScheduled) { rafScheduled=true; nativeRaf(runRafQueue); }
          }
          window.requestAnimationFrame=function(cb) {
            var id=++rafId; rafCallbacks[id]=cb; rafQueue.push(id);
            if (!rafScheduled) { rafScheduled=true; nativeRaf(runRafQueue); }
            return id;
          };
          window.cancelAnimationFrame=function(id) { delete rafCallbacks[id]; };
          loadFpsCap();
          fpsSelect.onchange=function() {
            fpsCap=Number(fpsSelect.value)||0; lastFrameAt=0;
            try { localStorage.setItem('mu_idle_fps_cap',String(fpsCap)); } catch(e) {}
            status.textContent=fpsCap ? 'Đã giới hạn tốc độ cập nhật hình ảnh ở '+fpsCap+' FPS. Hình ảnh sẽ kém mượt hơn.' : 'Đã trả giới hạn FPS về cài đặt gốc 30/60 của game.';
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
