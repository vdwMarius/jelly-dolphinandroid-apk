package android.opengl;
import java.nio.*;
import java.nio.file.*;
import java.io.*;
/** Captures renderer output for the real Mesa GLES shader/rasterization test. */
public class GLES20 {
 public static final int GL_VERTEX_SHADER=35633,GL_FRAGMENT_SHADER=35632,GL_LINK_STATUS=35714,GL_COMPILE_STATUS=35713,GL_DEPTH_TEST=2929,GL_COLOR_BUFFER_BIT=16384,GL_DEPTH_BUFFER_BIT=256,GL_FLOAT=5126,GL_TRIANGLES=4;
 public static float[] matrix,eye; public static FloatBuffer data; static int shader;
 public static int glCreateProgram(){return 1;}public static int glCreateShader(int t){return t;}
 public static void glShaderSource(int s,String code){try{Files.writeString(Path.of("build/scene/"+(s==GL_VERTEX_SHADER?"vertex":"fragment")+".glsl"),code);}catch(IOException e){throw new RuntimeException(e);}}
 public static void glCompileShader(int s){}public static void glAttachShader(int a,int b){}public static void glLinkProgram(int s){}public static void glGetProgramiv(int a,int b,int[] c,int d){c[d]=1;}public static void glGetShaderiv(int a,int b,int[] c,int d){c[d]=1;}public static String glGetProgramInfoLog(int s){return "";}public static String glGetShaderInfoLog(int s){return "";}
 public static int glGetAttribLocation(int p,String s){return s.equals("aPosition")?0:s.equals("aNormal")?1:2;}public static int glGetUniformLocation(int p,String s){return s.equals("uVP")?0:1;}
 public static void glEnable(int n){}public static void glViewport(int a,int b,int c,int d){}public static void glClearColor(float a,float b,float c,float d){}public static void glClear(int n){}public static void glUseProgram(int n){}
 public static void glUniformMatrix4fv(int a,int b,boolean c,float[] d,int e){matrix=d.clone();}public static void glUniform3fv(int a,int b,float[] c,int d){eye=c.clone();}
 public static void glVertexAttribPointer(int a,int b,int c,boolean d,int e,Buffer f){if(a==0){data=((FloatBuffer)f).duplicate();data.position(0);}}public static void glEnableVertexAttribArray(int n){}
 public static void glDrawArrays(int a,int b,int count){try{ByteBuffer bytes=ByteBuffer.allocate((19+count*9)*4).order(ByteOrder.LITTLE_ENDIAN);for(float v:matrix)bytes.putFloat(v);for(float v:eye)bytes.putFloat(v);for(int i=0;i<count*9;i++)bytes.putFloat(data.get(i));Files.write(Path.of("build/scene/scene.bin"),bytes.array());}catch(IOException e){throw new RuntimeException(e);}}
}
