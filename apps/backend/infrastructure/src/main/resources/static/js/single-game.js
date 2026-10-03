    const frameStage = document.querySelector('#frame-stage'), lineScore = document.querySelector('#line-score'), orderTableScroll = document.querySelector('#order-table-scroll');
    const frameDurationMillis = Number(frameStage.dataset.frameDurationMillis) || 1000;
    const frameDurations = {none:frameDurationMillis, hit:Number(frameStage.dataset.hitFrameDurationMillis) || frameDurationMillis, score:Number(frameStage.dataset.scoreFrameDurationMillis) || frameDurationMillis, 'home-run':Number(frameStage.dataset.homeRunFrameDurationMillis) || frameDurationMillis, bunt:Number(frameStage.dataset.buntFrameDurationMillis) || frameDurationMillis};
    let frameTimer = null;
    // 再生速度は「普通」を基準に2倍ずつ変える。値はフレーム表示時間に掛ける倍率。
    const PLAYBACK_SPEED_MULTIPLIERS = {slow:2, normal:1, fast:0.5};
    let playbackSpeed = 'normal';
    const speedOptions = [...document.querySelectorAll('.speed-option')];
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
    function classifyEffect(transition, previousScore) { const runs=transition.cumulativeScore-previousScore; const bases=HIT_BASES[transition.actionResult] ?? 0; if (bases===4) return {kind:'home-run',runs,bases}; if (runs>0) return {kind:'score',runs,bases}; if (bases>0) return {kind:'hit',runs:0,bases}; if (transition.actionResult==='バント成功') return {kind:'bunt',runs:0,bases:0}; return {kind:'none',runs:0,bases:0}; }
    // 推移のアウト・走者・得点はプレー直前の状況で記録されるため、次の推移からプレー直後の状況を求める。
    function resolvePlayOutcomes(gameTransitions) { return gameTransitions.map((transition,index)=>{ const next=gameTransitions[index+1]; const sameInning=next !== undefined && next.inning === transition.inning; return {...transition, scoreBefore:transition.cumulativeScore, cumulativeScore:next === undefined ? transition.cumulativeScore : next.cumulativeScore, outCount:sameInning ? next.outCount : 3, runnerState:sameInning ? next.runnerState : transition.runnerState}; }); }
    function annotateEffects(annotated) { return annotated.map((transition,index)=>({...transition,effect:classifyEffect(transition,transition.scoreBefore),direction:BALL_DIRECTIONS[index % BALL_DIRECTIONS.length]})); }
    const headlines = {'home-run':effect=>effect.runs===4?'GRAND SLAM!':'HOME RUN!', score:effect=>effect.bases>0?'タイムリー!':'得点!', hit:effect=>HIT_HEADLINES[effect.bases], bunt:()=>'バント成功!', none:()=>''};
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
      if (effect.kind === 'bunt') diamond.append(element('span','ball ball-bunt'));
      if (effect.kind === 'score') diamond.append(element('span','runner runner-home'));
      if (effect.kind === 'home-run') diamond.append(...buildFireworks());
      if (effect.runs > 0) diamond.append(element('span','score-burst',`+${effect.runs}点`));
      frame.append(meta,banner,diamond);
      const headline=headlines[effect.kind](effect);
      if (headline) frame.append(element('p',`effect-headline headline-${effect.kind}`,headline));
      if (effect.kind === 'home-run') frame.append(element('span','flash'));
      return frame;
    }
    function applyPlaybackRate(frame) { frame.getAnimations({subtree:true}).forEach(animation=>{ animation.playbackRate=1 / PLAYBACK_SPEED_MULTIPLIERS[playbackSpeed]; }); }
    function selectPlaybackSpeed(speed) { playbackSpeed=speed; speedOptions.forEach(option=>option.setAttribute('aria-pressed',String(option.dataset.speed === playbackSpeed))); const frame=frameStage.querySelector('.frame'); if (frame) applyPlaybackRate(frame); }
    speedOptions.forEach(option=>option.addEventListener('click',()=>selectPlaybackSpeed(option.dataset.speed)));
    function stopFramePlayback() { if (frameTimer !== null) { clearTimeout(frameTimer); frameTimer=null; } }
    function playFrames(annotated) {
      stopFramePlayback();
      let index=0;
      const show=()=>{ const transition=annotated[index]; const {effect}=transition; const frame=buildFrame(transition); frameStage.replaceChildren(frame); applyPlaybackRate(frame); renderLineScore(annotated, transition.inning); index+=1; const delay=frameDurations[effect.kind] * PLAYBACK_SPEED_MULTIPLIERS[playbackSpeed]; if (index >= annotated.length) { frameTimer=setTimeout(()=>{ frameTimer=null; renderLineScore(annotated, null); }, delay); return; } frameTimer=setTimeout(show, delay); };
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
        const th=element('th',inning === currentInning ? 'is-current' : '',String(inning)); th.scope='col'; headRow.append(th);
        const runs=summary.innings.get(inning);
        bodyRow.append(element('td',inning === currentInning ? 'is-current' : '',runs === undefined ? '' : String(runs)));
      }
      const totals=[summary.runs,summary.hits,summary.errors];
      ['R','H','E'].forEach((label,index)=>{ const th=element('th','line-score-total',label); th.scope='col'; headRow.append(th); bodyRow.append(element('td','line-score-total',String(totals[index]))); });
      const thead=document.createElement('thead'); thead.append(headRow); const tbody=document.createElement('tbody'); tbody.append(bodyRow);
      table.append(thead,tbody);
      return table;
    }
    // スコアボードは再生前から試合全体の結果を表示し、再生中のイニングだけを強調する。
    function renderLineScore(annotated, currentInning) { lineScore.replaceChildren(buildLineScore(summarizeLineScore(annotated), Math.max(REGULATION_INNINGS, annotated[annotated.length - 1].inning), currentInning)); }
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
    startLineupForm({readyMessage:'準備完了。1試合を実行できます。',runningMessage:'試合を実行中…',endpoint:'/simulations/single-game',onSuccess:data=>{resultFeedback.className='hint success';resultFeedback.textContent='試合が終了しました。';renderGame(data.transitions);}});
