import com.jellydolphin.app.JellyPhysics;
public class PhysicsTest {
 static int checks;
 static void check(boolean condition,String description){checks++;if(!condition)throw new AssertionError(description);}
 static JellyPhysics body(){JellyPhysics p=new JellyPhysics();p.bounds(600,600,100,60);p.reset();return p;}
 static void run(JellyPhysics p,int count){for(int i=0;i<count;i++)p.step(1f/60);}
 public static void main(String[] args){
  JellyPhysics p=body();p.poke(1);run(p,8);check(Math.abs(p.squish)>.02,"Tap visibly deforms the body");run(p,1200);check(Math.abs(p.squish)<.001,"Deformation settles");
  p=body();p.x=499;p.vx=700;p.step(.016f);check(p.vx<0,"Right wall reflects velocity");check(p.squishVelocity>0,"Collision excites deformation");
  p=body();p.gravity=true;run(p,1800);check(Math.abs(p.y-540)<2,"Gravity settles at floor");
  p=body();p.dragging=true;p.targetX=420;p.targetY=220;run(p,180);check(Math.abs(p.x-420)<1&&Math.abs(p.y-220)<1,"Drag spring follows target");
  p.dragging=false;p.vx=600;float before=p.x;run(p,5);check(p.x>before,"Release preserves momentum");
  JellyPhysics firm=body(),soft=body();firm.softness=0;soft.softness=1;firm.poke(1);soft.poke(1);run(firm,12);run(soft,12);check(Math.abs(soft.squish)>Math.abs(firm.squish),"Soft body deforms more than firm body");
  for(int s=0;s<=10;s++){p=body();p.softness=s/10f;p.gravity=true;for(int i=0;i<18000;i++){if(i%77==0)p.shake(1300,-1800);p.step(i%2==0?.05f:.001f);check(Float.isFinite(p.x)&&Float.isFinite(p.squish),"Stable state under repeated impulses");check(p.x>=100&&p.x<=500&&p.y>=60&&p.y<=540,"Body stays within bounds");}}
  p.reset();check(p.vx==0&&p.squish==0&&!p.dragging,"Reset clears motion");
  System.out.println("PASS: "+checks+" physics assertions");
 }
}
