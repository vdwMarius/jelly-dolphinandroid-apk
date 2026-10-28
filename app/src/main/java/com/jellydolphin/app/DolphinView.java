package com.jellydolphin.app;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.*;

public class DolphinView extends View {
    public final JellyPhysics physics=new JellyPhysics();
    public int colour=0xFF76E8D0;
    private final Paint p=new Paint(3);
    private final Path path=new Path();
    private float scale=1, time, offsetX,offsetY,downX,downY;
    private long last,downTime;
    private boolean running=true;
    private int pointer=-1;
    private Shader backdrop, bodyGradient;
    private int gradientColour;
    private VelocityTracker velocity;
    public DolphinView(Context c){super(c);setLayerType(View.LAYER_TYPE_HARDWARE,null);setFocusable(true);setClickable(true);}
    public void pause(){running=false;last=0;cancelDrag();}
    public void resume(){running=true;last=0;invalidate();}
    @Override protected void onSizeChanged(int w,int h,int ow,int oh){scale=Math.min(w/370f,h/270f);physics.bounds(w,h,152*scale,90*scale);physics.reset();backdrop=new RadialGradient(w*.5f,h*.48f,Math.max(w,h)*.65f,new int[]{0xFF1D4659,0xFF0D2434},null,Shader.TileMode.CLAMP);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);long now=SystemClock.uptimeMillis();float dt=last==0?0:Math.min((now-last)/1000f,.05f);last=now;time+=dt;physics.step(dt);
        float w=getWidth(),h=getHeight();
        p.setShader(backdrop);c.drawRect(0,0,w,h,p);p.setShader(null);
        // Quiet, drifting bubbles give the playground depth.
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);
        for(int i=0;i<15;i++){float bx=(i*97.3f+23)%Math.max(1,w),by=(h+i*71-time*(5+i%3))%Math.max(1,h);p.setColor(0x244C98AE);c.drawCircle(bx,by,3+i%4*2,p);}
        p.setStyle(Paint.Style.FILL);p.setColor(0x193FC6C0);c.drawOval(w*.2f,h-16*scale,w*.8f,h-4*scale,p);
        c.save();c.translate(physics.x,physics.y+(physics.gravity||physics.dragging?0:(float)Math.sin(time*1.6)*4*scale));c.scale(scale,scale);
        float rotation=JellyPhysics.clamp(physics.vx*.012f,-16,16);c.rotate(rotation);
        // Every outline and facial point uses the same deformation field.
        p.setColor(0x25000000);c.save();c.translate(0,8);outline();c.drawPath(path,p);c.restore();
        if(bodyGradient==null||gradientColour!=colour){gradientColour=colour;bodyGradient=new LinearGradient(-40,-85,40,80,new int[]{lighten(colour,.35f),colour,darken(colour,.28f)},null,Shader.TileMode.CLAMP);}
        outline();p.setShader(bodyGradient);c.drawPath(path,p);p.setShader(null);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(lighten(colour,.55f));c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
        // Soft belly.
        path.reset();move(-65,23);curve(-20,61,61,56,100,11);curve(63,34,3,40,-65,23);p.setColor(0x55FFFFFF);c.drawPath(path,p);
        // Near flipper.
        path.reset();move(6,22);curve(26,27,35,62,16,73);curve(9,62,-13,40,-14,28);p.setColor(darken(colour,.20f));c.drawPath(path,p);
        p.setStyle(Paint.Style.STROKE);p.setColor(0x6692FFF0);p.setStrokeWidth(2);path.reset();move(-5,29);curve(6,37,16,55,16,65);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
        // Gloss highlights, curved brow, smiling face.
        p.setColor(0x77FFFFFF);path.reset();move(-60,-21);curve(-38,-46,-6,-45,16,-38);curve(-8,-38,-35,-31,-60,-21);c.drawPath(path,p);
        float blink=(time%5.3f)>5.12f?.12f:1;
        c.save();c.translate(dx(72,-11),dy(72,-11));c.scale(1,blink);p.setColor(0xFF102E40);c.drawOval(-6,-9,6,8,p);p.setColor(Color.WHITE);c.drawCircle(1,-4,2.3f,p);c.restore();
        p.setColor(0x55FF87AD);c.drawOval(dx(77,7)-10,dy(77,7)-4,dx(77,7)+10,dy(77,7)+5,p);
        p.setColor(0xFF236174);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.2f);p.setStrokeCap(Paint.Cap.ROUND);path.reset();move(84,13);curve(94,20,104,12,107,9);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
        c.restore();
        if(running)postInvalidateOnAnimation();
    }
    private float dx(float x,float y){return x*(1-physics.squish*.65f);}
    private float dy(float x,float y){float tail=Math.max(0,(-x-20)/120);return y*(1+physics.squish)+physics.bend*(x*.25f+tail*35);}
    private void move(float x,float y){path.moveTo(dx(x,y),dy(x,y));}
    private void line(float x,float y){path.lineTo(dx(x,y),dy(x,y));}
    private void curve(float a,float b,float c,float d,float e,float f){path.cubicTo(dx(a,b),dy(a,b),dx(c,d),dy(c,d),dx(e,f),dy(e,f));}
    private void outline(){
        path.reset();move(-110,10);
        curve(-92,-14,-69,-37,-31,-40);
        curve(-20,-44,-16,-68,1,-73);curve(-1,-53,16,-48,23,-41);
        curve(67,-43,91,-24,97,-7);curve(108,-4,127,-5,132,3);curve(138,14,117,22,103,20);
        curve(86,45,45,51,6,42);curve(-42,43,-78,19,-110,23);
        curve(-122,40,-139,51,-149,43);curve(-138,32,-137,14,-129,10);
        curve(-148,6,-151,-11,-147,-23);curve(-130,-19,-119,-5,-110,10);path.close();
    }
    private static int lighten(int col,float amount){return Color.rgb((int)(Color.red(col)+(255-Color.red(col))*amount),(int)(Color.green(col)+(255-Color.green(col))*amount),(int)(Color.blue(col)+(255-Color.blue(col))*amount));}
    private static int darken(int col,float amount){return Color.rgb((int)(Color.red(col)*(1-amount)),(int)(Color.green(col)*(1-amount)),(int)(Color.blue(col)*(1-amount)));}
    @Override public boolean performClick(){super.performClick();physics.poke(1);performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);invalidate();return true;}
    @Override public boolean onTouchEvent(android.view.MotionEvent e){
        int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){
            float nx=(e.getX()-physics.x)/(155*scale),ny=(e.getY()-physics.y)/(95*scale);
            if(nx*nx+ny*ny>1.15f)return false;
            pointer=e.getPointerId(0);downX=e.getX();downY=e.getY();downTime=e.getEventTime();offsetX=physics.x-downX;offsetY=physics.y-downY;
            physics.targetX=physics.x;physics.targetY=physics.y;physics.dragging=true;velocity=VelocityTracker.obtain();velocity.addMovement(e);getParent().requestDisallowInterceptTouchEvent(true);return true;
        }
        if(pointer<0)return false;
        velocity.addMovement(e);
        if(action==MotionEvent.ACTION_CANCEL || (action==MotionEvent.ACTION_POINTER_UP && e.getPointerId(e.getActionIndex())==pointer)){cancelDrag();return true;}
        int index=e.findPointerIndex(pointer);if(index<0){cancelDrag();return true;}
        if(action==MotionEvent.ACTION_MOVE){physics.targetX=e.getX(index)+offsetX;physics.targetY=e.getY(index)+offsetY;return true;}
        if(action==MotionEvent.ACTION_UP){
            physics.dragging=false;
            if(Math.hypot(e.getX()-downX,e.getY()-downY)<12*getResources().getDisplayMetrics().density && e.getEventTime()-downTime<400)performClick();
            else{velocity.computeCurrentVelocity(1000,1600);physics.vx=velocity.getXVelocity(pointer);physics.vy=velocity.getYVelocity(pointer);physics.squishVelocity+=2;}
            cancelDrag();return true;
        }
        return true;
    }
    private void cancelDrag(){physics.dragging=false;pointer=-1;if(velocity!=null){velocity.recycle();velocity=null;}if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);}
}
