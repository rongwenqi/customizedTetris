package cn.fixedblocks;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public final class MainActivity extends Activity {
    private BoardView gameView;
    @Override public void onCreate(Bundle state){super.onCreate(state);gameView=new BoardView(this);setContentView(gameView);}
    @Override protected void onPause(){super.onPause();gameView.suspend();}
    @Override protected void onResume(){super.onResume();if(gameView!=null)gameView.startLoop();}
    @Override public void onBackPressed(){gameView.pauseGame();}

    final class BoardView extends View {
        final Game game=new Game();final Game.Clock clock=new Game.Clock();final Game.Records records=new Game.Records();final Game.InputTracker controls=new Game.InputTracker();
        final Paint p=new Paint(3);final Handler handler=new Handler();
        final SharedPreferences prefs;
        final int[] colors={0,0xff32b7d7,0xff648bf2,0xffee9b41,0xff49bc82,0xffed6d86,0xffa87be0,0xffe6bd40};
        final ArrayList<RectF> buttons=new ArrayList<>();
        float scale=1,height,unit,bx,by;int insetTop,insetBottom;
        boolean started=false,dark=true,dialogOpen=false,hapticsEnabled=true;
        int bg,muted,ink,accent,panel,boardColor,grid,border,pressed;
        BoardView(Context context){super(context);setFocusable(true);setContentDescription("文奇俄罗斯方块游戏棋盘和控制区");prefs=getSharedPreferences("blocks",0);Object previousBest=prefs.getAll().get("best");records.best=previousBest instanceof Number?((Number)previousBest).longValue():0;records.totalLines=prefs.getLong("total_lines",0);records.games=prefs.getLong("games",0);records.accountedLines=prefs.getInt("accounted_lines",0);records.roundCounted=prefs.getBoolean("round_counted",false);dark=prefs.getBoolean("dark",true);hapticsEnabled=prefs.getBoolean("haptics_enabled",true);int speed=prefs.getInt("speed",Game.FALL_MS);if(!prefs.getBoolean("v12_speed_migrated",false)&&speed==600)speed=700;clock.setInterval(speed);String saved=prefs.getString("state","");if(!saved.isEmpty()){game.restore(saved);started=prefs.getBoolean("started",true);if(started)records.startRound();}applyTheme();save();}
        @Override public WindowInsets onApplyWindowInsets(WindowInsets insets){insetTop=insets.getSystemWindowInsetTop();insetBottom=insets.getSystemWindowInsetBottom();return insets;}
        void applyTheme(){bg=dark?0xff0b121f:0xfff1f5f9;muted=dark?0xff91a3ba:0xff53667d;ink=dark?0xffeaf1fa:0xff192d45;accent=dark?0xff56e0c4:0xff007e6c;panel=dark?0xff1b2c41:0xffe0e8f1;boardColor=dark?0xff101d2d:0xfffcfdff;grid=dark?0xff213047:0xffe3eaf2;border=dark?0xff354860:0xffb7c8db;pressed=dark?0xff355874:0xffbbd2e5;getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);invalidate();}
        void startLoop(){handler.removeCallbacks(frame);clock.resetStep(SystemClock.uptimeMillis());handler.post(frame);}
        void pauseGame(){clock.pause();controls.clear();invalidate();}
        void save(){records.sync(game);prefs.edit().putString("state",game.save()).putLong("best",records.best).putLong("total_lines",records.totalLines).putLong("games",records.games).putInt("accounted_lines",records.accountedLines).putBoolean("round_counted",records.roundCounted).putBoolean("started",started).putInt("speed",clock.interval).putBoolean("v12_speed_migrated",true).putBoolean("dark",dark).putBoolean("haptics_enabled",hapticsEnabled).apply();}
        void suspend(){pauseGame();handler.removeCallbacks(frame);save();}
        void step(boolean soft){int oldLines=game.lines;if(soft)game.softDrop();else game.tick();records.sync(game);if(oldLines!=game.lines||game.over)save();}
        final Runnable frame=new Runnable(){public void run(){long now=SystemClock.uptimeMillis();if(clock.update(now)&&!game.over)step(false);if(clock.active()&&!game.over)for(int id:controls.due(now))if(!game.over)action(id);invalidate();handler.postDelayed(this,16);}};
        String speed(){return String.format(Locale.CHINA,"%.1f 秒 / 格",clock.interval/1000f);}
        void text(Canvas c,String s,float x,float y,float size,int color,boolean bold){p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTextSize(size);p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));c.drawText(s,x,y,p);}
        void rect(Canvas c,float x,float y,float w,float h,int color,float radius){p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRoundRect(x,y,x+w,y+h,radius,radius,p);}
        void circle(Canvas c,float x,float y,float radius,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawCircle(x,y,radius,p);}
        void block(Canvas c,float x,float y,float u,int color){rect(c,x+1,y+1,u-2,u-2,color,3);p.setColor(0x45ffffff);c.drawRect(x+4,y+3,x+u-4,y+5,p);}
        void ghost(Canvas c,float x,float y,int color){rect(c,x+1,y+1,unit-2,unit-2,(color&0xffffff)|(dark?0x44000000:0x35000000),3);p.setColor((color&0xffffff)|0xb0000000);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);c.drawRoundRect(x+1.5f,y+1.5f,x+unit-1.5f,y+unit-1.5f,3,3,p);p.setStyle(Paint.Style.FILL);}
        void centered(Canvas c,String s,float x,float y,float size,int color,boolean bold){p.setTextAlign(Paint.Align.CENTER);text(c,s,x,y,size,color,bold);p.setTextAlign(Paint.Align.LEFT);}
        void themeIcon(Canvas c,float x,float y){
            int sun=dark?0xffffd476:0xffbc7b00,moon=dark?0xffc6d9ff:0xff3d598c;
            p.setColor(sun);p.setStrokeWidth(1.6f);for(int i=0;i<8;i++){double a=i*Math.PI/4;c.drawLine(x-6+(float)Math.cos(a)*11,y+(float)Math.sin(a)*11,x-6+(float)Math.cos(a)*14,y+(float)Math.sin(a)*14,p);}circle(c,x-6,y,8,sun);circle(c,x+5,y-1,11,panel);circle(c,x+6,y-1,10,moon);circle(c,x+11,y-5,9,panel);
        }
        void gear(Canvas c,float x,float y){p.setColor(ink);p.setStrokeWidth(3);for(int i=0;i<8;i++){double a=i*Math.PI/4;c.drawLine(x+(float)Math.cos(a)*9,y+(float)Math.sin(a)*9,x+(float)Math.cos(a)*13,y+(float)Math.sin(a)*13,p);}circle(c,x,y,10,ink);circle(c,x,y,6,panel);}
        void recordsIcon(Canvas c,float x,float y){rect(c,x-12,y-3,6,11,accent,1);rect(c,x-3,y-9,6,17,accent,1);rect(c,x+6,y-15,6,23,accent,1);centered(c,"记录",x,y+21,8,muted,false);}
        void pauseIcon(Canvas c,float x,float y){circle(c,x,y,24,panel);p.setColor(accent);p.setStrokeWidth(1.5f);p.setStyle(Paint.Style.STROKE);c.drawCircle(x,y,23,p);p.setStyle(Paint.Style.FILL);if(!clock.active()||game.over){Path triangle=new Path();triangle.moveTo(x-5,y-9);triangle.lineTo(x+9,y);triangle.lineTo(x-5,y+9);triangle.close();c.drawPath(triangle,p);}else{c.drawRect(x-7,y-8,x-2,y+8,p);c.drawRect(x+2,y-8,x+7,y+8,p);}}
        void drawControl(Canvas c,RectF r,int id,String symbol,String label){
            boolean round=id==1||id==4,pressedNow=controls.pressed(id);int fill,stroke,foreground;
            if(id==1){fill=dark?0xff292846:0xffeceafc;stroke=dark?0xffada2ff:0xff7062d9;foreground=dark?0xffc7bfff:0xff4d409e;}
            else if(id==4){fill=dark?0xff3a2c1c:0xfffff2df;stroke=dark?0xffe9b266:0xffbd7d28;foreground=dark?0xffffc981:0xff865110;}
            else{fill=dark?0xff163831:0xffdff3ec;stroke=dark?0xff57bfa5:0xff3c9e84;foreground=dark?0xff8be2cc:0xff147662;}
            if(pressedNow)fill=pressed;float radius=round?r.width()/2:14;rect(c,r.left,r.top,r.width(),r.height(),fill,radius);
            p.setColor(stroke);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);c.drawRoundRect(r,radius,radius,p);p.setStyle(Paint.Style.FILL);
            float iconSize=id==4?18:id==1?29:23, labelSize=id==4?10:id==1?16:14;
            centered(c,symbol,r.centerX(),r.centerY()-(id==4?2:3),iconSize,foreground,true);
            centered(c,label,r.centerX(),r.centerY()+(id==4?14:18),labelSize,foreground,true);
        }
        @Override protected void onDraw(Canvas c){super.onDraw(c);c.drawColor(bg);scale=getWidth()/360f;height=(getHeight()-insetTop-insetBottom)/scale;c.save();c.translate(0,insetTop);c.scale(scale,scale);
            rect(c,18,10,96,48,dark?0xff142b32:0xffdcefe9,12);text(c,"恒速下落",26,29,10,muted,false);text(c,speed(),26,48,13,accent,true);
            rect(c,122,10,48,48,panel,12);centered(c,"↻",146,37,28,ink,true);centered(c,"重开",146,52,8,muted,false);
            rect(c,178,10,48,48,panel,12);recordsIcon(c,202,31);rect(c,234,10,48,48,panel,12);themeIcon(c,258,34);rect(c,290,10,52,48,panel,12);gear(c,316,34);
            by=78;unit=Math.max(7,Math.min(23,(height-310)/20));bx=18;float bw=unit*10,bh=unit*20;
            rect(c,bx-2,by-2,bw+4,bh+4,border,5);rect(c,bx,by,bw,bh,boardColor,2);
            p.setColor(grid);p.setStrokeWidth(.6f);for(int i=1;i<10;i++)c.drawLine(bx+i*unit,by,bx+i*unit,by+bh,p);for(int i=1;i<20;i++)c.drawLine(bx,by+i*unit,bx+bw,by+i*unit,p);
            for(int j=0;j<20;j++)for(int i=0;i<10;i++)if(game.board[j][i]>0)block(c,bx+i*unit,by+j*unit,unit,colors[game.board[j][i]]);
            int n=Game.size(game.type),landing=game.landingY();if(!game.over&&landing>game.y)for(int j=0;j<n;j++)for(int i=0;i<n;i++)if(Game.cell(game.type,game.rotation,i,j)&&landing+j>=0)ghost(c,bx+(game.x+i)*unit,by+(landing+j)*unit,colors[game.type+1]);
            for(int j=0;j<n;j++)for(int i=0;i<n;i++)if(Game.cell(game.type,game.rotation,i,j)&&game.y+j>=0)block(c,bx+(game.x+i)*unit,by+(game.y+j)*unit,unit,colors[game.type+1]);
            float sx=bx+bw+15,sw=342-sx,sb=Math.min(1,bh/360);c.save();c.translate(sx,by);c.scale(sb,sb);String[] names={"得分","本局消行","最高分"};long[] values={game.score,game.lines,records.best};for(int i=0;i<3;i++){text(c,names[i],0,11+i*42,11,muted,false);text(c,""+values[i],0,34+i*42,values[i]>999999?14:21,ink,true);}text(c,"下一个",0,138,11,muted,false);int nt=game.next(),ns=Game.size(nt);float nu=Math.min(17,sw/4);for(int j=0;j<ns;j++)for(int i=0;i<ns;i++)if(Game.cell(nt,0,i,j))block(c,i*nu,150+j*nu,nu,colors[nt+1]);c.restore();
            float px=sx+sw/2,py=by+bh-28;pauseIcon(c,px,py);centered(c,game.over?"再来一局":clock.counting()?"取消倒计时":clock.active()?"暂停":"开始 / 继续",px,py+36,10,muted,false);
            float cy=by+bh+14;buttons.clear();RectF[] controlRects={new RectF(16,cy+88,94,cy+166),new RectF(221,cy+80,317,cy+176),new RectF(104,cy+88,182,cy+166),new RectF(70,cy+16,128,cy+74),new RectF(280,cy+24,330,cy+74)};
            String[] icons={"◀","↻","▶","▼","↓"},labels={"左移","旋转","右移","软降","硬降"};for(int i=0;i<5;i++){buttons.add(controlRects[i]);drawControl(c,controlRects[i],i,icons[i],labels[i]);}
            buttons.add(new RectF(122,10,170,58));buttons.add(new RectF(234,10,282,58));buttons.add(new RectF(290,10,342,58));buttons.add(new RectF(px-27,py-27,px+27,py+27));buttons.add(new RectF(178,10,226,58));
            if(!clock.active()||game.over){rect(c,bx,by,bw,bh,dark?0xe50b121f:0xeef1f5f9,2);float mid=by+bh/2;if(clock.counting()){centered(c,""+clock.remaining(SystemClock.uptimeMillis()),bx+bw/2,mid+8,64,accent,true);centered(c,"准备继续",bx+bw/2,mid+43,13,muted,false);}else{centered(c,game.over?"游戏结束":started?"已暂停":"准备好了吗",bx+bw/2,mid-24,23,ink,true);centered(c,game.over?"本局得分  "+game.score:"按右侧圆形按钮继续",bx+bw/2,mid+6,12,muted,false);centered(c,game.over?"点击棋盘，再来一局":"点击棋盘也可开始",bx+bw/2,mid+39,12,accent,true);}}
            c.restore();
        }
        void resumePlay(){if(dialogOpen||clock.counting())return;boolean continuation=started&&!game.over;if(game.over)records.newRound(game);records.startRound();started=true;clock.start(SystemClock.uptimeMillis(),continuation);save();invalidate();}
        Context dialogContext(){return new ContextThemeWrapper(MainActivity.this,dark?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert);}
        void restart(){pauseGame();dialogOpen=true;AlertDialog dialog=new AlertDialog.Builder(dialogContext()).setTitle("重新开始？").setMessage("当前棋盘进度会清除，已获得的最高分和累计消行会保留。").setNegativeButton("保留进度",null).setPositiveButton("重新开始",(d,w)->{records.newRound(game);records.startRound();started=true;clock.start(SystemClock.uptimeMillis(),false);save();}).create();dialog.setOnDismissListener(d->{dialogOpen=false;invalidate();});dialog.show();}
        TextView label(Context ctx,String value,int size){TextView t=new TextView(ctx);t.setText(value);t.setTextSize(size);t.setTextColor(ink);t.setPadding(0,12,0,12);return t;}
        void showRecords(){pauseGame();save();dialogOpen=true;Context ctx=dialogContext();LinearLayout layout=new LinearLayout(ctx);layout.setOrientation(LinearLayout.VERTICAL);int pad=(int)(24*getResources().getDisplayMetrics().density);layout.setPadding(pad,0,pad,8);layout.addView(label(ctx,"最高分："+records.best+"\n历史消行总数："+records.totalLines+"\n已开始局数："+records.games,18));layout.addView(label(ctx,"数据仅保存在本机；开始新局计一次，续玩不重复计数。",12));ScrollView scroll=new ScrollView(ctx);scroll.addView(layout);AlertDialog dialog=new AlertDialog.Builder(ctx).setTitle("数据记录").setView(scroll).setPositiveButton("关闭",null).create();dialog.setOnDismissListener(d->{dialogOpen=false;invalidate();});dialog.show();}
        void settings(){pauseGame();save();dialogOpen=true;Context ctx=dialogContext();LinearLayout layout=new LinearLayout(ctx);layout.setOrientation(LinearLayout.VERTICAL);int pad=(int)(24*getResources().getDisplayMetrics().density);layout.setPadding(pad,0,pad,8);TextView value=label(ctx,"下落速度："+speed(),18);layout.addView(value);SeekBar slider=new SeekBar(ctx);slider.setMax(19);slider.setProgress(clock.interval/100-1);layout.addView(slider);layout.addView(label(ctx,"0.1～2.0 秒 / 格，每次调整 0.1 秒。\n数值越小，下落越快；整局不会自动加速。",14));slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int v,boolean user){value.setText(String.format(Locale.CHINA,"下落速度：%.1f 秒 / 格",(v+1)/10f));}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});Switch haptics=new Switch(ctx);haptics.setText("震动反馈");haptics.setTextSize(16);haptics.setTextColor(ink);haptics.setPadding(0,16,0,16);haptics.setChecked(hapticsEnabled);layout.addView(haptics);layout.addView(label(ctx,"开启时，按下操作键会有轻触反馈，实际效果遵循手机系统设置。",12));layout.addView(label(ctx,"　　说明",17));layout.addView(label(ctx,"　　1.本软件从未进行收费，可以到QQ群1095699226下载。\n\n　　2.本游戏开发主要目的为利用《俄罗斯方块》改善侵入性记忆以及PTSD，所以设置为恒定速度。",15));ScrollView scroll=new ScrollView(ctx);scroll.addView(layout);AlertDialog dialog=new AlertDialog.Builder(ctx).setTitle("设置 · v1.9").setView(scroll).setNegativeButton("取消",null).setPositiveButton("保存",(d,w)->{clock.setInterval((slider.getProgress()+1)*100);hapticsEnabled=haptics.isChecked();save();}).create();dialog.setOnDismissListener(d->{dialogOpen=false;invalidate();});dialog.show();}
        void action(int id){if(dialogOpen)return;if(id==9){showRecords();return;}if(id==5){restart();return;}if(id==6){dark=!dark;applyTheme();save();return;}if(id==7){settings();return;}if(id==8){if(game.over)resumePlay();else if(clock.active()||clock.counting())pauseGame();else resumePlay();return;}if(clock.active()&&!game.over){if(id==0)game.move(-1);else if(id==2)game.move(1);else if(id==1)game.rotate();else if(id==3){step(true);clock.resetStep(SystemClock.uptimeMillis());}else if(id==4){game.hardDrop();clock.resetStep(SystemClock.uptimeMillis());save();}}}
        @Override public boolean onTouchEvent(MotionEvent e){
            int index=e.getActionIndex(),event=e.getActionMasked();
            if(event==MotionEvent.ACTION_DOWN||event==MotionEvent.ACTION_POINTER_DOWN){
                float tx=e.getX(index)/scale,ty=(e.getY(index)-insetTop)/scale;
                for(int i=0;i<buttons.size();i++)if(buttons.get(i).contains(tx,ty)){
                    if(i<5&&clock.active()&&!game.over&&!dialogOpen){controls.down(e.getPointerId(index),i,SystemClock.uptimeMillis());if(hapticsEnabled)performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);}
                    action(i);invalidate();return true;
                }
                if(tx>=bx&&tx<bx+10*unit&&ty>=by&&ty<by+20*unit&&!clock.counting()){if(!clock.active()||game.over)resumePlay();else action(1);}
                invalidate();return true;
            }
            if(event==MotionEvent.ACTION_MOVE){for(int pointer:controls.pointers()){int hi=e.findPointerIndex(pointer),id=controls.actionFor(pointer);if(hi<0||id<0||!buttons.get(id).contains(e.getX(hi)/scale,(e.getY(hi)-insetTop)/scale))controls.up(pointer);}invalidate();return true;}
            if(event==MotionEvent.ACTION_CANCEL)controls.clear();
            else if(event==MotionEvent.ACTION_UP||event==MotionEvent.ACTION_POINTER_UP){controls.up(e.getPointerId(index));if(event==MotionEvent.ACTION_UP)performClick();}
            invalidate();return true;
        }
        @Override public boolean performClick(){super.performClick();return true;}
        @Override public boolean onKeyDown(int code,KeyEvent e){if(code==KeyEvent.KEYCODE_DPAD_LEFT)action(0);else if(code==KeyEvent.KEYCODE_DPAD_RIGHT)action(2);else if(code==KeyEvent.KEYCODE_DPAD_UP)action(1);else if(code==KeyEvent.KEYCODE_DPAD_DOWN)action(3);else if(code==KeyEvent.KEYCODE_SPACE)action(4);else return super.onKeyDown(code,e);invalidate();return true;}
    }
}






