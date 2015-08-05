package com.moocher.squarecameralibrary;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by moocher on 2015/8/2.
 */
public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PictureCallback, LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "CameraActivity";

    public static final int REQUEST_ALBUMS_CODE = 0x110;
    public static final int RESULT_ALBUMS = 0x101;

    public static final String CAMERA_ID_KEY = "camera_id";
    private static final String CAMERA_FLASH_KEY = "flash_mode";
    private static final String PREVIEW_HEIGHT_KEY = "preview_height";
    private static final int DEFAULT_PICTURE_SIZE = 800;

    private int squareLength;
    private Uri saveFileUri;
    private int displayOrientation;
    private int mCameraId;
    private String mFlashMode;
    private Camera mCamera;
    private SquareCameraSurfaceView mPreviewView;
    private SurfaceHolder mSurfaceHolder;

    private int mPreviewHeight;
    private int mCoverHeight;

    private ImageView cameraBack;
    private ImageView cameraSwitch;
    private ImageView cameraFlash;

    private boolean isMove = false;
    private PointF firstPoint = new PointF();
    private PointF movePoint = new PointF();

    private ImageView focusView;
    private ImageView flashView;
    private ImageView showGridView;
    private CoverGridView coverGridView;
    private ImageView takePictureView;

    //album
    private ImageView albumView;
    private ArrayList<String> folderNameList = new ArrayList<>();
    private ArrayList<String> imagePathList = new ArrayList<>();
    private String[] projection = {
            ImageStore.IMAGES_BUCKET_NAME,
            ImageStore.IMAGES_PATH
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        initViewAndListener();

        if (savedInstanceState == null) {
            mCameraId = getBackCameraId();
            mFlashMode = Camera.Parameters.FLASH_MODE_AUTO;
        } else {
            mCameraId = savedInstanceState.getInt(CAMERA_ID_KEY);
            mFlashMode = savedInstanceState.getString(CAMERA_FLASH_KEY);
            mPreviewHeight = savedInstanceState.getInt(PREVIEW_HEIGHT_KEY);
        }

        mPreviewView = (SquareCameraSurfaceView) findViewById(R.id.camera_preview);
        mPreviewView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isMove = false;
                        firstPoint.set(motionEvent.getX(), motionEvent.getY());
                        break;
                    case MotionEvent.ACTION_MOVE:
                        movePoint.set(motionEvent.getX(), motionEvent.getY());
                        double dx = Math.abs(movePoint.x - firstPoint.x);
                        double dy = Math.abs(movePoint.y - firstPoint.y);
                        if (dx > 2 || dy > 2) {
                            isMove = true;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (!isMove) {
                            float posX, posY;
                            float x = motionEvent.getX();
                            float y = motionEvent.getY();
                            float diffLength = focusView.getWidth() / 2;

                            //正常
                            posX = x - diffLength;
                            posY = y - diffLength;

                            //左右边界
                            if (x - diffLength < 0) {
                                posX = 0;
                            } else if (x + diffLength > getResources().getDisplayMetrics().widthPixels) {
                                posX = getResources().getDisplayMetrics().widthPixels - diffLength * 2;
                            }
                            //上下边界
                            if (y - diffLength < dip2px(50)) {
                                posY = dip2px(50);
                            } else if (y + diffLength > getResources().getDisplayMetrics().widthPixels) {
                                posY = getResources().getDisplayMetrics().widthPixels + dip2px(50) - diffLength * 2;
                            }
                            if (y > getResources().getDisplayMetrics().widthPixels) {
                                break;
                            }
                            focusView.setX(posX);
                            focusView.setY(posY);
                            focusView.setVisibility(View.VISIBLE);
                            ViewCompat.animate(focusView).scaleX(0.8f).scaleY(0.8f).setDuration(500).start();
                            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                                @Override
                                public void onAutoFocus(boolean b, Camera camera) {

                                    if (b) {
                                        Log.d(TAG, "focus success");
                                        ViewCompat.animate(focusView).alpha(0).setDuration(500).start();

                                    } else {
                                        Log.e(TAG, "focus failed.");
                                        Toast.makeText(CameraActivity.this, "聚焦失败", Toast.LENGTH_SHORT).show();

                                    }
                                    focusView.setVisibility(View.GONE);
                                    ViewCompat.animate(focusView).scaleX(1.25f).scaleY(1.25f).alpha(1f).start();
                                }
                            });

                        }
                        break;

                }
                return true;
            }
        });
        SurfaceHolder holder = mPreviewView.getHolder();
        holder.addCallback(this);

        final View mCoverView = findViewById(R.id.camera_cover);

        if (mCoverHeight == 0) {
            ViewTreeObserver observer = mPreviewView.getViewTreeObserver();
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int width = mPreviewView.getWidth();
                    mPreviewHeight = mPreviewView.getHeight();
                    mCoverHeight = mPreviewHeight - width;
                    Log.d(TAG, "preview width:" + width + " height: " + mPreviewHeight);
                    mCoverView.getLayoutParams().height = mCoverHeight;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        mPreviewView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } else {
                        mPreviewView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }

                }
            });
        } else {
            mCoverView.getLayoutParams().height = mCoverHeight;
        }

    }

    /**
     * init view and listener
     */
    private void initViewAndListener() {

        squareLength = getIntent().getIntExtra(ImageStore.SQUARE_LENGTH_KEY, DEFAULT_PICTURE_SIZE);
        saveFileUri = getIntent().getParcelableExtra(ImageStore.SQUARE_EXTRA_OUTPUT_KEY);

        focusView = (ImageView)findViewById(R.id.camera_focus);
        flashView = (ImageView) findViewById(R.id.camera_flash);
        showGridView = (ImageView) findViewById(R.id.camera_show_grid);
        coverGridView = (CoverGridView) findViewById(R.id.camera_grid);
        showGridView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int visible = coverGridView.getVisibility();
                if (visible == View.GONE) {
                    coverGridView.setVisibility(View.VISIBLE);
                    showGridView.setImageResource(R.mipmap.icon_grid_selected);
                } else {
                    coverGridView.setVisibility(View.GONE);
                    showGridView.setImageResource(R.mipmap.icon_grid_normal);
                }
            }
        });
        takePictureView = (ImageView) findViewById(R.id.camera_take);
        takePictureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.takePicture(null, null, null, CameraActivity.this);
            }
        });
        cameraBack = (ImageView) findViewById(R.id.camera_back);
        cameraBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        cameraSwitch = (ImageView) findViewById(R.id.camera_switch);
        cameraSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    mCameraId = getBackCameraId();
                    cameraSwitch.setImageResource(R.mipmap.ic_camera_back);
                } else {
                    mCameraId = getFrontCameraId();
                    cameraSwitch.setImageResource(R.mipmap.ic_camera_front);
                }
                restartCameraPreview();
            }
        });
        cameraFlash = (ImageView) findViewById(R.id.camera_flash);
        cameraFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFlashMode.equalsIgnoreCase(Camera.Parameters.FLASH_MODE_AUTO)) {
                    mFlashMode = Camera.Parameters.FLASH_MODE_ON;
                    cameraFlash.setImageResource(R.mipmap.ic_flash_on);
                } else if (mFlashMode.equalsIgnoreCase(Camera.Parameters.FLASH_MODE_ON)) {
                    mFlashMode = Camera.Parameters.FLASH_MODE_OFF;
                    cameraFlash.setImageResource(R.mipmap.ic_flash_off);
                } else if (mFlashMode.equalsIgnoreCase(Camera.Parameters.FLASH_MODE_OFF)) {
                    mFlashMode = Camera.Parameters.FLASH_MODE_AUTO;
                    cameraFlash.setImageResource(R.mipmap.ic_flash_auto);
                }
                setCameraParams();
            }
        });
        albumView = (ImageView)findViewById(R.id.camera_album);
        albumView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CameraActivity.this, AlbumActivity.class);
                intent.putStringArrayListExtra("folder", folderNameList);
                intent.putStringArrayListExtra("path", imagePathList);
                startActivityForResult(intent, REQUEST_ALBUMS_CODE);

            }
        });
        getSupportLoaderManager().initLoader(0, null, this);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(CAMERA_ID_KEY, mCameraId);
        outState.putString(CAMERA_FLASH_KEY, mFlashMode);
        outState.putInt(PREVIEW_HEIGHT_KEY, mPreviewHeight);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_ALBUMS_CODE && resultCode == RESULT_OK){
            setResult(RESULT_ALBUMS, data);
            finish();

        }
    }

    /**
     * 获取Camera对象
     *
     * @param cameraId
     */
    private void getCamera(int cameraId) {
        try {
            mCamera = Camera.open(cameraId);
        } catch (Exception e) {
            Log.e(TAG, "Can't open camera with id : " + cameraId);
            e.printStackTrace();
        }
    }

    /**
     * start camera in preview
     */
    private void startCameraPreview() {
        setDisplayOrientation();
        setCameraParams();
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Can't start camera preview");
            e.printStackTrace();
        }
    }

    /**
     * stop camera preview.
     */
    private void stopCameraPreview() {
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    private void restartCameraPreview() {
        stopCameraPreview();

        getCamera(mCameraId);
        startCameraPreview();
    }

    /**
     * set camera orientation
     */
    private void setDisplayOrientation() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, cameraInfo);

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degree = 0;
        switch (rotation) {
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

        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            displayOrientation = (cameraInfo.orientation + degree) % 360;
            displayOrientation = (360 - displayOrientation) % 360;
        } else {
            displayOrientation = (cameraInfo.orientation - degree + 360) % 360;
        }

        mCamera.setDisplayOrientation(displayOrientation);
    }

    /**
     * set the camera parameters
     */
    private void setCameraParams() {
        Camera.Parameters parameters = mCamera.getParameters();

        Camera.Size fitPreviewSize = searchFitPreviewSize(parameters);
        Camera.Size fitPictureSize = searchFitPictureSize(parameters);

        parameters.setPreviewSize(fitPreviewSize.width, fitPreviewSize.height);
        parameters.setPictureSize(fitPictureSize.width, fitPictureSize.height);

        /**
         * some phone not support focus
         * if set the FocusMode.you needn't to set Camera.autoFocus().
         */
//        if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
//            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
//        }
        //check phone support flash
        List<String> flashModes = parameters.getSupportedFlashModes();
        if (flashModes != null && flashModes.contains(mFlashMode)) {
            parameters.setFlashMode(mFlashMode);
            flashView.setVisibility(View.VISIBLE);
        } else {
            flashView.setVisibility(View.INVISIBLE);
        }

        mCamera.setParameters(parameters);
    }

    /**
     * search fit preview size.
     *
     * @param parameters
     * @return
     */
    private Camera.Size searchFitPreviewSize(Camera.Parameters parameters) {
        return fitBestSize(parameters.getSupportedPreviewSizes());
    }

    /**
     * search fit picture size.
     *
     * @param parameters
     * @return
     */
    private Camera.Size searchFitPictureSize(Camera.Parameters parameters) {
        return fitBestSize(parameters.getSupportedPictureSizes());
    }

    /**
     * search the best fit size from phone support.
     *
     * @param sizes
     * @return
     */
    private Camera.Size fitBestSize(List<Camera.Size> sizes) {
        Camera.Size fitSize = null;
        for (Camera.Size size : sizes) {
            boolean isDesireRatio = (size.width / 4) == (size.height / 3);
            boolean isBetterSize = (fitSize == null) || size.width > fitSize.width;
            if (isDesireRatio && isBetterSize) {
                fitSize = size;
            }
        }
        if (fitSize == null) {
            Log.d(TAG, "cannot find the best fit camera size");
            return sizes.get(sizes.size() - 1);
        }

        return fitSize;
    }

    /**
     * 获取手机后置摄像头ID
     *
     * @return
     */
    private int getBackCameraId() {
        return Camera.CameraInfo.CAMERA_FACING_BACK;
    }

    /**
     * 获取手机前置摄像头
     *
     * @return
     */
    private int getFrontCameraId() {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
            return Camera.CameraInfo.CAMERA_FACING_FRONT;
        }
        return Camera.CameraInfo.CAMERA_FACING_BACK;
    }

    /**
     * dp to dx
     * @param dipValue
     * @return
     */
    public int dip2px(float dipValue){
        final float scale = getResources().getDisplayMetrics().density;
        return (int)(dipValue * scale + 0.5f);
    }

    @Override
    protected void onDestroy() {
        stopCameraPreview();
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
        getCamera(mCameraId);
        startCameraPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        restartCameraPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);
        options.inJustDecodeBounds = false;
        float width = options.outWidth;
        float height = options.outHeight;
        int sampleSize = 1;
        float scale = 1f;
        if(width >= height){
            sampleSize = (int)height / squareLength;
        }else {
            sampleSize = (int)width / squareLength;
        }
        if(sampleSize < 1){
            sampleSize = 1;
        }
        options.inSampleSize = sampleSize;
        Bitmap originalBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
        Matrix matrix = new Matrix();
        matrix.postRotate(displayOrientation);
        float originalSquare = originalBitmap.getWidth() > originalBitmap.getHeight() ? originalBitmap.getHeight() : originalBitmap.getWidth();
        if(originalSquare != squareLength){
            scale = (float)squareLength / originalSquare;
            matrix.postScale(scale, scale);
        }
        Bitmap rotationBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, false);
        originalBitmap.recycle();
        int square = rotationBitmap.getWidth() > rotationBitmap.getHeight() ? rotationBitmap.getHeight() : rotationBitmap.getWidth();
        Bitmap cropBitmap = Bitmap.createBitmap(rotationBitmap, 0, 0, square, square, null, false);
        rotationBitmap.recycle();
        Log.d(TAG, "picture:" + cropBitmap.getWidth() + "x" + cropBitmap.getHeight());
        if(saveFileUri != null){
            try {
                File saveFile = new File(saveFileUri.getPath());
                FileOutputStream fos = new FileOutputStream(saveFile);
                cropBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                fos.close();
            }catch (Exception e){
                Log.d(TAG, "failed save file to sd.");
                e.printStackTrace();
            }
            setResult(RESULT_OK);
        }else{
            Intent intent = getIntent();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            cropBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            intent.putExtra("data", baos.toByteArray());
            setResult(RESULT_OK, intent);
        }
        cropBitmap.recycle();
        finish();

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                this,
                ImageStore.IMAGES_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Images.Media.DEFAULT_SORT_ORDER
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        int isFirst = 0;
        while (data.moveToNext()){
            String folderName = data.getString(data.getColumnIndex(ImageStore.IMAGES_BUCKET_NAME));
            String imagePath = data.getString(data.getColumnIndex(ImageStore.IMAGES_PATH));
            if(isFirst == 0){
                Glide.with(this)
                        .load(imagePath)
                        .override(100, 100)
                        .placeholder(R.mipmap.ic_album)
                        .centerCrop()
                        .crossFade()
                        .into(albumView);
//                BitmapFactory.Options options = new BitmapFactory.Options();
//                options.inJustDecodeBounds = true;
//                BitmapFactory.decodeFile(imagePath, options);
//                float width = options.outWidth;
//                float height = options.outHeight;
//                int sampleSize;
//                int w = 100;
//                if(width > height){
//                    sampleSize = (int)height / w;
//                }else{
//                    sampleSize = (int)width / w;
//                }
//                if(sampleSize < 1){
//                    sampleSize = 1;
//                }
//                options.inJustDecodeBounds = false;
//                options.inSampleSize = sampleSize;
//                Bitmap firstAlbumImageBitmap = BitmapFactory.decodeFile(imagePath, options);
//                albumView.setImageBitmap(firstAlbumImageBitmap);
            }
            folderNameList.add(folderName);
            imagePathList.add(imagePath);
            isFirst++;

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
