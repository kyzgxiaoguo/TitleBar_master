package com.dc.loading;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.dc.loading.utils.DotNumberUtil;

import java.util.ArrayList;

/**
 * @author Zhangzhenguo
 * @create 2020/1/21
 * @Email 18311371235@163.com
 * @Describe
 */
public class LoadingView extends View implements Runnable{
    /**
     * 普通模式
     */
    private static final int MODE_NORMAL = 1;
    /**
     * 缩放模式
     */
    private static final int MODE_SCALE = 2;

    /**
     * 总旋转角度
     */
    private static final int TOTAL_ROTATION_ANGLE = 360;
    /**
     * 间隔时间
     */
    private static final int INTERVAL_TIME = 100;
    /**
     * View默认最小宽度
     */
    private static final int DEFAULT_MIN_WIDTH = 70;

    /**
     * 控件宽
     */
    private int mViewWidth;
    /**
     * 控件高
     */
    private int mViewHeight;

    /**
     * 画笔
     */
    private Paint mPaint;
    /**
     * 外接圆的半径
     */
    private float mCircleRadius;
    /**
     * 起始点的颜色
     */
    private int mStartColor;
    /**
     * 终止点的颜色
     */
    private int mEndColor;
    /**
     * 一共多少个点
     */
    private int mDotCount;
    /**
     * 圆点半径
     */
    private float mDotRadius;
    /**
     * 平均角度
     */
    private int mAngle;
    /**
     * 旋转角度，默认和平均角度一样
     */
    private int mRotateAngle;
    /**
     * 每个点的数据
     */
    private ArrayList<DotNumberUtil> mDots;
    /**
     * 当前旋转到的角度
     */
    private int mCurrentAngle = 0;
    /**
     * 是否自动开始
     */
    private boolean isAutoStart;
    /**
     * 点的模式
     */
    private int mDotMode;


    public LoadingView(Context context) {
        this(context,null);
    }

    public LoadingView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public LoadingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    /**
     * 实例化
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    private void init(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        initAttr(context, attrs, defStyleAttr);
        mPaint = new Paint();
        mPaint.setColor(mStartColor);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(mDotRadius);
    }

    /**
     * 实例化并设置属性
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    private void initAttr(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.RotateView, defStyleAttr, 0);
        mStartColor = array.getColor(R.styleable.RotateView_r_auto_start, Color.argb(255, 180, 180, 180));
        //如果不设置endColor，默认取startColor的30%透明度作为endColor
        mEndColor = array.getColor(R.styleable.RotateView_r_end_color, Color.argb(76, Color.red(mStartColor), Color.green(mStartColor), Color.blue(mStartColor)));
        mDotCount = array.getInt(R.styleable.RotateView_r_dot_count, 8);
        mDotRadius = array.getDimension(R.styleable.RotateView_r_dot_radius, dip2px(context, 2.6f));
        isAutoStart = array.getBoolean(R.styleable.RotateView_r_auto_start, true);
        //计算平均角度，默认是360 / 点的数量，例如8个点，算出来的平均角度就是45度
        mAngle = TOTAL_ROTATION_ANGLE / mDotCount;
        mRotateAngle = array.getInt(R.styleable.RotateView_r_rotate_angle, mAngle);
        //获取模式
        mDotMode = array.getInt(R.styleable.RotateView_r_dot_mode, MODE_NORMAL);
        array.recycle();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mViewWidth = w;
        mViewHeight = h;
        mCircleRadius = (Math.min(mViewHeight, mViewWidth) / 2f) * 0.8f;
        mDots = generateDot();
    }

//    绘制图像
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //将坐标系原点移动到画布正中心
        canvas.translate(mViewWidth / 2, mViewHeight / 2);
        canvas.rotate(mCurrentAngle);
        for (DotNumberUtil dot : mDots) {
            mPaint.setColor(dot.color);
            canvas.drawCircle(dot.x, dot.y, dot.dotRadius, mPaint);
        }
    }
//    计算大小
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(handleMeasure(widthMeasureSpec), handleMeasure(heightMeasureSpec));
    }

    /**
     * 处理MeasureSpec
     */
    private int handleMeasure(int measureSpec) {
        int result = DEFAULT_MIN_WIDTH;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            //处理wrap_content的情况
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isAutoStart) {
            postDelayed(this, INTERVAL_TIME);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(this);
    }

    /**
     * 每个圆点之间线程延迟
     */
    @Override
    public void run() {
        if (mCurrentAngle >= TOTAL_ROTATION_ANGLE) {
            mCurrentAngle = mCurrentAngle - TOTAL_ROTATION_ANGLE;
        } else {
            //每次叠加一个圆点的角度，就不会觉得在圆圈转动，而是点在切换
            mCurrentAngle += mRotateAngle;
        }
        invalidate();
        postDelayed(this, INTERVAL_TIME);
    }

    /**
     * 计算生成点数据
     */
    private ArrayList<DotNumberUtil> generateDot() {
        //创建颜色估值器
        ArgbEvaluator argbEvaluator = new ArgbEvaluator();
        ArrayList<DotNumberUtil> points = new ArrayList<>();
        for (int i = 0; i < mDotCount; i++) {
            float currentAngle = i * mAngle;
            //三角函数，计算坐标，注意这里Math的三角函数方法，传入的是弧长，需要乘以Math.PI来将角度换算为弧长，再进行计算
            float x = (float) (mCircleRadius * Math.cos((currentAngle / 180) * Math.PI));
            float y = (float) (mCircleRadius * Math.sin((currentAngle / 180) * Math.PI));
            //估算颜色，计算每个点的颜色
            float fraction = currentAngle / TOTAL_ROTATION_ANGLE;
            int color = (int) argbEvaluator.evaluate(fraction, mEndColor, mStartColor);
            float dotRadius;
            //是否按比例缩放点
            if (mDotMode == MODE_SCALE) {
                dotRadius = (int) (fraction * mDotRadius);
            } else {
                dotRadius = mDotRadius;
            }
            points.add(new DotNumberUtil(x, y, color, dotRadius));
        }
        return points;
    }

    /**
     * 尺寸大小换算
     * @param context
     * @param dipValue
     * @return
     */
    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

}
