package com.bigwen.opengl.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.util.Log;

import java.util.List;

/**
 * Created by bigwen on 2020/9/6.
 */
public class CameraProxy {

    private static final String TAG = "CameraHandler";

    //camera
    private Camera mCamera;
    private Camera.CameraInfo mCameraInfo;
    private int mFront = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private int mCameraWidth, mCameraHeight;
    private int mDisplayRotation = 0;
    private int mImageRotation;
    private int mFrameRate = 15;

    public void createCamera() throws Exception {
        // 获取欲设置camera的索引号
        int nFacing = (mFront != 0) ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;
        mCameraInfo = new Camera.CameraInfo();
        int cameraNum = Camera.getNumberOfCameras();
        for (int i = 0; i < cameraNum; i++) {
            Camera.getCameraInfo(i, mCameraInfo);
            if (mCameraInfo.facing == nFacing) {
                mCamera = Camera.open(i);
                break;
            }
        }
        if (mCamera == null) {
            mCamera = Camera.open();
            if (mCamera == null) {
                throw new Exception("open camera fail ");
            }
        }

        // 设置camera的采集视图size
        Camera.Parameters parameters = mCamera.getParameters();
        Camera.Size cameraSize = parameters.getPreferredPreviewSizeForVideo();
        if (cameraSize == null) {
            cameraSize = mCamera.new Size(640, 480);
        }
        parameters.setPreviewSize(cameraSize.width, cameraSize.height);
        mCameraWidth = cameraSize.width;
        mCameraHeight = cameraSize.height;

        //camera帧率
        List<int[]> frameRate = parameters.getSupportedPreviewFpsRange();
        for (int[] entry: frameRate) {
            if (entry[0] == entry[1] && entry[0] == mFrameRate * 1000) {
                parameters.setPreviewFpsRange(entry[0], entry[1]);
                break;
            }
        }

        // 获取camera的实际帧率
        int[] realRate = new int[2];
        parameters.getPreviewFpsRange(realRate);
        if (realRate[0] == realRate[1]) {
            mFrameRate = realRate[0] / 1000;
        } else {
            mFrameRate = realRate[1] / 2 / 1000;
        }

        // 设置camera的对焦模式
        boolean bFocusModeSet = false;
        for (String mode : parameters.getSupportedFocusModes()) {
            if (mode.compareTo(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) == 0) {
                try {
                    parameters.setFocusMode(mode);
                    bFocusModeSet = true;
                    break;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        if (!bFocusModeSet) {
            Log.e("Camera", "[WARNING] vcap: focus mode left unset !!\n");
        }

        // 设置camera的参数
        try {
            mCamera.setParameters(parameters);
        } catch (Exception ex) {
            Log.e("Camera",  "vcap: set camera parameters error with exception\n");
            ex.printStackTrace();
        }

        Camera.Parameters actualParm = mCamera.getParameters();
        mCameraWidth = actualParm.getPreviewSize().width;
        mCameraWidth = actualParm.getPreviewSize().height;
        Log.e("Camera", "[WARNING] vcap: focus mode " + actualParm.getFocusMode());

        // 设置预览图像旋转方向
        int result;
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (mCameraInfo.orientation + mDisplayRotation) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (mCameraInfo.orientation - mDisplayRotation + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);
        mImageRotation = result;
    }

    public void startCamera(SurfaceTexture mCameraSurfaceTexture) throws Exception {
        if (mCamera == null) {
            throw new Exception(" Camera is stopped");
        }

        if (mCameraSurfaceTexture == null) {
            throw new Exception(" mCameraSurfaceTexture is null");
        }

        //sometime need add this method
//        mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
//            @Override
//            public void onPreviewFrame(byte[] data, Camera camera) {
//
//            }
//        });

        try {
            // 设置预览SurfaceTexture
            mCamera.setPreviewTexture(mCameraSurfaceTexture);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mCamera.startPreview();

        Log.i("Camera", "startCamera success");
    }

    // camera停止采集
    public int stopPreview() {
        Log.d(TAG, "stopCaptureOnCameraThread");
        if (mCamera != null) {
            // 停止camera预览
            mCamera.stopPreview();
        }
        return 0;
    }

    // 释放camera
    public int releaseCamera() {
        // * release cam
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }

        // * release cam info
        mCameraInfo = null;
        return 0;
    }

    public int getCameraWidth() {
        if (mImageRotation == 90 || mImageRotation == 270) {
            return mCameraHeight;
        }
        return mCameraWidth;
    }

    public int getCameraHeight() {
        if (mImageRotation == 90 || mImageRotation == 270) {
            return mCameraWidth;
        }
        return mCameraHeight;
    }
}
