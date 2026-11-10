package net.hoainiem2003.muidle

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var errorView: TextView
    private val homeUrl = "https://hoainiem2003.net/muidle"

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
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                errorView.visibility = View.GONE
                webView.visibility = View.VISIBLE
                CookieManager.getInstance().flush()
                installAutoMoveMap()
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

    private fun installAutoMoveMap() {
        val js = """
        (function () {
          if (window.__muAutoMoveMapInstalled) return;
          window.__muAutoMoveMapInstalled = true;

          var css = document.createElement('style');
          css.textContent = '#mu-map-tools{position:fixed;z-index:2147483647;top:12px;left:12px;font:14px Arial,sans-serif;color:#fff;max-width:calc(100vw - 24px)}#mu-map-tools button,#mu-map-tools select,#mu-map-tools input{font:inherit}#mu-map-tools .gear{width:42px;height:42px;border:1px solid #777;border-radius:7px;background:rgba(15,15,15,.96);color:#fff;font-size:22px}#mu-map-tools .panel{display:none;margin-top:6px;padding:11px;background:rgba(10,10,10,.97);border:1px solid #777;border-radius:8px;width:min(340px,calc(100vw - 48px));max-height:72vh;overflow:auto;box-sizing:border-box}#mu-map-tools .field{display:flex;align-items:center;gap:7px;margin:9px 0}#mu-map-tools select,#mu-map-tools input{min-width:0;flex:1;padding:9px 6px;background:#222;color:#fff;border:1px solid #777;border-radius:5px}#mu-map-tools button.action{width:100%;padding:10px;margin-top:7px;border:1px solid #777;border-radius:6px;background:#444;color:#fff}#mu-map-tools button.on{background:#286b37}#mu-map-tools .status{font-size:12px;line-height:1.5;color:#ffdf8a;margin-top:9px;white-space:normal}';
          document.head.appendChild(css);

          var root = document.createElement('div');
          root.id = 'mu-map-tools';
          root.innerHTML = '<button class="gear" type="button" aria-label="Auto Move Map">⚙</button><div class="panel"><b>MU IDLE · AUTO MOVE MAP</b><div class="field"><span style="white-space:nowrap">Lv 1 đến</span><input id="mu-map-limit" type="number" min="1" max="400" value="300"><span>Map Y</span></div><div class="field"><span style="white-space:nowrap">Lv trên ngưỡng</span><span style="white-space:nowrap">Map Z</span></div><div class="field"><select id="mu-map-y"><option value="">Chọn Map Y</option></select></div><div class="field"><select id="mu-map-z"><option value="">Chọn Map Z</option></select></div><button class="action" id="mu-map-refresh" type="button">Tải danh sách bản đồ</button><button class="action" id="mu-map-toggle" type="button">AUTO MOVE MAP · TẮT</button><div class="status" id="mu-map-status">Chọn Map Y, Map Z và ngưỡng cấp. Game tự đánh; công cụ chỉ chuyển bản đồ.</div></div>';
          document.documentElement.appendChild(root);

          var panel = root.querySelector('.panel');
          var limitInput = root.querySelector('#mu-map-limit');
          var mapY = root.querySelector('#mu-map-y');
          var mapZ = root.querySelector('#mu-map-z');
          var status = root.querySelector('#mu-map-status');
          var toggle = root.querySelector('#mu-map-toggle');
          var cfg = { limit: 300, y: '', z: '', on: false };
          var options = [];
          var busy = false;
          var lastReset = null;
          var lastStage = '';
          var tickTimer = null;

          function say(message) { status.textContent = message; }
          function safeLoad() {
            try {
              var saved = JSON.parse(localStorage.getItem('mu_auto_move_map_v1') || '{}');
              if (saved && typeof saved === 'object') cfg = Object.assign(cfg, saved);
            } catch (e) {}
            cfg.limit = Math.max(1, Math.min(400, Math.floor(Number(cfg.limit) || 300)));
            cfg.on = cfg.on === true;
          }
          function save() {
            cfg.limit = Math.max(1, Math.min(400, Math.floor(Number(limitInput.value) || 300)));
            cfg.y = mapY.value || cfg.y || '';
            cfg.z = mapZ.value || cfg.z || '';
            try { localStorage.setItem('mu_auto_move_map_v1', JSON.stringify(cfg)); }
            catch (e) { say('Không lưu được cấu hình. Hãy kiểm tra bộ nhớ trình duyệt.'); }
          }
          function paintToggle() {
            toggle.textContent = cfg.on ? 'AUTO MOVE MAP · ĐANG BẬT' : 'AUTO MOVE MAP · ĐÃ TẮT';
            toggle.classList.toggle('on', cfg.on);
          }
          function isVisible(el) {
            if (!el) return false;
            var r = el.getBoundingClientRect();
            var s = window.getComputedStyle(el);
            return r.width > 0 && r.height > 0 && s.display !== 'none' && s.visibility !== 'hidden';
          }
          function mapRows() {
            return Array.from(document.querySelectorAll('#wMap .zr[data-z]'));
          }
          function renderOptions() {
            var html = '<option value="">-- Chọn bản đồ --</option>' + options.map(function (m) {
              return '<option value="' + String(m.id).replace(/&/g,'&amp;').replace(/"/g,'&quot;') + '">' +
                String(m.name).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;') +
                (m.locked ? ' · Chưa mở khóa' : '') + '</option>';
            }).join('');
            mapY.innerHTML = html;
            mapZ.innerHTML = html;
            mapY.value = options.some(function (m) { return m.id === cfg.y; }) ? cfg.y : '';
            mapZ.value = options.some(function (m) { return m.id === cfg.z; }) ? cfg.z : '';
            if (cfg.y && !mapY.value) say('Đã tải danh sách map, nhưng Map Y đã lưu không còn trong danh sách. Vui lòng chọn lại.');
            else if (cfg.z && !mapZ.value) say('Đã tải danh sách map, nhưng Map Z đã lưu không còn trong danh sách. Vui lòng chọn lại.');
            else say('Đã tải ' + options.length + ' bản đồ. Chọn Map Y và Map Z để tiếp tục.');
          }
          function readMapOptions() {
            var win = document.getElementById('wMap');
            var button = document.getElementById('bMap');
            var openedByUs = !isVisible(win);
            if (openedByUs) {
              if (!button) { say('Không tìm thấy nút mở danh sách bản đồ của game.'); return; }
              button.click();
            }
            say('Đang đọc danh sách bản đồ từ game…');
            setTimeout(function () {
              var rows = mapRows();
              options = rows.map(function (row) {
                return {
                  id: row.getAttribute('data-z'),
                  name: (row.querySelector('b') || row).textContent.trim(),
                  locked: row.classList.contains('lock')
                };
              }).filter(function (m) { return m.id !== null && m.id !== ''; });
              if (openedByUs && button) button.click();
              if (!options.length) {
                say('Chưa đọc được danh sách map. Hãy vào nhân vật trong game rồi tải lại danh sách.');
                return;
              }
              renderOptions();
            }, 650);
          }
          function getLevel() {
            var stateLevel = Number(window.S && window.S.lv);
            if (Number.isFinite(stateLevel) && stateLevel > 0) return stateLevel;
            var text = (document.getElementById('lvl') || {}).textContent || '';
            var match = text.match(/(?:Lv|Level)\s*(\d+)/i);
            return match ? Number(match[1]) : null;
          }
          function getReset() {
            var value = Number(window.S && window.S.reset);
            return Number.isFinite(value) && value >= 0 ? value : null;
          }
          function currentMapId() {
            var row = document.querySelector('#wMap .zr.cur[data-z]');
            return row ? row.getAttribute('data-z') : null;
          }
          function switchMap(id, label) {
            if (!id || busy) return;
            busy = true;
            var win = document.getElementById('wMap');
            var button = document.getElementById('bMap');
            var openedByUs = !isVisible(win);
            if (openedByUs) {
              if (!button) { busy = false; say('Không tìm thấy nút mở bản đồ.'); return; }
              button.click();
            }
            say('Đang chuyển sang ' + label + '…');
            setTimeout(function () {
              var row = mapRows().find(function (item) { return item.getAttribute('data-z') === String(id); });
              if (!row) {
                if (openedByUs && button) button.click();
                busy = false;
                say('Không tìm thấy bản đồ đã chọn trong danh sách. Hãy tải lại danh sách map.');
                return;
              }
              if (row.classList.contains('lock')) {
                if (openedByUs && button) button.click();
                busy = false;
                say(label + ' chưa mở khóa ở cấp hiện tại. Không gửi lệnh chuyển map.');
                return;
              }
              if (row.classList.contains('cur')) {
                if (openedByUs && button) button.click();
                busy = false;
                say('Đang ở đúng ' + label + '.');
                return;
              }
              row.click();
              setTimeout(function () {
                var now = mapRows().find(function (item) { return item.getAttribute('data-z') === String(id); });
                if (openedByUs && button) button.click();
                busy = false;
                if (now && now.classList.contains('cur')) say('Đã chuyển thành công sang ' + label + '.');
                else say('Đã gửi yêu cầu chuyển ' + label + ', nhưng game chưa xác nhận map hiện tại. Sẽ kiểm tra lại.');
              }, 900);
            }, 450);
          }
          function tick() {
            if (!cfg.on || busy) return;
            var level = getLevel();
            var reset = getReset();
            if (level === null || reset === null) {
              say('Đang chờ dữ liệu cấp độ và Reset từ nhân vật trong game…');
              return;
            }
            if (lastReset === null) lastReset = reset;
            var targetStage;
            if (reset > lastReset) {
              lastReset = reset;
              targetStage = 'y';
              lastStage = 'y';
              if (!cfg.y) { say('Đã phát hiện Reset mới nhưng chưa chọn Map Y.'); return; }
              switchMap(cfg.y, 'Map Y sau Reset');
              return;
            }
            if (reset < lastReset) lastReset = reset;
            targetStage = level <= cfg.limit ? 'y' : 'z';
            var targetId = targetStage === 'y' ? cfg.y : cfg.z;
            var label = targetStage === 'y' ? 'Map Y' : 'Map Z';
            if (!targetId) { say('Chưa chọn ' + label + '.'); return; }
            if (lastStage !== targetStage || currentMapId() !== targetId) {
              lastStage = targetStage;
              switchMap(targetId, label);
            }
          }

          safeLoad();
          limitInput.value = String(cfg.limit);
          paintToggle();
          root.querySelector('.gear').onclick = function () {
            panel.style.display = panel.style.display === 'block' ? 'none' : 'block';
            if (panel.style.display === 'block' && !options.length) readMapOptions();
          };
          root.querySelector('#mu-map-refresh').onclick = readMapOptions;
          limitInput.onchange = function () { save(); lastStage = ''; say('Đã lưu ngưỡng cấp ' + cfg.limit + '.'); };
          mapY.onchange = function () { cfg.y = mapY.value; save(); lastStage = ''; say('Đã lưu Map Y.'); };
          mapZ.onchange = function () { cfg.z = mapZ.value; save(); lastStage = ''; say('Đã lưu Map Z.'); };
          toggle.onclick = function () {
            if (!cfg.on && (!mapY.value || !mapZ.value)) {
              say('Hãy tải danh sách và chọn đủ Map Y, Map Z trước khi bật.');
              return;
            }
            cfg.on = !cfg.on;
            if (cfg.on) {
              cfg.y = mapY.value;
              cfg.z = mapZ.value;
              cfg.limit = Math.max(1, Math.min(400, Math.floor(Number(limitInput.value) || 300)));
              lastReset = getReset();
              lastStage = '';
              save();
              paintToggle();
              say('Auto Move Map đã bật. Đang kiểm tra cấp độ để chọn map phù hợp…');
              tick();
            } else {
              save();
              paintToggle();
              say('Auto Move Map đã tắt. Game giữ nguyên bản đồ hiện tại.');
            }
          };
          tickTimer = window.setInterval(tick, 1000);
          window.addEventListener('beforeunload', function () { if (tickTimer) clearInterval(tickTimer); });
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
