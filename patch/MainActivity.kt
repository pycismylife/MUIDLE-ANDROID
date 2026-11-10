package net.hoainiem2003.muidle

import android.annotation.SuppressLint
import android.app.Activity
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
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
                    var patchedSource = source.replace(target, replacement)
                    val dropHook = "h.drops.push(a)"
                    val dropTap = """h.drops.push(a);if(a.it&&window.__muIdleDropSeen){try{var z=h.zone||{},it=r.item[a.it.g+"_"+a.it.n]||{};window.__muIdleDropSeen({map:z.name||z.id||("Map "+(z.id||"?")),item:it.name||("Item "+a.it.g+"_"+a.it.n),g:a.it.g,n:a.it.n,at:Date.now()})}catch(e){}}"""
                    patchedSource = patchedSource.replace(dropHook, dropTap)
                    val patched = patchedSource.toByteArray(Charsets.UTF_8)
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
          box.innerHTML = '<button class="gear">⚙</button><div class="panel"><b>MU IDLE – Công cụ bổ sung</b><div class="line"><span>Ẩn quái</span><button class="switch" id="mu-hide-monsters">Tắt</button></div><div class="line"><span>Ẩn minimap</span><button class="switch" id="mu-minimap">Tắt</button></div><div class="line" id="mu-offline-speed-row" style="display:none"><span>Tốc độ xem thử offline</span><select id="mu-offline-speed" style="min-width:125px;background:#222;color:#fff;border:1px solid #777;padding:8px;border-radius:5px"><option value="1">1x · Gốc</option><option value="1.5">1.5x</option><option value="2">2x</option><option value="3">3x</option><option value="5">5x</option></select></div><div class="line"><span>Giới hạn FPS bổ sung</span><select id="mu-fps" style="min-width:125px;background:#222;color:#fff;border:1px solid #777;padding:8px;border-radius:5px"><option value="0">Theo game (30/60)</option><option value="5">5 FPS</option><option value="10">10 FPS</option><option value="20">20 FPS</option></select></div><div class="note" id="mu-status">Các mục đồ họa 3D, hiệu ứng kỹ năng, chất lượng hình và ẩn người chơi đã có sẵn trong Cài đặt game.</div></div>';
          document.documentElement.appendChild(box);
          var panel = box.querySelector('.panel');
          box.querySelector('.gear').onclick = function() { panel.style.display = panel.style.display === 'block' ? 'none' : 'block'; };
          var status = box.querySelector('#mu-status');
          var automation=document.createElement('div');
          automation.style.cssText='margin-top:9px;padding-top:8px;border-top:1px solid #665638';
          automation.innerHTML='<b>MU IDLE – TỰ ĐỘNG HÓA</b><div class="line"><span>Auto Filter PRO</span><button class="switch" id="mu-auto-filter">Chưa bật</button></div><div class="line"><span>Auto Map PRO</span><button class="switch" id="mu-auto-map">Chưa bật</button></div><div class="line"><span>Auto Reset · reset khi đủ cấp</span><button class="switch" id="mu-auto-reset">Tắt</button></div><div class="note">Auto Filter PRO và Auto Map PRO đang được triển khai độc lập; không sử dụng cài đặt bán đồ/đổi bản đồ gốc của game.</div>';
          panel.insertBefore(automation,box.querySelector('.note'));
          var autoFilterBtn=automation.querySelector('#mu-auto-filter'),autoMapBtn=automation.querySelector('#mu-auto-map'),autoResetBtn=automation.querySelector('#mu-auto-reset');
          function paintToggle(btn,on){btn.textContent=on?'Bật':'Tắt';btn.classList.toggle('on',!!on);}
          autoFilterBtn.disabled=false;autoMapBtn.disabled=false;
          autoMapBtn.textContent='Cấu hình';
          var mapPanel=document.createElement('div');
          mapPanel.id='mu-auto-map-panel';
          mapPanel.style.cssText='display:none;margin-top:8px;padding:9px;background:#171717;border:1px solid #806b43;border-radius:7px;font-size:12px';
          mapPanel.innerHTML='<div style="font-weight:bold;color:#f2d58e;margin-bottom:7px">AUTO MAP PRO</div><div style="display:flex;align-items:center;gap:5px;margin:7px 0;color:#f0ddaf">Lv 1 đến <input id="mu-map-level" type="number" min="1" max="400" value="300" style="width:58px;padding:8px 3px;background:#222;color:#fff;border:1px solid #777;border-radius:4px"> farm map <select id="mu-map-one" style="flex:1;min-width:0;padding:8px;background:#222;color:#fff;border:1px solid #777"><option value="">Chọn map Y</option></select></div><div style="display:flex;align-items:center;gap:5px;margin:7px 0;color:#f0ddaf">Lv <span id="mu-map-level-label">300</span> đến Reset farm map <select id="mu-map-two" style="flex:1;min-width:0;padding:8px;background:#222;color:#fff;border:1px solid #777"><option value="">Chọn map Z</option></select></div><button id="mu-map-refresh" style="width:100%;margin-top:5px">Làm mới danh sách map</button><button id="mu-map-toggle" class="switch" style="width:100%;margin-top:7px">Tắt Auto Map PRO</button><div id="mu-map-status" style="color:#ffdf8a;line-height:1.45;margin-top:7px">Độc lập với tùy chọn Tự đổi bãi của game.</div>';
          autoMapBtn.parentNode.insertBefore(mapPanel,autoMapBtn.parentNode.nextSibling);
          var mapOne=mapPanel.querySelector('#mu-map-one'),mapTwo=mapPanel.querySelector('#mu-map-two'),mapLevel=mapPanel.querySelector('#mu-map-level'),mapStatus=mapPanel.querySelector('#mu-map-status'),mapToggle=mapPanel.querySelector('#mu-map-toggle');
          var mapConfig={one:'',two:'',level:300,on:false},mapOptions=[],mapLastStage='',mapBusy=false;
          try{var savedMap=JSON.parse(localStorage.getItem('mu_idle_auto_map')||'{}');if(savedMap&&typeof savedMap==='object')mapConfig=Object.assign(mapConfig,savedMap);}catch(e){}
          mapLevel.value=String(Math.max(1,Math.min(400,Number(mapConfig.level)||300)));
          function saveMapConfig(){if(mapOne.options.length)mapConfig.one=mapOne.value;if(mapTwo.options.length)mapConfig.two=mapTwo.value;mapConfig.level=Math.max(1,Math.min(400,Number(mapLevel.value)||300));try{localStorage.setItem('mu_idle_auto_map',JSON.stringify(mapConfig));}catch(e){mapStatus.textContent='Không lưu được cấu hình trên thiết bị.';}}
          function renderMapOptions(){
            [mapOne,mapTwo].forEach(function(sel){sel.innerHTML='<option value="">-- Chọn bản đồ --</option>'+mapOptions.map(function(m){return '<option value="'+filterEscape(m.id)+'">'+filterEscape(m.name)+(m.locked?' · Chưa mở khóa':'')+'</option>';}).join('');});
            mapOne.value=mapConfig.one||'';mapTwo.value=mapConfig.two||'';
          }
          function readMapOptions(){
            var mapWin=document.getElementById('wMap'),wasOpen=!!(mapWin&&!mapWin.hidden),mapBtn=document.getElementById('bMap');
            if(!wasOpen&&mapBtn)mapBtn.click();
            setTimeout(function(){
              var rows=Array.from(document.querySelectorAll('#wMap .zr[data-z]'));
              if(rows.length){
                mapOptions=rows.map(function(row){return {id:row.getAttribute('data-z'),name:(row.querySelector('b')||row).textContent.trim(),locked:row.classList.contains('lock')};});
                renderMapOptions();mapStatus.textContent='Đã đọc '+mapOptions.length+' bản đồ từ danh sách Di chuyển của game.';
              }else mapStatus.textContent='Chưa đọc được danh sách map. Hãy vào game và thử làm mới.';
              if(!wasOpen&&mapBtn)mapBtn.click();
            },350);
          }
          function mapLevelNow(){var txt=(document.getElementById('lvl')||{}).textContent||'';var m=txt.match(/Lv\s*(\d+)/i);return m?Number(m[1]):null;}
          function mapSwitchTo(id,stage){
            if(!id||mapBusy)return;
            mapBusy=true;
            var mapBtn=document.getElementById('bMap'),mapWin=document.getElementById('wMap'),wasOpen=!!(mapWin&&!mapWin.hidden);
            if(!wasOpen&&mapBtn)mapBtn.click();
            setTimeout(function(){
              var row=Array.from(document.querySelectorAll('#wMap .zr[data-z]')).find(function(x){return x.getAttribute('data-z')===id;});
              if(!row){mapStatus.textContent='Không tìm thấy map đã chọn. Hãy làm mới danh sách.';if(!wasOpen&&mapBtn)mapBtn.click();mapBusy=false;return;}
              if(row.classList.contains('lock')){mapStatus.textContent='Map đã chọn chưa mở khóa ở cấp hiện tại. Chưa chuyển map.';if(!wasOpen&&mapBtn)mapBtn.click();mapBusy=false;return;}
              if(row.classList.contains('cur')){mapStatus.textContent='Đang ở đúng map của giai đoạn '+stage+'.';if(!wasOpen&&mapBtn)mapBtn.click();mapBusy=false;return;}
              row.click();mapStatus.textContent='Đã yêu cầu game chuyển sang '+(row.querySelector('b')||row).textContent.trim()+'.';
              mapBusy=false;
            },350);
          }
          function mapTick(){
            if(!mapConfig.on||mapBusy)return;
            var lv=mapLevelNow();if(lv==null)return;
            var stage=lv<=mapConfig.level?'one':'two',target=stage==='one'?mapConfig.one:mapConfig.two;
            if(!target){mapStatus.textContent='Chưa chọn Map '+(stage==='one'?'1':'2')+'.';return;}
            if(mapLastStage!==stage){mapLastStage=stage;mapSwitchTo(target,stage==='one'?1:2);}
          }
          autoMapBtn.onclick=function(){mapPanel.style.display=mapPanel.style.display==='block'?'none':'block';if(!mapOptions.length)readMapOptions();};
          mapPanel.querySelector('#mu-map-refresh').onclick=readMapOptions;
          mapOne.onchange=mapTwo.onchange=saveMapConfig;
          mapLevel.onchange=function(){saveMapConfig();mapPanel.querySelector('#mu-map-level-label').textContent=mapLevel.value;mapLastStage='';};
          function paintMapToggle(){mapToggle.textContent=mapConfig.on?'Auto Map PRO · ĐANG BẬT':'Auto Map PRO · ĐÃ TẮT';mapToggle.classList.toggle('on',!!mapConfig.on);}
          mapToggle.onclick=function(){mapConfig.on=!mapConfig.on;saveMapConfig();paintMapToggle();mapLastStage='';mapTick();};
          paintMapToggle();saveMapConfig();
          setInterval(mapTick,2000);
          autoFilterBtn.textContent='Mở bộ lọc';
          var filterPanel=document.createElement('div');
          filterPanel.id='mu-filter-dryrun';
          filterPanel.style.cssText='display:none;margin-top:8px;padding:9px;background:#171717;border:1px solid #806b43;border-radius:7px;max-height:58vh;overflow:auto;font-size:12px';
          filterPanel.innerHTML='<div style="font-weight:bold;color:#f2d58e;margin-bottom:6px">AUTO FILTER PRO · CHẠY THỬ</div><div style="color:#ffcf70;line-height:1.45;margin-bottom:8px">Chỉ đọc túi đồ và mô phỏng luật giữ/bán. Không gửi lệnh bán, không thay đổi vật phẩm.</div><div id="mu-filter-rules"></div><button id="mu-filter-add" style="width:100%;margin-top:6px">+ Thêm luật giữ</button><button id="mu-filter-scan" style="width:100%;margin-top:6px;background:#315b3c">Quét túi đồ</button><div id="mu-filter-summary" style="margin-top:8px;color:#f0dba7"></div><div id="mu-filter-results" style="margin-top:6px;line-height:1.5"></div>';
          autoFilterBtn.parentNode.insertBefore(filterPanel,autoFilterBtn.parentNode.nextSibling);
          autoFilterBtn.onclick=function(){filterPanel.style.display=filterPanel.style.display==='block'?'none':'block';};
          var filterGroups=[['w','Weapon'],['s','Shield'],['h','Headgear'],['a','Armor'],['p','Pants'],['g','Gloves'],['b','Boots']];
          var filterRules=[];
          try{var savedRules=JSON.parse(localStorage.getItem('mu_idle_filter_rules')||'[]');if(Array.isArray(savedRules))filterRules=savedRules.filter(function(r){return r&&filterGroups.some(function(g){return g[0]===r.group;});});}catch(e){}
          function filterEscape(v){return String(v==null?'':v).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];});}
          function saveFilterRules(){try{localStorage.setItem('mu_idle_filter_rules',JSON.stringify(filterRules));}catch(e){status.textContent='Không lưu được luật lọc trên thiết bị.';}}
          function renderFilterRules(){
            var host=filterPanel.querySelector('#mu-filter-rules');
            host.innerHTML=filterRules.map(function(r,i){
              return '<div data-rule="'+i+'" style="border:1px solid #51452f;border-radius:6px;padding:7px;margin:7px 0"><div style="display:flex;justify-content:space-between;align-items:center;gap:6px"><b>Luật giữ '+(i+1)+'</b><button data-remove="'+i+'" style="padding:4px 8px;background:#713333">Xóa</button></div><label style="display:block;margin-top:6px">Nhóm trang bị</label><select data-field="group" style="width:100%;padding:7px;background:#222;color:#fff;border:1px solid #777">'+filterGroups.map(function(g){return '<option value="'+g[0]+'" '+(r.group===g[0]?'selected':'')+'>'+g[1]+'</option>';}).join('')+'</select><div style="display:grid;grid-template-columns:1fr 1fr;gap:6px;margin-top:6px"><label>Số dòng Ex tối thiểu<input data-field="minEx" type="number" min="1" max="6" value="'+(r.minEx||'')+'" placeholder="Bỏ qua" style="width:100%;box-sizing:border-box;padding:6px;background:#222;color:#fff"></label><label>Cấp + tối thiểu<input data-field="minLevel" type="number" min="0" max="15" value="'+(r.minLevel==null?'':r.minLevel)+'" placeholder="Bỏ qua" style="width:100%;box-sizing:border-box;padding:6px;background:#222;color:#fff"></label><label>Luck<select data-field="luck" style="width:100%;padding:6px;background:#222;color:#fff"><option value="any" '+((r.luck||'any')==='any'?'selected':'')+'>Không xét</option><option value="yes" '+(r.luck==='yes'?'selected':'')+'>Phải có</option><option value="no" '+(r.luck==='no'?'selected':'')+'>Không có</option></select></label><label>Option tối thiểu<input data-field="minOption" type="number" min="0" max="255" value="'+(r.minOption==null?'':r.minOption)+'" placeholder="Bỏ qua" style="width:100%;box-sizing:border-box;padding:6px;background:#222;color:#fff"></label></div><label style="display:flex;align-items:center;gap:6px;margin-top:7px"><input data-field="classOnly" type="checkbox" '+(r.classOnly?'checked':'')+'> Chỉ giữ đồ phù hợp class hiện tại</label><label style="display:block;margin-top:7px">Dòng Excellent cần có<select data-field="exMode" style="width:100%;padding:6px;background:#222;color:#fff"><option value="any" '+((r.exMode||'any')==='any'?'selected':'')+'>Có ít nhất một dòng đã chọn</option><option value="all" '+(r.exMode==='all'?'selected':'')+'>Phải có tất cả dòng đã chọn</option></select></label><div style="display:grid;grid-template-columns:1fr 1fr;gap:4px;margin-top:5px">'+[['0','Sát thương xuất sắc +10%'],['1','Sát thương +2%'],['2','Tốc độ đánh +7'],['3','Hồi máu khi hạ quái'],['4','Máu tối đa +4%'],['5','Giảm sát thương 4%'],['6','Zen rơi +40%'],['7','Né đòn +10%'],['8','Sát thương + cấp / 20'],['9','Hồi mana khi hạ quái'],['10','Mana tối đa +4%'],['11','Phản 5% sát thương']].map(function(x){return '<label style="display:flex;align-items:flex-start;gap:4px;color:#ddd"><input data-ex-line="'+x[0]+'" type="checkbox" '+((r.exLines||[]).indexOf(Number(x[0]))>=0?'checked':'')+'>'+x[1]+'</label>';}).join('')+'</div><div style="color:#b9aa8a;margin-top:5px">Các điều kiện trong luật này kết hợp AND.</div></div>';
            }).join('')||'<div style="color:#aaa">Chưa có luật giữ. Khi quét thử, đồ Ex nhận diện được sẽ được đánh dấu “Đủ điều kiện bán” (chỉ mô phỏng).</div>';
            host.querySelectorAll('[data-rule]').forEach(function(row){
              var idx=Number(row.getAttribute('data-rule'));
              row.querySelectorAll('[data-ex-line]').forEach(function(el){el.onchange=function(){var line=Number(el.dataset.exLine),arr=filterRules[idx].exLines||[];if(el.checked&&arr.indexOf(line)<0)arr.push(line);if(!el.checked)arr=arr.filter(function(v){return v!==line;});filterRules[idx].exLines=arr;saveFilterRules();};});
              row.querySelectorAll('[data-field]').forEach(function(el){
                el.onchange=function(){
                  var key=el.getAttribute('data-field');
                  if(key==='classOnly')filterRules[idx][key]=el.checked;
                  else if(key==='exMode')filterRules[idx][key]=el.value;
                  else if(key==='group'||key==='luck')filterRules[idx][key]=el.value;
                  else filterRules[idx][key]=el.value===''?null:Number(el.value);
                  saveFilterRules();
                };
              });
              var rm=row.querySelector('[data-remove]');if(rm)rm.onclick=function(){filterRules.splice(idx,1);saveFilterRules();renderFilterRules();};
            });
          }
          filterPanel.querySelector('#mu-filter-add').onclick=function(){filterRules.push({group:'w',luck:'any'});saveFilterRules();renderFilterRules();};
          function filterGroup(item){
            var g=Number(item&&item.g);
            if(g>=0&&g<=5)return 'w';
            return ({6:'s',7:'h',8:'a',9:'p',10:'g',11:'b'})[g]||null;
          }
          function filterEquipped(item,state){
            var eq=state&&state.eq||{};
            return Object.keys(eq).some(function(k){
              var x=eq[k];if(!x)return false;
              if(x===item)return true;
              if(x.u!=null&&item.u!=null&&String(x.u)===String(item.u))return true;
              return x.g!=null&&x.n!=null&&item.g!=null&&item.n!=null&&Number(x.g)===Number(item.g)&&Number(x.n)===Number(item.n);
            });
          }
          function filterMatches(rule,item,state){
            if(rule.group!==filterGroup(item))return false;
            var wantedLines=Array.isArray(rule.exLines)?rule.exLines:[],actualLines=Array.isArray(item.ex)?item.ex.map(Number):[];
            if(wantedLines.length){var hits=wantedLines.filter(function(v){return actualLines.indexOf(v)>=0;}).length;if((rule.exMode==='all'&&hits!==wantedLines.length)||(rule.exMode!=='all'&&hits===0))return false;}
            if(rule.minEx!=null&&Number(item.ex&&item.ex.length||0)<Number(rule.minEx))return false;
            if(rule.minLevel!=null&&Number(item.l||0)<Number(rule.minLevel))return false;
            if(rule.luck==='yes'&&!item.lk)return false;
            if(rule.luck==='no'&&!!item.lk)return false;
            if(rule.minOption!=null&&Number(item.o||0)<Number(rule.minOption))return false;
            if(rule.classOnly){
              var cls=window.CLS&&window.CLS[state.cls],ci=cls&&Number(cls.ci);
              var cat=window.D&&window.D.item&&window.D.item[Number(item.g)+'_'+Number(item.n)];
              if(!cat||!Array.isArray(cat.cls)||!Number.isFinite(ci)||Number(cat.cls[ci])<=0)return false;
            }
            return true;
          }
          function runFilterDryRun(){
            var state=window.S,host=filterPanel.querySelector('#mu-filter-results'),summary=filterPanel.querySelector('#mu-filter-summary');
            if(!state||!Array.isArray(state.inv)){summary.textContent='Chưa đọc được túi đồ (window.S.inv).';host.textContent='Không có kết quả. Hãy đăng nhập và mở nhân vật trong game.';return;}
            var counts={keep:0,sell:0,protected:0,unknown:0,nonEx:0},rows=[];
            state.inv.forEach(function(item){
              if(!item||typeof item!=='object')return;
              var isEx=Array.isArray(item.ex)&&item.ex.length>0;
              if(!isEx){counts.nonEx++;return;}
              var group=filterGroup(item),name=(window.D&&window.D.item&&window.D.item[Number(item.g)+'_'+Number(item.n)]||{}).name||('Item '+item.g+'_'+item.n);
              var equipped=filterEquipped(item,state);
              if(equipped){counts.protected++;rows.push({name:name,group:group,label:'BẢO VỆ · Đang trang bị',color:'#8ec8ff'});return;}
              if(!group){counts.unknown++;rows.push({name:name,group:null,label:'BẢO VỆ · Không nhận diện',color:'#ffcf70'});return;}
              var kept=filterRules.some(function(rule){return filterMatches(rule,item,state);});
              if(kept){counts.keep++;rows.push({name:name,group:group,label:'GIỮ · Khớp luật',color:'#8edb9c'});}
              else{counts.sell++;rows.push({name:name,group:group,label:'ĐỦ ĐIỀU KIỆN BÁN · Mô phỏng',color:'#ff9b8f'});}
            });
            summary.textContent='Tổng Ex: '+(counts.keep+counts.sell+counts.protected+counts.unknown)+' · Giữ: '+counts.keep+' · Đủ điều kiện bán: '+counts.sell+' · Bảo vệ: '+(counts.protected+counts.unknown)+' · Đồ thường bỏ qua: '+counts.nonEx;
            host.innerHTML=rows.map(function(x){return '<div style="padding:5px 2px;border-bottom:1px solid #38332a"><b>'+filterEscape(x.name)+'</b><div style="color:'+(x.color)+'">'+filterEscape(x.label)+(x.group?' · '+filterEscape((filterGroups.find(function(g){return g[0]===x.group;})||[])[1]||''):'')+'</div></div>';}).join('')||'<div style="color:#aaa">Túi đồ hiện không có trang bị Excellent.</div>';
            status.textContent='Auto Filter PRO: đã quét thử '+state.inv.length+' ô/vật phẩm trong túi. Không có lệnh bán nào được gửi.';
          }
          filterPanel.querySelector('#mu-filter-scan').onclick=runFilterDryRun;
          renderFilterRules();
          // Auto Stats PRO: use the game's own stat buttons and server action path.
          var statsPanel=document.createElement('div');
          statsPanel.id='mu-auto-stats-panel';
          statsPanel.style.cssText='margin-top:8px;padding:9px;background:#21190f;border:1px solid #705632;border-radius:7px;font-size:12px';
          statsPanel.innerHTML='<div style="font-weight:bold;color:#f0ddaf;margin-bottom:7px">AUTO STATS</div><div id="mu-stat-rows"></div><div id="mu-stat-status" style="color:#d9c58f;margin-top:7px;line-height:1.4">Nhập số điểm muốn cộng rồi nhấn +. Dùng nút cộng điểm gốc của game.</div>';
          panel.insertBefore(statsPanel,automation.nextSibling);
          var statRows=statsPanel.querySelector('#mu-stat-rows'),statStatus=statsPanel.querySelector('#mu-stat-status');
          var statNames=['STR','AGI','ENE','VIT','CMD'],statIndex=[0,1,3,2,4],statConfig=[0,0,0,0,0],statBusy=false;
          try{var savedStats=JSON.parse(localStorage.getItem('mu_idle_auto_stats')||'{}');if(savedStats&&Array.isArray(savedStats.values))statConfig=savedStats.values.map(function(v){return Math.max(0,Math.min(500000,Math.floor(Number(v)||0)));});}catch(e){}
          while(statConfig.length<5)statConfig.push(0);
          function saveStats(){try{localStorage.setItem('mu_idle_auto_stats',JSON.stringify({values:statConfig,auto:statAuto}));}catch(e){statStatus.textContent='Không lưu được số điểm trên thiết bị.';}}
          var statAuto=[false,false,false,false,false];try{var sa=JSON.parse(localStorage.getItem('mu_idle_auto_stats')||'{}');if(Array.isArray(sa.auto))statAuto=sa.auto.map(Boolean);}catch(e){};statRows.innerHTML=statNames.map(function(name,i){return '<div style="display:flex;align-items:center;gap:5px;margin:6px 0"><div style="flex:1;min-width:0;padding:10px 6px;background:#302416;border:1px solid #705632;border-radius:4px;color:#f0ddaf;font-weight:bold">'+name+'</div><input data-stat-input="'+i+'" type="number" min="0" step="1" value="'+statConfig[i]+'" style="width:58px;box-sizing:border-box;padding:10px 3px;text-align:center;background:#21190f;border:1px solid #705632;border-radius:4px;color:#f0ddaf;font-weight:bold"><button data-stat-add="'+i+'" style="width:38px;height:40px;padding:0;background:#9d3928;border:1px solid #c77a50;border-radius:4px;color:#fff;font-size:24px;font-weight:bold">+</button><label style="font-size:10px;color:#f0ddaf;white-space:nowrap"><input data-stat-auto="'+i+'" type="checkbox" '+(statAuto[i]?'checked':'')+'> + Hết</label></div>';}).join('');
          statRows.querySelectorAll('[data-stat-input]').forEach(function(input){input.onchange=function(){var i=Number(input.dataset.statInput);statConfig[i]=Math.max(0,Math.min(500000,Math.floor(Number(input.value)||0)));input.value=String(statConfig[i]);saveStats();};});statRows.querySelectorAll('[data-stat-auto]').forEach(function(input){input.onchange=function(){statAuto[Number(input.dataset.statAuto)]=input.checked;saveStats();statStatus.textContent=input.checked?'Đã bật tự động cộng toàn bộ điểm khả dụng vào '+statNames[Number(input.dataset.statAuto)]+'.':'Đã tắt tự động cộng cho '+statNames[Number(input.dataset.statAuto)]+'.';};});
          function statAvailablePoints(){var el=document.querySelector('#wChar .cpts');var m=el&&el.textContent.match(/(\d+)/);return m?Number(m[1]):0;}
          function statCurrentValue(index){var row=document.querySelectorAll('#wChar .cst')[index];if(!row)return null;var el=row.querySelector('.vl');return el?Number(el.textContent.trim()):null;}
          function statAdd(index){
            if(statBusy)return;
            var input=statRows.querySelector('[data-stat-input="'+index+'"]');
            var wanted=Math.max(0,Math.floor(Number(input&&input.value)||0));
            if(!wanted){statStatus.textContent='Nhập số điểm lớn hơn 0 cho '+statNames[index]+'.';return;}
            var charWin=document.getElementById('wChar'),needsOpen=!charWin||charWin.hidden;
            if(needsOpen){var charBtn=document.getElementById('bChar');if(charBtn)charBtn.click();}
            statBusy=true;
            var gameIndex=statIndex[index],left=wanted,added=0,attempts=0,lastValue=statCurrentValue(gameIndex);
            statStatus.textContent='Đang cộng '+statNames[index]+'…';
            function step(){
              var rows=document.querySelectorAll('#wChar .cst'),row=rows[gameIndex],available=statAvailablePoints(),value=statCurrentValue(gameIndex);
              if(!row||value==null){finish('Không đọc được bảng chỉ số. Hãy mở Nhân vật rồi thử lại.');return;}
              if(value!==lastValue&&lastValue!=null){var delta=value-lastValue;if(delta>0){added+=delta;left=Math.max(0,left-delta);}lastValue=value;}
              if(left<=0){finish('Hoàn tất: đã cộng '+added+' điểm vào '+statNames[index]+'.');return;}
              if(available<=0){finish('Đã cộng '+added+' điểm. Không còn điểm khả dụng.');return;}
              if(attempts++>wanted+5){finish('Đã dừng để tránh gửi lặp. Đã cộng '+added+' điểm; hãy kiểm tra nhân vật.');return;}
              var buttons=row.querySelectorAll('button[data-a]');
              var amount=Math.min(10,left,available),button=amount>=10?row.querySelector('button[data-a="'+gameIndex+'"][data-n="10"]'):row.querySelector('button[data-a="'+gameIndex+'"][data-n="1"]');
              if(amount>=10&&button){button.click();}
              else {button=row.querySelector('button[data-a="'+gameIndex+'"][data-n="1"]');if(!button){finish('Nút cộng điểm gốc của game chưa sẵn sàng.');return;}button.click();}
              setTimeout(function(){
                var now=statCurrentValue(gameIndex);
                if(now!=null&&lastValue!=null&&now>lastValue){var delta=now-lastValue;added+=delta;left=Math.max(0,left-delta);lastValue=now;}
                step();
              },220);
            }
            function finish(message){statBusy=false;statStatus.textContent=message;}
            step();
          }
          statRows.querySelectorAll('[data-stat-add]').forEach(function(button){button.onclick=function(){statAdd(Number(button.dataset.statAdd));};});
          var statAutoPending=false,statAutoCursor=0;
          function statAutoTick(){
            if(statBusy||statAutoPending||!statAuto.some(Boolean)||!window.S)return;
            var available=statAvailablePoints();if(available<=0)return;
            var idx=-1;for(var k=0;k<5;k++){var candidate=(statAutoCursor+k)%5;if(statAuto[candidate]){idx=candidate;break;}}if(idx<0)return;statAutoCursor=(idx+1)%5;
            var charWin=document.getElementById('wChar'),needsOpen=!charWin||charWin.hidden;
            if(needsOpen){var cb=document.getElementById('bChar');if(cb)cb.click();}
            statAutoPending=true;statBusy=true;
            var gi=statIndex[idx],row=document.querySelectorAll('#wChar .cst')[gi],value=statCurrentValue(gi);
            if(!row||value==null){statBusy=false;statAutoPending=false;return;}
            var btn=row.querySelector('button[data-a="'+gi+'"][data-n="10"]')||row.querySelector('button[data-a="'+gi+'"][data-n="1"]');
            if(!btn){statBusy=false;statAutoPending=false;return;}
            setTimeout(function(){btn.click();setTimeout(function(){statBusy=false;statAutoPending=false;},350);},needsOpen?500:0);
          }
          setInterval(statAutoTick,700);
          var autoReset=false,resetPending=false;
          try{autoReset=localStorage.getItem('mu_idle_auto_reset')==='1';}catch(e){}
          autoResetBtn.onclick=function(){
            autoReset=!autoReset;paintToggle(autoResetBtn,autoReset);
            try{localStorage.setItem('mu_idle_auto_reset',autoReset?'1':'0');}catch(e){}
            status.textContent=autoReset?'Auto Reset đã bật: tự reset khi đạt cấp yêu cầu.':'Auto Reset đã tắt.';
          };
          paintToggle(autoResetBtn,autoReset);
          setInterval(function(){
            if(!autoReset||resetPending||!window.S)return;
            var state=window.S,level=Number(state.lv),resets=Number(state.reset)||0;
            if(!Number.isFinite(level)||level<=0)return;
            var required=Math.min(400,300+20*Math.floor((resets+1)/5));
            if(level<required)return;
            resetPending=true;
            var charWin=document.getElementById('wChar');
            if(!charWin||charWin.hidden){var cb=document.getElementById('bChar');if(cb)cb.click();}
            setTimeout(function(){
              var resetButton=document.getElementById('bReset');
              if(resetButton&&!resetButton.disabled&&resetButton.getClientRects().length){
                resetButton.click();
                status.textContent='Auto Reset: đã gửi lệnh Reset · Lv '+level+' / yêu cầu '+required+'.';
              }else{
                status.textContent='Auto Reset: đủ cấp Lv '+level+' nhưng nút Reset chưa sẵn sàng.';
              }
              setTimeout(function(){resetPending=false;},5000);
            },500);
          },1500);
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
          var dbButton=document.createElement('button');
          dbButton.textContent='Drop Table';
          dbButton.style.cssText='width:100%;margin-top:8px;background:#4a3b20';
          panel.insertBefore(dbButton,automation.nextSibling);
          var dbPanel=document.createElement('div');
          dbPanel.style.cssText='display:none;margin-top:8px;padding:9px;background:#171717;border:1px solid #806b43;border-radius:7px;max-height:55vh;overflow:auto';
          dbPanel.innerHTML='<input id="mu-db-search" placeholder="Tìm kiếm..." style="box-sizing:border-box;width:100%;padding:9px;background:#222;color:#fff;border:1px solid #777;border-radius:5px"><div id="mu-db-summary" style="font-size:11px;color:#d9c58f;margin:7px 0"></div><div id="mu-db-results" style="font-size:12px;line-height:1.5"></div>';
          panel.insertBefore(dbPanel,dbButton.nextSibling);
          var dbKind='drops';
          window.__muIdleDropSeen=function(entry){
            try{
              var logs=JSON.parse(localStorage.getItem('mu_idle_drop_log')||'[]');
              var last=logs[logs.length-1];
              if(last&&last.map===entry.map&&last.g===entry.g&&last.n===entry.n&&entry.at-last.at<1500)return;
              logs.push(entry);if(logs.length>5000)logs=logs.slice(-5000);
              localStorage.setItem('mu_idle_drop_log',JSON.stringify(logs));
            }catch(e){}
          };
          function dbText(v){return String(v==null?'':v).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];});}
          function dbRows(){
            var logs=[];try{logs=JSON.parse(localStorage.getItem('mu_idle_drop_log')||'[]');}catch(e){}
            var grouped={};
            logs.forEach(function(x){var key=x.map+'|'+x.g+'_'+x.n;if(!grouped[key])grouped[key]={title:x.item,sub:x.map+' · '+x.g+'_'+x.n,count:0,last:x.at};grouped[key].count++;grouped[key].last=Math.max(grouped[key].last,x.at);});
            return Object.values(grouped).sort(function(a,b){return b.last-a.last;}).map(function(x){return {title:x.title,sub:x.sub+' · Ghi nhận '+x.count+' lần',raw:x};});
          }
          function renderDb(){
            var rows=dbRows(), q=(dbPanel.querySelector('#mu-db-search').value||'').trim().toLowerCase();
            var filtered=rows.filter(function(x){return (x.title+' '+x.sub).toLowerCase().includes(q);});
            dbPanel.querySelector('#mu-db-summary').textContent=filtered.length+' / '+rows.length+' mục · Drop Table ghi nhận trên máy này';
            dbPanel.querySelector('#mu-db-results').innerHTML=filtered.slice(0,250).map(function(x){return '<div style="padding:6px 2px;border-bottom:1px solid #38332a"><b>'+dbText(x.title)+'</b><div style="color:#b9aa8a">'+dbText(x.sub)+'</div></div>';}).join('')||(rows.length?'Không tìm thấy kết quả.':'Chưa ghi nhận món đồ rơi nào. Hãy chơi và quan sát drop trong game.');
            if(filtered.length>250)dbPanel.querySelector('#mu-db-summary').textContent+=' · Hiển thị 250 kết quả đầu';
            dbPanel.querySelector('#mu-db-results').insertAdjacentHTML('beforeend','<div style="color:#e9c978;padding:8px 0">Dữ liệu ghi nhận từ sự kiện drop thực tế: vật phẩm + bản đồ. Chưa xác định được quái gây rơi vì dữ liệu sự kiện không kèm ID quái.</div>');
          }
          dbButton.onclick=function(){dbPanel.style.display=dbPanel.style.display==='block'?'none':'block';renderDb();};
          dbPanel.querySelector('#mu-db-search').oninput=renderDb;
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
