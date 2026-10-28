import com.jellydolphin.app.RoomRenderer;
import android.opengl.*;
public class SceneTest {
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 static float[] screen(float[] p){float[] v={p[0],p[1],p[2],1},o=new float[4];Matrix.multiplyMV(o,0,GLES20.matrix,0,v,0);return new float[]{(o[0]/o[3]+1)*360,(1-o[1]/o[3])*400};}
 public static void main(String[] args){RoomRenderer r=new RoomRenderer();r.onSurfaceCreated(null,null);r.onSurfaceChanged(null,720,800);r.onDrawFrame(null);check(GLES20.data!=null,"Renderer submits geometry");
  float[] s=screen(r.body.centre());r.down(s[0],s[1]);check(r.body.grabbed>=0,"Ray picks dolphin in perspective");r.move(s[0],s[1]-80,0,-80);check(r.body.target[1]>r.body.centre()[1],"Dragging lifts in camera plane");float z=r.body.target[2];r.pinch(1.15f);check(Math.abs(r.body.target[2]-z)>.01,"Pinch changes grab depth");r.up(true);check(r.body.grabbed==-1,"Cancel releases dolphin");
  s=screen(r.body.ball);r.down(s[0],s[1]);check(r.body.grabBall,"Ray picks interactive ball");r.up(false);check(r.body.ballVelocity[1]>0,"Tapping ball launches it");
  r.down(1,1);float yaw=r.yaw;r.move(100,40,99,39);check(r.yaw!=yaw,"Empty-space drag orbits camera");r.up(true);r.reset();r.onDrawFrame(null);
  System.out.println("PASS: renderer geometry, perspective picking, lift, pinch depth, ball interaction, orbit and reset");}
}
