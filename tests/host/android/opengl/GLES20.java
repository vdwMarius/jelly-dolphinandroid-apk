package android.opengl;
import java.nio.*;
import java.nio.file.*;
import java.io.*;
/** Captures renderer output for the real Mesa GLES shader/rasterization test. */
public class GLES20 {
 public static final int GL_VERTEX_SHADER=35633,GL_FRAGMENT_SHADER=35632,GL_LINK_STATUS=35714,GL_COMPILE_STATUS=35713,GL_DEPTH_TEST=2929,GL_COLOR_BUFFER_BIT=16384,GL_DEPTH_BUFFER_BIT=256,GL_FLOAT=5126,GL_TRIANGLES=4,GL_BLEND=3042,GL_BACK=1029,GL_FRONT=1028,GL_ONE=1,GL_ONE_MINUS_SRC_ALPHA=771;
 public static float[] matrix,eye; public static FloatBuffer data; static int shader;
 public static final java.util.ArrayList<String> calls=new java.util.ArrayList<>();
 static int cull=GL_BACK;static boolean blend,depthWrite=true;static float jelly,clarity;
 public static void glUniform1f(int loc,float value){if(loc==2)jelly=value;else if(loc==3)clarity=value;}
 public static void glDepthMask(boolean value){depthWrite=value;}public static void glCullFace(int value){cull=value;}public static void glBlendFunc(int a,int b){if(a!=GL_ONE||b!=GL_ONE_MINUS_SRC_ALPHA)throw new AssertionError("Expected premultiplied blending");}
 public static void glDisable(int value){if(value==GL_BLEND)blend=false;}
 public static int glCreateProgram(){return 1;}public static int glCreateShader(int t){return t;}
 public static void glShaderSource(int s,String code){try{Files.writeString(Path.of("build/scene/"+(s==GL_VERTEX_SHADER?"vertex":"fragment")+".glsl"),code);}catch(IOException e){throw new RuntimeException(e);}}
 public static void glCompileShader(int s){}public static void glAttachShader(int a,int b){}public static void glLinkProgram(int s){}public static void glGetProgramiv(int a,int b,int[] c,int d){c[d]=1;}public static void glGetShaderiv(int a,int b,int[] c,int d){c[d]=1;}public static String glGetProgramInfoLog(int s){return "";}public static String glGetShaderInfoLog(int s){return "";}
 public static int glGetAttribLocation(int p,String s){return s.equals("aPosition")?0:s.equals("aNormal")?1:2;}public static int glGetUniformLocation(int p,String s){return s.equals("uVP")?0:s.equals("uEye")?1:s.equals("uJelly")?2:3;}
 public static void glEnable(int n){if(n==GL_BLEND)blend=true;}public static void glViewport(int a,int b,int c,int d){}public static void glClearColor(float a,float b,float c,float d){}public static void glClear(int n){calls.clear();}public static void glUseProgram(int n){}
 public static void glUniformMatrix4fv(int a,int b,boolean c,float[] d,int e){matrix=d.clone();}public static void glUniform3fv(int a,int b,float[] c,int d){eye=c.clone();}
 public static void glVertexAttribPointer(int a,int b,int c,boolean d,int e,Buffer f){if(a==0){data=((FloatBuffer)f).duplicate();data.position(0);}}public static void glEnableVertexAttribArray(int n){}
 public static void glDrawArrays(int mode,int first,int count){
  calls.add(String.format(java.util.Locale.US,"{\"first\":%d,\"count\":%d,\"jelly\":%f,\"clarity\":%f,\"cull\":%d,\"blend\":%b,\"depthWrite\":%b}",first,count,jelly,clarity,cull,blend,depthWrite));
  try{ByteBuffer bytes=ByteBuffer.allocate((19+(first+count)*9)*4).order(ByteOrder.LITTLE_ENDIAN);for(float v:matrix)bytes.putFloat(v);for(float v:eye)bytes.putFloat(v);for(int i=0;i<(first+count)*9;i++)bytes.putFloat(data.get(i));Files.write(Path.of("build/scene/scene.bin"),bytes.array());Files.writeString(Path.of("build/scene/draws.json"),"["+String.join(",",calls)+"]");}catch(IOException e){throw new RuntimeException(e);}
 }
}
