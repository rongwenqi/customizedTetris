import{Game,Clock,Records,size,cell}from'./game.mjs';
const $=id=>document.getElementById(id),game=new Game(),clock=new Clock(),records=new Records();
const KEY='wenqi-blocks-web-v1',colors=['','#32b7d7','#648bf2','#ee9b41','#49bc82','#ed6d86','#a87be0','#e6bd40'];
let started=false,dark=true,storageOK=true,toastTimer,installPrompt=null,lastAutoSave=0;
const holds=new Map();
function toast(message){$('toast').textContent=message;$('toast').classList.add('visible');clearTimeout(toastTimer);toastTimer=setTimeout(()=>$('toast').classList.remove('visible'),3500);}
try{const s=JSON.parse(localStorage.getItem(KEY)||'null');if(s&&s.version===1){dark=s.dark!==false;if(Number.isFinite(s.speed))clock.setInterval(s.speed);records.restore(s.records);if(game.restore(s.game)){started=s.started===true;if(started)records.startRound();}else{records.accountedLines=0;records.roundCounted=false;}}}catch{storageOK=false;}
function save(){records.sync(game);try{localStorage.setItem(KEY,JSON.stringify({version:1,game:game.snapshot(),records,started,dark,speed:clock.interval}));}catch{if(storageOK)toast('浏览器暂时无法保存数据，请检查存储空间。');storageOK=false;}}
function theme(){document.documentElement.dataset.theme=dark?'dark':'light';document.querySelector('meta[name=theme-color]').content=dark?'#0b121f':'#f0f5fa';$('theme').setAttribute('aria-label',dark?'切换亮色模式':'切换暗黑模式');}
theme();
function release(){for(const hold of holds.values())hold.button?.classList.remove('held');holds.clear();}
function pause(){clock.pause();release();save();render();}
function resume(){if(document.querySelector('dialog[open]')||clock.end)return;const continuation=started&&!game.over;if(game.over)records.newRound(game);records.startRound();started=true;clock.start(performance.now(),continuation);save();render();}
function afterStep(locked){records.sync(game);if(locked||game.over)save();}
function action(name){if(!clock.active||game.over||document.querySelector('dialog[open]'))return;if(name==='left')game.move(-1);if(name==='right')game.move(1);if(name==='rotate')game.rotate();if(name==='soft'){afterStep(game.softDrop());clock.last=performance.now();}if(name==='hard'){afterStep(game.hardDrop());clock.last=performance.now();}render();}
function openDialog(id){pause();$(id).returnValue='cancel';$(id).showModal();}
$('restart').addEventListener('click',()=>openDialog('restart-dialog'));
$('restart-dialog').addEventListener('close',()=>{if($('restart-dialog').returnValue==='restart'){records.newRound(game);records.startRound();started=true;clock.start(performance.now());save();render();}});
$('records').addEventListener('click',()=>{openDialog('records-dialog');$('record-best').textContent=records.best;$('record-lines').textContent=records.totalLines;$('record-games').textContent=records.games;});
$('theme').addEventListener('click',()=>{dark=!dark;theme();save();render();});
$('settings').addEventListener('click',()=>{openDialog('settings-dialog');$('speed-slider').value=clock.interval/100;sliderLabel();});
function sliderLabel(){$('slider-value').textContent=`${(Number($('speed-slider').value)/10).toFixed(1)} 秒 / 格`;}
$('speed-slider').addEventListener('input',sliderLabel);
$('settings-dialog').addEventListener('close',()=>{if($('settings-dialog').returnValue==='save'){clock.setInterval(Number($('speed-slider').value)*100);save();render();}});
$('pause').addEventListener('click',()=>{if(game.over)resume();else if(clock.active||clock.end)pause();else resume();});
$('overlay').addEventListener('click',()=>{if(!clock.end)resume();});
$('board').addEventListener('pointerdown',e=>{e.preventDefault();action('rotate');});
for(const button of document.querySelectorAll('[data-action]')){
 button.addEventListener('contextmenu',e=>e.preventDefault());
 button.addEventListener('pointerdown',e=>{e.preventDefault();if(!clock.active||game.over)return;button.setPointerCapture(e.pointerId);const name=button.dataset.action;action(name);button.classList.add('held');holds.set(e.pointerId,{name,button,next:performance.now()+(name==='soft'?50:250)});});
 const end=e=>{const hold=holds.get(e.pointerId);hold?.button.classList.remove('held');holds.delete(e.pointerId);};
 button.addEventListener('pointerup',end);button.addEventListener('pointercancel',end);button.addEventListener('lostpointercapture',end);
 button.addEventListener('pointermove',e=>{const r=button.getBoundingClientRect();if(e.clientX<r.left||e.clientX>r.right||e.clientY<r.top||e.clientY>r.bottom)end(e);});
 // A keyboard or screen reader dispatches clicks without pointer events.
 button.addEventListener('click',e=>{if(e.detail===0)action(button.dataset.action);});
}
document.addEventListener('keydown',e=>{if(document.querySelector('dialog[open]'))return;const name={ArrowLeft:'left',ArrowRight:'right',ArrowUp:'rotate',ArrowDown:'soft',' ':'hard'}[e.key];if(name){e.preventDefault();action(name);}else if(e.key==='Escape'||e.key.toLowerCase()==='p'){e.preventDefault();if(clock.active||clock.end)pause();else resume();}});
document.addEventListener('visibilitychange',()=>{if(document.hidden)pause();});window.addEventListener('pagehide',pause);window.addEventListener('blur',()=>{if(clock.active||clock.end)pause();});
const board=$('board'),ctx=board.getContext('2d'),nextCtx=$('next').getContext('2d');let width=0,height=0,dpr=1;
new ResizeObserver(()=>{const r=board.getBoundingClientRect();dpr=Math.min(window.devicePixelRatio||1,3);width=r.width;height=r.height;board.width=Math.round(width*dpr);board.height=Math.round(height*dpr);render();}).observe(board);
function tile(c,x,y,u,color,ghost=false){c.save();c.globalAlpha=ghost?.23:1;c.fillStyle=color;c.beginPath();c.roundRect(x+1,y+1,Math.max(1,u-2),Math.max(1,u-2),Math.min(3,u/6));c.fill();c.globalAlpha=ghost?.7:.25;if(ghost){c.strokeStyle=color;c.lineWidth=1;c.stroke();}else{c.fillStyle='#fff';c.fillRect(x+4,y+3,Math.max(1,u-8),2);}c.restore();}
let css={};function draw(){if(!width)return;const style=getComputedStyle(document.documentElement);css={board:style.getPropertyValue('--board'),grid:style.getPropertyValue('--grid')};ctx.setTransform(dpr,0,0,dpr,0,0);ctx.clearRect(0,0,width,height);ctx.fillStyle=css.board;ctx.fillRect(0,0,width,height);const u=width/10;ctx.strokeStyle=css.grid;ctx.lineWidth=.6;ctx.beginPath();for(let i=1;i<10;i++){ctx.moveTo(i*u,0);ctx.lineTo(i*u,height);}for(let i=1;i<20;i++){ctx.moveTo(0,i*u);ctx.lineTo(width,i*u);}ctx.stroke();for(let j=0;j<20;j++)for(let i=0;i<10;i++)if(game.board[j][i])tile(ctx,i*u,j*u,u,colors[game.board[j][i]]);const landing=game.landingY();if(!game.over&&landing>game.y)for(let j=0;j<size(game.type);j++)for(let i=0;i<size(game.type);i++)if(cell(game.type,game.rotation,i,j)&&landing+j>=0)tile(ctx,(game.x+i)*u,(landing+j)*u,u,colors[game.type+1],true);for(let j=0;j<size(game.type);j++)for(let i=0;i<size(game.type);i++)if(cell(game.type,game.rotation,i,j)&&game.y+j>=0)tile(ctx,(game.x+i)*u,(game.y+j)*u,u,colors[game.type+1]);nextCtx.clearRect(0,0,144,108);const t=game.queue[0],n=size(t),nu=30,offsetX=(144-n*nu)/2;for(let j=0;j<n;j++)for(let i=0;i<n;i++)if(cell(t,0,i,j))tile(nextCtx,offsetX+i*nu,j*nu,nu,colors[t+1]);}
function render(){records.sync(game);for(const[id,value]of[['score',game.score],['lines',game.lines],['best',records.best]]){$(id).textContent=value;$(id).classList.toggle('long',value>999999);} $('speed-readout').textContent=`${(clock.interval/1000).toFixed(1)} 秒 / 格`;
 const overlay=$('overlay');overlay.hidden=clock.active&&!game.over;overlay.classList.toggle('countdown',!!clock.end);const title=overlay.querySelector('.overlay-title'),detail=overlay.querySelector('.overlay-detail'),hint=overlay.querySelector('.overlay-hint');
 if(clock.end){title.textContent=clock.remaining(performance.now());detail.textContent='准备继续';}else{title.textContent=game.over?'游戏结束':started?'已暂停':'准备好了吗';detail.textContent=game.over?`本局得分 ${game.score}`:started?'按右侧圆形按钮继续':'每一格，都从容';hint.textContent=game.over?'轻点再来一局':started?'轻点继续':'轻点开始';}
 const active=clock.active&&!game.over;$('pause-use').setAttribute('href',active?'#pause-icon':'#play-icon');$('pause-label').textContent=game.over?'再来一局':clock.end?'取消倒计时':active?'暂停':'开始 / 继续';$('pause').setAttribute('aria-label',$('pause-label').textContent);
 for(const button of document.querySelectorAll('[data-action]'))button.setAttribute('aria-disabled',String(!active));draw();
}
let previous=0;function frame(now){if(previous&&now-previous>1500&&(clock.active||clock.end)){pause();}previous=now;if(clock.update(now)&&!game.over)afterStep(game.tick());if(clock.active&&!game.over)for(const hold of holds.values())if(now>=hold.next&&['left','right','soft'].includes(hold.name)){action(hold.name);hold.next=now+(hold.name==='soft'?50:85);}if(now-lastAutoSave>2000&&clock.active){save();lastAutoSave=now;}render();requestAnimationFrame(frame);}render();requestAnimationFrame(frame);
$('install').addEventListener('click',()=>openDialog('install-dialog'));window.addEventListener('beforeinstallprompt',e=>{e.preventDefault();installPrompt=e;$('native-install').hidden=false;});$('native-install').addEventListener('click',async()=>{if(installPrompt){await installPrompt.prompt();installPrompt=null;$('native-install').hidden=true;}});
if(!storageOK)toast('当前浏览器无法读取存档，新进度可能无法保存。');
if('serviceWorker'in navigator){navigator.serviceWorker.register('./sw.js').then(()=>navigator.serviceWorker.ready).then(()=>$('offline-state').textContent='已可离线游玩').catch(()=>$('offline-state').textContent='当前可在线游玩');}
