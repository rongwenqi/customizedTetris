export const DEFAULT_SPEED=700;
const SHAPES=['0000111100000000','100111000','001111000','011110000','110011000','010111000','1111'];
const NORMAL=[[[0,0],[-1,0],[-1,-1],[0,2],[-1,2]],[[0,0],[1,0],[1,1],[0,-2],[1,-2]],[[0,0],[1,0],[1,-1],[0,2],[1,2]],[[0,0],[-1,0],[-1,1],[0,-2],[-1,-2]]];
const LONG=[[[0,0],[-2,0],[1,0],[-2,1],[1,-2]],[[0,0],[-1,0],[2,0],[-1,-2],[2,1]],[[0,0],[2,0],[-1,0],[2,-1],[-1,2]],[[0,0],[1,0],[-2,0],[1,2],[-2,-1]]];
export const size=t=>t===0?4:t===6?2:3;
export function cell(t,r,cx,cy){const n=size(t);if(t===6)r=0;for(let k=0;k<r;k++){const old=cx;cx=cy;cy=n-1-old;}return SHAPES[t][cy*n+cx]==='1';}
const add=(a,b)=>Math.min(Number.MAX_SAFE_INTEGER,a+b);
export class Game {
 constructor(random=Math.random){this.random=random;this.reset();}
 refill(){if(this.queue.length<8){const bag=[0,1,2,3,4,5,6];for(let i=6;i>0;i--){const j=Math.floor(this.random()*(i+1));[bag[i],bag[j]]=[bag[j],bag[i]];}this.queue.push(...bag);}}
 reset(){this.board=Array.from({length:20},()=>Array(10).fill(0));this.queue=[];this.score=0;this.lines=0;this.over=false;this.spawn();}
 spawn(){this.refill();this.type=this.queue.shift();this.rotation=0;this.x=3;this.y=-1;if(!this.fits(this.type,0,this.x,this.y))this.over=true;}
 fits(t,r,x,y){for(let j=0;j<size(t);j++)for(let i=0;i<size(t);i++)if(cell(t,r,i,j)){const bx=x+i,by=y+j;if(bx<0||bx>=10||by>=20||by< -2||(by>=0&&this.board[by][bx]))return false;}return true;}
 move(dx){if(!this.over&&this.fits(this.type,this.rotation,this.x+dx,this.y)){this.x+=dx;return true;}return false;}
 rotate(){if(this.over||this.type===6)return;const nr=(this.rotation+1)%4;for(const [dx,dy]of(this.type===0?LONG:NORMAL)[this.rotation])if(this.fits(this.type,nr,this.x+dx,this.y+dy)){this.x+=dx;this.y+=dy;this.rotation=nr;return;}}
 landingY(){let result=this.y;if(!this.over)while(this.fits(this.type,this.rotation,this.x,result+1))result++;return result;}
 tick(){if(this.over)return false;if(this.fits(this.type,this.rotation,this.x,this.y+1)){this.y++;return false;}for(let j=0;j<size(this.type);j++)for(let i=0;i<size(this.type);i++)if(cell(this.type,this.rotation,i,j)&&this.y+j<0){this.over=true;return true;}for(let j=0;j<size(this.type);j++)for(let i=0;i<size(this.type);i++)if(cell(this.type,this.rotation,i,j))this.board[this.y+j][this.x+i]=this.type+1;this.clearLines();this.spawn();return true;}
 clearLines(){const rows=this.board.filter(row=>!row.every(Boolean));const count=20-rows.length;while(rows.length<20)rows.unshift(Array(10).fill(0));this.board=rows;this.lines=add(this.lines,count);this.score=add(this.score,[0,100,300,500,800][count]??0);return count;}
 softDrop(){return this.tick();}
 hardDrop(){if(this.over)return false;this.y=this.landingY();return this.tick();}
 snapshot(){return {board:this.board.map(row=>[...row]),queue:[...this.queue],type:this.type,rotation:this.rotation,x:this.x,y:this.y,score:this.score,lines:this.lines,over:this.over};}
 restore(s){const integer=(v,min,max)=>Number.isSafeInteger(v)&&v>=min&&v<=max;
  if(!s||!Array.isArray(s.board)||s.board.length!==20||!s.board.every(r=>Array.isArray(r)&&r.length===10&&r.every(v=>integer(v,0,7)))||!Array.isArray(s.queue)||!s.queue.length||s.queue.length>21||!s.queue.every(v=>integer(v,0,6))||!integer(s.type,0,6)||!integer(s.rotation,0,3)||!integer(s.x,-4,10)||!integer(s.y,-2,19)||!integer(s.score,0,Number.MAX_SAFE_INTEGER)||!integer(s.lines,0,Number.MAX_SAFE_INTEGER)||typeof s.over!=='boolean')return false;
  const previous=this.snapshot();Object.assign(this,{...s,board:s.board.map(r=>[...r]),queue:[...s.queue]});if(!this.over&&!this.fits(this.type,this.rotation,this.x,this.y)){Object.assign(this,previous);return false;}return true;
 }
}
export class Clock {
 constructor(){this.interval=DEFAULT_SPEED;this.paused=true;this.end=0;this.last=0;}
 setInterval(ms){this.interval=Math.max(100,Math.min(2000,Math.round(ms/100)*100));}
 start(now,countdown=false){this.paused=countdown;this.end=countdown?now+3000:0;this.last=now;}
 pause(){this.paused=true;this.end=0;}
 get active(){return !this.paused&&!this.end;}
 remaining(now){return this.end?Math.max(0,Math.ceil((this.end-now)/1000)):0;}
 update(now){if(this.end){if(now>=this.end){this.end=0;this.paused=false;this.last=now;}return false;}if(this.paused)return false;if(now-this.last>=this.interval){this.last=now;return true;}return false;}
}
export class Records {
 constructor(){this.best=0;this.totalLines=0;this.games=0;this.accountedLines=0;this.roundCounted=false;}
 sync(game){this.best=Math.max(this.best,game.score);this.totalLines=add(this.totalLines,Math.max(0,game.lines-this.accountedLines));this.accountedLines=game.lines;}
 startRound(){if(!this.roundCounted){this.games=add(this.games,1);this.roundCounted=true;}}
 newRound(game){this.sync(game);game.reset();this.accountedLines=0;this.roundCounted=false;}
 restore(s){if(!s)return;for(const key of ['best','totalLines','games','accountedLines'])if(Number.isSafeInteger(s[key])&&s[key]>=0)this[key]=s[key];this.roundCounted=s.roundCounted===true;}
}
