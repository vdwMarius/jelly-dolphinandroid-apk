import com.jellydolphin.app.SoftBody3D;
public final class PhysicsTest {
 static int assertions;
 static void check(boolean test,String message){assertions++;if(!test)throw new AssertionError(message);}
 static void run(SoftBody3D b,int frames){for(int i=0;i<frames;i++)b.advance(1f/60);}
 static float dist(float[] p,int a,int b){float d=0;for(int k=0;k<3;k++){float x=p[3*a+k]-p[3*b+k];d+=x*x;}return (float)Math.sqrt(d);}
 static float shapeError(SoftBody3D b){float e=0;int n=0;for(int i=0;i<132;i++){e+=Math.abs(dist(b.position,i,i+24)-dist(b.rest,i,i+24));n++;}return e/n;}
 public static void main(String[] args){
  SoftBody3D b=new SoftBody3D();check(b.count()>400,"Body, dorsal fin, flippers and tail must have volumetric particles");check(b.triangles.length>2000,"3D mesh must have surface faces");
  b.gravity=false;float initial=b.centre()[1];run(b,60);check(Math.abs(b.centre()[1]-initial)<.01,"No-gravity body remains in place");
  b.poke(72);run(b,8);check(shapeError(b)>.0001,"Local tap deforms the actual mesh");run(b,300);check(shapeError(b)<.04,"Springs restore body shape after poke");
  b.reset();b.gravity=false;b.grabbed=72;b.target[0]=b.position[216];b.target[1]=b.position[217]+1.2f;b.target[2]=1;run(b,60);check(Math.abs(b.position[217]-b.target[1])<.1,"Grab constraint follows the target");check(b.position[218]>.8,"Grab works in depth");check(b.centre()[1]>3,"Grab lifts the whole connected body");
  b.grabbed=-1;b.impulse(2,1,-3);float before=b.centre()[2];run(b,10);check(b.centre()[2]<before,"Throw moves through the third dimension");
  b.reset();b.setDepth(-1.5f);check(Math.abs(b.centre()[2]+1.5f)<.02,"Depth slider translates the body");
  b.reset();b.gravity=true;run(b,300);check(b.centre()[1]<1.2,"Gravity drops dolphin onto floor");check(shapeError(b)<.2,"Interior braces resist collapse at rest");
  for(float softness:new float[]{0,.65f,1}){b.reset();b.softness=softness;b.gravity=true;for(int f=0;f<1200;f++){if(f%140==0)b.impulse(4,7,-4);b.advance(1f/60);for(int i=0;i<b.position.length;i++){float v=b.position[i];check(Float.isFinite(v),"Simulation stays finite");check(v>=(i%3==1?.044f:-SoftBody3D.LIMIT)&&v<=(i%3==1?SoftBody3D.CEILING:SoftBody3D.LIMIT),"Particle stays inside room");}}}
  SoftBody3D firm=new SoftBody3D(),soft=new SoftBody3D();firm.gravity=soft.gravity=false;firm.softness=0;soft.softness=1;firm.poke(72);soft.poke(72);run(firm,8);run(soft,8);check(shapeError(soft)>shapeError(firm),"Softness controls real mesh compliance");
  b.reset();b.gravity=true;b.grabBall=true;b.target[0]=0;b.target[1]=3;b.target[2]=2;run(b,60);check(Math.abs(b.ball[2]-2)<.01,"Ball can be dragged in depth");b.grabBall=false;run(b,180);check(b.ball[1]>=.419f,"Ball collides with floor");
  b.reset();check(b.grabbed==-1&&!b.grabBall,"Reset releases all constraints");
  System.out.println("PASS: "+assertions+" 3D physics assertions");
 }
}
