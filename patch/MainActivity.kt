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
                installAutoFilter()
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
          if (window.__muAutoMoveMapInstalled && document.getElementById('mu-map-tools')) return;
          window.__muAutoMoveMapInstalled = true;
          var old = document.getElementById('mu-map-tools');
          if (old) old.remove();

          var style = document.getElementById('mu-auto-tools-style');
          if (!style) {
            style = document.createElement('style');
            style.id = 'mu-auto-tools-style';
            style.textContent = '#mu-map-tools{position:fixed;z-index:2147483647;top:12px;left:12px;color:#f3dfb5;font:13px Arial,sans-serif;max-width:calc(100vw - 24px)}#mu-map-tools button,#mu-map-tools input,#mu-map-tools select{font:inherit;box-sizing:border-box}#mu-map-tools .gear{width:42px;height:42px;background:#21190f;color:#f3dfb5;border:1px solid #80683f;font-size:21px}#mu-map-tools .panel{display:none;margin-top:6px;padding:11px;background:#17130f;border:1px solid #80683f;width:min(480px,calc(100vw - 30px));max-height:82vh;overflow:auto;box-sizing:border-box;box-shadow:0 8px 28px #0009}#mu-map-tools .heading{font-weight:700;margin-bottom:10px}#mu-map-tools .mapline{display:grid;grid-template-columns:58px 12px 64px 28px minmax(100px,1fr);align-items:center;gap:5px;margin:8px 0}#mu-map-tools input,#mu-map-tools select{width:100%;min-width:0;padding:8px 5px;background:#302515;color:#f3dfb5;border:1px solid #80683f}#mu-map-tools .action{width:100%;padding:9px;margin-top:8px;background:#382c1b;color:#f3dfb5;border:1px solid #80683f}#mu-map-tools .on{background:#205b32!important;border-color:#4c9c61!important}#mu-map-tools .status{font-size:11px;line-height:1.45;color:#e8c979;margin-top:8px;white-space:normal}#mu-map-tools .af-section{border-top:1px solid #80683f;margin-top:13px;padding-top:11px}#mu-map-tools .af-title{font-weight:700;margin-bottom:9px;font-size:14px}#mu-map-tools .af-grid{display:grid;grid-template-columns:1fr 1fr;gap:7px}#mu-map-tools .af-field{min-width:0}#mu-map-tools .af-label{font-size:11px;color:#d7c49d;margin-bottom:4px}#mu-map-tools .af-rule{border:1px solid #80683f;background:#21190f;padding:8px;margin:8px 0}#mu-map-tools .af-rulehead{display:flex;align-items:center;justify-content:space-between;gap:6px;font-weight:700;margin-bottom:7px}#mu-map-tools .af-rulehead>span{flex:1;min-width:48px}#mu-map-tools .af-rulehead select{width:auto;max-width:31%;min-width:0;flex:1;font-size:10px;padding:6px 3px}#mu-map-tools .af-tags{display:flex;flex-wrap:wrap;gap:5px;margin-bottom:7px}#mu-map-tools .af-tag{display:inline-flex;align-items:center;gap:5px;padding:5px 7px;background:#302515;border:1px solid #b58b48;font-size:11px}#mu-map-tools .af-tag button{border:0;background:transparent;color:#ffd9c8;padding:0 2px;font-size:16px}#mu-map-tools .af-row{display:flex;gap:6px;align-items:center;margin-top:7px}#mu-map-tools .af-row>*{min-width:0;flex:1}#mu-map-tools .af-btn{padding:8px;background:#382c1b;color:#f3dfb5;border:1px solid #80683f}#mu-map-tools .af-danger{background:#65231b;border-color:#d87855}#mu-map-tools .af-muted{font-size:11px;color:#cdb98e;line-height:1.4}#mu-map-tools .af-check{display:inline-flex;align-items:center;gap:5px;margin:6px 9px 2px 0}#mu-map-tools .af-check input{width:17px;height:17px;accent-color:#1ebc79}#mu-map-tools .af-status{font-size:11px;color:#e8c979;line-height:1.4;margin:8px 0}#mu-map-tools .af-list{max-height:20vh;overflow:auto;font-size:11px}#mu-map-tools .af-item{padding:5px 0;border-top:1px solid #594329}';
            document.head.appendChild(style);
          }

          var root = document.createElement('div');
          root.id = 'mu-map-tools';
          root.innerHTML = '<button class="gear" type="button" aria-label="Cài đặt Auto Move Map và Auto Filter">⚙</button><div class="panel"><div class="heading">AUTO MOVE MAP</div><div class="mapline"><span>Lv 1</span><span>-</span><input id="mu-map-limit" type="number" min="1" max="400" value="300"><span>tại</span><select id="mu-map-y"><option value="">Chọn Map Y</option></select></div><div class="mapline"><span id="mu-map-level">Lv 301+</span><span>-</span><span id="mu-map-reset" style="text-align:center;padding:8px;border:1px solid #80683f">Reset</span><span>tại</span><select id="mu-map-z"><option value="">Chọn Map Z</option></select></div><button class="action" id="mu-map-toggle" type="button">AUTO MOVE MAP · ĐÃ TẮT</button><div class="status" id="mu-map-status">Đang chờ dữ liệu bản đồ trong game…</div><button class="action" id="mu-map-reload" type="button">TẢI LẠI MAP</button><div id="mu-auto-filter-slot"></div></div>';
          document.documentElement.appendChild(root);

          var panel = root.querySelector('.panel');
          var limitInput = root.querySelector('#mu-map-limit');
          var mapY = root.querySelector('#mu-map-y');
          var mapZ = root.querySelector('#mu-map-z');
          var toggle = root.querySelector('#mu-map-toggle');
          var status = root.querySelector('#mu-map-status');
          var cfg = {limit:300,y:'',z:'',on:false};
          var maps = [];
          var busy = false;
          var lastReset = null;
          var resetOverrideUntil = 0;
          var lastAttempt = 0;
          var failed = 0;
          var readBusy = false;
          var readTries = 0;

          function visible(el) {
            if (!el) return false;
            var r = el.getBoundingClientRect(), s = getComputedStyle(el);
            return r.width > 0 && r.height > 0 && s.display !== 'none' && s.visibility !== 'hidden';
          }
          function rows() { return Array.prototype.slice.call(document.querySelectorAll('#wMap .zr[data-z]')); }
          function currentMap() {
            var row = document.querySelector('#wMap .zr.cur[data-z]');
            return row ? row.getAttribute('data-z') : null;
          }
          function safeSave() {
            cfg.limit = Math.max(1,Math.min(400,Math.floor(Number(limitInput.value)||300)));
            cfg.y = mapY.value || cfg.y || '';
            cfg.z = mapZ.value || cfg.z || '';
            try { localStorage.setItem('mu_auto_move_map_v2',JSON.stringify(cfg)); } catch(e) {}
          }
          function loadConfig() {
            try {
              var saved = JSON.parse(localStorage.getItem('mu_auto_move_map_v2') || 'null');
              if (saved && typeof saved === 'object') cfg = Object.assign(cfg,saved);
              else {
                var legacy = JSON.parse(localStorage.getItem('mu_auto_move_map_v1') || 'null');
                if (legacy && typeof legacy === 'object') cfg = Object.assign(cfg,legacy);
              }
            } catch(e) {}
            cfg.limit = Math.max(1,Math.min(400,Math.floor(Number(cfg.limit)||300)));
            cfg.on = cfg.on === true;
            limitInput.value = String(cfg.limit);
          }
          function paint() {
            root.querySelector('#mu-map-level').textContent = 'Lv ' + (cfg.limit + 1) + '+';
            var reset = Number(window.S && window.S.reset);
            root.querySelector('#mu-map-reset').textContent = Number.isFinite(reset) && reset >= 0 ? String(reset) : 'Reset';
            toggle.textContent = cfg.on ? 'AUTO MOVE MAP · ĐANG BẬT' : 'AUTO MOVE MAP · ĐÃ TẮT';
            toggle.classList.toggle('on',cfg.on);
          }
          function renderMaps() {
            var html = '<option value="">-- Chọn bản đồ --</option>' + maps.map(function(m) {
              return '<option value="' + String(m.id).replace(/&/g,'&amp;').replace(/"/g,'&quot;') + '">' +
                String(m.name).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;') +
                (m.locked ? ' · Chưa mở khóa' : '') + '</option>';
            }).join('');
            mapY.innerHTML = html;
            mapZ.innerHTML = html;
            mapY.value = maps.some(function(m){return m.id===cfg.y;}) ? cfg.y : '';
            mapZ.value = maps.some(function(m){return m.id===cfg.z;}) ? cfg.z : '';
            if (!maps.some(function(m){return m.id===cfg.y;})) cfg.y = '';
            if (!maps.some(function(m){return m.id===cfg.z;})) cfg.z = '';
            safeSave();
            status.textContent = 'Đã tải ' + maps.length + ' bản đồ. Chọn Map Y và Map Z.';
          }
          function readMaps(force) {
            if (readBusy || (!force && maps.length)) return;
            var win = document.getElementById('wMap');
            var button = document.getElementById('bMap');
            if (!win || !button) {
              status.textContent = 'Chưa tìm thấy bảng bản đồ trong game.';
              return;
            }
            readBusy = true;
            var openedByUs = !visible(win);
            status.textContent = 'Đang mở danh sách bản đồ để tải dữ liệu…';
            if (openedByUs) button.click();
            var started = Date.now();
            (function poll() {
              var found = rows();
              if (found.length) {
                maps = found.map(function(row) {
                  return {id:row.getAttribute('data-z'),name:(row.querySelector('b')||row).textContent.trim(),locked:row.classList.contains('lock')};
                }).filter(function(m){return m.id!==null && m.id!=='';});
              }
              if (maps.length || Date.now()-started >= 6000) {
                if (openedByUs && visible(win)) button.click();
                readBusy = false;
                readTries++;
                if (maps.length) renderMaps();
                else status.textContent = 'Không tải được danh sách map. Hãy vào game đầy đủ rồi bấm TẢI LẠI MAP.';
                return;
              }
              setTimeout(poll,150);
            })();
          }
          function levelValue() {
            var n = Number(window.S && window.S.lv);
            if (Number.isFinite(n) && n > 0) return n;
            var t = (document.getElementById('lvl') || {}).textContent || '';
            var m = t.match(/(?:Lv|Level)\s*(\d+)/i);
            return m ? Number(m[1]) : null;
          }
          function resetValue() {
            var n = Number(window.S && window.S.reset);
            return Number.isFinite(n) && n >= 0 ? n : null;
          }
          function switchMap(id,label) {
            if (!id || busy || Date.now()-lastAttempt < 9000) return;
            busy = true;
            lastAttempt = Date.now();
            var win = document.getElementById('wMap'), button = document.getElementById('bMap');
            var openedByUs = !visible(win);
            if (openedByUs) {
              if (!button) { busy=false; failed++; status.textContent='Không tìm thấy bảng bản đồ.'; return; }
              button.click();
            }
            setTimeout(function() {
              var row = rows().find(function(r){return r.getAttribute('data-z')===String(id);});
              if (!row || row.classList.contains('lock')) {
                if (openedByUs && visible(win)) button.click();
                busy=false; failed++;
                status.textContent = !row ? 'Không tìm thấy map đã chọn.' : label + ' chưa mở khóa.';
                return;
              }
              if (row.classList.contains('cur')) {
                if (openedByUs && visible(win)) button.click();
                busy=false; failed=0; status.textContent='Đã ở đúng ' + label + '.';
                return;
              }
              row.click();
              setTimeout(function() {
                var reached = currentMap() === String(id);
                if (openedByUs && visible(win)) button.click();
                busy=false;
                if (reached) { failed=0; status.textContent='Đã chuyển đến ' + label + '.'; }
                else { failed++; status.textContent='Game chưa xác nhận chuyển map. Đã gửi một lần, không click lặp.'; }
              },1100);
            },350);
          }
          function tick() {
            paint();
            if (!cfg.on || busy || failed >= 2) return;
            var lv = levelValue(), reset = resetValue();
            if (lv === null || reset === null) return;
            if (lastReset === null) lastReset = reset;
            if (reset > lastReset) {
              lastReset = reset;
              resetOverrideUntil = Date.now() + 8000;
              failed = 0;
            } else if (reset < lastReset) lastReset = reset;
            var targetStage = (Date.now() < resetOverrideUntil || lv <= cfg.limit) ? 'y' : 'z';
            var id = targetStage === 'y' ? cfg.y : cfg.z;
            var label = targetStage === 'y' ? 'Map Y' : 'Map Z';
            if (!id) { status.textContent='Chưa chọn ' + label + '.'; return; }
            if (currentMap() === String(id)) { failed=0; return; }
            if (Date.now()-lastAttempt < 9000) return;
            switchMap(id,label);
          }

          loadConfig();
          paint();
          root.querySelector('.gear').onclick = function() {
            panel.style.display = panel.style.display === 'block' ? 'none' : 'block';
            if (panel.style.display === 'block') readMaps(true);
          };
          limitInput.onchange = function() { safeSave(); paint(); failed=0; };
          mapY.onchange = function() { cfg.y=mapY.value; safeSave(); failed=0; };
          mapZ.onchange = function() { cfg.z=mapZ.value; safeSave(); failed=0; };
          root.querySelector('#mu-map-reload').onclick = function(){ readMaps(true); };
          toggle.onclick = function() {
            if (!cfg.on && (!mapY.value || !mapZ.value)) {
              status.textContent='Hãy chờ tải map và chọn đủ Map Y, Map Z trước khi bật.';
              return;
            }
            cfg.on = !cfg.on;
            if (cfg.on) { lastReset=resetValue(); failed=0; }
            safeSave(); paint(); tick();
          };
          window.setInterval(function(){ tick(); },1000);
        })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun installAutoFilter() {
        val js = """
        (function() {
          var root = document.getElementById('mu-map-tools');
          var slot = document.getElementById('mu-auto-filter-slot');
          if (!root || !slot || slot.dataset.ready === '1') return;
          slot.dataset.ready = '1';

          var groups = {w:'Vũ khí',s:'Khiên',h:'Headgear (Helm + Circlet)',a:'Giáp',p:'Quần',g:'Găng',b:'Giày'};
          var exCatalog = [
            {id:3,name:'Hồi máu khi hạ quái',kind:'w'},
            {id:9,name:'Hồi mana khi hạ quái',kind:'w'},
            {id:1,name:'Sát thương +2%',kind:'w'},
            {id:0,name:'Sát thương xuất sắc +10%',kind:'w'},
            {id:8,name:'Sát thương + cấp / 20',kind:'w'},
            {id:2,name:'Tốc độ đánh +7',kind:'w'},
            {id:6,name:'Zen rơi +40%',kind:'d'},
            {id:4,name:'Máu tối đa +4%',kind:'d'},
            {id:10,name:'Mana tối đa +4%',kind:'d'},
            {id:7,name:'Né đòn +10%',kind:'d'},
            {id:5,name:'Giảm sát thương 4%',kind:'d'},
            {id:11,name:'Phản 5% sát thương',kind:'d'}
          ];
          var common = {option:0,upgrade:0,luck:false,bonus:false,bonusMin:0};
          var rules = [{excellent:[]}];
          var active = false, busy = false, lastRows = [], status, list;
          var $ = function(q){return slot.querySelector(q);};
          function esc(v) { return String(v).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }
          function state() {
            return window.S && Array.isArray(window.S.inv) && window.D && window.D.item
              ? {s:window.S,items:window.S.inv,db:window.D.item} : null;
          }
          function groupType(g) {
            g=Number(g);
            if (g>=0 && g<=5) return 'w';
            return ({6:'s',7:'h',8:'a',9:'p',10:'g',11:'b'})[g] || null;
          }
          function equippedIds(s) {
            var out={};
            Object.keys(s.eq||{}).forEach(function(k) {
              var x=s.eq[k];
              if (x && x.u!=null) out[String(x.u)]=true;
            });
            return out;
          }
          function baseOf(it,db) { return db[String(it.g)+'_'+String(it.n)] || null; }
          function itemName(it,db) { var b=baseOf(it,db); return b && b.name || 'Không rõ item'; }
          function classAllowed(base,selected) {
            if (!selected || selected==='all') return true;
            var names={dw:'Dark Wizard',dk:'Dark Knight',el:'Fairy Elf',mg:'Magic Gladiator',dl:'Dark Lord'};
            var keys=Object.keys(base||{}).filter(function(k){return /class|cls|job|use/i.test(k);});
            if (!keys.length) return false;
            var vals=[];
            keys.forEach(function(k){var v=base[k]; if(Array.isArray(v)) vals=vals.concat(v); else if(v!=null) vals.push(v);});
            return vals.some(function(v) {
              if (typeof v==='number') return ({0:'dw',1:'dk',2:'el',3:'mg',4:'dl'})[v]===selected;
              var t=String(v).toLowerCase();
              return t===selected || t.indexOf(names[selected].toLowerCase())>=0;
            });
          }
          function commonMatch(it,eq,db) {
            var base=baseOf(it,db), kind=groupType(it.g);
            if (!base || !kind || kind!==common.type) return false;
            if (!classAllowed(base,common.class)) return false;
            if (Number(it.o||0)<Number(common.option||0)) return false;
            if (Number(it.l||0)<Number(common.upgrade||0)) return false;
            if (common.luck && !(it.lk===true || Number(it.lk)>0)) return false;
            if (common.bonus && Number(it.bonus||it.lvBonus||0)<Number(common.bonusMin||0)) return false;
            if (eq[String(it.u)]) return false;
            return true;
          }
          function ruleScopeMatch(it,base,rule) {
            return !!base && groupType(it.g)===rule.type && classAllowed(base,rule.class);
          }
          function ruleMatch(it,rule,base) {
            if (!ruleScopeMatch(it,base,rule)) return false;
            var selected=Array.isArray(rule.excellent)?rule.excellent:[];
            if (!selected.length || !Array.isArray(it.ex)) return false;
            var codes=it.ex.map(Number);
            return selected.every(function(c){return codes.indexOf(Number(c))>=0;});
          }
          function matchesKeep(it,base) { return rules.some(function(r){return ruleMatch(it,r,base);}); }
          function scan() {
            var st=state();
            if (!st) return null;
            var eq=equippedIds(st.s);
            return st.items.map(function(it) {
              var base=baseOf(it,st.db), equipped=!!eq[String(it.u)];
              var inScope=!!base && !!groupType(it.g) &&
                rules.some(function(rule){return ruleScopeMatch(it,base,rule);}) &&
                Number(it.o||0)>=Number(common.option||0) &&
                Number(it.l||0)>=Number(common.upgrade||0) &&
                (!common.luck || it.lk===true || Number(it.lk)>0) &&
                (!common.bonus || Number(it.bonus||it.lvBonus||0)>=Number(common.bonusMin||0));
              var keep = equipped || !base || !inScope || matchesKeep(it,base);
              return {raw:it,id:String(it.u),name:itemName(it,st.db),known:!!base,equipped:equipped,inScope:inScope,keep:keep,candidate:!keep};
            });
          }
          function save() {
            try {
              localStorage.setItem('muAutoFilterCleanRules',JSON.stringify(rules));
              localStorage.setItem('muAutoFilterCleanCommon',JSON.stringify(common));
              localStorage.setItem('muAutoFilterCleanActive',active?'1':'0');
              localStorage.setItem('muAutoFilterProRules',JSON.stringify(rules));
              localStorage.setItem('muAutoFilterProCommon',JSON.stringify(common));
              localStorage.setItem('muAutoFilterProActive',active?'1':'0');
            } catch(e) {}
          }
          function load() {
            try {
              var r=JSON.parse(localStorage.getItem('muAutoFilterCleanRules')||'null');
              if (!Array.isArray(r)) r=JSON.parse(localStorage.getItem('muAutoFilterProRules')||'null');
              if (Array.isArray(r) && r.length) rules=r.map(function(x){return {type:x.type||'w',class:x.class||'all',excellent:Array.isArray(x.excellent)?x.excellent.map(Number).filter(function(n){return exCatalog.some(function(e){return e.id===n;});}).slice(0,6):[]};});
              var c=JSON.parse(localStorage.getItem('muAutoFilterCleanCommon')||'null');
              if (!c) c=JSON.parse(localStorage.getItem('muAutoFilterProCommon')||'null');
              if (c && typeof c==='object') common=Object.assign(common,c);
              rules.forEach(function(rule){if(!rule.type)rule.type=common.type||'w';if(!rule.class)rule.class=common.class||'all';});
              active=localStorage.getItem('muAutoFilterCleanActive')==='1' || localStorage.getItem('muAutoFilterProActive')==='1';
              if (!rules.some(function(rule){return rule.excellent.length>0;})) active=false;
            } catch(e) { rules=[{excellent:[]}]; active=false; }
          }
          function renderRules() {
            $('#af-rules').innerHTML=rules.map(function(rule,i) {
              var tags=(rule.excellent||[]).map(function(code) {
                var x=exCatalog.find(function(e){return e.id===Number(code);});
                return x ? '<span class="af-tag">'+esc(x.name)+'<button type="button" data-remove="'+i+'" data-code="'+x.id+'" aria-label="Xóa">×</button></span>' : '';
              }).join('');
              var options=exCatalog.filter(function(x){return !(rule.excellent||[]).includes(x.id);})
                .map(function(x){return '<option value="'+x.id+'">'+esc(x.name)+'</option>';}).join('');
              var typeOptions=[['w','Weapon · Vũ khí'],['s','Shield · Khiên'],['h','Headgear · Helm + Circlet'],['a','Armor · Giáp'],['p','Pants · Quần'],['g','Gloves · Găng'],['b','Boots · Giày']].map(function(x){return '<option value="'+x[0]+'" '+(rule.type===x[0]?'selected':'')+'>'+x[1]+'</option>';}).join('');
              var classOptions=[['all','Tất cả class'],['dw','Dark Wizard'],['dk','Dark Knight'],['el','Fairy Elf'],['mg','Magic Gladiator'],['dl','Dark Lord']].map(function(x){return '<option value="'+x[0]+'" '+(rule.class===x[0]?'selected':'')+'>'+x[1]+'</option>';}).join('');
              return '<div class="af-rule"><div class="af-rulehead"><span>Rule '+(i+1)+' · '+(rule.excellent.length)+'/6 EX</span><select data-rule-class="'+i+'">'+classOptions+'</select><select data-rule-type="'+i+'">'+typeOptions+'</select><button class="af-btn af-danger" type="button" data-delete="'+i+'">Xóa</button></div><div class="af-tags">'+(tags||'<span class="af-muted">Chưa chọn EX Option</span>')+'</div><div class="af-row"><select data-select="'+i+'"><option value="">Chọn EX Option</option>'+options+'</select><button class="af-btn" type="button" data-add="'+i+'">＋ Thêm</button></div></div>';
            }).join('');
          }
          function fill() {
            $('#af-option').value=String(common.option);
            $('#af-upgrade').value=String(common.upgrade);
            $('#af-luck').checked=!!common.luck;
            $('#af-bonus').checked=!!common.bonus;
            $('#af-bonus-min').value=String(common.bonusMin);
            renderRules();
            paintToggle();
          }
          function readForm() {
            common.option=Number($('#af-option').value);
            common.upgrade=Number($('#af-upgrade').value);
            common.luck=$('#af-luck').checked;
            common.bonus=$('#af-bonus').checked;
            common.bonusMin=Number($('#af-bonus-min').value);
            save();
          }
          function paintToggle() {
            $('#af-toggle').textContent='AUTO FILTER · '+(active?'ĐANG BẬT':'ĐÃ TẮT');
            $('#af-toggle').classList.toggle('on',active);
          }
          function preview() {
            readForm();
            var rows=scan();
            if (!rows) { status.textContent='Chưa đọc được inventory/catalog. Chưa thực hiện thao tác.'; return null; }
            lastRows=rows;
            var candidates=rows.filter(function(x){return x.candidate;});
            list.innerHTML=rows.map(function(x) {
              var label=x.candidate?'ỨNG VIÊN BÁN':'GIỮ';
              return '<div class="af-item"><b>'+label+'</b> · '+esc(x.name)+' · ID '+esc(x.id)+(x.equipped?' · ĐANG MẶC':'')+(!x.known?' · KHÔNG NHẬN DIỆN':'')+'</div>';
            }).join('');
            status.textContent='Đã quét '+rows.length+' item · '+candidates.length+' ứng viên bán. Item đang mặc/không nhận diện luôn được giữ.';
            return rows;
          }
          function waitFor(test,timeout) {
            return new Promise(function(resolve) {
              var started=Date.now();
              (function poll() {
                var value=false;
                try { value=test(); } catch(e) {}
                if (value) return resolve(value);
                if (Date.now()-started>=timeout) return resolve(false);
                setTimeout(poll,100);
              })();
            });
          }
          function sameItem(id,signature) {
            var st=state();
            if (!st) return false;
            var it=st.items.find(function(x){return String(x.u)===String(id);});
            if (!it) return false;
            return JSON.stringify(it)===signature && !equippedIds(st.s)[String(id)];
          }
          async function sellOne() {
            if (busy) return false;
            busy=true;
            try {
              var rows=preview();
              var target=rows && rows.find(function(x){return x.candidate;});
              if (!target) return false;
              var st=state();
              var live=st && st.items.find(function(x){return String(x.u)===target.id;});
              if (!live || equippedIds(st.s)[target.id]) return false;
              var signature=JSON.stringify(live);
              var node=Array.prototype.slice.call(document.querySelectorAll('.it[data-u]')).find(function(x){return x.getAttribute('data-u')===target.id;});
              if (!node) { status.textContent='Không tìm thấy ô item tương ứng. Đã dừng.'; return false; }
              node.click();
              var sell=await waitFor(function() {
                var e=document.querySelector('button[data-k="sell"]');
                if (!e) return false;
                var r=e.getBoundingClientRect(),s=getComputedStyle(e);
                return r.width>0 && r.height>0 && s.display!=='none' && s.visibility!=='hidden' ? e : false;
              },2500);
              if (!sell || !sameItem(target.id,signature)) { status.textContent='Không xác minh được item/nút bán. Đã hủy.'; return false; }
              sell.click();
              var confirm=await waitFor(function() {
                var e=document.querySelector('button[data-k="sell"][data-ok="1"]');
                if (!e || !/bấm lần nữa để bán/i.test(e.textContent||'')) return false;
                var r=e.getBoundingClientRect();
                return r.width>0 && r.height>0 ? e : false;
              },2500);
              if (!confirm || !sameItem(target.id,signature)) { status.textContent='Không xác minh được bước xác nhận thứ hai. Đã hủy.'; return false; }
              confirm.click();
              var gone=await waitFor(function() {
                var q=state();
                return !q || !q.items.some(function(x){return String(x.u)===target.id;});
              },4000);
              status.textContent=gone?'Đã bán và xác minh item ID '+target.id+'.':'Chưa xác minh item biến mất. Dừng thao tác để tránh bán nhầm.';
              if (gone) preview();
              return gone;
            } catch(e) {
              status.textContent='Auto Filter dừng do lỗi: '+String(e);
              return false;
            } finally { busy=false; }
          }
          function tick() {
            if (!active || busy) return;
            var rows=scan();
            if (!rows) return;
            if (rows.some(function(x){return x.candidate;})) sellOne();
          }

          slot.innerHTML='<div class="af-section"><div class="af-title">AUTO FILTER</div><div class="af-grid"><div class="af-field"><div class="af-label">Option tối thiểu</div><select id="af-option"><option value="0">Không xét</option><option value="1">+1 trở lên</option><option value="2">+2 trở lên</option><option value="3">+3 trở lên</option><option value="4">+4 trở lên</option></select></div><div class="af-field"><div class="af-label">Upgrade tối thiểu</div><select id="af-upgrade"><option value="0">Không xét</option><option value="3">+3 trở lên</option><option value="4">+4 trở lên</option><option value="5">+5 trở lên</option><option value="6">+6 trở lên</option><option value="7">+7 trở lên</option><option value="9">+9 trở lên</option><option value="10">+10 trở lên</option></select></div></div><label class="af-check"><input id="af-luck" type="checkbox">Luck</label><label class="af-check"><input id="af-bonus" type="checkbox">Bonus cấp</label><select id="af-bonus-min" style="width:95px"><option value="0">Bonus bất kỳ</option><option value="1">+1 trở lên</option><option value="2">+2 trở lên</option><option value="3">+3 trở lên</option></select><div class="af-rule"><div class="af-rulehead"><span>Danh sách Rule</span><button class="af-btn" id="af-add-rule" type="button">＋ Thêm Rule</button></div><div class="af-muted">Trong cùng Rule: AND (tất cả EX phải có). Giữa các Rule: OR (đạt một Rule là giữ). Tối đa 6 EX mỗi Rule.</div><div id="af-rules"></div></div><div class="af-row"><button class="af-btn" id="af-preview" type="button">CHẠY THỬ</button><button class="af-btn" id="af-toggle" type="button">AUTO FILTER · ĐÃ TẮT</button></div><div class="af-status" id="af-status">Chưa quét inventory.</div><div class="af-list" id="af-list"></div></div>';
          status=$('#af-status'); list=$('#af-list');
          load(); fill();
          ['af-option','af-upgrade','af-luck','af-bonus','af-bonus-min'].forEach(function(id) {
            $('#'+id).onchange=readForm;
          });
          $('#af-add-rule').onclick=function() {
            readForm();
            rules.push({type:'w',class:'all',excellent:[]});
            renderRules(); save();
          };
          $('#af-rules').addEventListener('click',function(ev) {
            var add=ev.target.closest('[data-add]'), remove=ev.target.closest('[data-remove]'), del=ev.target.closest('[data-delete]');
            var ruleType=ev.target.closest('[data-rule-type]'), ruleClass=ev.target.closest('[data-rule-class]');
            if (ruleType) { var ti=Number(ruleType.getAttribute('data-rule-type')); if(rules[ti]){rules[ti].type=ruleType.value;save();} return; }
            if (ruleClass) { var ci=Number(ruleClass.getAttribute('data-rule-class')); if(rules[ci]){rules[ci].class=ruleClass.value;save();} return; }
            if (add) {
              var i=Number(add.getAttribute('data-add')), select=$('#af-rules').querySelector('[data-select="'+i+'"]');
              if (!select || !select.value) return;
              if (rules[i].excellent.length>=6) { status.textContent='Mỗi Rule chỉ được chọn tối đa 6 EX.'; return; }
              var code=Number(select.value);
              if (!rules[i].excellent.includes(code)) rules[i].excellent.push(code);
              renderRules(); save(); return;
            }
            if (remove) {
              var ri=Number(remove.getAttribute('data-remove')), code=Number(remove.getAttribute('data-code'));
              rules[ri].excellent=rules[ri].excellent.filter(function(x){return Number(x)!==code;});
              renderRules(); save(); return;
            }
            if (del) {
              var di=Number(del.getAttribute('data-delete'));
              if (rules.length===1) rules[0]={type:'w',class:'all',excellent:[]}; else rules.splice(di,1);
              renderRules(); save();
            }
          });
          $('#af-preview').onclick=preview;
          $('#af-toggle').onclick=function() {
            readForm();
            if (!active && (!rules.length || rules.every(function(r){return !r.excellent.length;}))) {
              status.textContent='Hãy tạo ít nhất một Rule có EX trước khi bật để tránh lọc nhầm.';
              return;
            }
            active=!active; save(); paintToggle();
            status.textContent=active?'Đã bật Auto Filter. Hệ thống sẽ quét và bán từng item không đạt Rule trong phạm vi đã chọn.':'Đã tắt Auto Filter.';
            if (active) tick();
          };
          window.setInterval(tick,1800);
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
