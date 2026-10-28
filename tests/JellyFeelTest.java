import com.jellydolphin.app.SoftBody3D;
/** Regressions for the requested toy feel: bounded shape, visible oscillation, and recovery. */
public final class JellyFeelTest {
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 static float distance(float[] p,int a,int b){float v=0;for(int k=0;k<3;k++){float d=p[a*3+k]-p[b*3+k];v+=d*d;}return (float)Math.sqrt(v);}
 public static void main(String[] args){
  for(float softness:new float[]{0,.65f,1}){
   SoftBody3D b=new SoftBody3D();b.gravity=false;b.softness=softness;
   float rest=distance(b.rest,72,78),peak=0,late=0,previous=0;int rebounds=0;
   b.poke(72);
   for(int frame=0;frame<180;frame++){
    b.advance(1f/60);float signed=distance(b.position,72,78)-rest;peak=Math.max(peak,Math.abs(signed));
    if(Math.abs(signed)>.002){if(previous*signed<0)rebounds++;previous=signed;}
    if(frame>150)late=Math.max(late,Math.abs(signed));
   }
   check(peak>.01,"A tap must visibly squish the mesh at softness "+softness);
   check(peak<.16,"A tap must keep the dolphin recognisable");
   check(rebounds>=4,"Toy must wobble through repeated compression and expansion");
   check(late<peak*.12,"Wobble must decay naturally");
   b.reset();b.softness=softness;b.gravity=false;b.grabbed=72;
   b.target[0]=b.position[216]+2;b.target[1]=b.position[217]+1;b.target[2]=1.5f;
   float maxStretch=1;
   for(int frame=0;frame<180;frame++){
    b.advance(1f/60);
    for(int i=24;i<132;i++)maxStretch=Math.max(maxStretch,distance(b.position,i,i+24)/distance(b.rest,i,i+24));
   }
   check(maxStretch<1.30,"A hard drag must lift a toy, not stretch a vertex into a string: "+maxStretch);
   check(b.centre()[0]>1,"Distributed grab must still move the dolphin");
   // Rotation is allowed; the shape envelope is not anchored to a world-space orientation.
   b.reset();b.gravity=false;
   for(int i=0;i<b.count();i++){float x=b.rest[i*3],y=b.rest[i*3+1];b.position[i*3]=-y;b.position[i*3+1]=x+2.8f;}
   for(int i=0;i<30;i++)b.advance(1f/60);
   float xDiff=Math.abs(b.position[120]-b.position[0]),yDiff=Math.abs(b.position[121]-b.position[1]);
   check(yDiff>xDiff*2,"Shape recovery must preserve a freely rotated dolphin");
   System.out.printf(java.util.Locale.US,"softness %.2f: peak squish %.3f, rebounds %d, late %.4f, maximum drag stretch %.3f%n",softness,peak,rebounds,late,maxStretch);
  }
  SoftBody3D resting=new SoftBody3D();
  for(int i=0;i<300;i++)resting.advance(1f/60);
  float min=10,max=-10;
  for(int frame=0;frame<120;frame++){resting.advance(1f/60);float y=resting.centre()[1];min=Math.min(min,y);max=Math.max(max,y);}
  check(max<1.2f&&max-min<.15f,"Resting contacts must not inject energy or create endless bouncing");
  System.out.println("PASS: toy shape retention, visible damped wobble, bounded dragging, free rotation and resting contact stability");
 }
}
