package com.example.imac.alivoicerecognition.View;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import com.example.imac.alivoicerecognition.R;


public class RippleSpeechRecordView extends View {

    private float mCircleRadius = 80; //起始圆半径
    private int mProgress = 0; //声音分贝等级
    private int mCircleColor; //起始圆颜色
    private int mProgressColor; //进度条颜色
    private float mProgressWidth = 2;
    private Paint mPaint;
    private Context mContext;

    private static final int MSG_START_RECORD = 0;

    private android.os.Handler mHandler = new android.os.Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_START_RECORD) {
                if (mProgress >= 100) {
                    mProgress = 0;
                }
                mProgress++;
                setProgressChange(mProgress);
                mHandler.sendEmptyMessageDelayed(0, 600);
            }
        }
    };

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
        mCircleColor = typedArray.getColor(R.styleable.RippleSpeechRecordView_circleColor, Color.BLUE);
        mProgressColor = typedArray.getColor(R.styleable.RippleSpeechRecordView_progressColor, Color.GRAY);
        mProgressWidth = typedArray.getDimension(R.styleable.RippleSpeechRecordView_progressWidth, mProgressWidth);

        typedArray.recycle();
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mCircleColor);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWith = (int) (2 * mCircleRadius + getPaddingLeft() + getPaddingRight() + convertDpToPixel(mProgressWidth));
        int measuredHeight = (int) (2 * mCircleRadius + getPaddingTop() + getPaddingBottom()) + convertDpToPixel(mProgressWidth);
        setMeasuredDimension(measuredWith, measuredHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int circleCenter = getWidth() / 2;
        mPaint.setColor(mCircleColor);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(circleCenter, circleCenter, mCircleRadius, mPaint);

        Drawable backDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.mic, null);
        backDrawable.setBounds(circleCenter - ((int) mCircleRadius / 2), circleCenter - ((int) mCircleRadius / 2), circleCenter + ((int) mCircleRadius / 2), circleCenter + ((int) mCircleRadius / 2));
        backDrawable.draw(canvas);
        // 画进度条
        mPaint.setStrokeWidth(convertDpToPixel(mProgressWidth)); // 设置圆环的宽度
        mPaint.setColor(mProgressColor); // 设置进度的颜色
        RectF oval = new RectF(circleCenter - mCircleRadius, circleCenter - mCircleRadius, circleCenter + mCircleRadius, circleCenter + mCircleRadius); // 用于定义的圆弧的形状和大小的界限
        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawArc(oval, -90, 360 * mProgress / 100, false, mPaint); // 根据进度画圆弧

    }

    public void setProgressChange(float progress) {
        mProgress = (int) progress;
        postInvalidate();
    }


    public void startRecording() {
        mProgress = 0;
        mHandler.sendEmptyMessage(MSG_START_RECORD);
    }


    public void startRecognition() {
        mProgress = 0;
        mHandler.removeMessages(MSG_START_RECORD);
        postInvalidate();

    }

    public void stopRecognition() {
        mProgress = 0;
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
