import fs from'node:fs';import path from'node:path';
const files=['index.html','style.css','app.mjs','game.mjs','manifest.webmanifest','sw.js','icon.svg','icon-192.png','icon-512.png'];
const root=path.resolve('dist'),destination=path.join(root,'tetris');fs.mkdirSync(destination,{recursive:true});
for(const file of files){if(file==='index.html'){const source=fs.readFileSync(path.join(root,file),'utf8');fs.writeFileSync(path.join(destination,file),source.replace('<head>','<head>\n <base href="/tetris/">'));}else fs.copyFileSync(path.join(root,file),path.join(destination,file));}
console.log('Prepared static /tetris/ files and scoped PWA assets.');
