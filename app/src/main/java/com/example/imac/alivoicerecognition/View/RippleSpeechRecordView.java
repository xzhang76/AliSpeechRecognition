package com.example.imac.alivoicerecognition.View;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import com.example.imac.alivoicerecognition.R;

import java.nio.file.FileSystems;

public class RippleSpeechRecordView extends View {

    private float mCircleRadius = 80; //起始圆半径
    private float mCirclesInterval = 1; //同心圆间隔
    private int mVoiceDegree = 0; //声音分贝等级
    private int mCircleColor; //起始圆颜色
    private Paint mPaint;
    private Context mContext;
    private static final int MAX_CIRCLE_COUNT = 10;

    public RippleSpeechRecordView(Context context) {
        super(context);
    }

    public RippleSpeechRecordView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RippleSpeechRecordView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RippleSpeechRecordView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RippleSpeechRecordView);
        mCircleRadius = typedArray.getDimension(R.styleable.RippleSpeechRecordView_circleRadius, mCircleRadius);
        mCirclesInterval = typedArray.getDimension(R.styleable.RippleSpeechRecordView_circleInterval, mCirclesInterval);
        mCircleColor = typedArray.getColor(R.styleable.RippleSpeechRecordView_circleColor, Color.BLUE);

        typedArray.recycle();
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mCircleColor);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWith = (int) (2 * mCircleRadius + getPaddingLeft() + getPaddingRight() + convertDpToPixel((mCirclesInterval * 2) * MAX_CIRCLE_COUNT));;
        int measuredHeight = (int) (2 * mCircleRadius + getPaddingTop() + getPaddingBottom() + convertDpToPixel((mCirclesInterval * 2) * MAX_CIRCLE_COUNT));;
        setMeasuredDimension(measuredWith, measuredHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //依次画同心圆
        int circleCenter = getWidth() / 2;
        for (int i = mVoiceDegree + 1; i > 1; i--) {
            mPaint.setAlpha(255 / i);
            canvas.drawCircle(circleCenter, circleCenter, mCircleRadius + i * (mCirclesInterval * 2), mPaint);
        }
        mPaint.setAlpha(255);
        canvas.drawCircle(circleCenter, circleCenter, mCircleRadius, mPaint);

    }

    public void setProgressChange(float progress) {
        mVoiceDegree = (int) progress;
        if (mVoiceDegree > MAX_CIRCLE_COUNT) {
            mVoiceDegree = MAX_CIRCLE_COUNT;
        }
        postInvalidate();
    }

    /**
     * 将dp转换为px
     *
     * @param dp
     * @return
     */
    public int convertDpToPixel(float dp) {
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        return (int) (metrics.density * dp + 0.5f);
    }
}
