package com.bigwen.opengl.camera;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.TextureView;

import com.bigwen.opengl.R;

/**
 * Created by bigwen on 2020/9/6.
 */
public class CameraActivity extends Activity {

    private TextureView mTextureView;
    private DisplayBridge displayBridge;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        mTextureView = findViewById(R.id.texture_view);
        final PreviewProxy previewProxy = new PreviewProxy();
        previewProxy.doSetRendererView(mTextureView, new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                displayBridge.runGLThread(new Runnable() {
                    @Override
                    public void run() {
                        previewProxy.releasePreviewSurface();
                    }
                });
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                displayBridge.runGLThread(new Runnable() {
                    @Override
                    public void run() {
                        previewProxy.releasePreviewSurface();
                    }
                });
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
        displayBridge = new DisplayBridge(previewProxy);
        displayBridge.init();
        displayBridge.startCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        displayBridge.release();
    }
}
