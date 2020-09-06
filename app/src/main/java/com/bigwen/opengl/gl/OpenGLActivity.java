package com.bigwen.opengl.gl;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

import com.bigwen.opengl.R;

public class OpenGLActivity extends Activity {

    private GLSurfaceView surfaceView;
    private Activity mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_opengl);
        surfaceView = findViewById(R.id.gl_surface_view);
        //设置opengl es版
        surfaceView.setEGLContextClientVersion(2);
        //设置renderer
        surfaceView.setRenderer(new TextureRender(this));
        //设置渲染模式
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        surfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        surfaceView.onPause();
    }
}