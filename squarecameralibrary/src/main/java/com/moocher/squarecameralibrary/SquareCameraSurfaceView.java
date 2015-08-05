package com.moocher.squarecameralibrary;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;

/**
 * Created by moocher on 2015/8/2.
 */
public class SquareCameraSurfaceView extends SurfaceView {
    private static final String TAG = "SquareCameraSurfaceView";

    //设定Preview的宽高比
    private static final double ASPECT_RATIO = 3.0 / 4.0;

    public SquareCameraSurfaceView(Context context){
        this(context, null, 0);
    }

    public SquareCameraSurfaceView(Context context, AttributeSet attrs){
        this(context, attrs, 0);
    }

    public SquareCameraSurfaceView(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
    }

    /**
     * 重写Measure方法，计算Preview的宽高
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if(width > height * ASPECT_RATIO){
            width = (int) (height * ASPECT_RATIO + 0.5);
        }else {
            height = (int) (width / ASPECT_RATIO + 0.5);
        }
        setMeasuredDimension(width, height);
    }
}
