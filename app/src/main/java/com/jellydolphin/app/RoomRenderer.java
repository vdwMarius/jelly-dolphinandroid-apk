package com.jellydolphin.app;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import java.nio.*;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.EGLConfig;

public final class RoomRenderer implements GLSurfaceView.Renderer {
    public final SoftBody3D body=new SoftBody3D();
    public volatile int colour=0xFF76E8D0;
    public volatile float softness=.65f;
    public volatile boolean gravity=true;
    private int program,positionLoc,normalLoc,colourLoc,matrixLoc,eyeLoc;
    private final float[] projection=new float[16],view=new float[16],vp=new float[16],inverse=new float[16];
    private final float[] eye=new float[3];
    private final FloatBuffer buffer=ByteBuffer.allocateDirect(220000*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private final float[] data=new float[220000];
    private int used,width=1,height=1;
    private final float[] normals=new float[body.position.length];
    public float yaw=.48f,pitch=.42f,distance=11.5f;
    private long last;
    private float grabDistance;
    private int tapNode=-1;
    private boolean picked,moved;
    private float downX,downY;
    private final float[] rayOrigin=new float[3],rayDir=new float[3];
    private final float[][] sphere=makeSphere();
    @Override public void onSurfaceCreated(GL10 unused,EGLConfig config){
        String vertex="uniform mat4 uVP; attribute vec3 aPosition; attribute vec3 aNormal; attribute vec3 aColour; varying vec3 vP; varying vec3 vN; varying vec3 vC; void main(){vP=aPosition;vN=aNormal;vC=aColour;gl_Position=uVP*vec4(aPosition,1.0);}";
        String fragment="precision mediump float; varying vec3 vP; varying vec3 vN; varying vec3 vC; uniform vec3 uEye; void main(){vec3 n=normalize(vN);vec3 l=normalize(vec3(-.4,1.0,.7));float d=max(dot(n,l),0.0);float spec=pow(max(dot(n,normalize(l+normalize(uEye-vP))),0.0),48.0);vec3 c=vC*(.48+.52*d)+vec3(.24)*spec;float fog=clamp(length(uEye-vP)/35.0,0.0,.35);gl_FragColor=vec4(mix(c,vec3(.055,.13,.18),fog),1.0);}";
        program=GLES20.glCreateProgram();GLES20.glAttachShader(program,shader(GLES20.GL_VERTEX_SHADER,vertex));GLES20.glAttachShader(program,shader(GLES20.GL_FRAGMENT_SHADER,fragment));GLES20.glLinkProgram(program);
        int[] ok=new int[1];GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetProgramInfoLog(program));
        positionLoc=GLES20.glGetAttribLocation(program,"aPosition");normalLoc=GLES20.glGetAttribLocation(program,"aNormal");colourLoc=GLES20.glGetAttribLocation(program,"aColour");matrixLoc=GLES20.glGetUniformLocation(program,"uVP");eyeLoc=GLES20.glGetUniformLocation(program,"uEye");
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);GLES20.glEnable(2884);last=0;
    }
    private static int shader(int type,String code){int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,code);GLES20.glCompileShader(s);int[] ok=new int[1];GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetShaderInfoLog(s));return s;}
    @Override public void onSurfaceChanged(GL10 gl,int w,int h){width=w;height=Math.max(h,1);GLES20.glViewport(0,0,w,h);camera();}
    private void camera(){
        eye[0]=(float)(Math.sin(yaw)*Math.cos(pitch)*distance);eye[1]=2+(float)Math.sin(pitch)*distance;eye[2]=(float)(Math.cos(yaw)*Math.cos(pitch)*distance);
        Matrix.perspectiveM(projection,0,46,width/(float)height,.1f,60);
        Matrix.setLookAtM(view,0,eye[0],eye[1],eye[2],0,2,0,0,1,0);Matrix.multiplyMM(vp,0,projection,0,view,0);Matrix.invertM(inverse,0,vp,0);
    }
    public void resetClock(){last=0;}
    public void reset(){body.reset();yaw=.48f;pitch=.42f;distance=11.5f;last=0;camera();}
    @Override public void onDrawFrame(GL10 gl){
        long now=System.nanoTime();float dt=last==0?0:Math.min((now-last)*1e-9f,.05f);last=now;body.softness=softness;body.gravity=gravity;body.advance(dt);camera();
        GLES20.glClearColor(.047f,.105f,.15f,1);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);used=0;
        room();
        // A subtle projected contact shadow makes height legible without hiding the mesh.
        float[] centre=body.centre();ellipsoid(centre[0],.03f,centre[2],1.25f,.001f,.65f,0xFF163C47);
        java.util.Arrays.fill(normals,0);
        for(int i=0;i<body.triangles.length;i+=3){int a=body.triangles[i]*3,b=body.triangles[i+1]*3,c=body.triangles[i+2]*3;float[] p=body.position;float ux=p[b]-p[a],uy=p[b+1]-p[a+1],uz=p[b+2]-p[a+2],vx=p[c]-p[a],vy=p[c+1]-p[a+1],vz=p[c+2]-p[a+2];float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;for(int node:new int[]{a,b,c}){normals[node]+=nx;normals[node+1]+=ny;normals[node+2]+=nz;}}
        for(int index:body.triangles){int a=index*3;float n=(float)Math.sqrt(normals[a]*normals[a]+normals[a+1]*normals[a+1]+normals[a+2]*normals[a+2]);if(n<1e-8)n=1;vertex(body.position[a],body.position[a+1],body.position[a+2],normals[a]/n,normals[a+1]/n,normals[a+2]/n,colour);}
        for(int side=-1;side<=1;side+=2){
            float[] e=attach(1.02f,.12f,side*.385f);ellipsoid(e[0],e[1],e[2],.072f,.09f,.035f,0xFF092B3C);ellipsoid(e[0]+.015f,e[1]+.027f,e[2]+side*.028f,.022f,.025f,.012f,0xFFFFFFFF);
            for(int i=0;i<10;i++){float t=i/9f;float[] m=attach(1.04f+t*.47f,-.11f-(float)Math.sin(t*Math.PI)*.045f,side*(.40f-t*.19f));ellipsoid(m[0],m[1],m[2],.03f,.014f,.016f,0xFF1A6575);}
        }
        ellipsoid(body.ball[0],body.ball[1],body.ball[2],.42f,.42f,.42f,0xFFFFAA86);
        // Gold stripe on the ball gives rotation-free depth and a distinctive pick target.
        ellipsoid(body.ball[0],body.ball[1]+.25f,body.ball[2],.33f,.06f,.33f,0xFFFFD398);
        buffer.clear();buffer.put(data,0,used).position(0);GLES20.glUseProgram(program);GLES20.glUniformMatrix4fv(matrixLoc,1,false,vp,0);GLES20.glUniform3fv(eyeLoc,1,eye,0);
        buffer.position(0);GLES20.glVertexAttribPointer(positionLoc,3,GLES20.GL_FLOAT,false,36,buffer);GLES20.glEnableVertexAttribArray(positionLoc);
        buffer.position(3);GLES20.glVertexAttribPointer(normalLoc,3,GLES20.GL_FLOAT,false,36,buffer);GLES20.glEnableVertexAttribArray(normalLoc);
        buffer.position(6);GLES20.glVertexAttribPointer(colourLoc,3,GLES20.GL_FLOAT,false,36,buffer);GLES20.glEnableVertexAttribArray(colourLoc);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,used/9);
    }
    private float[] attach(float x,float y,float z){float[] p=new float[3];float total=0;for(int i=0;i<156;i++){float dx=body.rest[i*3]-x,dy=body.rest[i*3+1]-y,dz=body.rest[i*3+2]-z;float d=dx*dx+dy*dy+dz*dz;float w=1/(d*d+.0001f);total+=w;for(int k=0;k<3;k++)p[k]+=(body.position[i*3+k]-body.rest[i*3+k])*w;}p[0]=x+p[0]/total;p[1]=y+p[1]/total;p[2]=z+p[2]/total;return p;}
    private void room(){
        box(0,-.10f,0,3.85f,.1f,3.85f,0xFF315B64);
        // Open front, side walls and a recessed window: a small, tangible play room.
        float[] back={-3.85f,0,-3.85f,3.85f,0,-3.85f,3.85f,5.5f,-3.85f,-3.85f,5.5f,-3.85f};quad(back,0xFF264957);
        float[] left={-3.85f,0,3.85f,-3.85f,0,-3.85f,-3.85f,5.5f,-3.85f,-3.85f,5.5f,3.85f};quad(left,0xFF21424F);
        box(0,.10f,-3.8f,3.8f,.10f,.06f,0xFF76B1AD);box(-3.8f,.10f,0,.06f,.10f,3.8f,0xFF76B1AD);
        box(3.8f,.07f,0,.025f,.07f,3.8f,0xFF8CB4B3);box(0,.07f,3.8f,3.8f,.07f,.025f,0xFF8CB4B3);
        for(int i=-3;i<=3;i++){box(i,.004f,0,.008f,.004f,3.75f,0xFF3A6871);box(0,.004f,i,3.75f,.004f,.008f,0xFF3A6871);}
        box(.8f,3.55f,-3.82f,1.15f,.85f,.025f,0xFF5EADB8);box(.8f,3.55f,-3.77f,.025f,.85f,.03f,0xFF9EDDD5);box(.8f,3.55f,-3.77f,1.15f,.025f,.03f,0xFF9EDDD5);
        for(float[] b:SoftBody3D.BLOCKS)box(b[0],b[1],b[2],b[3],b[4],b[5],0xFF498D91);
    }
    private void quad(float[] p,int col){triangle(p,0,p,3,p,6,col);triangle(p,0,p,6,p,9,col);}
    private void vertex(float x,float y,float z,float nx,float ny,float nz,int col){data[used++]=x;data[used++]=y;data[used++]=z;data[used++]=nx;data[used++]=ny;data[used++]=nz;data[used++]=((col>>16)&255)/255f;data[used++]=((col>>8)&255)/255f;data[used++]=(col&255)/255f;}
    private void triangle(float[] a,int ai,float[] b,int bi,float[] c,int ci,int col){float ux=b[bi]-a[ai],uy=b[bi+1]-a[ai+1],uz=b[bi+2]-a[ai+2],vx=c[ci]-a[ai],vy=c[ci+1]-a[ai+1],vz=c[ci+2]-a[ai+2];float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;float len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(len<1e-8)return;nx/=len;ny/=len;nz/=len;vertex(a[ai],a[ai+1],a[ai+2],nx,ny,nz,col);vertex(b[bi],b[bi+1],b[bi+2],nx,ny,nz,col);vertex(c[ci],c[ci+1],c[ci+2],nx,ny,nz,col);}
    private void box(float x,float y,float z,float rx,float ry,float rz,int col){
        float[] p={x-rx,y-ry,z-rz,x+rx,y-ry,z-rz,x+rx,y+ry,z-rz,x-rx,y+ry,z-rz,x-rx,y-ry,z+rz,x+rx,y-ry,z+rz,x+rx,y+ry,z+rz,x-rx,y+ry,z+rz};
        int[] ids={0,2,1,0,3,2,4,5,6,4,6,7,0,4,7,0,7,3,1,2,6,1,6,5,3,7,6,3,6,2,0,1,5,0,5,4};for(int i=0;i<ids.length;i+=3)triangle(p,ids[i]*3,p,ids[i+1]*3,p,ids[i+2]*3,col);
    }
    private static float[][] makeSphere(){float[][] s=new float[8*12*6][3];int at=0;for(int r=0;r<8;r++)for(int t=0;t<12;t++){float[][] q=new float[4][3];for(int k=0;k<4;k++){double a=Math.PI*(r+(k>=2?1:0))/8,b=Math.PI*2*(t+(k==1||k==2?1:0))/12;q[k]=new float[]{(float)(Math.sin(a)*Math.cos(b)),(float)Math.cos(a),(float)(Math.sin(a)*Math.sin(b))};}for(int k:new int[]{0,1,2,0,2,3})s[at++]=q[k];}return s;}
    private void ellipsoid(float x,float y,float z,float rx,float ry,float rz,int col){for(float[] s:sphere){float nx=s[0]/rx,ny=s[1]/ry,nz=s[2]/rz;float n=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);vertex(x+s[0]*rx,y+s[1]*ry,z+s[2]*rz,nx/n,ny/n,nz/n,col);}}
    private void ray(float sx,float sy){float[] a={sx/width*2-1,1-sy/height*2,-1,1},b={a[0],a[1],1,1},pa=new float[4],pb=new float[4];Matrix.multiplyMV(pa,0,inverse,0,a,0);Matrix.multiplyMV(pb,0,inverse,0,b,0);float len=0;for(int k=0;k<3;k++){rayOrigin[k]=pa[k]/pa[3];rayDir[k]=pb[k]/pb[3]-rayOrigin[k];len+=rayDir[k]*rayDir[k];}len=(float)Math.sqrt(len);for(int k=0;k<3;k++)rayDir[k]/=len;}
    private float hit(float x,float y,float z,float radius){float dx=x-rayOrigin[0],dy=y-rayOrigin[1],dz=z-rayOrigin[2];float t=dx*rayDir[0]+dy*rayDir[1]+dz*rayDir[2];float dd=dx*dx+dy*dy+dz*dz-t*t;return t>0&&dd<radius*radius?t-(float)Math.sqrt(radius*radius-dd):Float.MAX_VALUE;}
    public void down(float x,float y){camera();ray(x,y);float nearest=hit(body.ball[0],body.ball[1],body.ball[2],.48f);body.grabBall=nearest<Float.MAX_VALUE;tapNode=-1;for(int i=0;i<body.count();i++){float t=hit(body.position[i*3],body.position[i*3+1],body.position[i*3+2],.15f);if(t<nearest){nearest=t;tapNode=i;body.grabBall=false;}}picked=nearest<Float.MAX_VALUE;body.grabbed=tapNode;grabDistance=nearest;downX=x;downY=y;moved=false;if(picked)for(int k=0;k<3;k++)body.target[k]=body.grabBall?body.ball[k]:body.position[tapNode*3+k];}
    public void move(float x,float y,float dx,float dy){if(Math.hypot(x-downX,y-downY)>8)moved=true;if(picked){ray(x,y);for(int k=0;k<3;k++)body.target[k]=SoftBody3D.clamp(rayOrigin[k]+rayDir[k]*grabDistance,k==1?.2f:-3.6f,k==1?5.2f:3.6f);}else {yaw+=dx*.008f;pitch=SoftBody3D.clamp(pitch+dy*.005f,.12f,1.1f);}}
    public void pinch(float ratio){if(picked){grabDistance=SoftBody3D.clamp(grabDistance/ratio,3,19);for(int k=0;k<3;k++)body.target[k]=SoftBody3D.clamp(rayOrigin[k]+rayDir[k]*grabDistance,k==1?.2f:-3.6f,k==1?5.2f:3.6f);}else distance=SoftBody3D.clamp(distance/ratio,7,20);}
    public void up(boolean cancel){if(!cancel&&!moved&&picked){if(body.grabBall){body.ballVelocity[1]+=4;body.ballVelocity[0]-=2;}else body.poke(tapNode);}body.grabbed=-1;body.grabBall=false;picked=false;}
}
