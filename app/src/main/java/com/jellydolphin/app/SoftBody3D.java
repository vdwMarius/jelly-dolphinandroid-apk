package com.jellydolphin.app;

import java.util.ArrayList;
import java.util.HashSet;

/** Position-based 3D soft body. Mesh vertices ARE simulated particles, not a shader wobble. */
public final class SoftBody3D {
    public static final float LIMIT=3.8f, CEILING=5.5f;
    public static final float[][] BLOCKS={{-2.8f,.35f,-1.8f,.7f,.35f,.8f},{2.6f,.65f,-2.1f,.65f,.65f,.7f}};
    public float[] rest, position, velocity;
    public int[] triangles;
    private int[] edgeA,edgeB;
    private float[] length,lambda,old;
    private float[] goals,animatedRest;
    private float wobble,wobbleVelocity,bend,bendVelocity;
    private final boolean[] contactLow=new boolean[3],contactHigh=new boolean[3];
    private final float[] rotation={0,0,0,1}, matrix=new float[9], covariance=new float[9];
    private final float[] restCentre=new float[3];
    private final ArrayList<Float> vertices=new ArrayList<>();
    private final ArrayList<Integer> faces=new ArrayList<>();
    public float softness=.65f;
    public boolean gravity=true;
    public int grabbed=-1;
    public final float[] target=new float[3];
    public final float[] ball={2,1.8f,1.5f},ballVelocity=new float[3];
    public boolean grabBall;
    private float accumulator;
    public SoftBody3D(){
        // Longitudinal body, tapered tail stock, rounded forehead, and projecting beak.
        float[] xs={-1.65f,-1.4f,-1.1f,-.75f,-.3f,.2f,.65f,1,1.25f,1.42f,1.6f,1.95f,2.12f};
        float[] ys={.09f,.14f,.28f,.45f,.57f,.61f,.57f,.48f,.35f,.22f,.115f,.09f,.025f};
        float[] zs={.09f,.15f,.26f,.38f,.46f,.49f,.47f,.42f,.31f,.20f,.13f,.10f,.025f};
        for(int r=0;r<xs.length;r++)for(int s=0;s<12;s++){double a=s*Math.PI/6;add(xs[r],(float)Math.cos(a)*ys[r],(float)Math.sin(a)*zs[r]);}
        for(int r=0;r<xs.length-1;r++)for(int s=0;s<12;s++){int a=r*12+s,b=r*12+(s+1)%12,c=b+12,d=a+12;face(a,b,c);face(a,c,d);}
        for(int s=1;s<11;s++){face(0,s,s+1);int last=(xs.length-1)*12;face(last,last+s+1,last+s);}
        // Closed, flattened ellipsoids form genuinely volumetric fins and horizontal tail flukes.
        ellipsoid(-.30f,.67f,0,.38f,.55f,.065f,-.35f,0);
        ellipsoid(.05f,-.34f,.66f,.27f,.10f,.57f,.25f,-.35f);
        ellipsoid(.05f,-.34f,-.66f,.27f,.10f,.57f,.25f,.35f);
        ellipsoid(-1.67f,-.02f,.46f,.28f,.075f,.62f,.2f,0);
        ellipsoid(-1.67f,-.02f,-.46f,.28f,.075f,.62f,-.2f,0);
        rest=new float[vertices.size()];for(int i=0;i<rest.length;i++)rest[i]=vertices.get(i);
        triangles=new int[faces.size()];for(int i=0;i<triangles.length;i++)triangles[i]=faces.get(i);
        position=rest.clone();velocity=new float[rest.length];old=new float[rest.length];goals=new float[rest.length];animatedRest=rest.clone();
        for(int i=0;i<count();i++)for(int k=0;k<3;k++)restCentre[k]+=rest[3*i+k]/count();
        HashSet<Long> seen=new HashSet<>();ArrayList<Integer> aa=new ArrayList<>(),bb=new ArrayList<>();
        for(int t=0;t<triangles.length;t+=3){edge(triangles[t],triangles[t+1],seen,aa,bb);edge(triangles[t+1],triangles[t+2],seen,aa,bb);edge(triangles[t+2],triangles[t],seen,aa,bb);}
        // Interior braces preserve thickness. Longer braces restore shape without pinning world orientation.
        for(int i=0;i<count();i++){
            float[] distances=new float[count()];for(int j=0;j<count();j++)distances[j]=i==j?Float.MAX_VALUE:distance(rest,i,j);
            for(int k=0;k<10;k++){int best=0;for(int j=1;j<count();j++)if(distances[j]<distances[best])best=j;edge(i,best,seen,aa,bb);distances[best]=Float.MAX_VALUE;}
            if(i<156){edge(i,(i/12)*12+(i%12+6)%12,seen,aa,bb);if(i<132)edge(i,i+24,seen,aa,bb);}
            else { // Each fin is braced to the body in its neighbourhood, so it bends and follows.
                int near=0;float best=999;for(int j=0;j<156;j++){float d=distance(rest,i,j);if(d<best){best=d;near=j;}}
                edge(i,near,seen,aa,bb);edge(i,(near/12)*12+(near%12+4)%12,seen,aa,bb);
            }
        }
        edgeA=new int[aa.size()];edgeB=new int[aa.size()];length=new float[aa.size()];lambda=new float[aa.size()];
        for(int i=0;i<aa.size();i++){edgeA[i]=aa.get(i);edgeB[i]=bb.get(i);length[i]=distance(rest,edgeA[i],edgeB[i]);}
        reset();
    }
    private void add(float x,float y,float z){vertices.add(x);vertices.add(y);vertices.add(z);}
    private void face(int a,int b,int c){faces.add(a);faces.add(b);faces.add(c);}
    private void ellipsoid(float x,float y,float z,float rx,float ry,float rz,float skewX,float skewY){
        int start=vertices.size()/3,n=8,m=6;
        for(int r=0;r<=m;r++){double a=.015+(Math.PI-.03)*r/m;for(int s=0;s<n;s++){double b=2*Math.PI*s/n;float zz=(float)(Math.sin(a)*Math.sin(b));float xx=x+rx*(float)(Math.sin(a)*Math.cos(b))+skewX*zz;if(ry>.4f&&rz<.1f){float t=((float)Math.cos(a)+1)*.5f;xx=x-.35f*t+rx*(float)(Math.sin(a)*Math.cos(b))*(1-t)*1.7f;}add(xx,y+ry*(float)Math.cos(a)+skewY*zz,z+rz*zz);}}
        for(int r=0;r<m;r++)for(int s=0;s<n;s++){int a=start+r*n+s,b=start+r*n+(s+1)%n;face(a,b,b+n);face(a,b+n,a+n);}
        for(int s=1;s<n-1;s++){face(start,start+s+1,start+s);int end=start+m*n;face(end,end+s,end+s+1);}
    }
    private static void edge(int a,int b,HashSet<Long> seen,ArrayList<Integer> aa,ArrayList<Integer> bb){if(a==b)return;long key=((long)Math.min(a,b)<<32)|Math.max(a,b);if(seen.add(key)){aa.add(a);bb.add(b);}}
    public int count(){return position==null?vertices.size()/3:position.length/3;}
    public void reset(){wobble=wobbleVelocity=bend=bendVelocity=0;if(animatedRest!=null)System.arraycopy(rest,0,animatedRest,0,rest.length);rotation[0]=rotation[1]=rotation[2]=0;rotation[3]=1;for(int i=0;i<position.length;i++){position[i]=rest[i]+(i%3==1?2.8f:0);velocity[i]=0;}ball[0]=2;ball[1]=1.8f;ball[2]=1.5f;java.util.Arrays.fill(ballVelocity,0);grabbed=-1;grabBall=false;accumulator=0;}
    public float[] centre(){float[] c=new float[3];for(int i=0;i<156;i++)for(int k=0;k<3;k++)c[k]+=position[i*3+k]/156;return c;}
    public void impulse(float x,float y,float z){wobbleVelocity-=Math.min(.6f,(Math.abs(x)+Math.abs(y)+Math.abs(z))*.06f);bendVelocity+=clamp(x*.08f,-.5f,.5f);for(int i=0;i<count();i++){velocity[3*i]+=x;velocity[3*i+1]+=y;velocity[3*i+2]+=z;}}
    public void poke(int node){if(node<0)return;wobbleVelocity-=.7f+.45f*softness;bendVelocity+=rest[node*3]>.2f?.55f:-.55f;float[] c=centre();for(int i=0;i<count();i++){float d=distance(position,i,node);float force=(float)Math.exp(-d*d*5)*5.5f;for(int k=0;k<3;k++){float dir=c[k]-position[node*3+k];velocity[i*3+k]+=dir*force;}}}
    public void setDepth(float z){float dz=z-centre()[2];for(int i=2;i<position.length;i+=3){position[i]=clamp(position[i]+dz,-LIMIT+.05f,LIMIT-.05f);velocity[i]=0;}}
    public void advance(float dt){accumulator+=clamp(dt,0,.05f);while(accumulator>=1f/120){step(1f/120);accumulator-=1f/120;}}
    private void step(float h){
        java.util.Arrays.fill(contactLow,false);java.util.Arrays.fill(contactHigh,false);
        System.arraycopy(position,0,old,0,position.length);java.util.Arrays.fill(lambda,0);
        float omega=19-7*softness;
        wobbleVelocity+=(-omega*omega*wobble-2.8f*wobbleVelocity)*h;wobble=clamp(wobble+wobbleVelocity*h,-.025f-.065f*softness,.025f+.065f*softness);
        bendVelocity+=(-95*bend-2.5f*bendVelocity)*h;bend=clamp(bend+bendVelocity*h,-.10f,.10f);
        float vertical=1+wobble,horizontal=1/(float)Math.sqrt(vertical);
        for(int i=0;i<count();i++){float x=rest[3*i]-restCentre[0],y=rest[3*i+1]-restCentre[1],z=rest[3*i+2]-restCentre[2];animatedRest[3*i]=restCentre[0]+x*horizontal;animatedRest[3*i+1]=restCentre[1]+y*vertical+bend*(x*x-.5f);animatedRest[3*i+2]=restCentre[2]+z*horizontal;}
        // Modal targets conserve the mass centre: wobble must not levitate the toy.
        float[] modalCentre=new float[3];
        for(int i=0;i<count();i++)for(int k=0;k<3;k++)modalCentre[k]+=animatedRest[3*i+k]/count();
        for(int i=0;i<count();i++)for(int k=0;k<3;k++)animatedRest[3*i+k]-=modalCentre[k]-restCentre[k];
        for(int e=0;e<length.length;e++)length[e]=distance(animatedRest,edgeA[e],edgeB[e]);
        fitShape();
        float stiffness=135-65*clamp(softness,0,1);
        for(int i=0;i<position.length;i++)velocity[i]+=(goals[i]-position[i])*stiffness*h;
        for(int i=0;i<count();i++)for(int k=0;k<3;k++){int j=i*3+k;velocity[j]*=.9995f;if(k==1&&gravity)velocity[j]-=7*h;position[j]+=clamp(velocity[j],-15,15)*h;}
        float[] predictedMean=new float[3];for(int i=0;i<count();i++)for(int k=0;k<3;k++)predictedMean[k]+=velocity[3*i+k]/count();
        if(grabBall){for(int k=0;k<3;k++){ballVelocity[k]=(target[k]-ball[k])*12;ball[k]+=ballVelocity[k]*h;}}
        else for(int k=0;k<3;k++){if(k==1&&gravity)ballVelocity[k]-=7*h;ball[k]+=ballVelocity[k]*h;ballVelocity[k]*=.998f;}
        for(int k=0;k<3;k++){float lo=k==1?.42f:-LIMIT+.42f,hi=k==1?CEILING-.42f:LIMIT-.42f;if(ball[k]<lo){ball[k]=lo;ballVelocity[k]=Math.abs(ballVelocity[k])*.7f;}if(ball[k]>hi){ball[k]=hi;ballVelocity[k]=-Math.abs(ballVelocity[k])*.7f;}}
        collideBallBlocks();
        // Grabbing moves the connected toy through a broad patch; it cannot pull a single vertex into a string.
        if(grabbed>=0){int a=grabbed*3;float gx=target[0]-position[a],gy=target[1]-position[a+1],gz=target[2]-position[a+2];float d=(float)Math.sqrt(gx*gx+gy*gy+gz*gz);float gain=Math.min(.18f,.09f/Math.max(d,.0001f));
            for(int i=0;i<count();i++){float w=.88f+.12f*(float)Math.exp(-distance(rest,i,grabbed)*distance(rest,i,grabbed)*2);position[3*i]+=gx*gain*w;position[3*i+1]+=gy*gain*w;position[3*i+2]+=gz*gain*w;}}
        float alpha=(.000002f+softness*softness*.00010f)/(h*h);
        for(int iter=0;iter<7;iter++){
            for(int e=0;e<edgeA.length;e++){
                int a=edgeA[e]*3,b=edgeB[e]*3;float dx=position[a]-position[b],dy=position[a+1]-position[b+1],dz=position[a+2]-position[b+2];float d=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);if(d<.00001f)continue;
                float dl=(-(d-length[e])-alpha*lambda[e])/(2+alpha);lambda[e]+=dl;float f=dl/d;
                position[a]+=dx*f;position[a+1]+=dy*f;position[a+2]+=dz*f;position[b]-=dx*f;position[b+1]-=dy*f;position[b+2]-=dz*f;
            }
            // A rotating rest-shape envelope limits strain while leaving room for elastic wobble.
            fitShape();
            for(int i=0;i<count();i++){
                int a=i*3;float dx=position[a]-goals[a],dy=position[a+1]-goals[a+1],dz=position[a+2]-goals[a+2];float d=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
                float limit=i<156?.075f+.075f*softness:.12f+.12f*softness;
                if(d>limit){float f=limit/d;position[a]=goals[a]+dx*f;position[a+1]=goals[a+1]+dy*f;position[a+2]=goals[a+2]+dz*f;}
            }
            for(int i=0;i<count();i++)collide(i);
        }
        float impact=0;
        for(int i=0;i<count();i++)for(int k=0;k<3;k++){
            int j=i*3+k;float before=velocity[j];velocity[j]=clamp((position[j]-old[j])/h,-16,16)*.9995f;
            float lo=k==1?.045f:-LIMIT+.045f,hi=k==1?CEILING-.045f:LIMIT-.045f;
            if(position[j]<=lo+.001f&&before<-.2f){velocity[j]=Math.abs(before)*.55f;impact=Math.max(impact,Math.abs(before));}
            if(position[j]>=hi-.001f&&before>.2f){velocity[j]=-Math.abs(before)*.55f;impact=Math.max(impact,Math.abs(before));}
        }
        if(grabbed<0){
            // Constraint corrections may change local jiggle, but cannot manufacture bulk momentum.
            float[] mean=new float[3];for(int i=0;i<count();i++)for(int k=0;k<3;k++)mean[k]+=velocity[3*i+k]/count();
            for(int k=0;k<3;k++){float desired=predictedMean[k];if(contactLow[k]&&desired<0)desired=Math.abs(desired)>.5f?-desired*.30f:0;if(contactHigh[k]&&desired>0)desired=Math.abs(desired)>.5f?-desired*.30f:0;for(int i=0;i<count();i++)velocity[3*i+k]+=desired-mean[k];}
        }
        if(impact>1){wobbleVelocity-=Math.min(.45f,impact*.045f);bendVelocity+=Math.min(.25f,impact*.025f);}
    }
    /** Best-fit rigid pose from the body covariance, using iterative polar rotation extraction.
     * Goals follow translation AND rotation, so the envelope never pins the toy in world space. */
    private void fitShape(){
        float[] c=new float[3];for(int i=0;i<count();i++)for(int k=0;k<3;k++)c[k]+=position[3*i+k]/count();java.util.Arrays.fill(covariance,0);
        for(int i=0;i<count();i++)for(int row=0;row<3;row++)for(int col=0;col<3;col++)covariance[row*3+col]+=(position[3*i+row]-c[row])*(rest[3*i+col]-restCentre[col]);
        for(int iteration=0;iteration<5;iteration++){
            rotationMatrix();float wx=0,wy=0,wz=0,denom=0;
            for(int col=0;col<3;col++){float rx=matrix[col],ry=matrix[3+col],rz=matrix[6+col],ax=covariance[col],ay=covariance[3+col],az=covariance[6+col];wx+=ry*az-rz*ay;wy+=rz*ax-rx*az;wz+=rx*ay-ry*ax;denom+=rx*ax+ry*ay+rz*az;}
            float f=1/(Math.abs(denom)+.000001f);wx*=f;wy*=f;wz*=f;float angle=(float)Math.sqrt(wx*wx+wy*wy+wz*wz);if(angle<.00001f)break;
            float sin=(float)Math.sin(angle*.5f)/angle,dx=wx*sin,dy=wy*sin,dz=wz*sin,dw=(float)Math.cos(angle*.5f);
            float x=rotation[0],y=rotation[1],z=rotation[2],w=rotation[3];rotation[0]=dw*x+dx*w+dy*z-dz*y;rotation[1]=dw*y-dx*z+dy*w+dz*x;rotation[2]=dw*z+dx*y-dy*x+dz*w;rotation[3]=dw*w-dx*x-dy*y-dz*z;
            float n=0;for(float q:rotation)n+=q*q;n=(float)Math.sqrt(n);for(int k=0;k<4;k++)rotation[k]/=n;
        }
        rotationMatrix();
        for(int i=0;i<count();i++)for(int row=0;row<3;row++){float g=c[row];for(int col=0;col<3;col++)g+=matrix[row*3+col]*(animatedRest[3*i+col]-restCentre[col]);goals[3*i+row]=g;}
    }
    private void rotationMatrix(){float x=rotation[0],y=rotation[1],z=rotation[2],w=rotation[3];matrix[0]=1-2*(y*y+z*z);matrix[1]=2*(x*y-z*w);matrix[2]=2*(x*z+y*w);matrix[3]=2*(x*y+z*w);matrix[4]=1-2*(x*x+z*z);matrix[5]=2*(y*z-x*w);matrix[6]=2*(x*z-y*w);matrix[7]=2*(y*z+x*w);matrix[8]=1-2*(x*x+y*y);}
    private void collide(int i){int j=i*3;
        for(int k=0;k<3;k++){float lo=k==1?.045f:-LIMIT+.045f,hi=k==1?CEILING-.045f:LIMIT-.045f;if(position[j+k]<lo)contactLow[k]=true;if(position[j+k]>hi)contactHigh[k]=true;}position[j]=clamp(position[j],-LIMIT+.045f,LIMIT-.045f);position[j+1]=clamp(position[j+1],.045f,CEILING-.045f);position[j+2]=clamp(position[j+2],-LIMIT+.045f,LIMIT-.045f);
        for(float[] box:BLOCKS){float px=position[j],py=position[j+1],pz=position[j+2];pushBox(position,j,box,.045f);if(position[j]>px)contactLow[0]=true;if(position[j]<px)contactHigh[0]=true;if(position[j+1]>py)contactLow[1]=true;if(position[j+1]<py)contactHigh[1]=true;if(position[j+2]>pz)contactLow[2]=true;if(position[j+2]<pz)contactHigh[2]=true;}
        float dx=position[j]-ball[0],dy=position[j+1]-ball[1],dz=position[j+2]-ball[2],d=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
        if(d<.46f&&d>.0001f){float f=(.46f-d)/d;position[j]+=dx*f;position[j+1]+=dy*f;position[j+2]+=dz*f;if(!grabBall){ballVelocity[0]-=dx*f*.9f;ballVelocity[1]-=dy*f*.9f;ballVelocity[2]-=dz*f*.9f;}}
        position[j]=clamp(position[j],-LIMIT+.045f,LIMIT-.045f);position[j+1]=clamp(position[j+1],.045f,CEILING-.045f);position[j+2]=clamp(position[j+2],-LIMIT+.045f,LIMIT-.045f);
    }
    private void collideBallBlocks(){for(float[] box:BLOCKS){float[] prev=ball.clone();pushBox(ball,0,box,.42f);for(int k=0;k<3;k++)if(ball[k]!=prev[k])ballVelocity[k]*=-.6f;}}
    static void pushBox(float[] p,int j,float[] b,float margin){float x=p[j]-b[0],y=p[j+1]-b[1],z=p[j+2]-b[2];float dx=b[3]+margin-Math.abs(x),dy=b[4]+margin-Math.abs(y),dz=b[5]+margin-Math.abs(z);if(dx>0&&dy>0&&dz>0){if(dx<dy&&dx<dz)p[j]+=Math.copySign(dx,x);else if(dy<dz)p[j+1]+=Math.copySign(dy,y);else p[j+2]+=Math.copySign(dz,z);}}
    static float distance(float[] p,int a,int b){float x=p[3*a]-p[3*b],y=p[3*a+1]-p[3*b+1],z=p[3*a+2]-p[3*b+2];return (float)Math.sqrt(x*x+y*y+z*z);}
    public static float clamp(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}
}
