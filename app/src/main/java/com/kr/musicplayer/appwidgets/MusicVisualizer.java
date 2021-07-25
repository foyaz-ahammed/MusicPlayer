package com.kr.musicplayer.appwidgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.kr.musicplayer.R;

import java.util.Random;

/**
 * 음악 시각화를 위한 view
 */
public class MusicVisualizer extends View {

    Random random = new Random();

    Paint paint = new Paint();
    public static boolean isPlaying = true; // 현재의 재생상태판별

    private Runnable animateView = new Runnable() {
        @Override
        public void run() {

            //180ms 마다 실행
            postDelayed(this, 180);

            if (isPlaying)
                invalidate();
        }
    };

    public MusicVisualizer(Context context) {
        super(context);
        new MusicVisualizer(context, null);
    }

    public MusicVisualizer(Context context, AttributeSet attrs) {
        super(context, attrs);

        //start runnable
        removeCallbacks(animateView);
        post(animateView);
    }

    public static void pauseVisualizer() {
        isPlaying = false;
    }

    public static void resumeVisualizer() {
        isPlaying = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //set paint style, Style.FILL will fill the color, Style.STROKE will stroke the color
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getResources().getColor(R.color.music_visualizer_color));

        canvas.drawRect(getDimensionInPixel(0), getHeight() - (20 + random.nextInt(Math.abs((int) (getHeight() / 1.5f) - 15))), getDimensionInPixel(2), getHeight() - 5, paint);
        canvas.drawRect(getDimensionInPixel(4), getHeight() - (20 + random.nextInt(Math.abs((int) (getHeight() / 1.5f) - 15))), getDimensionInPixel(6), getHeight() -5, paint);
        canvas.drawRect(getDimensionInPixel(8), getHeight() - (20 + random.nextInt(Math.abs((int) (getHeight() / 1.5f) - 15))), getDimensionInPixel(10), getHeight() -5, paint);
        canvas.drawRect(getDimensionInPixel(12), getHeight() - (20 + random.nextInt(Math.abs((int) (getHeight() / 1.5f) - 15))), getDimensionInPixel(14), getHeight() - 5, paint);
        canvas.drawRect(getDimensionInPixel(16), getHeight() - (20 + random.nextInt(Math.abs((int) (getHeight() / 1.5f) - 15))), getDimensionInPixel(18), getHeight() -5, paint);
    }

    /**
     * 색상설정
     * @param color 설정할 색
     */
    public void setColor(int color) {
        paint.setColor(color);
        invalidate();
    }

    /**
     * view 가 다른 화면해상도에서도 제대로 동작하도록 모든 크기를 dp로 가져온다
     */
    private int getDimensionInPixel(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            post(animateView);
        } else if (visibility == GONE) {
            removeCallbacks(animateView);
        }
    }
}