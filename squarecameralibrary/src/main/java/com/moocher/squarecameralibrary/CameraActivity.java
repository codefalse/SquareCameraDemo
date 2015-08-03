package com.moocher.squarecameralibrary;

import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * Created by moocher on 2015/8/2.
 */
public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback{
    private static final String TAG = "CameraActivity";
    public static final String CAMERA_ID_KEY = "camera_id";
    private static final String CAMERA_FLASH_KEY = "flash_mode";
    private static final String PREVIEW_HEIGHT_KEY = "preview_height";

    private int mCameraId;
    private String mFlashMode;
    private Camera mCamera;
    private SquareCameraSurfaceView mPreviewView;
    private SurfaceHolder mSurfaceHolder;

    private int mPreviewHeight;
    private int mCoverHeight;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        if(savedInstanceState == null){
            mCameraId = getBackCameraId();
            mFlashMode = Camera.Parameters.FLASH_MODE_AUTO;
        }else {
            mCameraId = savedInstanceState.getInt(CAMERA_ID_KEY);
            mFlashMode = savedInstanceState.getString(CAMERA_FLASH_KEY);
            mPreviewHeight = savedInstanceState.getInt(PREVIEW_HEIGHT_KEY);
        }

        mPreviewView = (SquareCameraSurfaceView)findViewById(R.id.camera_preview);
        SurfaceHolder holder = mPreviewView.getHolder();
        holder.addCallback(this);

        final View mCoverView = findViewById(R.id.camera_cover);

        if(mCoverHeight == 0){
            ViewTreeObserver observer = mPreviewView.getViewTreeObserver();
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int width = mPreviewView.getWidth();
                    mPreviewHeight = mPreviewView.getHeight();
                    mCoverHeight = mPreviewHeight - width;
                    Log.d(TAG, "preview width:" + width + " height: " + mPreviewHeight);
                    mCoverView.getLayoutParams().height = mCoverHeight;

                    mPreviewView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
        }else {
            mCoverView.getLayoutParams().height = mCoverHeight;
        }

    }

    /**
     * 获取手机后置摄像头ID
     * @return
     */
    private int getBackCameraId(){
        return Camera.CameraInfo.CAMERA_FACING_BACK;
    }

    /**
     * 获取手机前置摄像头
     * @return
     */
    private int getFrontCameraId(){
        if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)){
            return Camera.CameraInfo.CAMERA_FACING_FRONT;
        }
        return Camera.CameraInfo.CAMERA_FACING_BACK;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
