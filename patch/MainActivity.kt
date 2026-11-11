package net.hoainiem2003.muidle

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast
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
        webView.addJavascriptInterface(DiagnosticBridge(), "AndroidDiag")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                errorView.visibility = View.GONE
                webView.visibility = View.VISIBLE
                CookieManager.getInstance().flush()
                installAutoMoveMap()
                installDiagnostic()
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
          css.textContent = '#mu-map-tools{position:fixed;z-index:2147483647;top:12px;left:12px;font:13px Arial,sans-serif;color:#f1dfb2;max-width:calc(100vw - 24px)}#mu-map-tools button,#mu-map-tools select,#mu-map-tools input{font:inherit}#mu-map-tools .gear{width:42px;height:42px;border:1px solid #80683f;border-radius:0;background:#21190f;color:#f3dfb5;font-size:21px;box-shadow:0 3px 12px #0008}#mu-map-tools .panel{display:none;margin-top:6px;padding:10px;background:#17130f;border:1px solid #80683f;border-radius:0;width:min(470px,calc(100vw - 30px));max-height:72vh;overflow:auto;box-sizing:border-box;box-shadow:0 8px 28px #0009}#mu-map-tools .heading{font-weight:700;margin-bottom:12px}#mu-map-tools .rule{display:grid;grid-template-columns:86px 12px 70px 28px minmax(110px,1fr);align-items:center;gap:5px;margin:8px 0;white-space:nowrap}#mu-map-tools .rule input,#mu-map-tools .reset-value{width:100%;min-width:0;box-sizing:border-box;padding:8px 4px;background:#302515;color:#f3dfb5;border:1px solid #80683f;border-radius:0;text-align:center}#mu-map-tools .reset-value{display:block;border-color:#80683f}#mu-map-tools select{width:100%;min-width:0;box-sizing:border-box;padding:8px 5px;background:#302515;color:#f3dfb5;border:1px solid #80683f;border-radius:0}#mu-map-tools button.action{width:100%;padding:9px;margin-top:8px;border:1px solid #80683f;border-radius:0;background:#382c1b;color:#f3dfb5}#mu-map-tools button.on{background:#205b32;border-color:#4c9c61}';
          document.head.appendChild(css);

          var root = document.createElement('div');
          root.id = 'mu-map-tools';
          root.innerHTML = '<button class="gear" type="button" aria-label="Auto Move Map">⚙</button><div class="panel"><div class="heading">MU IDLE · AUTO MOVE MAP</div><div class="rule"><span>Lv 1</span><span>-</span><input id="mu-map-limit" type="number" min="1" max="400" value="300"><span>tại</span><select id="mu-map-y"><option value="">Chọn Map Y</option></select></div><div class="rule"><span id="mu-map-level-label">Lv 300</span><span>-</span><span class="reset-value" id="mu-map-reset-value">Reset</span><span>tại</span><select id="mu-map-z"><option value="">Chọn Map Z</option></select></div><button class="action" id="mu-map-refresh" type="button">Tải danh sách bản đồ</button><button class="action" id="mu-map-toggle" type="button">AUTO MOVE MAP · ĐÃ TẮT</button></div>';
          document.documentElement.appendChild(root);

          var panel = root.querySelector('.panel');
          var limitInput = root.querySelector('#mu-map-limit');
          var mapY = root.querySelector('#mu-map-y');
          var mapZ = root.querySelector('#mu-map-z');
          var levelLabel = root.querySelector('#mu-map-level-label');
          var resetValue = root.querySelector('#mu-map-reset-value');
          var toggle = root.querySelector('#mu-map-toggle');
          var cfg = { limit: 300, y: '', z: '', on: false };
          var options = [];
          var busy = false;
          var lastReset = null;
          var lastStage = '';
          var tickTimer = null;

          function say(message) { /* trạng thái chuyển map không hiển thị trên panel */ }
          function resetRequiredLevel(resetCount) { return Math.min(400, 300 + 20 * Math.floor((Math.max(0, Number(resetCount) || 0) + 1) / 5)); }
          function paintLevelLabels() {
            var threshold = Math.max(1, Math.min(400, Math.floor(Number(limitInput.value) || 300)));
            levelLabel.textContent = 'Lv ' + threshold;
            var resetCount = getReset();
            resetValue.textContent = resetCount === null ? 'Reset' : String(resetRequiredLevel(resetCount));
          }
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
          var pendingTarget = '';
          var lastAttemptAt = 0;
          var failedAttempts = 0;
          function closeMapOnlyIfWeOpenedIt(win, button, openedByUs) {
            if (openedByUs && button && isVisible(win)) button.click();
          }
          function switchMap(id, label) {
            if (!id || busy || pendingTarget === String(id)) return;
            var nowTime = Date.now();
            if (nowTime - lastAttemptAt < 12000) return;
            if (failedAttempts >= 2) {
              say('Chưa xác nhận được chuyển map sau 2 lần. Tạm dừng để không làm phiền thao tác của bạn; hãy kiểm tra map rồi bật lại.');
              return;
            }
            busy = true;
            pendingTarget = String(id);
            lastAttemptAt = nowTime;
            var win = document.getElementById('wMap');
            var button = document.getElementById('bMap');
            var openedByUs = !isVisible(win);
            if (openedByUs) {
              if (!button) {
                busy = false; pendingTarget = '';
                say('Không tìm thấy nút mở bản đồ.');
                return;
              }
              button.click();
            }
            say('Đang chuyển một lần sang ' + label + '…');
            setTimeout(function () {
              var row = mapRows().find(function (item) { return item.getAttribute('data-z') === String(id); });
              if (!row) {
                closeMapOnlyIfWeOpenedIt(win, button, openedByUs);
                busy = false; pendingTarget = '';
                failedAttempts++;
                say('Không tìm thấy map đã chọn. Đã dừng, không tự mở danh sách liên tục.');
                return;
              }
              if (row.classList.contains('lock')) {
                closeMapOnlyIfWeOpenedIt(win, button, openedByUs);
                busy = false; pendingTarget = '';
                failedAttempts = 2;
                say(label + ' chưa mở khóa. Auto Move tạm dừng.');
                return;
              }
              if (row.classList.contains('cur')) {
                closeMapOnlyIfWeOpenedIt(win, button, openedByUs);
                busy = false; pendingTarget = '';
                failedAttempts = 0;
                say('Đã ở đúng ' + label + '. Không cần chuyển map.');
                return;
              }
              row.click();
              setTimeout(function () {
                var current = mapRows().find(function (item) { return item.classList.contains('cur'); });
                var reached = current && current.getAttribute('data-z') === String(id);
                closeMapOnlyIfWeOpenedIt(win, button, openedByUs);
                busy = false;
                if (reached) {
                  pendingTarget = '';
                  failedAttempts = 0;
                  say('Đã đến ' + label + '. Auto Move sẽ không gửi lệnh nữa khi vẫn ở đúng map.');
                } else {
                  failedAttempts++;
                  pendingTarget = '';
                  say('Đã gửi 1 lệnh chuyển ' + label + '. Đang chờ game cập nhật; sẽ không click lặp liên tục.');
                }
              }, 1400);
            }, 350);
          }
          function tick() {
            if (!cfg.on || busy) return;
            var level = getLevel();
            var reset = getReset();
            if (level === null || reset === null) return;
            paintLevelLabels();
            if (lastReset === null) lastReset = reset;
            if (reset > lastReset) {
              lastReset = reset;
              lastStage = 'y';
              failedAttempts = 0;
              pendingTarget = '';
              if (!cfg.y) { say('Đã Reset nhưng chưa chọn Map Y.'); return; }
            } else if (reset < lastReset) {
              lastReset = reset;
            }
            var targetStage = level < cfg.limit ? 'y' : 'z';
            var targetId = targetStage === 'y' ? cfg.y : cfg.z;
            var label = targetStage === 'y' ? 'Map Y' : 'Map Z';
            if (!targetId) { say('Chưa chọn ' + label + '.'); return; }
            var currentId = currentMapId();
            if (currentId && String(currentId) === String(targetId)) {
              pendingTarget = '';
              failedAttempts = 0;
              lastStage = targetStage;
              return;
            }
            if (pendingTarget === String(targetId) || Date.now() - lastAttemptAt < 12000) return;
            if (failedAttempts >= 2) return;
            lastStage = targetStage;
            switchMap(targetId, label);
          }

          safeLoad();
          limitInput.value = String(cfg.limit);
          paintLevelLabels();
          paintToggle();
          root.querySelector('.gear').onclick = function () {
            panel.style.display = panel.style.display === 'block' ? 'none' : 'block';
            if (panel.style.display === 'block' && !options.length) readMapOptions();
          };
          root.querySelector('#mu-map-refresh').onclick = readMapOptions;
          limitInput.onchange = function () { save(); paintLevelLabels(); lastStage = ''; };
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

    private inner class DiagnosticBridge {
        @JavascriptInterface
        fun copyReport(value: String) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("MU IDLE Diagnostic", value))
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Đã sao chép dữ liệu Diagnostic. Hãy dán vào ChatGPT.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun installDiagnostic() {
        val js = """
        (function () {
          if (window.__muInventoryDiagnosticInstalled) return;
          window.__muInventoryDiagnosticInstalled = true;
          var css = document.createElement('style');
          css.textContent = '#mu-diag{position:fixed;z-index:2147483646;right:10px;top:12px;font:12px Arial,sans-serif;color:#f2dfb7}#mu-diag button,#mu-diag textarea{font:inherit;box-sizing:border-box;border-radius:0}#mu-diag .gear{width:40px;height:40px;background:#21190f;color:#f3dfb5;border:1px solid #80683f;font-size:18px}#mu-diag .panel{display:none;width:min(340px,calc(100vw - 24px));max-height:72vh;overflow:auto;background:#17130f;border:1px solid #80683f;padding:9px;box-shadow:0 5px 20px #000b}#mu-diag .head{font-weight:bold;margin-bottom:8px}#mu-diag button.action{display:block;width:100%;padding:8px;margin:5px 0;background:#382c1b;color:#f3dfb5;border:1px solid #80683f;text-align:left}#mu-diag .status{font-size:11px;color:#e8c979;line-height:1.4;margin:7px 0}#mu-diag textarea{width:100%;height:90px;background:#0d0b08;color:#eee;border:1px solid #80683f;padding:5px}';
          document.head.appendChild(css);
          var root=document.createElement('div'); root.id='mu-diag';
          root.innerHTML='<button class="gear" type="button">D</button><div class="panel"><div class="head">MU IDLE · INVENTORY DIAGNOSTIC</div><button class="action" id="md-scan">1. Quét túi đồ</button><button class="action" id="md-arm">2. Bắt lần bấm vào một món</button><button class="action" id="md-popup">3. Quét popup đang mở</button><button class="action" id="md-copy">Sao chép báo cáo</button><div class="status" id="md-status">Chỉ đọc dữ liệu giao diện. Không bán, không sửa hoặc di chuyển vật phẩm.</div><textarea id="md-output" readonly placeholder="Báo cáo Diagnostic sẽ hiện ở đây"></textarea></div>';
          document.documentElement.appendChild(root);
          var panel=root.querySelector('.panel'), output=root.querySelector('#md-output'), status=root.querySelector('#md-status');
          var report={createdAt:new Date().toISOString(),page:location.href,steps:[],clickCapture:null};
          function visible(el){if(!el||!el.getBoundingClientRect)return false;var r=el.getBoundingClientRect(),s=getComputedStyle(el);return r.width>0&&r.height>0&&s.display!=='none'&&s.visibility!=='hidden'&&Number(s.opacity)!==0;}
          function attrs(el){var o={};if(!el||!el.attributes)return o;Array.from(el.attributes).forEach(function(a){if(a.name==='style')return;if(a.name==='class'||a.name==='id'||a.name.indexOf('data-')===0||a.name==='role'||a.name==='onclick'||a.name==='title')o[a.name]=a.value.slice(0,300);});return o;}
          function shortHtml(el){return (el&&el.outerHTML?el.outerHTML:'').slice(0,1800);}
          function describe(el){if(!el)return null;var r=el.getBoundingClientRect();return {tag:el.tagName,id:el.id||'',className:typeof el.className==='string'?el.className.slice(0,300):'',text:(el.innerText||el.textContent||'').trim().replace(/\s+/g,' ').slice(0,350),attrs:attrs(el),rect:{x:Math.round(r.x),y:Math.round(r.y),w:Math.round(r.width),h:Math.round(r.height)},html:shortHtml(el)};}
          function candidates(){
            var all=Array.from(document.querySelectorAll('body *')).filter(visible);
            var re=/(item|slot|bag|inventory|equip|goods|prop|grid|cell|popup|modal|window|panel|tip|detail|wbag|witem|^it$)/i;
            var found=all.filter(function(el){var s=(el.id||'')+' '+(typeof el.className==='string'?el.className:'')+' '+Array.from(el.attributes||[]).filter(function(a){return a.name.indexOf('data-')===0;}).map(function(a){return a.name+'='+a.value;}).join(' ');return re.test(s);});
            return {matched:found.slice(0,180).map(describe),visibleTextNodes:Array.from(document.querySelectorAll('button,a,[role=button]')).filter(visible).map(describe).slice(0,100)};
          }
          function snapshot(label){var snap={label:label,time:new Date().toISOString(),url:location.href,title:document.title,bodyText:(document.body.innerText||'').slice(0,5000),elements:candidates(),visibleDialogs:Array.from(document.querySelectorAll('[role=dialog],.modal,.popup,[id*="wBag"],[id*="bag"],[id*="Bag"],[id*="item"],[id*="Item"]')).filter(visible).slice(0,35).map(describe)};snap.clickCapture=report.clickCapture;report.steps.push(snap);render();status.textContent='Đã quét '+label+'. Không có thao tác thay đổi vật phẩm.';}
          function render(){var text=JSON.stringify(report,null,2);output.value=text;}
          root.querySelector('.gear').onclick=function(){panel.style.display=panel.style.display==='block'?'none':'block';};
          root.querySelector('#md-scan').onclick=function(){snapshot('inventory_scan');};
          root.querySelector('#md-popup').onclick=function(){snapshot('popup_scan');};
          root.querySelector('#md-arm').onclick=function(){
            status.textContent='Đang chờ bạn chạm đúng một món đồ trong túi. Không bấm nút Bán.';
            var handler=function(ev){
              if(root.contains(ev.target))return;
              var path=ev.composedPath?ev.composedPath():[];
              var chain=[],el=ev.target;
              for(var i=0;el&&i<7;i++,el=el.parentElement)chain.push(describe(el));
              report.clickCapture={time:new Date().toISOString(),target:describe(ev.target),chain:chain,composedPath:path.slice(0,12).filter(function(x){return x&&x.tagName;}).map(describe)};
              document.removeEventListener('click',handler,true);
              render();status.textContent='Đã ghi nhận lần bấm. Bây giờ mở popup chi tiết rồi chọn Quét popup đang mở.';
            };
            document.addEventListener('click',handler,true);
          };
          root.querySelector('#md-copy').onclick=function(){render();if(window.AndroidDiag&&AndroidDiag.copyReport)AndroidDiag.copyReport(output.value);else{output.focus();output.select();status.textContent='Không có cầu nối clipboard. Hãy sao chép nội dung trong ô.';}};
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
