    // 大規模実行画面と1試合実行画面で共通の打順入力フォーム。画面固有スクリプトより先に読み込み、画面側でstartLineupFormを呼び出す。
    const lineup = [
      {hitAverage:'0.35',sluggish:'0.35',buntEnabled:true,stealEnabled:true,personality:'DEFAULT'},
      {hitAverage:'0.35',sluggish:'0.35',buntEnabled:true,stealEnabled:true,personality:'DEFAULT'},
      {hitAverage:'0.35',sluggish:'0.50',buntEnabled:false,stealEnabled:false,personality:'DEFAULT'},
      {hitAverage:'0.35',sluggish:'0.40',buntEnabled:false,stealEnabled:false,personality:'DEFAULT'},
      {hitAverage:'0.35',sluggish:'0.40',buntEnabled:false,stealEnabled:false,personality:'DEFAULT'},
      {hitAverage:'0.33',sluggish:'0.40',buntEnabled:true,stealEnabled:true,personality:'DEFAULT'},
      {hitAverage:'0.29',sluggish:'0.40',buntEnabled:true,stealEnabled:true,personality:'DEFAULT'},
      {hitAverage:'0.28',sluggish:'0.40',buntEnabled:true,stealEnabled:true,personality:'DEFAULT'},
      {hitAverage:'0.27',sluggish:'0.40',buntEnabled:true,stealEnabled:true,personality:'DEFAULT'}
    ];
    let inFlight = false;
    let hasResults = false;
    let lineupFormConfig = null;
    const order = document.querySelector('#order'), submit = document.querySelector('#submit'), feedback = document.querySelector('#feedback'), toggleAllBunt = document.querySelector('#toggle-all-bunt'), toggleAllSteal = document.querySelector('#toggle-all-steal'), resetAllPersonalities = document.querySelector('#reset-all-personalities'), inputView = document.querySelector('#input-view'), resultsView = document.querySelector('#results'), tabInput = document.querySelector('#tab-input'), tabResults = document.querySelector('#tab-results'), editLineup = document.querySelector('#edit-lineup'), resultFeedback = document.querySelector('#result-feedback');
    const ranges = {hitAverage:[0.01,0.6], sluggish:[0.1,0.6]};
    function valid(player) { return Object.values(player).every(value => value !== '') && Object.entries(ranges).every(([key,[min,max]]) => Number(player[key]) >= min && Number(player[key]) <= max); }
    function validLineup() { return lineup.every(valid); }
    const fields = [{key:'hitAverage',label:'出塁率',min:0.01,max:0.6},{key:'sluggish',label:'長打率',min:0.1,max:0.6}];
    function toggle(label, enabled, onClick) { const button=document.createElement('button'); button.type='button'; button.className='bunt-toggle'; button.disabled=inFlight; const caption=document.createElement('span'); caption.className='toggle-label'; caption.textContent=`${label}:`; const state=document.createElement('span'); state.className='toggle-state'; state.textContent=enabled?'する':'しない'; button.append(caption, state); button.setAttribute('aria-label',`${label}: ${enabled?'する':'しない'}`); button.setAttribute('aria-pressed',String(enabled)); button.addEventListener('click',onClick); return button; }
    function formatPercentage(value) { return value === '' ? value : Number(value).toFixed(2); }
    const personalityLabels = {DEFAULT:'標準',EAGER_SLUGGISH:'長距離砲',EAGER_STEAL:'盗塁重視',EAGER_BUNT:'バント重視'};
    function fieldWrapper(caption, control, extraClass) { const wrapper=document.createElement('label'); wrapper.className=extraClass ? `field ${extraClass}` : 'field'; const label=document.createElement('span'); label.className='eyebrow field-caption'; label.textContent=caption; wrapper.append(label, control); return wrapper; }
    function personalitySelect(player) { const select=document.createElement('select'); select.disabled=inFlight; Object.entries(personalityLabels).forEach(([value,label])=>{const option=document.createElement('option');option.value=value;option.textContent=label;option.selected=player.personality===value;select.append(option);}); select.addEventListener('change',()=>{player.personality=select.value;}); return select; }
    function render() { order.replaceChildren(); lineup.forEach((player,index) => { const row=document.createElement('div'); row.className='slot'; const position=document.createElement('span'); position.className='slot-index'; position.textContent=`${index+1}番`; row.append(position); fields.forEach(field => { const input=document.createElement('input'); input.type='number'; input.required=true; input.min=String(field.min); if (field.max !== undefined) input.max=String(field.max); input.step='0.01'; input.value=player[field.key]; input.disabled=inFlight || (field.enabledKey && !player[field.enabledKey]); input.addEventListener('input',()=>{const value=input.value.startsWith('.') ? `0${input.value}` : input.value;input.value=value;player[field.key]=value;update();}); input.addEventListener('change',()=>{const value=formatPercentage(input.value);input.value=value;player[field.key]=value;update();}); row.append(fieldWrapper(field.label, input)); }); row.append(fieldWrapper('性格', personalitySelect(player), 'field-personality'),toggle('バント',player.buntEnabled,()=>{player.buntEnabled=!player.buntEnabled; render();}),toggle('盗塁',player.stealEnabled,()=>{player.stealEnabled=!player.stealEnabled; render();})); order.append(row); }); update(); }
    function update() { const complete=validLineup(); const allBuntEnabled=lineup.every(player=>player.buntEnabled); const allStealEnabled=lineup.every(player=>player.stealEnabled); submit.disabled=inFlight || !complete; toggleAllBunt.disabled=inFlight; toggleAllBunt.setAttribute('aria-pressed',String(allBuntEnabled)); toggleAllSteal.disabled=inFlight; toggleAllSteal.setAttribute('aria-pressed',String(allStealEnabled)); resetAllPersonalities.disabled=inFlight; tabInput.disabled=inFlight; tabResults.disabled=inFlight || !hasResults; editLineup.disabled=inFlight; if (!inFlight) { feedback.className=''; feedback.textContent=complete?lineupFormConfig.readyMessage:'出塁率は0.01〜0.6、長打率は0.1〜0.6の範囲ですべての項目を入力してください。'; } }
    function showView(resultsVisible) { inputView.hidden=resultsVisible; resultsView.hidden=!resultsVisible; tabInput.setAttribute('aria-selected',String(!resultsVisible)); tabResults.setAttribute('aria-selected',String(resultsVisible)); update(); window.scrollTo({top:0}); }
    // config: readyMessage(入力完了時の案内), runningMessage(実行中の案内), endpoint(打順の送信先), onSuccess(成功時の応答JSONを描画する関数)
    function startLineupForm(config) {
      lineupFormConfig=config;
      tabInput.addEventListener('click',()=>showView(false));
      tabResults.addEventListener('click',()=>showView(true));
      editLineup.addEventListener('click',()=>showView(false));
      toggleAllBunt.addEventListener('click',()=>{const enabled=!lineup.every(player=>player.buntEnabled);lineup.forEach(player=>{player.buntEnabled=enabled;});render();});
      toggleAllSteal.addEventListener('click',()=>{const enabled=!lineup.every(player=>player.stealEnabled);lineup.forEach(player=>{player.stealEnabled=enabled;});render();});
      resetAllPersonalities.addEventListener('click',()=>{lineup.forEach(player=>{player.personality='DEFAULT';});render();});
      submit.addEventListener('click',async()=>{if(inFlight||!validLineup())return; inFlight=true;render();feedback.className='';feedback.textContent=lineupFormConfig.runningMessage;try {const response=await fetch(lineupFormConfig.endpoint,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(lineup.map(player=>({hit_average:Number(player.hitAverage),sluggish:Number(player.sluggish),bunt_enabled:player.buntEnabled,steal_enabled:player.stealEnabled,personality:player.personality})))});const data=await response.json();if(!response.ok)throw new Error(data.error||data.message||'リクエストに失敗しました。');lineupFormConfig.onSuccess(data);hasResults=true;showView(true);}catch(error){feedback.className='error';feedback.textContent=error.message;}finally{inFlight=false;render();}});
      render();
    }
