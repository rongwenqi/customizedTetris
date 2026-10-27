package cn.fixedblocks;

import java.util.*;
import java.io.*;

/** Platform-independent rules. The fall interval never depends on score or lines. */
public final class Game {
    public static final int COLS=10, ROWS=20, FALL_MS=700;
    public final int[][] board=new int[ROWS][COLS];
    public int type,rotation,x,y,lines;
    public long score;
    public boolean over;
    private final Random random=new Random();
    private final ArrayList<Integer> queue=new ArrayList<>();
    private static final String[] SHAPES={"0000111100000000","100111000","001111000","011110000","110011000","010111000","1111"};
    // I, J, L, S, Z, T, O. Rotation around the SRS pivot.
    public Game(){reset();}
    private void refill(){if(queue.size()<8){ArrayList<Integer>b=new ArrayList<>();for(int i=0;i<7;i++)b.add(i);Collections.shuffle(b,random);queue.addAll(b);}}
    public int next(){return queue.get(0);}
    public void reset(){for(int[] row:board)Arrays.fill(row,0);queue.clear();score=0;lines=0;over=false;refill();spawn();}
    private void spawn(){refill();type=queue.remove(0);rotation=0;x=3;y=-1;if(!fits(type,rotation,x,y))over=true;}
    public static int size(int t){return t==0?4:t==6?2:3;}
    public static boolean cell(int t,int r,int cx,int cy){int n=size(t);if(t==6)r=0;for(int i=0;i<r;i++){int old=cx;cx=cy;cy=n-1-old;}return SHAPES[t].charAt(cy*n+cx)=='1';}
    public boolean fits(int t,int r,int px,int py){int n=size(t);for(int j=0;j<n;j++)for(int i=0;i<n;i++)if(cell(t,r,i,j)){int bx=px+i,by=py+j;if(bx<0||bx>=COLS||by>=ROWS||by< -2||(by>=0&&board[by][bx]!=0))return false;}return true;}
    public boolean move(int dx){if(over)return false;if(fits(type,rotation,x+dx,y)){x+=dx;return true;}return false;}
    // Clockwise SRS wall kicks, coordinates use downward-positive Y.
    private static final int[][][] NORMAL={{{0,0},{-1,0},{-1,-1},{0,2},{-1,2}},{{0,0},{1,0},{1,1},{0,-2},{1,-2}},{{0,0},{1,0},{1,-1},{0,2},{1,2}},{{0,0},{-1,0},{-1,1},{0,-2},{-1,-2}}};
    private static final int[][][] LONG={{{0,0},{-2,0},{1,0},{-2,1},{1,-2}},{{0,0},{-1,0},{2,0},{-1,-2},{2,1}},{{0,0},{2,0},{-1,0},{2,-1},{-1,2}},{{0,0},{1,0},{-2,0},{1,2},{-2,-1}}};
    public void rotate(){if(over||type==6)return;int nr=(rotation+1)%4;for(int[] k:(type==0?LONG:NORMAL)[rotation])if(fits(type,nr,x+k[0],y+k[1])){x+=k[0];y+=k[1];rotation=nr;return;}}
    public void tick(){if(over)return;if(fits(type,rotation,x,y+1)){y++;return;}int n=size(type);for(int j=0;j<n;j++)for(int i=0;i<n;i++)if(cell(type,rotation,i,j)&&y+j<0){over=true;return;}for(int j=0;j<n;j++)for(int i=0;i<n;i++)if(cell(type,rotation,i,j))board[y+j][x+i]=type+1;clearLines();spawn();}
    /** User-requested instant drop; the next piece retains the configured fall interval. */
    public int landingY(){int result=y;if(!over)while(fits(type,rotation,x,result+1))result++;return result;}
    public void softDrop(){tick();}
    public void hardDrop(){if(over)return;y=landingY();tick();}
    /** Monotonic game clock, independent of Android so pause/countdown can be tested. */
    public static final class Clock {
        public int interval=FALL_MS;
        public boolean paused=true;
        private long countdownEnd=0,lastStep=0;
        public void setInterval(int value){interval=Math.max(100,Math.min(2000,Math.round(value/100f)*100));}
        public void start(long now,boolean countdown){paused=countdown;countdownEnd=countdown?now+3000:0;lastStep=now;}
        public void pause(){paused=true;countdownEnd=0;}
        public boolean counting(){return countdownEnd!=0;}
        public int remaining(long now){return countdownEnd==0?0:(int)Math.max(0,(countdownEnd-now+999)/1000);}
        public boolean active(){return !paused&&!counting();}
        public void resetStep(long now){lastStep=now;}
        public boolean update(long now){if(counting()){if(now>=countdownEnd){countdownEnd=0;paused=false;lastStep=now;}return false;}if(paused)return false;if(now-lastStep>=interval){lastStep=now;return true;}return false;}
    }
    /** Tracks each finger independently so movement and rotation can overlap. */
    public static final class InputTracker {
        private static final class Hold {final int action;long next;Hold(int action,long next){this.action=action;this.next=next;}}
        private final LinkedHashMap<Integer,Hold> fingers=new LinkedHashMap<>();
        public void down(int pointer,int action,long now){fingers.put(pointer,new Hold(action,now+(action==3?50:250)));}
        public void up(int pointer){fingers.remove(pointer);}
        public void clear(){fingers.clear();}
        public boolean pressed(int action){for(Hold h:fingers.values())if(h.action==action)return true;return false;}
        public int actionFor(int pointer){Hold h=fingers.get(pointer);return h==null?-1:h.action;}
        public int[] due(long now){ArrayList<Integer> result=new ArrayList<>();for(Hold h:fingers.values())if((h.action==0||h.action==2||h.action==3)&&now>=h.next){result.add(h.action);h.next=now+(h.action==3?50:85);}int[] out=new int[result.size()];for(int i=0;i<out.length;i++)out[i]=result.get(i);return out;}
        public int[] pointers(){int[] ids=new int[fingers.size()];int i=0;for(int id:fingers.keySet())ids[i++]=id;return ids;}
    }
    public int clearLines(){int count=0;for(int row=ROWS-1;row>=0;row--){boolean full=true;for(int v:board[row])if(v==0)full=false;if(full){for(int k=row;k>0;k--)System.arraycopy(board[k-1],0,board[k],0,COLS);Arrays.fill(board[0],0);row++;count++;}}lines+=count;score+=new int[]{0,100,300,500,800}[count];return count;}
    public String save(){try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(bytes);d.writeInt(2);for(int[] row:board)for(int v:row)d.writeInt(v);for(int v:new int[]{type,rotation,x,y})d.writeInt(v);d.writeLong(score);d.writeInt(lines);d.writeBoolean(over);d.writeInt(queue.size());for(int v:queue)d.writeInt(v);d.close();return Base64.getEncoder().encodeToString(bytes.toByteArray());}catch(IOException e){return "";}}
    public void restore(String state){try{DataInputStream d=new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(state)));int version=d.readInt();if(version!=1&&version!=2)throw new IOException();for(int[] row:board)for(int i=0;i<COLS;i++)row[i]=d.readInt();type=d.readInt();rotation=d.readInt();x=d.readInt();y=d.readInt();score=version==1?d.readInt():d.readLong();lines=d.readInt();over=d.readBoolean();int length=d.readInt();if(length<1||length>21)throw new IOException();queue.clear();for(int i=0;i<length;i++)queue.add(d.readInt());}catch(Exception e){reset();}}
    /** Lifetime records: count a round once at start, count each cleared line once. */
    public static final class Records {
        public long best,totalLines,games;
        public int accountedLines;
        public boolean roundCounted;
        private long add(long a,long b){return a>Long.MAX_VALUE-b?Long.MAX_VALUE:a+b;}
        public void sync(Game game){best=Math.max(best,game.score);int delta=Math.max(0,game.lines-accountedLines);totalLines=add(totalLines,delta);accountedLines=game.lines;}
        public void startRound(){if(!roundCounted){games=add(games,1);roundCounted=true;}}
        public void newRound(Game game){sync(game);game.reset();accountedLines=0;roundCounted=false;}
    }
}
