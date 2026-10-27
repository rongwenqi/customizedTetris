import assert from'node:assert/strict';import fs from'node:fs';import vm from'node:vm';
const source=fs.readFileSync('dist/sw.js','utf8');
for(const pathname of ['/','/tetris/']){
 const prefix='wenqi-blocks-'+encodeURIComponent(pathname)+'-',current=prefix+'6',other='wenqi-blocks-'+encodeURIComponent(pathname==='/'?'/tetris/':'/')+'-6';
 const handlers={},removed=[],opened=[];let installed=[];
 const expected={html:pathname};
 const cache={addAll:async files=>{installed=files;},match:async key=>key==='./index.html'?expected:undefined,put:async()=>{}};
 const context={URL,self:{registration:{scope:'https://game.rongwenqi.me'+pathname},location:{origin:'https://game.rongwenqi.me'},addEventListener:(event,handler)=>handlers[event]=handler,skipWaiting:async()=>{},clients:{claim:async()=>{}}},caches:{keys:async()=>[prefix+'2',current,other,'other-application-cache'],delete:async key=>removed.push(key),open:async key=>{opened.push(key);return cache;}},fetch:async()=>{throw new Error('offline');}};
 vm.runInNewContext(source,context);
 let pending;handlers.install({waitUntil:p=>pending=p});await pending;assert(installed.includes('./manifest.webmanifest'));assert(installed.includes('./style.css?v=6'));assert(opened.includes(current));
 handlers.activate({waitUntil:p=>pending=p});await pending;assert.deepEqual(removed,[prefix+'2']);
 let response;handlers.fetch({request:{method:'GET',mode:'navigate',url:'https://game.rongwenqi.me'+pathname},respondWith:p=>response=p});assert.equal(await response,expected);assert(opened.every(key=>key===current));
}
const html=fs.readFileSync('dist/tetris/index.html','utf8');assert(html.includes('<base href="/tetris/">'));const manifest=JSON.parse(fs.readFileSync('dist/tetris/manifest.webmanifest','utf8'));assert.equal(new URL(manifest.scope,'https://game.rongwenqi.me/tetris/manifest.webmanifest').pathname,'/tetris/');
console.log('PASS: /tetris/ base and manifest scope, separate root/subpath offline caches, no deletion of other app caches, scoped offline navigation fallback.');
