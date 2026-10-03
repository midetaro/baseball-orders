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
    const order = document.querySelector('#order'), submit = document.querySelector('#submit'), feedback = document.querySelector('#feedback'), toggleAllBunt = document.querySelector('#toggle-all-bunt'), toggleAllSteal = document.querySelector('#toggle-all-steal'), resetAllPersonalities = document.querySelector('#reset-all-personalities'), inputView = document.querySelector('#input-view'), resultsView = document.querySelector('#results'), tabInput = document.querySelector('#tab-input'), tabResults = document.querySelector('#tab-results'), editLineup = document.querySelector('#edit-lineup'), resultFeedback = document.querySelector('#result-feedback'), frameStage = document.querySelector('#frame-stage'), lineScore = document.querySelector('#line-score'), orderTableScroll = document.querySelector('#order-table-scroll');
    const frameDurationMillis = Number(frameStage.dataset.frameDurationMillis) || 1000;
    const frameDurations = {none:frameDurationMillis, hit:Number(frameStage.dataset.hitFrameDurationMillis) || frameDurationMillis, score:Number(frameStage.dataset.scoreFrameDurationMillis) || frameDurationMillis, 'home-run':Number(frameStage.dataset.homeRunFrameDurationMillis) || frameDurationMillis};
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
    function update() { const complete=validLineup(); const allBuntEnabled=lineup.every(player=>player.buntEnabled); const allStealEnabled=lineup.every(player=>player.stealEnabled); submit.disabled=inFlight || !complete; toggleAllBunt.disabled=inFlight; toggleAllBunt.setAttribute('aria-pressed',String(allBuntEnabled)); toggleAllSteal.disabled=inFlight; toggleAllSteal.setAttribute('aria-pressed',String(allStealEnabled)); resetAllPersonalities.disabled=inFlight; tabInput.disabled=inFlight; tabResults.disabled=inFlight || !hasResults; editLineup.disabled=inFlight; if (!inFlight) { feedback.className=''; feedback.textContent=complete?'準備完了。1試合を実行できます。':'出塁率は0.01〜0.6、長打率は0.1〜0.6の範囲ですべての項目を入力してください。'; } }
    function showView(resultsVisible) { inputView.hidden=resultsVisible; resultsView.hidden=!resultsVisible; tabInput.setAttribute('aria-selected',String(!resultsVisible)); tabResults.setAttribute('aria-selected',String(resultsVisible)); update(); window.scrollTo({top:0}); }
    tabInput.addEventListener('click',()=>showView(false));
    tabResults.addEventListener('click',()=>showView(true));
    editLineup.addEventListener('click',()=>showView(false));
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
    const HIT_BASES = {'単打':1,'二塁打':2,'三塁打':3,'本塁打':4};
    const HIT_HEADLINES = {1:'ヒット!',2:'ツーベース!',3:'スリーベース!'};
    const BALL_DIRECTIONS = ['left','center','right'];
    const FIREWORK_BURSTS = 3, FIREWORK_SPARKS = 12;
    function classifyEffect(transition, previousScore) { const runs=transition.cumulativeScore-previousScore; const bases=HIT_BASES[transition.actionResult] ?? 0; if (bases===4) return {kind:'home-run',runs,bases}; if (runs>0) return {kind:'score',runs,bases}; if (bases>0) return {kind:'hit',runs:0,bases}; return {kind:'none',runs:0,bases:0}; }
    // 推移のアウト・走者・得点はプレー直前の状況で記録されるため、次の推移からプレー直後の状況を求める。
    function resolvePlayOutcomes(gameTransitions) { return gameTransitions.map((transition,index)=>{ const next=gameTransitions[index+1]; const sameInning=next !== undefined && next.inning === transition.inning; return {...transition, scoreBefore:transition.cumulativeScore, cumulativeScore:next === undefined ? transition.cumulativeScore : next.cumulativeScore, outCount:sameInning ? next.outCount : 3, runnerState:sameInning ? next.runnerState : transition.runnerState}; }); }
    function annotateEffects(annotated) { return annotated.map((transition,index)=>({...transition,effect:classifyEffect(transition,transition.scoreBefore),direction:BALL_DIRECTIONS[index % BALL_DIRECTIONS.length]})); }
    const headlines = {'home-run':effect=>effect.runs===4?'GRAND SLAM!':'HOME RUN!', score:effect=>effect.bases>0?'タイムリー!':'得点!', hit:effect=>HIT_HEADLINES[effect.bases], none:()=>''};
    function element(tag, className, text) { const node=document.createElement(tag); node.className=className; if (text !== undefined) node.textContent=text; return node; }
    function buildFireworks() { return Array.from({length:FIREWORK_BURSTS},(_,burstIndex)=>{ const burst=element('span',`firework firework-${burstIndex}`); for (let sparkIndex=0; sparkIndex < FIREWORK_SPARKS; sparkIndex+=1) { const spark=element('span','spark'); spark.style.setProperty('--angle',`${sparkIndex * 360 / FIREWORK_SPARKS}deg`); burst.append(spark); } return burst; }); }
    function buildFrame(transition) {
      const layout = RUNNER_LAYOUT[transition.runnerState] ?? {first:false,second:false,third:false};
      const effect = transition.effect;
      const frame=document.createElement('div'); frame.className=`frame effect-${effect.kind}`;
      const meta=element('div','frame-meta scoreboard');
      const outs='●'.repeat(transition.outCount)+'○'.repeat(3-transition.outCount);
      [`${transition.inning}回`,`アウト ${outs}`,`得点 `].forEach((text,index)=>{ const span=document.createElement('span'); span.textContent=text; if(index===1) span.className='out-count'; if(index===2) span.append(element('strong','score-value',String(transition.cumulativeScore))); meta.append(span); });
      const banner=document.createElement('p'); banner.className='frame-banner'; banner.textContent=transition.actionResult;
      const diamond=document.createElement('div'); diamond.className='diamond ballpark';
      diamond.append(element('span','infield'),element('span','home-plate'));
      [['second',layout.second],['first',layout.first],['third',layout.third]].forEach(([base,occupied])=>{ const marker=document.createElement('span'); marker.className=`base base-${base}${occupied?' occupied':''}`; diamond.append(marker); });
      const batter=document.createElement('span'); batter.className='batter-order'; batter.textContent=`${transition.battingOrder}番打者`; diamond.append(batter);
      if (effect.bases > 0) diamond.append(element('span',`ball ball-${transition.direction}`),element('span',`runner runner-${effect.bases}`));
      if (effect.kind === 'score') diamond.append(element('span','runner runner-home'));
      if (effect.kind === 'home-run') diamond.append(...buildFireworks());
      if (effect.runs > 0) diamond.append(element('span','score-burst',`+${effect.runs}点`));
      frame.append(meta,banner,diamond);
      const headline=headlines[effect.kind](effect);
      if (headline) frame.append(element('p',`effect-headline headline-${effect.kind}`,headline));
      if (effect.kind === 'home-run') frame.append(element('span','flash'));
      return frame;
    }
    function stopFramePlayback() { if (frameTimer !== null) { clearTimeout(frameTimer); frameTimer=null; } }
    function playFrames(annotated) {
      stopFramePlayback();
      let index=0;
      const show=()=>{ const transition=annotated[index]; const {effect}=transition; frameStage.replaceChildren(buildFrame(transition)); renderLineScore(annotated, index + 1); index+=1; if (index >= annotated.length) { frameTimer=null; return; } frameTimer=setTimeout(show, frameDurations[effect.kind]); };
      show();
    }
    const REGULATION_INNINGS = 9;
    // シミュレーターは失策を扱わないため、失策数(E)は常に0になる。
    function summarizeLineScore(played) { const innings=new Map(); let runs=0, hits=0; for (const transition of played) { innings.set(transition.inning,(innings.get(transition.inning) ?? 0)+transition.effect.runs); runs+=transition.effect.runs; if (transition.effect.bases > 0) hits+=1; } return {innings,runs,hits,errors:0}; }
    function buildLineScore(summary, inningCount, currentInning) {
      const table=element('table','line-score'); table.setAttribute('aria-label','スコアボード');
      const headRow=document.createElement('tr'); const bodyRow=document.createElement('tr');
      headRow.append(element('th','line-score-team')); const team=element('th','line-score-team','自チーム'); team.scope='row'; bodyRow.append(team);
      for (let inning=1; inning <= inningCount; inning+=1) {
        const th=element('th','',String(inning)); th.scope='col'; headRow.append(th);
        const runs=summary.innings.get(inning);
        bodyRow.append(element('td',inning === currentInning ? 'is-current' : '',runs === undefined ? '' : String(runs)));
      }
      const totals=[summary.runs,summary.hits,summary.errors];
      ['R','H','E'].forEach((label,index)=>{ const th=element('th','line-score-total',label); th.scope='col'; headRow.append(th); bodyRow.append(element('td','line-score-total',String(totals[index]))); });
      const thead=document.createElement('thead'); thead.append(headRow); const tbody=document.createElement('tbody'); tbody.append(bodyRow);
      table.append(thead,tbody);
      return table;
    }
    function renderLineScore(annotated, playedCount) {
      const played=annotated.slice(0, playedCount);
      const inningCount=Math.max(REGULATION_INNINGS, annotated[annotated.length - 1].inning);
      lineScore.replaceChildren(buildLineScore(summarizeLineScore(played), inningCount, played[played.length - 1].inning));
    }
    function buildOrderTable(annotated) {
      const plateAppearances = annotated.filter(t=>!t.actionResult.startsWith('盗塁'));
      const innings = [...new Set(plateAppearances.map(t=>t.inning))].sort((a,b)=>a-b);
      const results = new Map(plateAppearances.map(t=>[`${t.battingOrder}-${t.inning}`, t]));
      const table=document.createElement('table'); table.className='order-table';
      const thead=document.createElement('thead'); const headRow=document.createElement('tr');
      const corner=document.createElement('th'); corner.textContent='打順'; headRow.append(corner);
      innings.forEach(inning=>{ const th=document.createElement('th'); th.scope='col'; th.textContent=`${inning}回`; headRow.append(th); });
      thead.append(headRow);
      const tbody=document.createElement('tbody');
      for (let battingOrder=1; battingOrder <= 9; battingOrder+=1) {
        const row=document.createElement('tr');
        const label=document.createElement('th'); label.scope='row'; label.textContent=`${battingOrder}番`; row.append(label);
        innings.forEach(inning=>{ const cell=document.createElement('td'); const result=results.get(`${battingOrder}-${inning}`); if (result) { cell.textContent=result.actionResult; cell.className=`cell-${result.effect.kind}`; } row.append(cell); });
        tbody.append(row);
      }
      table.append(thead,tbody);
      return table;
    }
    function renderGame(gameTransitions) {
      stopFramePlayback();
      if (!Array.isArray(gameTransitions) || gameTransitions.length === 0) {
        frameStage.replaceChildren(placeholder('試合結果はありません。'));
        lineScore.replaceChildren(placeholder('試合結果はありません。'));
        orderTableScroll.replaceChildren(placeholder('試合結果はありません。'));
        return;
      }
      const annotated = annotateEffects(annotateBattingOrder(resolvePlayOutcomes(gameTransitions)));
      playFrames(annotated);
      orderTableScroll.replaceChildren(buildOrderTable(annotated));
    }
    toggleAllBunt.addEventListener('click',()=>{const enabled=!lineup.every(player=>player.buntEnabled);lineup.forEach(player=>{player.buntEnabled=enabled;});render();});
    toggleAllSteal.addEventListener('click',()=>{const enabled=!lineup.every(player=>player.stealEnabled);lineup.forEach(player=>{player.stealEnabled=enabled;});render();});
    resetAllPersonalities.addEventListener('click',()=>{lineup.forEach(player=>{player.personality='DEFAULT';});render();});
    submit.addEventListener('click',async()=>{if(inFlight||!validLineup())return; inFlight=true;render();feedback.className='';feedback.textContent='試合を実行中…';try {const response=await fetch('/simulations/single-game',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(lineup.map(player=>({hit_average:Number(player.hitAverage),sluggish:Number(player.sluggish),bunt_enabled:player.buntEnabled,steal_enabled:player.stealEnabled,personality:player.personality})))});const data=await response.json();if(!response.ok)throw new Error(data.error||data.message||'リクエストに失敗しました。');resultFeedback.className='hint success';resultFeedback.textContent='試合が終了しました。';renderGame(data.transitions);hasResults=true;showView(true);}catch(error){feedback.className='error';feedback.textContent=error.message;}finally{inFlight=false;render();}});
    render();
