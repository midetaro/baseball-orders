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
    const order = document.querySelector('#order'), submit = document.querySelector('#submit'), feedback = document.querySelector('#feedback'), toggleAllBunt = document.querySelector('#toggle-all-bunt'), toggleAllSteal = document.querySelector('#toggle-all-steal'), resetAllPersonalities = document.querySelector('#reset-all-personalities'), toggleLineup = document.querySelector('#toggle-lineup'), lineupBody = document.querySelector('#lineup-body'), frameStage = document.querySelector('#frame-stage'), orderTableScroll = document.querySelector('#order-table-scroll');
    const frameDurationMillis = Number(frameStage.dataset.frameDurationMillis) || 1000;
    let frameTimer = null;
    const ranges = {hitAverage:[0.01,0.6], sluggish:[0.1,0.6]};
    function valid(player) { return Object.values(player).every(value => value !== '') && Object.entries(ranges).every(([key,[min,max]]) => Number(player[key]) >= min && Number(player[key]) <= max); }
    function validLineup() { return lineup.every(valid); }
    const fields = [{key:'hitAverage',label:'出塁率',min:0.01,max:0.6},{key:'sluggish',label:'長打率',min:0.1,max:0.6}];
    function toggle(label, enabled, onClick) { const button=document.createElement('button'); button.type='button'; button.className='bunt-toggle'; button.disabled=inFlight; const caption=document.createElement('span'); caption.className='toggle-label'; caption.textContent=`${label}:`; const state=document.createElement('span'); state.className='toggle-state'; state.textContent=enabled?'する':'しない'; button.append(caption, state); button.setAttribute('aria-label',`${label}: ${enabled?'する':'しない'}`); button.setAttribute('aria-pressed',String(enabled)); button.addEventListener('click',onClick); return button; }
    function formatPercentage(value) { return value === '' ? value : Number(value).toFixed(2); }
    const personalityLabels = {DEFAULT:'標準',EAGER_SLUGGISH:'長距離砲',EAGER_STEAL:'盗塁重視',EAGER_BUNT:'バント重視'};
    function fieldWrapper(caption, control, extraClass) { const wrapper=document.createElement('label'); wrapper.className=extraClass ? `field ${extraClass}` : 'field'; const label=document.createElement('span'); label.className='eyebrow field-caption'; label.textContent=caption; wrapper.append(label, control); return wrapper; }
    function personalitySelect(player) { const select=document.createElement('select'); select.disabled=inFlight; Object.entries(personalityLabels).forEach(([value,label])=>{const option=document.createElement('option');option.value=value;option.textContent=label;option.selected=player.personality===value;select.append(option);}); select.addEventListener('change',()=>{player.personality=select.value;}); return select; }
    function render() { order.replaceChildren(); lineup.forEach((player,index) => { const row=document.createElement('div'); row.className='slot'; const position=document.createElement('span'); position.className='slot-index'; position.textContent=`${index+1}番`; row.append(position); fields.forEach(field => { const input=document.createElement('input'); input.type='number'; input.required=true; input.min=String(field.min); if (field.max !== undefined) input.max=String(field.max); input.step='0.01'; input.value=player[field.key]; input.disabled=inFlight || (field.enabledKey && !player[field.enabledKey]); input.addEventListener('input',()=>{const value=input.value.startsWith('.') ? `0${input.value}` : input.value;input.value=value;player[field.key]=value;update();}); input.addEventListener('change',()=>{const value=formatPercentage(input.value);input.value=value;player[field.key]=value;update();}); row.append(fieldWrapper(field.label, input)); }); row.append(fieldWrapper('性格', personalitySelect(player)),toggle('バント',player.buntEnabled,()=>{player.buntEnabled=!player.buntEnabled; render();}),toggle('盗塁',player.stealEnabled,()=>{player.stealEnabled=!player.stealEnabled; render();})); order.append(row); }); update(); }
    function update() { const complete=validLineup(); const allBuntEnabled=lineup.every(player=>player.buntEnabled); const allStealEnabled=lineup.every(player=>player.stealEnabled); submit.disabled=inFlight || !complete || lineupBody.hidden; toggleAllBunt.disabled=inFlight; toggleAllBunt.setAttribute('aria-pressed',String(allBuntEnabled)); toggleAllSteal.disabled=inFlight; toggleAllSteal.setAttribute('aria-pressed',String(allStealEnabled)); resetAllPersonalities.disabled=inFlight; toggleLineup.disabled=inFlight; if (!inFlight) { feedback.className=''; feedback.textContent=lineupBody.hidden?'打順入力を開くと再実行できます。':complete?'準備完了。1試合を実行できます。':'出塁率は0.01〜0.6、長打率は0.1〜0.6の範囲ですべての項目を入力してください。'; } }
    function setLineupCollapsed(collapsed) { lineupBody.hidden=collapsed; toggleLineup.setAttribute('aria-expanded',String(!collapsed)); toggleLineup.textContent=collapsed?'打順入力を開く':'入力欄を閉じる'; update(); }
    toggleLineup.addEventListener('click',()=>setLineupCollapsed(!lineupBody.hidden));
    const RUNNER_LAYOUT = {
      '走者なし':{first:false,second:false,third:false},
      '一塁':{first:true,second:false,third:false},
      '二塁':{first:false,second:true,third:false},
      '一・二塁':{first:true,second:true,third:false},
      '三塁':{first:false,second:false,third:true},
      '一・三塁':{first:true,second:false,third:true},
      '二・三塁':{first:false,second:true,third:true},
      '満塁':{first:true,second:true,third:true}
    };
    function placeholder(text) { const p=document.createElement('p'); p.className='empty-chart-state'; p.textContent=text; return p; }
    function annotateBattingOrder(gameTransitions) { let battingOrder=1; return gameTransitions.map(transition=>{ const annotated={...transition,battingOrder}; if (!transition.actionResult.startsWith('盗塁')) { battingOrder = battingOrder === 9 ? 1 : battingOrder + 1; } return annotated; }); }
    function buildFrame(transition) {
      const layout = RUNNER_LAYOUT[transition.runnerState] ?? {first:false,second:false,third:false};
      const frame=document.createElement('div'); frame.className='frame';
      const banner=document.createElement('p'); banner.className='frame-banner'; banner.textContent=transition.actionResult;
      const diamond=document.createElement('div'); diamond.className='diamond';
      [['second',layout.second],['first',layout.first],['third',layout.third]].forEach(([base,occupied])=>{ const marker=document.createElement('span'); marker.className=`base base-${base}${occupied?' occupied':''}`; diamond.append(marker); });
      const batter=document.createElement('span'); batter.className='batter-order'; batter.textContent=`${transition.battingOrder}番打者`; diamond.append(batter);
      const meta=document.createElement('div'); meta.className='frame-meta';
      const outs='●'.repeat(transition.outCount)+'○'.repeat(3-transition.outCount);
      [`${transition.inning}回`,`アウト ${outs}`,`得点 ${transition.cumulativeScore}`].forEach((text,index)=>{ const span=document.createElement('span'); span.textContent=text; if(index===1) span.className='out-count'; meta.append(span); });
      frame.append(banner,diamond,meta);
      return frame;
    }
    function stopFramePlayback() { if (frameTimer !== null) { clearInterval(frameTimer); frameTimer=null; } }
    function playFrames(annotated) {
      stopFramePlayback();
      let index=0;
      frameStage.replaceChildren(buildFrame(annotated[index]));
      frameTimer=setInterval(()=>{ index+=1; if (index >= annotated.length) { stopFramePlayback(); return; } frameStage.replaceChildren(buildFrame(annotated[index])); }, frameDurationMillis);
    }
    function buildOrderTable(annotated) {
      const plateAppearances = annotated.filter(t=>!t.actionResult.startsWith('盗塁'));
      const innings = [...new Set(plateAppearances.map(t=>t.inning))].sort((a,b)=>a-b);
      const results = new Map(plateAppearances.map(t=>[`${t.battingOrder}-${t.inning}`, t.actionResult]));
      const table=document.createElement('table'); table.className='order-table';
      const thead=document.createElement('thead'); const headRow=document.createElement('tr');
      const corner=document.createElement('th'); corner.textContent='打順'; headRow.append(corner);
      innings.forEach(inning=>{ const th=document.createElement('th'); th.scope='col'; th.textContent=`${inning}回`; headRow.append(th); });
      thead.append(headRow);
      const tbody=document.createElement('tbody');
      for (let battingOrder=1; battingOrder <= 9; battingOrder+=1) {
        const row=document.createElement('tr');
        const label=document.createElement('th'); label.scope='row'; label.textContent=`${battingOrder}番`; row.append(label);
        innings.forEach(inning=>{ const cell=document.createElement('td'); cell.textContent=results.get(`${battingOrder}-${inning}`) ?? ''; row.append(cell); });
        tbody.append(row);
      }
      table.append(thead,tbody);
      return table;
    }
    function renderGame(gameTransitions) {
      stopFramePlayback();
      if (!Array.isArray(gameTransitions) || gameTransitions.length === 0) {
        frameStage.replaceChildren(placeholder('試合結果はありません。'));
        orderTableScroll.replaceChildren(placeholder('試合結果はありません。'));
        return;
      }
      const annotated = annotateBattingOrder(gameTransitions);
      playFrames(annotated);
      orderTableScroll.replaceChildren(buildOrderTable(annotated));
    }
    toggleAllBunt.addEventListener('click',()=>{const enabled=!lineup.every(player=>player.buntEnabled);lineup.forEach(player=>{player.buntEnabled=enabled;});render();});
    toggleAllSteal.addEventListener('click',()=>{const enabled=!lineup.every(player=>player.stealEnabled);lineup.forEach(player=>{player.stealEnabled=enabled;});render();});
    resetAllPersonalities.addEventListener('click',()=>{lineup.forEach(player=>{player.personality='DEFAULT';});render();});
    submit.addEventListener('click',async()=>{if(inFlight||!validLineup())return; inFlight=true;render();feedback.className='';feedback.textContent='試合を実行中…';try {const response=await fetch('/simulations/single-game',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(lineup.map(player=>({hit_average:Number(player.hitAverage),sluggish:Number(player.sluggish),bunt_enabled:player.buntEnabled,steal_enabled:player.stealEnabled,personality:player.personality})))});const data=await response.json();if(!response.ok)throw new Error(data.error||data.message||'リクエストに失敗しました。');renderGame(data.transitions);setLineupCollapsed(true);feedback.className='success';feedback.textContent='試合が終了しました。';}catch(error){feedback.className='error';feedback.textContent=error.message;}finally{inFlight=false;render();}});
    render();
