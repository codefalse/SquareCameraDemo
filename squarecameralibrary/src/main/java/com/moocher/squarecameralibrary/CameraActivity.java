package com.moocher.squarecameralibrary;

import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import java.io.IOException;
import java.util.List;

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

    private ImageView flashView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        flashView = (ImageView)findViewById(R.id.camera_flash);

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
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        mPreviewView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }else {
                        mPreviewView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }

                }
            });
        }else {
            mCoverView.getLayoutParams().height = mCoverHeight;
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(CAMERA_ID_KEY, mCameraId);
        outState.putString(CAMERA_FLASH_KEY, mFlashMode);
        outState.putInt(PREVIEW_HEIGHT_KEY, mPreviewHeight);
        super.onSaveInstanceState(outState);
    }

    /**
     * 获取Camera对象
     * @param cameraId
     */
    private void getCamera(int cameraId){
        try{
            mCamera = Camera.open(cameraId);
        }catch (Exception e){
            Log.e(TAG, "Can't open camera with id : " + cameraId);
            e.printStackTrace();
        }
    }

    /**
     * start camera in preview
     */
    private void startCameraPreview(){
        setDisplayOrientation();
        setCameraParams();
        try{
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
        }catch (IOException e){
            Log.d(TAG, "Can't start camera preview");
            e.printStackTrace();
        }
    }

    /**
     * stop camera preview.
     */
    private void stopCameraPreview(){
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    private void restartCameraPreview(){
        stopCameraPreview();

        getCamera(mCameraId);
        startCameraPreview();
    }

    /**
     * set camera orientation
     */
    private void setDisplayOrientation(){
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, cameraInfo);

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degree = 0;
        switch (rotation){
            case Surface.ROTATION_0:
                degree = 0;
                break;
            case Surface.ROTATION_90:
                degree = 90;
                break;
            case Surface.ROTATION_180:
                degree = 180;
                break;
            case Surface.ROTATION_270:
                degree = 270;
                break;
        }
        int displayOrientation;
        if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
            displayOrientation = (cameraInfo.orientation + degree) % 360;
            displayOrientation = (360 - displayOrientation) % 360;
        }else {
            displayOrientation = (cameraInfo.orientation - degree + 360) % 360;
        }

        mCamera.setDisplayOrientation(displayOrientation);
    }

    /**
     * set the camera parameters
     */
    private void setCameraParams(){
        Camera.Parameters parameters = mCamera.getParameters();

        Camera.Size fitPreviewSize = searchFitPreviewSize(parameters);
        Camera.Size fitPictureSize = searchFitPictureSize(parameters);

        parameters.setPreviewSize(fitPreviewSize.width, fitPreviewSize.height);
        parameters.setPictureSize(fitPictureSize.width, fitPictureSize.height);

        // some phone not support focus
        if(parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        //check phone support flash
        List<String> flashModes = parameters.getSupportedFlashModes();
        if(flashModes != null && flashModes.contains(mFlashMode)){
            parameters.setFlashMode(mFlashMode);
            flashView.setVisibility(View.VISIBLE);
        }else {
            flashView.setVisibility(View.INVISIBLE);
        }

        mCamera.setParameters(parameters);
    }

    /**
     * search fit preview size.
     * @param parameters
     * @return
     */
    private Camera.Size searchFitPreviewSize(Camera.Parameters parameters){
        return fitBestSize(parameters.getSupportedPreviewSizes());
    }

    /**
     * search fit picture size.
     * @param parameters
     * @return
     */
    private Camera.Size searchFitPictureSize(Camera.Parameters parameters){
        return fitBestSize(parameters.getSupportedPictureSizes());
    }

    /**
     * search the best fit size from phone support.
     * @param sizes
     * @return
     */
    private Camera.Size fitBestSize(List<Camera.Size> sizes){
        Camera.Size fitSize = null;
        for (Camera.Size size : sizes){
            boolean isDesireRatio = (size.width / 4) == (size.height / 3);
            boolean isBetterSize = (fitSize == null) || size.width > fitSize.width;
            if( isDesireRatio && isBetterSize){
                fitSize = size;
            }
        }
        if(fitSize == null){
            Log.d(TAG, "cannot find the best fit camera size");
            return sizes.get(sizes.size() - 1);
        }

        return fitSize;
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
    protected void onStop() {

        stopCameraPreview();
        super.onStop();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
        getCamera(mCameraId);
        startCameraPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
