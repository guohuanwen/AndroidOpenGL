package com.bigwen.opengl.camera;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.TextureView;

import com.bigwen.opengl.R;
import com.bigwen.opengl.gl.GLUtil;
import com.bigwen.opengl.gl.ve_gl.EglBase;
import com.bigwen.opengl.gl.ve_gl.EglBase14;
import com.bigwen.opengl.gl.ve_gl.GlRectDrawer;

/**
 * Created by bigwen on 2020/9/6.
 */
public class DisplayBridge {

    private static final String TAG = "DisplayBridge";
    private CameraProxy cameraProxy;


    private HandlerThread displayThread;
    private Handler displayHandler;
    private boolean mIsEgl14 = false;

    private EglBase mRootContext;
    private GlRectDrawer mRootDrawer;
    private SurfaceTexture mCameraSurfaceTexture;
    private float[] mCameraSurfaceMatrix = new float[16];
    private int mOesTextureId;
    private int mTextureId = 0;
    private int mFrameBufferId = 0;

    private PreviewProxy mPreviewProxy;

    public DisplayBridge(PreviewProxy previewProxy) {
        this.mPreviewProxy = previewProxy;
    }

    public void init() {
        displayThread = new HandlerThread("display_thread");
        displayThread.start();
        displayHandler = new Handler(displayThread.getLooper());
        displayHandler.post(new Runnable() {
            @Override
            public void run() {
                mRootContext = EglBase.create(null, EglBase.CONFIG_PIXEL_BUFFER);

                try {
                    mRootContext.createDummyPbufferSurface();
                    mRootContext.makeCurrent();
                    mRootDrawer = new GlRectDrawer();
                }catch (Exception e) {
                    mRootContext.releaseSurface();
                    e.printStackTrace();
                }

                mIsEgl14 = EglBase14.isEGL14Supported();
                mOesTextureId = GLUtil.generateTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
                mCameraSurfaceTexture = new SurfaceTexture(mOesTextureId);
                mCameraSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        mRootContext.makeCurrent();
                        surfaceTexture.updateTexImage();
                        long timestamp = surfaceTexture.getTimestamp();
                        surfaceTexture.getTransformMatrix(mCameraSurfaceMatrix);

                        int width = cameraProxy.getCameraWidth();
                        int height = cameraProxy.getCameraHeight();

                        if (mTextureId == 0) {
                            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                            mTextureId = GLUtil.generateTexture(GLES20.GL_TEXTURE_2D);
                            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

                            // 创建帧缓冲对象，绘制纹理到帧缓冲区并返回缓冲区索引
                            mFrameBufferId = GLUtil.generateFrameBuffer(mTextureId);
                        } else {
                            // 绑定帧缓冲区
                            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferId);
                        }
                        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                        mRootDrawer.drawOes(mOesTextureId, mCameraSurfaceMatrix, width, height, 0, 0, width, height);
                        // 解邦帧缓冲区
                        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

                        mPreviewProxy.drawToPreview(mTextureId, width, height, mCameraSurfaceMatrix, mRootContext, 0);
                    }
                });
            }
        });
    }

    private void clearFbo() {
        // 绑定eglContext、eglDisplay、eglSurface
        mRootContext.makeCurrent();;

        if (mTextureId != 0) {
            int[] textures = new int[]{mTextureId};
            GLES20.glDeleteTextures(1, textures, 0);
            mTextureId = 0;
        }

        if (mFrameBufferId != 0) {
            int[] frameBuffers = new int[]{mFrameBufferId};
            GLES20.glDeleteFramebuffers(1, frameBuffers, 0);
            mFrameBufferId = 0;
        }
    }

    public void startCamera() {
        displayHandler.post(new Runnable() {
            @Override
            public void run() {
                if (cameraProxy == null) {
                    cameraProxy = new CameraProxy();
                }
                try {
                    cameraProxy.createCamera();
                    cameraProxy.startCamera(mCameraSurfaceTexture);
                    clearFbo();
                } catch (Exception e) {
                    cameraProxy.releaseCamera();
                    e.printStackTrace();
                }
            }
        });
    }

    public void release() {
        if (cameraProxy != null) {
            displayHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (cameraProxy == null) return;
                    cameraProxy.stopPreview();
                    cameraProxy.releaseCamera();
                }
            });
        }
        displayHandler.removeCallbacksAndMessages(null);
        displayThread.quit();
    }

    public void runGLThread(Runnable runnable) {
        if (displayHandler != null) {
            displayHandler.post(runnable);
        }
    }
}
