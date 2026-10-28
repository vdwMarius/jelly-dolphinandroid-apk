package com.jellydolphin.app;

/** Damped spring body with bounded impulses and fixed substeps, independent of rendering. */
public final class JellyPhysics {
    public float x, y, vx, vy, squish, squishVelocity, bend, bendVelocity;
    public float softness = .65f;
    public boolean gravity, dragging;
    public float targetX, targetY;
    private float width=400, height=400, radiusX=100, radiusY=70;
    public void bounds(float w, float h, float rx, float ry) {
        width=w; height=h; radiusX=Math.min(rx,w*.45f); radiusY=Math.min(ry,h*.45f);
        x=clamp(x,radiusX,width-radiusX); y=clamp(y,radiusY,height-radiusY);
    }
    public void reset() { x=width*.5f; y=height*.45f; vx=vy=squish=squishVelocity=bend=bendVelocity=0; dragging=false; }
    public void poke(float direction) { squishVelocity+=3.5f; bendVelocity+=direction*5; vy-=100; }
    public void shake(float dx, float dy) { vx=clamp(vx+dx,-1500,1500); vy=clamp(vy+dy,-1500,1500); squishVelocity+=4; bendVelocity+=3; }
    public void step(float seconds) {
        float remaining=clamp(seconds,0,.05f);
        while (remaining>0) { float dt=Math.min(remaining,1f/240); remaining-=dt; integrate(dt); }
    }
    private void integrate(float dt) {
        float ax=0, ay=gravity?900:0;
        if (dragging) { ax=(targetX-x)*95-vx*12; ay=(targetY-y)*95-vy*12; }
        else { ax=-vx*.35f; ay-=vy*.35f; }
        vx=clamp(vx+ax*dt,-1800,1800); vy=clamp(vy+ay*dt,-1800,1800);
        x+=vx*dt; y+=vy*dt;
        float impact=0;
        if(x<radiusX) { x=radiusX; impact=Math.abs(vx); vx=Math.abs(vx)*.78f; }
        if(x>width-radiusX) { x=width-radiusX; impact=Math.abs(vx); vx=-Math.abs(vx)*.78f; }
        if(y<radiusY) { y=radiusY; impact=Math.max(impact,Math.abs(vy)); vy=Math.abs(vy)*.78f; }
        if(y>height-radiusY) { y=height-radiusY; impact=Math.max(impact,Math.abs(vy)); vy=-Math.abs(vy)*.78f; if(gravity && Math.abs(vy)<20)vy=0; }
        if(impact>50) { squishVelocity+=Math.min(impact/150,6); bendVelocity+=vx/250; }
        float stiffness=180-145*softness, damping=9-5*softness;
        float desired=dragging?clamp(vy/2500,-.12f,.12f):0;
        squishVelocity+=((desired-squish)*stiffness-squishVelocity*damping)*dt;
        squish=clamp(squish+squishVelocity*dt,-.30f,.38f);
        bendVelocity+=(-bend*stiffness*.65f-bendVelocity*damping-vx*.001f)*dt;
        bend=clamp(bend+bendVelocity*dt,-.65f,.65f);
    }
    public static float clamp(float v,float low,float high) { return Math.max(low,Math.min(high,v)); }
}
