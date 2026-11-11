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
                installInventoryInspector()
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
          var report={createdAt:new Date().toISOString(),page:location.href,steps:[],clickCapture:null,postClickSnapshots:[],sellCandidates:[]};
          function visible(el){if(!el||!el.getBoundingClientRect)return false;var r=el.getBoundingClientRect(),s=getComputedStyle(el);return r.width>0&&r.height>0&&s.display!=='none'&&s.visibility!=='hidden'&&Number(s.opacity)!==0;}
          function attrs(el){var o={};if(!el||!el.attributes)return o;Array.from(el.attributes).forEach(function(a){if(a.name==='style')return;if(a.name==='class'||a.name==='id'||a.name.indexOf('data-')===0||a.name==='role'||a.name==='onclick'||a.name==='title')o[a.name]=a.value.slice(0,300);});return o;}
          function shortHtml(el){return (el&&el.outerHTML?el.outerHTML:'').slice(0,1800);}
          function describe(el){if(!el)return null;var r=el.getBoundingClientRect();return {tag:el.tagName,id:el.id||'',className:typeof el.className==='string'?el.className.slice(0,300):'',text:(el.innerText||el.textContent||'').trim().replace(/\s+/g,' ').slice(0,350),attrs:attrs(el),rect:{x:Math.round(r.x),y:Math.round(r.y),w:Math.round(r.width),h:Math.round(r.height)},html:shortHtml(el)};}
          function candidates(){
            var all=Array.from(document.querySelectorAll('body *')).filter(visible);
            var re=/(item|slot|bag|inventory|equip|goods|prop|grid|cell|popup|modal|window|panel|tip|detail|wbag|witem|^it$)/i;
            var found=all.filter(function(el){var s=(el.id||'')+' '+(typeof el.className==='string'?el.className:'')+' '+Array.from(el.attributes||[]).filter(function(a){return a.name.indexOf('data-')===0;}).map(function(a){return a.name+'='+a.value;}).join(' ');return re.test(s);});
            var controls=Array.from(document.querySelectorAll('button,a,[role=button],input[type=button],input[type=submit]')).filter(visible).map(describe).slice(0,140);
            var sell=controls.filter(function(x){return /(bán|sell|出售|ขาย)/i.test((x.text||'')+' '+JSON.stringify(x.attrs||{}));});
            report.sellCandidates=sell;
            return {matched:found.slice(0,220).map(describe),visibleTextNodes:controls};
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
              render();status.textContent='Đã ghi nhận click. Đang tự quét popup sau click…';
              [120,450,1000,1800].forEach(function(ms){setTimeout(function(){snapshot('after_item_click_'+ms+'ms');var snap=report.steps[report.steps.length-1];report.postClickSnapshots.push({delayMs:ms,visibleDialogs:snap.visibleDialogs,bodyText:snap.bodyText,elements:snap.elements});render();},ms);});
            };
            document.addEventListener('click',handler,true);
          };
          root.querySelector('#md-copy').onclick=function(){render();if(window.AndroidDiag&&AndroidDiag.copyReport)AndroidDiag.copyReport(output.value);else{output.focus();output.select();status.textContent='Không có cầu nối clipboard. Hãy sao chép nội dung trong ô.';}};
        })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }


    private fun installInventoryInspector() {
        val js = """
        (function () {
          if (window.__muInventoryInspectorInstalled) return;
          window.__muInventoryInspectorInstalled = true;
          var css = document.createElement('style');
          css.textContent = '#mu-inv-inspector{position:fixed;z-index:2147483645;left:10px;bottom:12px;font:12px Arial,sans-serif;color:#f2dfb7}#mu-inv-inspector button,#mu-inv-inspector input{font:inherit;box-sizing:border-box}#mu-inv-inspector .gear{width:42px;height:42px;background:#21190f;color:#f3dfb5;border:1px solid #80683f}#mu-inv-inspector .panel{display:none;width:min(390px,calc(100vw - 20px));max-height:72vh;overflow:auto;background:#17130f;border:1px solid #80683f;padding:10px;box-shadow:0 5px 20px #000b;box-sizing:border-box}#mu-inv-inspector .head{font-weight:bold;margin-bottom:8px}#mu-inv-inspector .hint{color:#d9bd7d;font-size:11px;line-height:1.4;margin:6px 0}#mu-inv-inspector .actions{display:flex;gap:6px;flex-wrap:wrap}#mu-inv-inspector button.action{padding:7px;background:#382c1b;color:#f3dfb5;border:1px solid #80683f}#mu-inv-inspector label{display:inline-flex;align-items:center;gap:4px;margin:4px 8px 4px 0}#mu-inv-inspector .opts{border-top:1px solid #57452d;margin-top:8px;padding-top:7px}#mu-inv-inspector .status{margin:7px 0;color:#e8c979}#mu-inv-inspector .list{font-size:11px;line-height:1.45;max-height:32vh;overflow:auto}#mu-inv-inspector .item{padding:5px 0;border-top:1px solid #493a28}#mu-inv-inspector .sell{color:#ffcf75}#mu-inv-inspector .keep{color:#9bd49e}#mu-inv-inspector textarea{width:100%;height:70px;background:#0d0b08;color:#eee;border:1px solid #80683f}';
          document.head.appendChild(css);
          var root=document.createElement('div');root.id='mu-inv-inspector';
          root.innerHTML='<button class="gear" type="button">INV</button><div class="panel"><div class="head">INVENTORY INSPECTOR · AUTO FILTER DRY RUN</div><div class="hint">Chỉ đọc dữ liệu game. Chế độ chạy thử không gửi lệnh bán hoặc thay đổi item.</div><div class="actions"><button class="action" id="mi-scan">Đọc túi đồ</button><button class="action" id="mi-dry">Chạy thử bộ lọc</button><button class="action" id="mi-copy">Sao chép báo cáo</button></div><div class="opts"><b>Loại item đưa vào kiểm tra bán</b><br><label><input type="checkbox" data-type="w">Vũ khí</label><label><input type="checkbox" data-type="s">Khiên</label><label><input type="checkbox" data-type="h">Mũ</label><label><input type="checkbox" data-type="a">Giáp</label><label><input type="checkbox" data-type="p">Quần</label><label><input type="checkbox" data-type="g">Găng</label><label><input type="checkbox" data-type="b">Giày</label></div><div class="opts"><b>Điều kiện giữ lại</b><br><label><input id="mi-ex" type="checkbox" checked>Excellent</label><label><input id="mi-luck" type="checkbox" checked>Luck</label><label><input id="mi-opt" type="checkbox" checked>Option</label><label>Giữ từ + <select id="mi-plus"><option value="0">Không xét</option><option value="3">+3</option><option value="4">+4</option><option value="5">+5</option><option value="6">+6</option><option value="7">+7</option><option value="9">+9</option></select></label></div><div class="status" id="mi-status">Chưa quét túi đồ.</div><div class="list" id="mi-list"></div><textarea id="mi-report" readonly placeholder="Báo cáo inventory"></textarea></div>';
          document.documentElement.appendChild(root);
          var panel=root.querySelector('.panel'),status=root.querySelector('#mi-status'),list=root.querySelector('#mi-list'),reportBox=root.querySelector('#mi-report'),lastItems=[];
          var typeNames={w:'Vũ khí',s:'Khiên',h:'Mũ',a:'Giáp',p:'Quần',g:'Găng',b:'Giày',x:'Cánh'};
          var itemDb=null;
          function itemTypeByGroup(g){
            g=Number(g);
            if(g>=0&&g<=5)return 'w';
            return ({6:'s',7:'h',8:'a',9:'p',10:'g',11:'b',12:'x'})[g]||null;
          }
          function typeLabel(k){return typeNames[k]||'Chưa rõ'}
          function readState(){
            var state=window.S||null,dbRoot=window.D||null;
            var items=state&&Array.isArray(state.inv)?state.inv:null;
            var catalog=dbRoot&&dbRoot.item?dbRoot.item:null;
            return {state:state,items:items,catalog:catalog,gridCount:document.querySelectorAll('.grid').length,domItems:document.querySelectorAll('.it[data-u]').length};
          }
          function excellentNames(ex){
            var names=['Sát thương xuất sắc +10%','Sát thương +2%','Tốc độ đánh +7','Hồi máu khi hạ quái','Máu tối đa +4%','Giảm sát thương 4%','Zen rơi +40%','Né đòn +10%','Sát thương + cấp / 20','Hồi mana khi hạ quái','Mana tối đa +4%','Phản 5% sát thương'];
            if(ex==null)return [];
            if(!Array.isArray(ex))return null;
            return ex.map(function(code){var n=Number(code);return {code:code,name:names[n]||('Mã EX chưa giải mã: '+code)}});
          }
          function selectedTypes(){var out={};root.querySelectorAll('[data-type]').forEach(function(x){if(x.checked)out[x.dataset.type]=true});return out}
          function equippedIds(state){var ids={};Object.keys(state.eq||{}).forEach(function(k){var it=state.eq[k];if(it&&it.u!=null)ids[String(it.u)]=true});return ids}
          async function scan(){
            var state=readState(),items=state.items,db=state.catalog;
            if(!state.state||!Array.isArray(items)||!db){
              var diag={at:new Date().toISOString(),mode:'READ_ONLY_RUNTIME_DIAGNOSTIC',windowS:!!window.S,windowD:!!window.D,hasInventory:!!(state.state&&state.state.inv),inventoryIsArray:Array.isArray(items),inventoryLength:items?items.length:null,hasItemCatalog:!!db,domItemCount:state.domItems,gridCount:state.gridCount};
              reportBox.value=JSON.stringify(diag,null,2);
              status.textContent='Không truy cập được window.S.inv hoặc window.D.item. Đã ghi chẩn đoán; chưa thao tác item.';
              return;
            }
            var equipped={};
            Object.keys(state.state.eq||{}).forEach(function(slot){var it=state.state.eq[slot];if(it&&it.u!=null)equipped[String(it.u)]=true});
            lastItems=items.map(function(it){
              var key=String(it.g)+'_'+String(it.n),base=db[key]||null,kind=itemTypeByGroup(it.g),ex=excellentNames(it.ex),exCodes=Array.isArray(it.ex)?it.ex.slice():(it.ex==null?[]:null);
              return {u:it.u,g:it.g,n:it.n,name:base?base.name:'Không rõ item',type:kind,typeName:typeLabel(kind),w:Number(it.w)||(base&&base.w)||1,h:Number(it.h)||(base&&base.h)||1,x:Number(it.x)||0,y:Number(it.y)||0,plus:Number(it.l)||0,luck:it.lk===true||Number(it.lk)>0,option:Number(it.o)||0,excellent:ex,excellentCodes:exCodes,hasExcellent:Array.isArray(exCodes)&&exCodes.length>0,equipped:!!equipped[String(it.u)],known:!!base,icon:'ic166b57/'+key+(Array.isArray(exCodes)&&exCodes.length?'e':'')+'.webp',base:base?{k:base.k,dl:base.dl,dmin:base.dmin,dmax:base.dmax,def:base.def,spd:base.spd,str:base.str,agi:base.agi,ene:base.ene,vit:base.vit}:null,attributesStatus:'Đọc trực tiếp từ window.S.inv'};
            });
            render(lastItems);
            status.textContent='Đã đọc trực tiếp '+lastItems.length+' item từ window.S.inv; thuộc tính lấy từ instance. Chưa gửi lệnh bán.';
            makeReport(lastItems);
          }
          function render(items){
            list.innerHTML=items.map(function(it){
              var attrs=[];
              if(it.plus)attrs.push('+'+it.plus);
              if(it.luck)attrs.push('Luck');
              if(it.option)attrs.push('Option '+it.option);
              if(it.hasExcellent)attrs.push('EX '+it.excellent.map(function(x){return x.name}).join(', '));
              attrs.push(it.w+'×'+it.h+' ô · ('+it.x+','+it.y+')');
              if(it.equipped)attrs.push('ĐANG TRANG BỊ');
              if(!it.known)attrs.push('CHƯA NHẬN DIỆN');
              return '<div class="item"><b>'+escapeHtml(it.name)+'</b> · '+escapeHtml(it.typeName)+' · ID '+it.u+'<br>'+escapeHtml(attrs.join(' · '))+'</div>';
            }).join('');
          }
          function escapeHtml(s){return String(s==null?'':s).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]})}
          function makeReport(items){var out={at:new Date().toISOString(),mode:'read-only',count:items.length,items:items};reportBox.value=JSON.stringify(out,null,2)}
          function dryRun(){
            if(!lastItems.length){scan();if(!lastItems.length)return}
            var types=selectedTypes(),keepEx=root.querySelector('#mi-ex').checked,keepLuck=root.querySelector('#mi-luck').checked,keepOpt=root.querySelector('#mi-opt').checked,minPlus=Number(root.querySelector('#mi-plus').value)||0;
            var rows=lastItems.map(function(it){
              var reasons=[];
              if(!it.known)reasons.push('Không nhận diện được');
              if(it.equipped)reasons.push('Đang trang bị');
              if(!it.known||it.equipped)return {item:it,decision:'KEEP',reason:reasons.join('; ')};
              if(!types[it.type])return {item:it,decision:'KEEP',reason:'Loại item chưa được chọn để bán'};
              if(it.excellent===null||it.luck===null||it.option===null)return {item:it,decision:'KEEP',reason:'Chưa đọc đủ thuộc tính Excellent/Luck/Option — giữ an toàn'};
              if(keepEx&&it.hasExcellent)reasons.push('Excellent');
              if(keepLuck&&it.luck===true)reasons.push('Luck');
              if(keepOpt&&Number(it.option)>0)reasons.push('Có Option');
              if(minPlus>0&&it.plus>=minPlus)reasons.push('Đạt ngưỡng giữ +'+minPlus);
              if(reasons.length)return {item:it,decision:'KEEP',reason:reasons.join('; ')};
              return {item:it,decision:'CANDIDATE',reason:'Khớp loại đã chọn và không có điều kiện giữ'};
            });
            var candidates=rows.filter(function(x){return x.decision==='CANDIDATE'});
            list.innerHTML=rows.map(function(x){var it=x.item;return '<div class="item '+(x.decision==='CANDIDATE'?'sell':'keep')+'"><b>'+escapeHtml(x.decision==='CANDIDATE'?'CÓ THỂ BÁN':'GIỮ')+'</b> · '+escapeHtml(it.name)+' · ID '+it.u+'<br>'+escapeHtml(x.reason||'Không khớp điều kiện bán')+' · '+it.w+'×'+it.h+' ô</div>'}).join('');
            var out={at:new Date().toISOString(),mode:'DRY_RUN_ONLY_NO_SELL_COMMAND',total:rows.length,candidateCount:candidates.length,keepCount:rows.length-candidates.length,settings:{types:types,keepExcellent:keepEx,keepLuck:keepLuck,keepOption:keepOpt,minPlus:minPlus},results:rows.map(function(x){return {u:x.item.u,g:x.item.g,n:x.item.n,name:x.item.name,type:x.item.type,decision:x.decision,reason:x.reason,position:{x:x.item.x,y:x.item.y},size:{w:x.item.w,h:x.item.h},icon:x.item.icon,plus:x.item.plus,luck:x.item.luck,option:x.item.option,excellent:x.item.excellent,excellentCodes:x.item.excellentCodes,hasExcellent:x.item.hasExcellent,equipped:x.item.equipped,attributesStatus:x.item.attributesStatus}})};
            reportBox.value=JSON.stringify(out,null,2);
            status.textContent='Chạy thử hoàn tất: '+candidates.length+' ứng viên / '+rows.length+' item. Đây KHÔNG phải lệnh bán.';
          }
          root.querySelector('.gear').onclick=function(){panel.style.display=panel.style.display==='block'?'none':'block'};
          root.querySelector('#mi-scan').onclick=scan;
          root.querySelector('#mi-dry').onclick=dryRun;
          root.querySelector('#mi-copy').onclick=function(){if(window.AndroidDiag&&AndroidDiag.copyReport)AndroidDiag.copyReport(reportBox.value);else{reportBox.focus();reportBox.select()}};
          scan();
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
