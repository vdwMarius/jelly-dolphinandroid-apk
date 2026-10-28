package com.jellydolphin.app;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

/** UI events are queued onto the GL thread; simulation and mesh are never mutated concurrently. */
public final class DolphinView extends GLSurfaceView {
    public final RoomRenderer room=new RoomRenderer();
    private float lastX,lastY,lastSpan;
    private int pointer=-1;
    private boolean multi;
    public DolphinView(Context context){super(context);setEGLContextClientVersion(2);setEGLConfigChooser(8,8,8,0,16,0);setRenderer(room);setPreserveEGLContextOnPause(true);setFocusable(true);setClickable(true);}
    public void resume(){onResume();queueEvent(room::resetClock);}
    public void pause(){queueEvent(()->{room.up(true);room.resetClock();});onPause();}
    @Override public boolean performClick(){super.performClick();queueEvent(()->room.body.impulse(0,2,0));return true;}
    @Override public boolean onTouchEvent(MotionEvent event){
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){pointer=event.getPointerId(0);lastX=event.getX();lastY=event.getY();multi=false;float x=lastX,y=lastY;queueEvent(()->room.down(x,y));getParent().requestDisallowInterceptTouchEvent(true);return true;}
        if(action==MotionEvent.ACTION_POINTER_DOWN){multi=true;lastSpan=span(event);return true;}
        if(action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_UP){queueEvent(()->room.up(action==MotionEvent.ACTION_CANCEL));pointer=-1;multi=false;getParent().requestDisallowInterceptTouchEvent(false);return true;}
        if(action==MotionEvent.ACTION_POINTER_UP){queueEvent(()->room.up(true));int keep=event.getActionIndex()==0?1:0;pointer=event.getPointerId(keep);lastX=event.getX(keep);lastY=event.getY(keep);multi=false;return true;}
        if(action==MotionEvent.ACTION_MOVE){int index=event.findPointerIndex(pointer);if(index<0)return true;float x=event.getX(index),y=event.getY(index);
            if(event.getPointerCount()>1){float span=span(event);if(lastSpan>10){float ratio=span/lastSpan;queueEvent(()->room.pinch(ratio));}lastSpan=span;}
            else if(!multi){float dx=x-lastX,dy=y-lastY;queueEvent(()->room.move(x,y,dx,dy));}
            lastX=x;lastY=y;return true;
        }
        return true;
    }
    private static float span(MotionEvent e){return e.getPointerCount()<2?0:(float)Math.hypot(e.getX(0)-e.getX(1),e.getY(0)-e.getY(1));}
}
