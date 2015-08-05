package com.moocher.squarecameralibrary;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by moocher on 2015/8/4.
 */
public class CoverGridView extends View {
    private static final String TAG = "GridView";

    private float edgeLength;
    private int dividerNum = 3;
    private float linePos;
    private Paint mPaint;

    public CoverGridView(Context context){
        super(context);
        initView(context);
    }

    public CoverGridView(Context context, AttributeSet attrs){
        super(context, attrs);
        initView(context);
    }

    public CoverGridView(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
        initView(context);
    }

    /**
     * init param
     * @param context
     */
    private void initView(Context context){
        edgeLength = context.getResources().getDisplayMetrics().widthPixels;
        linePos = edgeLength / dividerNum;
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(0xffffffff);
        mPaint.setStrokeWidth(1f);
        mPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //edge
        canvas.drawRect(0f, 0f, edgeLength, edgeLength, mPaint);
        //h line
        for (int i = 1; i < dividerNum; i++){
            canvas.drawLine(0f, linePos * i, edgeLength, linePos * i, mPaint);
        }

        //v line
        for (int i = 1; i < dividerNum; i++){
            canvas.drawLine(linePos * i, 0f, linePos * i, edgeLength, mPaint);
        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(widthMeasureSpec, widthMeasureSpec);
    }
}
