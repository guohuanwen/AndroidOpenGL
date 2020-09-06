package com.bigwen.opengl.camera;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.view.TextureView;

import com.bigwen.opengl.gl.ve_gl.EglBase;
import com.bigwen.opengl.gl.ve_gl.GlRectDrawer;

/**
 * Created by bigwen on 2020/8/22.
 */
public class PreviewProxy {

    private TextureView mTextureView = null;

    // 用于展示预览图的相关变量
    private EglBase previewEglBase = null;
    private GlRectDrawer previewDrawer = null;
    private float[] mPreviewMatrix = new float[16];
    private int mViewWidth = 0;
    private int mViewHeight = 0;

    // 设置Texture.SurfaceTextureListener回调监听
    public void doSetRendererView(TextureView temp, TextureView.SurfaceTextureListener textureListener) {
        if (mTextureView != null) {
            if (mTextureView.getSurfaceTextureListener().equals(textureListener)) {
                mTextureView.setSurfaceTextureListener(null);
            }
            releasePreviewSurface();
        }

        mTextureView = temp;
        if (mTextureView != null) {
            mTextureView.setSurfaceTextureListener(textureListener);
        }
    }

    // 绘制图像数据到屏幕
    public void drawToPreview(int textureId, int width, int height, float[] texMatrix, EglBase mDummyContext, int mViewMode) {
        if (previewEglBase == null) {
            previewEglBase = EglBase.create(mDummyContext.getEglBaseContext(), EglBase.CONFIG_RGBA);
        }

        if (mTextureView != null) {
            attachTextureView();
        }

        if (!previewEglBase.hasSurface()) {
            return ;
        }

        if (previewDrawer == null) {
            previewDrawer = new GlRectDrawer();
        }

        try {
            // 绑定eglContext、eglDisplay、eglSurface
            previewEglBase.makeCurrent();

            // 作用是使图像正立显示
            int scaleWidth = mViewWidth;
            int scaleHeight = mViewHeight;

            System.arraycopy(texMatrix, 0, mPreviewMatrix, 0, 16);
            if (mViewMode == 0) {
                if (mViewHeight * width <= mViewWidth * height) {
                    scaleWidth = mViewHeight * width / height;
                } else {
                    scaleHeight = mViewWidth * height / width;
                }
            } else if (mViewMode == 1) {
                if (mViewHeight * width <= mViewWidth * height) {
                    scaleHeight = mViewWidth * height / width;
                } else {
                    scaleWidth = mViewHeight * width / height;
                }
                float fWidthScale = (float)mViewWidth / scaleWidth;
                float fHeightScale = (float)mViewHeight / scaleHeight;
                Matrix.scaleM(mPreviewMatrix, 0, fWidthScale, fHeightScale, 1.0f);
                Matrix.translateM(mPreviewMatrix, 0, (1.0f - fWidthScale) / 2.0f, (1.0f - fHeightScale) / 2.0f, 1.0f);

                scaleWidth = mViewWidth;
                scaleHeight = mViewHeight;
            }

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            // 绘制rgb格式图像
            previewDrawer.drawRgb(textureId, mPreviewMatrix, width, height,
                    (mViewWidth - scaleWidth) / 2,
                    (mViewHeight - scaleHeight) / 2,
                    scaleWidth, scaleHeight);
            // 交换渲染好的buffer 去显示
            previewEglBase.swapBuffers();
            // 分离当前eglContext
            previewEglBase.detachCurrent();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public void release(TextureView.SurfaceTextureListener textureListener) {
        releasePreviewSurface();
        releaseDrawer();
        releaseTexture(textureListener);
    }

    private void releaseDrawer() {
        if (previewDrawer != null) {
            previewDrawer.release();
            previewDrawer = null;
        }
    }

    private void releaseTexture(TextureView.SurfaceTextureListener surfaceTextureListener) {
        if (mTextureView != null) {
            if (mTextureView.getSurfaceTextureListener().equals(surfaceTextureListener)) {
                mTextureView.setSurfaceTextureListener(null);
            }
            mTextureView = null;
        }
    }

    // 销毁用于屏幕显示的surface（预览）
    public void releasePreviewSurface() {
        if (previewEglBase == null) {
            return ;
        }

        if (previewEglBase.hasSurface()) {
            // 绑定eglContext、eglDisplay、eglSurface
            previewEglBase.makeCurrent();

            if (previewDrawer != null) {
                previewDrawer = null;
            }

            previewEglBase.releaseSurface();
            previewEglBase.detachCurrent();
        }

        previewEglBase.release();
        previewEglBase = null;
    }

    // 设置预览视图
    private void attachTextureView() {
        if (previewEglBase.hasSurface()) {
            return;
        }

        if (!mTextureView.isAvailable()) {
            return;
        }

        mViewWidth = mTextureView.getWidth();
        mViewHeight = mTextureView.getHeight();
        try {
            // 创建EGLSurface
            previewEglBase.createSurface(mTextureView.getSurfaceTexture());
        } catch (RuntimeException e) {
            e.printStackTrace();
            releasePreviewSurface();
            mViewWidth = 0;
            mViewHeight = 0;
        }
    }
}
