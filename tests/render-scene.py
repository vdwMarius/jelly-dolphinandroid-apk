"""Rasterize the Java renderer's captured frame using real headless Mesa GLES 2."""
import ctypes as C, os, pathlib
os.environ.setdefault('EGL_PLATFORM','surfaceless')
os.environ.setdefault('MESA_SHADER_CACHE_DIR','/tmp/jelly-mesa-cache')
from PIL import Image
import numpy as np
E=C.CDLL('libEGL.so.1')
def fn(lib,name,ret,args):
 f=getattr(lib,name);f.restype=ret;f.argtypes=args;return f
I=C.c_int;U=C.c_uint;P=C.c_void_p;F=C.c_float
get_display=fn(E,'eglGetDisplay',P,[P]);display=get_display(None)
major=I();minor=I();assert fn(E,'eglInitialize',U,[P,C.POINTER(I),C.POINTER(I)])(display,C.byref(major),C.byref(minor)),'EGL init failed'
assert fn(E,'eglBindAPI',U,[U])(0x30A0)
attrs=(I*17)(0x3033,1,0x3040,4,0x3024,8,0x3023,8,0x3022,8,0x3025,16,0x3021,8,0x3038,0,0)
config=P();num=I();assert fn(E,'eglChooseConfig',U,[P,C.POINTER(I),C.POINTER(P),I,C.POINTER(I)])(display,attrs,C.byref(config),1,C.byref(num)) and num.value
surface=fn(E,'eglCreatePbufferSurface',P,[P,P,C.POINTER(I)])(display,config,(I*5)(0x3057,720,0x3056,800,0x3038))
context=fn(E,'eglCreateContext',P,[P,P,P,C.POINTER(I)])(display,config,None,(I*3)(0x3098,2,0x3038))
assert context and surface
assert fn(E,'eglMakeCurrent',U,[P,P,P,P])(display,surface,surface,context)
get_proc=fn(E,'eglGetProcAddress',P,[C.c_char_p])
def gl(name,ret,*args):return C.CFUNCTYPE(ret,*args)(get_proc(name.encode()))
get_string=gl('glGetString',C.c_char_p,U)
print('GLES renderer:',get_string(0x1F01).decode())
def shader(kind,path):
 s=gl('glCreateShader',U,U)(kind);src=pathlib.Path(path).read_bytes();ptr=C.c_char_p(src)
 gl('glShaderSource',None,U,I,C.POINTER(C.c_char_p),P)(s,1,C.byref(ptr),None);gl('glCompileShader',None,U)(s)
 ok=I();gl('glGetShaderiv',None,U,U,C.POINTER(I))(s,0x8B81,C.byref(ok))
 if not ok.value:
  log=C.create_string_buffer(4096);gl('glGetShaderInfoLog',None,U,I,P,P)(s,4096,None,log);raise AssertionError(log.value)
 return s
program=gl('glCreateProgram',U)()
for kind,path in [(0x8B31,'build/scene/vertex.glsl'),(0x8B30,'build/scene/fragment.glsl')]:gl('glAttachShader',None,U,U)(program,shader(kind,path))
gl('glLinkProgram',None,U)(program);ok=I();gl('glGetProgramiv',None,U,U,C.POINTER(I))(program,0x8B82,C.byref(ok));assert ok.value,'Link failed'
gl('glUseProgram',None,U)(program)
data=np.fromfile('build/scene/scene.bin',dtype='<f4');vertices=data[19:].copy();assert np.isfinite(data).all()
get_uniform=gl('glGetUniformLocation',I,U,C.c_char_p)
gl('glUniformMatrix4fv',None,I,I,U,P)(get_uniform(program,b'uVP'),1,0,data.ctypes.data)
gl('glUniform3fv',None,I,I,P)(get_uniform(program,b'uEye'),1,data[16:19].ctypes.data)
vbo=U();gl('glGenBuffers',None,I,C.POINTER(U))(1,C.byref(vbo));gl('glBindBuffer',None,U,U)(0x8892,vbo);gl('glBufferData',None,U,C.c_ssize_t,P,U)(0x8892,vertices.nbytes,vertices.ctypes.data,0x88E4)
for index,name in enumerate([b'aPosition',b'aNormal',b'aColour']):
 loc=gl('glGetAttribLocation',I,U,C.c_char_p)(program,name);gl('glEnableVertexAttribArray',None,U)(loc);gl('glVertexAttribPointer',None,U,I,U,U,I,P)(loc,3,0x1406,0,36,C.c_void_p(index*12))
gl('glEnable',None,U)(0x0B71);gl('glEnable',None,U)(2884);gl('glViewport',None,I,I,I,I)(0,0,720,800);gl('glClearColor',None,F,F,F,F)(.047,.105,.15,1);gl('glClear',None,U)(0x4100)
gl('glDrawArrays',None,U,I,I)(4,0,len(vertices)//9);gl('glFinish',None)()
assert gl('glGetError',U)()==0,'GLES rendering error'
pixels=np.zeros((800,720,4),dtype=np.uint8);gl('glReadPixels',None,I,I,I,I,U,U,P)(0,0,720,800,0x1908,0x1401,pixels.ctypes.data)
assert len(np.unique(pixels.reshape(-1,4),axis=0))>500,'Frame lacks expected shaded geometry'
Image.fromarray(pixels[::-1]).save('build/scene/room.png')
print('PASS: actual GLES shader compilation, geometry submission and shaded frame. build/scene/room.png')
