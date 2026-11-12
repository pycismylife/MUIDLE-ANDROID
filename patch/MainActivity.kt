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
          var report={createdAt:new Date().toISOString(),page:location.href,steps:[],clickCapture:null,postClickSnapshots:[],sellCandidates:[],sellEvents:[]}; try{var saved=localStorage.getItem('mu_diag_sell_report_v1');if(saved){var prior=JSON.parse(saved);if(prior&&prior.sellEvents)report=prior;}}catch(e){}
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
          function render(){var text=JSON.stringify(report,null,2);output.value=text;try{localStorage.setItem('mu_diag_sell_report_v1',text);}catch(e){status.textContent='Bộ nhớ đầy, hãy sao chép báo cáo ngay.';}}
          function inventoryState(){
            var inv=window.S&&Array.isArray(window.S.inv)?window.S.inv:null;
            return {count:inv?inv.length:null,items:inv?inv.map(function(it){return {u:it.u,g:it.g,n:it.n,l:it.l,lk:it.lk,o:it.o,ex:it.ex,x:it.x,y:it.y}}):null};
          }
          document.addEventListener('click',function(ev){
            if(root.contains(ev.target))return;
            var path=ev.composedPath?ev.composedPath():[];
            var sell=path.find(function(el){return el&&el.matches&&el.matches('[data-k="sell"]')});
            if(!sell)return;
            var before=inventoryState();
            var event={time:new Date().toISOString(),kind:'single_item_sell_button_click',button:describe(sell),popupText:(document.body.innerText||'').slice(-1800),inventoryBefore:before,inventoryAfter:null,afterScanStatus:'waiting_for_inventory_change'};
            report.sellEvents.push(event);render();
            status.textContent='Đã quét túi trước khi bán. Đang chờ game cập nhật túi để quét lại đúng một lần…';
            var beforeSignature=JSON.stringify(before);
            var observer=new MutationObserver(function(){
              var after=inventoryState();
              if(JSON.stringify(after)!==beforeSignature){
                observer.disconnect();
                event.inventoryAfter=after;
                event.afterScanStatus='completed';
                event.afterTime=new Date().toISOString();
                event.popupVisible=visible(sell.closest('.popup,.modal,[role="dialog"]'));
                event.bodyTail=(document.body.innerText||'').slice(-1200);
                render();
                status.textContent='Đã quét túi trước và sau khi bán xong. Nhấn Sao chép báo cáo.';
              }
            });
            observer.observe(document.documentElement,{subtree:true,childList:true,attributes:true,characterData:true});
          },true);
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
          root.querySelector('#md-copy').onclick=function(){render();if(window.AndroidDiag&&AndroidDiag.copyReport)AndroidDiag.copyReport(output.value);else{output.focus();output.select();status.textContent='Không có cầu nối clipboard. Hãy sao chép nội dung trong ô.';}}; render(); if(report.sellEvents&&report.sellEvents.length)status.textContent='Đã khôi phục '+report.sellEvents.length+' lần bấm Bán từ bộ nhớ.';
        })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }


    private fun installInventoryInspector() {
        val js = """
        (function(){
          function attachFilterLauncher(){
            var settingsPanel=document.querySelector('#mu-map-tools .panel');
            if(!settingsPanel)return false;
            var launch=settingsPanel.querySelector('#mu-open-filter');
            if(!launch){
              launch=document.createElement('button');launch.id='mu-open-filter';launch.className='action';launch.type='button';launch.textContent='🎒 AUTO FILTER PRO · INV';
              settingsPanel.appendChild(launch);
            }
            launch.onclick=function(){
              var filterPanel=document.querySelector('#mu-inv-inspector .panel');
              if(!filterPanel)return;
              filterPanel.style.display=(getComputedStyle(filterPanel).display==='none')?'block':'none';
            };
            return true;
          }
          var old=document.getElementById('mu-inv-inspector');
          if(window.__muAutoFilterProInstalled&&old){
            attachFilterLauncher();
            if(!window.__muFilterLauncherObserver){
              window.__muFilterLauncherObserver=new MutationObserver(function(){attachFilterLauncher()});
              window.__muFilterLauncherObserver.observe(document.documentElement,{childList:true,subtree:true});
            }
            return;
          }
          if(old)old.remove();
          window.__muAutoFilterProInstalled=true;
          var css=document.createElement('style');
          css.textContent='#mu-inv-inspector{position:fixed;z-index:2147483646;left:10px;bottom:12px;color:#f3e5c2;font:14px Arial,sans-serif}#mu-inv-inspector *{box-sizing:border-box}#mu-inv-inspector .gear{width:44px;height:44px;background:#21180e;color:#f4dfb2;border:1px solid #b18a48;font-weight:bold}#mu-inv-inspector .panel{display:none;width:min(480px,calc(100vw - 20px));max-height:82vh;overflow:auto;background:linear-gradient(145deg,#1b140c,#100d09);border:1px solid #d0a653;padding:12px;box-shadow:0 5px 24px #000d}#mu-inv-inspector .brand{font-size:18px;font-weight:900;letter-spacing:.4px;margin:0 0 12px;color:#f7e8c6}#mu-inv-inspector .row{display:flex;align-items:center;gap:7px;margin:7px 0;flex-wrap:wrap}#mu-inv-inspector .row>label{font-weight:bold}#mu-inv-inspector select,#mu-inv-inspector input[type=number]{min-width:0;background:#302313;color:#f7e7c4;border:1px solid #c49a50;padding:8px;font:inherit}#mu-inv-inspector select{flex:1}#mu-inv-inspector button{font:inherit;color:#f7e7c4;background:#382916;border:1px solid #b58b48;padding:8px}#mu-inv-inspector button:disabled{opacity:.5}#mu-inv-inspector .iconbtn{min-width:38px;font-size:20px}#mu-inv-inspector .section{border:1px solid #9e793f;margin-top:9px;padding:8px}#mu-inv-inspector .sectionhead{font-weight:bold;margin-bottom:6px}#mu-inv-inspector label.check{display:inline-flex;align-items:center;gap:6px;margin:5px 9px 5px 0}#mu-inv-inspector input[type=checkbox]{width:19px;height:19px;accent-color:#1ebc79}#mu-inv-inspector .exgrid{display:grid;grid-template-columns:1fr 1fr;gap:4px}#mu-inv-inspector .exgrid label{margin:3px 0}#mu-inv-inspector .status{color:#e8c979;margin:9px 0;font-size:12px;line-height:1.4}#mu-inv-inspector .green{background:linear-gradient(90deg,#075a37,#0d7a46);border:2px solid #55d69a;text-align:center;font-weight:bold;width:100%;padding:13px}#mu-inv-inspector .red{background:#65231b;border-color:#d87855}#mu-inv-inspector .list{font-size:12px;max-height:23vh;overflow:auto;line-height:1.4}#mu-inv-inspector .item{padding:6px 0;border-top:1px solid #594329}#mu-inv-inspector .candidate{color:#ffc56b}#mu-inv-inspector .keep{color:#9bd6a5}#mu-inv-inspector .muted{font-size:11px;color:#cdb98e}#mu-inv-inspector .advanced{display:none}#mu-inv-inspector .simplegrid{display:grid;grid-template-columns:1fr 1fr;gap:6px}#mu-inv-inspector .simplegrid select{width:100%}#mu-inv-inspector .primary{width:100%;padding:12px;font-weight:bold}#mu-inv-inspector .mini{font-size:12px;padding:6px}';
          document.head.appendChild(css);
          var root=document.createElement('div');root.id='mu-inv-inspector';
          root.innerHTML='<button class="gear">AF</button><div class="panel"><div class="brand">AUTO FILTER PRO</div><div class="simplegrid"><div><div class="muted">Loại đồ</div><select id="af-type"><option value="w">Vũ khí</option><option value="s">Khiên</option><option value="h">Mũ</option><option value="a">Giáp</option><option value="p">Quần</option><option value="g">Găng</option><option value="b">Giày</option></select></div><div><div class="muted">Class sử dụng</div><select id="af-class"><option value="all">Tất cả class</option><option value="dw">Dark Wizard</option><option value="dk">Dark Knight</option><option value="el">Fairy Elf</option><option value="mg">Magic Gladiator</option><option value="dl">Dark Lord</option></select></div></div><div class="section"><div class="sectionhead">Điều kiện lọc</div><div class="simplegrid"><div><div class="muted">Option tối thiểu</div><select id="af-option"><option value="0">Không xét</option><option value="1">+1 trở lên</option><option value="2">+2 trở lên</option><option value="3">+3 trở lên</option><option value="4">+4 trở lên</option></select></div><div><div class="muted">Upgrade tối thiểu</div><select id="af-upgrade"><option value="0">Không xét</option><option value="3">+3 trở lên</option><option value="4">+4 trở lên</option><option value="5">+5 trở lên</option><option value="6">+6 trở lên</option><option value="7">+7 trở lên</option><option value="9">+9 trở lên</option><option value="10">+10 trở lên</option></select></div></div><label class="check"><input id="af-luck" type="checkbox">Luck (May mắn)</label><label class="check"><input id="af-bonus" type="checkbox">Bonus cấp</label><select id="af-bonus-min"><option value="0">Bonus bất kỳ</option><option value="1">+1 trở lên</option><option value="2">+2 trở lên</option><option value="3">+3 trở lên</option></select></div><div class="section"><div class="sectionhead">Excellent <span class="muted">· tối đa 6 dòng</span><span id="af-count" style="float:right">0/6</span></div><div class="exgrid" id="af-ex"></div></div><div class="section"><label class="check"><input id="af-equipped" type="checkbox" checked>Giữ đồ đang mặc</label><label class="check"><input id="af-onlyex" type="checkbox">Chỉ lọc đồ Excellent</label></div><div class="row"><button class="primary" id="af-preview">CHẠY THỬ BỘ LỌC</button><button class="primary" id="af-sell">BÁN THỬ 1 ỨNG VIÊN</button></div><div class="status" id="af-status">Chưa chạy bộ lọc.</div><div class="list" id="af-list"></div><div class="section"><button class="mini" id="af-advanced-toggle">Cài đặt Rule nâng cao ▾</button><div class="advanced" id="af-advanced"><div class="row"><label>Rule</label><select id="af-rule"></select><button class="iconbtn" id="af-add">+</button><button class="iconbtn" id="af-copy">▢</button><button class="iconbtn red" id="af-del">×</button></div><div class="muted">Điều kiện trong cùng Rule là AND; giữa các Rule là OR.</div></div></div><button class="green" id="af-toggle">AUTO FILTER · ĐANG TẮT</button><div class="row"><button id="af-copyreport">Sao chép báo cáo</button></div></div>';
          document.documentElement.appendChild(root);
          var ownGear=root.querySelector('.gear');if(ownGear)ownGear.style.display='none';
          var $=function(q){return root.querySelector(q)},panel=$('.panel'),status=$('#af-status'),list=$('#af-list');
          attachFilterLauncher();
          if(!window.__muFilterLauncherObserver){
            window.__muFilterLauncherObserver=new MutationObserver(function(){attachFilterLauncher()});
            window.__muFilterLauncherObserver.observe(document.documentElement,{childList:true,subtree:true});
          }
          var groups={w:'Vũ khí',s:'Khiên',h:'Mũ',a:'Giáp',p:'Quần',g:'Găng',b:'Giày'};
          var exNames={weapon:['Hồi máu khi hạ quái','Hồi mana khi hạ quái','Sát thương +2%','Sát thương xuất sắc +10%','Sát thương + cấp / 20','Tốc độ đánh +7'],defense:['Zen rơi +40%','Máu tối đa +4%','Mana tối đa +4%','Né đòn +10%','Giảm sát thương 4%','Phản 5% sát thương']};
          var exCodes={'0':0,'1':1,'2':2,'3':3,'4':4,'5':5,'6':6,'7':7,'8':8,'9':9,'10':10,'11':11};
          var rules=[],current=0,active=false,lastRows=[],reportText='';
          function defaults(){return {type:'w',class:'all',option:0,upgrade:0,luck:false,bonus:false,bonusMin:0,excellent:[],keepEquipped:true,onlyExcellent:false,autoSell:false}}
          function save(){try{localStorage.setItem('muAutoFilterProRules',JSON.stringify(rules));localStorage.setItem('muAutoFilterProActive',active?'1':'0')}catch(e){}}
          function load(){try{var x=JSON.parse(localStorage.getItem('muAutoFilterProRules')||'null');if(Array.isArray(x)&&x.length)rules=x;else rules=[defaults()];active=localStorage.getItem('muAutoFilterProActive')==='1'}catch(e){rules=[defaults()]}}
          function renderRuleSelect(){var sel=$('#af-rule');sel.innerHTML=rules.map(function(r,i){return '<option value="'+i+'">Rule '+(i+1)+'</option>'}).join('');current=Math.max(0,Math.min(current,rules.length-1));sel.value=current}
          function renderEx(){var type=$('#af-type').value,arr=(type==='w'?exNames.weapon:exNames.defense);$('#af-ex').innerHTML=arr.map(function(n,i){var code=type==='w'?[3,9,1,0,8,2][i]:[6,4,10,7,5,11][i];return '<label class="check"><input type="checkbox" data-ex="'+code+'">'+n+'</label>'}).join('');var selected=rules[current].excellent||[];$('#af-ex').querySelectorAll('[data-ex]').forEach(function(c){c.checked=selected.indexOf(Number(c.dataset.ex))>=0});countEx()}
          function countEx(){var n=$('#af-ex').querySelectorAll('input:checked').length;$('#af-count').textContent='Đã chọn: '+n+'/6'}
          function fill(){var r=rules[current];$('#af-type').value=r.type;$('#af-class').value=r.class||'all';$('#af-option').value=r.option||0;$('#af-upgrade').value=r.upgrade||0;$('#af-luck').checked=!!r.luck;$('#af-bonus').checked=!!r.bonus;$('#af-bonus-min').value=r.bonusMin||0;$('#af-equipped').checked=r.keepEquipped!==false;$('#af-onlyex').checked=!!r.onlyExcellent;$('#af-autosell').checked=!!r.autoSell;renderEx()}
          function readForm(){var r=rules[current];r.type=$('#af-type').value;r.class=$('#af-class').value;r.option=Number($('#af-option').value);r.upgrade=Number($('#af-upgrade').value);r.luck=$('#af-luck').checked;r.bonus=$('#af-bonus').checked;r.bonusMin=Number($('#af-bonus-min').value);r.keepEquipped=$('#af-equipped').checked;r.onlyExcellent=$('#af-onlyex').checked;r.autoSell=$('#af-autosell').checked;r.excellent=Array.from($('#af-ex').querySelectorAll('input:checked')).map(function(x){return Number(x.dataset.ex)});save()}
          function groupType(g){g=Number(g);if(g>=0&&g<=5)return 'w';return ({6:'s',7:'h',8:'a',9:'p',10:'g',11:'b'})[g]||null}
          function state(){return window.S&&Array.isArray(window.S.inv)&&window.D&&window.D.item?{s:window.S,items:window.S.inv,db:window.D.item}:null}
          function equippedIds(s){var out={};Object.keys(s.eq||{}).forEach(function(k){var x=s.eq[k];if(x&&x.u!=null)out[String(x.u)]=true});return out}
          function itemName(it,db){var x=db[String(it.g)+'_'+String(it.n)];return x&&x.name||'Không rõ item'}
          function classAllowed(base,selected){if(!selected||selected==='all')return true;var names={dw:'Dark Wizard',dk:'Dark Knight',el:'Fairy Elf',mg:'Magic Gladiator',dl:'Dark Lord'};var keys=Object.keys(base||{}).filter(function(k){return /class|cls|job|use/i.test(k)});if(!keys.length)return false;var vals=[];keys.forEach(function(k){var v=base[k];if(Array.isArray(v))vals=vals.concat(v);else if(v!=null)vals.push(v)});var target=names[selected].toLowerCase();return vals.some(function(v){if(typeof v==='number')return ({0:'dw',1:'dk',2:'el',3:'mg',4:'dl'})[v]===selected;var t=String(v).toLowerCase();return t===selected||t.indexOf(target)>=0})}\n          function ruleMatch(it,rule,eq,db){var key=String(it.g)+'_'+String(it.n),base=db[key];if(!base||!groupType(it.g)||groupType(it.g)!==rule.type)return false;if(!classAllowed(base,rule.class))return false;if(rule.keepEquipped&&eq[String(it.u)])return false;if(rule.onlyExcellent&&!(Array.isArray(it.ex)&&it.ex.length))return false;if(Number(it.o||0)<Number(rule.option||0)||Number(it.l||0)<Number(rule.upgrade||0))return false;if(rule.luck&&!(it.lk===true||Number(it.lk)>0))return false;if(rule.bonus&&Number(it.bonus||it.lvBonus||0)<Number(rule.bonusMin||0))return false; if(rule.excellent&&rule.excellent.length){if(!Array.isArray(it.ex)||!rule.excellent.every(function(c){return it.ex.map(Number).indexOf(Number(c))>=0}))return false}return true}
          function scan(){var st=state();if(!st){status.textContent='Không đọc được dữ liệu inventory/catalog. Chưa thao tác.';return []}var eq=equippedIds(st.s);return st.items.map(function(it){var base=st.db[String(it.g)+'_'+String(it.n)],kind=groupType(it.g);return {raw:it,id:String(it.u),name:itemName(it,st.db),type:kind,known:!!base,equipped:!!eq[String(it.u)],plus:Number(it.l||0),luck:it.lk===true||Number(it.lk)>0,option:Number(it.o||0),ex:Array.isArray(it.ex)?it.ex.map(Number):null}})}
          function preview(){readForm();var st=state();if(!st){status.textContent='Không đọc được inventory/catalog.';return}var eq=equippedIds(st.s),rows=st.items.map(function(it){var base=st.db[String(it.g)+'_'+String(it.n)],matched=rules.some(function(r){return ruleMatch(it,r,eq,st.db)});return {it:it,known:!!base,equipped:!!eq[String(it.u)],candidate:!!base&&!eq[String(it.u)]&&matched,name:itemName(it,st.db)}});lastRows=rows;list.innerHTML=rows.map(function(x){return '<div class="item '+(x.candidate?'candidate':'keep')+'"><b>'+(x.candidate?'LOẠI BỎ':'GIỮ')+'</b> · '+x.name+' · ID '+x.it.u+(x.equipped?' · ĐANG MẶC':'')+(!x.known?' · CHƯA NHẬN DIỆN':'')+'</div>'}).join('');reportText=JSON.stringify({mode:'AUTO_FILTER_PRO_PREVIEW',ruleLogic:'AND within rule; OR across rules',rules:rules,results:rows.map(function(x){return {id:x.it.u,name:x.name,decision:x.candidate?'REMOVE':'KEEP',equipped:x.equipped,known:x.known}})},null,2);status.textContent='Chạy thử: '+rows.filter(function(x){return x.candidate}).length+' ứng viên / '+rows.length+' item. Chưa bán.';return rows}
          function wait(test,ms){return new Promise(function(resolve){var t=Date.now();(function poll(){var v=false;try{v=test()}catch(e){}if(v)return resolve(v);if(Date.now()-t>=ms)return resolve(false);setTimeout(poll,100)})()})}
          var busy=false;
          async function sellOne(){if(busy)return;busy=true;$('#af-sell').disabled=true;try{var rows=preview()||[],target=rows.find(function(x){return x.candidate});if(!target){status.textContent='Không có ứng viên phù hợp.';return}var st=state(),before=JSON.stringify(st.items.find(function(x){return String(x.u)===String(target.it.u)})),node=Array.from(document.querySelectorAll('.it[data-u]')).find(function(x){return x.getAttribute('data-u')===String(target.it.u)});if(!node){status.textContent='Không tìm thấy ô item. Đã dừng.';return}node.click();var sell=await wait(function(){var e=document.querySelector('button[data-k="sell"]');if(!e)return false;var r=e.getBoundingClientRect(),c=getComputedStyle(e);return r.width>0&&r.height>0&&c.display!=='none'&&c.visibility!=='hidden'?e:false},2500);if(!sell){status.textContent='Không thấy nút Bán.';return}var cur=state();var item=cur&&cur.items.find(function(x){return String(x.u)===String(target.it.u)});if(!item||JSON.stringify(item)!==before||equippedIds(cur.s)[String(target.it.u)]){status.textContent='Item thay đổi hoặc đang mặc. Hủy.';return}sell.click();var confirm=await wait(function(){var e=document.querySelector('button[data-k="sell"][data-ok="1"]');if(!e||!/bấm lần nữa để bán/i.test(e.textContent||''))return false;var r=e.getBoundingClientRect();return r.width>0&&r.height>0?e:false},2500);if(!confirm){status.textContent='Không thấy nút xác nhận bán lần hai. Không click tiếp.';return}cur=state();item=cur&&cur.items.find(function(x){return String(x.u)===String(target.it.u)});if(!item||JSON.stringify(item)!==before){status.textContent='Item đã thay đổi trước xác nhận. Hủy.';return}confirm.click();var gone=await wait(function(){var q=state();return !q||!q.items.some(function(x){return String(x.u)===String(target.it.u)})},4000);status.textContent=gone?'Đã xác minh bán thành công ID '+target.it.u+'.':'Chưa xác minh được item biến mất. Đã dừng.';if(gone)preview()}catch(e){status.textContent='Lỗi: '+String(e)}finally{busy=false;$('#af-sell').disabled=false}}
          $('.gear').onclick=function(){panel.style.display=panel.style.display==='block'?'none':'block'};
          $('#af-rule').onchange=function(){readForm();current=Number(this.value);fill()};$('#af-advanced-toggle').onclick=function(){var x=$('#af-advanced');x.style.display=x.style.display==='block'?'none':'block'};
          $('#af-type').onchange=function(){readForm();rules[current].type=this.value;renderEx();save()};
          ['af-class','af-option','af-upgrade','af-luck','af-bonus','af-bonus-min','af-equipped','af-onlyex','af-autosell'].forEach(function(id){$('#'+id).onchange=readForm});
          $('#af-ex').onchange=function(e){var checks=$('#af-ex').querySelectorAll('input:checked');if(checks.length>6){e.target.checked=false;status.textContent='Mỗi Rule chọn tối đa 6 dòng Excellent.'}readForm();countEx()};
          $('#af-add').onclick=function(){readForm();rules.push(defaults());current=rules.length-1;renderRuleSelect();fill();save()};
          $('#af-copy').onclick=function(){readForm();rules.push(JSON.parse(JSON.stringify(rules[current])));current=rules.length-1;renderRuleSelect();fill();save()};
          $('#af-del').onclick=function(){if(rules.length===1){rules[0]=defaults();current=0}else{rules.splice(current,1);current=Math.max(0,current-1)}renderRuleSelect();fill();save()};
          $('#af-preview').onclick=preview;$('#af-sell').onclick=sellOne;
          $('#af-copyreport').onclick=function(){var t=reportText||JSON.stringify(rules,null,2);if(navigator.clipboard&&navigator.clipboard.writeText)navigator.clipboard.writeText(t).then(function(){status.textContent='Đã sao chép báo cáo.'}).catch(function(){status.textContent='Không sao chép được; hãy chạy thử trước.'})};
          $('#af-toggle').onclick=function(){active=!active;this.textContent='AUTO FILTER · '+(active?'ĐANG BẬT':'ĐANG TẮT');this.classList.toggle('red',active);save();status.textContent=active?'Đã bật cấu hình Auto Filter. Chạy thử và dùng BÁN THỬ 1 ỨNG VIÊN để xác nhận từng món.':'Đã tắt Auto Filter.'};
          load();renderRuleSelect();fill();$('#af-toggle').textContent='AUTO FILTER · '+(active?'ĐANG BẬT':'ĐANG TẮT');status.textContent='Auto Filter PRO đã sẵn sàng. Cấu hình được lưu trên thiết bị.';
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
