package com.bigwen.opengl.gl;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.bigwen.opengl.gl.GLUtil;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by bigwen on 2020/8/30.
 */
public class SquerRender implements GLSurfaceView.Renderer {

    private Context mContext;
    private int programHandle;
    private FloatBuffer vertexBuffer;
    private ShortBuffer indexBuffer;
    private short[] indexArray = new short[]{0, 1, 2, 0, 2, 3};

    public SquerRender(Context mContext) {
        this.mContext = mContext;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        String vertexCode = GLUtil.readFromAssets(mContext, "triangle_vertex.glsl");
        String fragmentCode = GLUtil.readFromAssets(mContext, "triangle_fragment.glsl");
        programHandle = GLUtil.createAndLinkProgram(vertexCode, fragmentCode);
        vertexBuffer = GLUtil.createBuffer(new float[]{
                -1f, 1f, 0.0f,
                -1f, -1f, 0.0f,
                1f, -1f, 0.0f,
                1f, 1f, 0.0f
        });
        indexBuffer = GLUtil.createBuffer(indexArray);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glUseProgram(programHandle);
        int uPosition = GLES20.glGetAttribLocation(programHandle, "aPosition");
        GLUtil.setAttribute(uPosition, vertexBuffer, 3);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexArray.length, GLES20.GL_UNSIGNED_SHORT, indexBuffer);
    }
}
