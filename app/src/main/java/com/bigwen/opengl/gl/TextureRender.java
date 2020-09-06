package com.bigwen.opengl.gl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;

import com.bigwen.opengl.R;
import com.bigwen.opengl.gl.GLUtil;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by bigwen on 2020/8/30.
 */
public class TextureRender implements GLSurfaceView.Renderer {

    private Context mContext;
    private int programHandle;

    private FloatBuffer vertexBuffer = GLUtil.createBuffer(new float[]{
            -1f, 1f, 0.0f,
            -1f, -1f, 0.0f,
            1f, -1f, 0.0f,
            1f, 1f, 0.0f
    });

    private short[] vertexIndexArray = new short[]{0, 1, 2, 0, 2, 3};
    private ShortBuffer indexBuffer = GLUtil.createBuffer(vertexIndexArray);

    //纹理坐标的原点是左上角，右下角是（1，1），将整张图片绘制的纹理顶点数据代码如下：
    private FloatBuffer texBuffer = GLUtil.createBuffer(
            new float[]{
                    0.0f, 0.0f,
                    0.0f, 1.0f,
                    1.0f, 1.0f,
                    1.0f, 0.0f});

    private int textureId;

    public TextureRender(Context mContext) {
        this.mContext = mContext;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        String vertexCode = GLUtil.readFromAssets(mContext, "texture_vertex.glsl");
        String fragmentCode = GLUtil.readFromAssets(mContext, "texture_fragment.glsl");
        programHandle = GLUtil.createAndLinkProgram(vertexCode, fragmentCode);

        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.bitmap);

        textureId = GLUtil.createTexture();
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glUseProgram(programHandle);
        //设置顶点数据
        vertexBuffer.position(0);
        int aPosition = GLES20.glGetAttribLocation(programHandle, "aPosition");
        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        //设置纹理顶点数据
        texBuffer.position(0);
        int texCoordinateLoc = GLES20.glGetAttribLocation(programHandle, "aTexCoordinate");
        GLES20.glEnableVertexAttribArray(texCoordinateLoc);
        GLES20.glVertexAttribPointer(texCoordinateLoc, 2, GLES20.GL_FLOAT, false, 0, texBuffer);

        //设置纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(texCoordinateLoc, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, vertexIndexArray.length, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

    }
}
