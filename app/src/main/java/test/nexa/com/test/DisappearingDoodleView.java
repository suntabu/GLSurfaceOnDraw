package test.nexa.com.test;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;

/**
 * Created by foruok，欢迎关注我的订阅号“程序视界”.
 */

public class DisappearingDoodleView extends View {
    public static float convertDipToPx(Context context, float fDip) {
        float fPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, fDip,
                context.getResources().getDisplayMetrics());
        return fPx;
    }

    final static String TAG = "DoodleView";

    class LineElement {
        static final public int ALPHA_STEP = 10;
        private int mAlpha = 255;

        public LineElement(float pathWidth) {
            mPaint = new Paint();
            mPaint.setARGB(255, 255, 0, 0);
            mPaint.setAntiAlias(true);
            mPaint.setStrokeWidth(0);
//            mPaint.setStyle(Paint.Style.STROKE);//设置为空心
            mPaint.setStrokeCap(Paint.Cap.BUTT);
            mPaint.setStyle(Paint.Style.FILL);
            mPath = new Path();
            mPathWidth = mTempPathWidth = pathWidth;
            for (int i = 0; i < mPoints.length; i++) {
                mPoints[i] = new PointF();
            }
        }

        public void setPaint(Paint paint) {
            mPaint = paint;
        }

        public void setAlpha(int alpha) {
            mAlpha = alpha;
            mPaint.setAlpha(mAlpha);

            mPathWidth = (mAlpha * mTempPathWidth) / 255;


//            Log.d(TAG, "ALPHA: " + mAlpha + "  " + mPathWidth);
        }

        private boolean caculatePoints(float k, float b, float x1, float y1, float distance, PointF pt1, PointF pt2) {
            //point-k formula
            // y= kx + b
            //distance formula of two points
            // distance*distance = Math.pow((x - x1), 2) + Math.pow((y - y1), 2)
            // |
            // V
            // ax*x + bx + c = 0;
            // |
            // V
            // x = (-b +/- Math.sqrt( b*b - 4*a*c ) ) / (2*a)
            double a1 = Math.pow(k, 2) + 1;
            double b1 = 2 * k * (b - y1) - 2 * x1;
            double c1 = Math.pow(x1, 2) + Math.pow(b - y1, 2) - Math.pow(distance, 2);
            double criterion = Math.pow(b1, 2) - 4 * a1 * c1;
            if (criterion > 0) {
                criterion = Math.sqrt(criterion);
                pt1.x = (float) ((-b1 + criterion) / (2 * a1));
                pt1.y = k * pt1.x + b;
                pt2.x = (float) ((-b1 - criterion) / (2 * a1));
                pt2.y = k * pt2.x + b;
                return true;
            }
            return false;
        }

        private void swapPoint(PointF pt1, PointF pt2) {
            float t = pt1.x;
            pt1.x = pt2.x;
            pt2.x = t;
            t = pt1.y;
            pt1.y = pt2.y;
            pt2.y = t;
        }

        private boolean pointCmp(PointF a, PointF b, PointF center) {
            if (a.x >= 0 && b.x < 0)
                return true;
            if (a.x == 0 && b.x == 0)
                return a.y > b.y;
            //向量OA和向量OB的叉积
            float det = (a.x - center.x) * (b.y - center.y) - (b.x - center.x) * (a.y - center.y);
            if (det < 0)
                return true;
            if (det > 0)
                return false;
            //向量OA和向量OB共线，以距离判断大小
            float d1 = (a.x - center.x) * (a.x - center.x) + (a.y - center.y) * (a.y - center.y);
            float d2 = (b.x - center.x) * (b.x - center.y) + (b.y - center.y) * (b.y - center.y);
            return d1 > d2;
        }


        private void reorderUntiClockwise(PointF[] vPoints) {
            //计算重心
            PointF center = new PointF();
            float x = 0, y = 0;
            for (int i = 0; i < vPoints.length; i++) {
                x += vPoints[i].x;
                y += vPoints[i].y;
            }
            center.x = x / vPoints.length;
            center.y = y / vPoints.length;

            //冒泡排序
            for (int i = 0; i < vPoints.length - 1; i++) {
                for (int j = 0; j < vPoints.length; j++) {
                    if (j < vPoints.length - 1) {
                        if (pointCmp(vPoints[j], vPoints[j + 1], center)) {
                            PointF tmp = vPoints[j];
                            vPoints[j] = vPoints[j + 1];
                            vPoints[j + 1] = tmp;
                        }
                    } else {
                        if (pointCmp(vPoints[j], vPoints[0], center)) {
                            PointF tmp = vPoints[j];
                            vPoints[j] = vPoints[0];
                            vPoints[0] = tmp;
                        }
                    }
                }
            }
        }


        public boolean updatePathPoints() {
            float distance = mPathWidth / 2;


            if (Math.abs(mEndX - mStartX) < 1) {
                mPoints[0].x = mStartX + distance;
                mPoints[0].y = mStartY - distance;
                mPoints[1].x = mStartX - distance;
                mPoints[1].y = mPoints[0].y;
                mPoints[2].x = mPoints[1].x;
                mPoints[2].y = mEndY + distance;
                mPoints[3].x = mPoints[0].x;
                mPoints[3].y = mPoints[2].y;
            } else if (Math.abs(mEndY - mStartY) < 1) {
                mPoints[0].x = mStartX - distance;
                mPoints[0].y = mStartY - distance;
                mPoints[1].x = mPoints[0].x;
                mPoints[1].y = mStartY + distance;
                mPoints[2].x = mEndX + distance;
                mPoints[2].y = mPoints[1].y;
                mPoints[3].x = mPoints[2].x;
                mPoints[3].y = mPoints[0].y;
            } else {

                PointF direction = new PointF(mEndX - mStartX, mEndY - mStartY);
                direction.x = direction.x / direction.length();
                direction.y = direction.y / direction.length();
                PointF directionV = new PointF(direction.y, -direction.x);

                float dx = distance * directionV.x;
                float dy = distance * directionV.y;

                PointF leftTop = new PointF(mEndX + dx, mEndY + dy);
                PointF rightTop = new PointF(mEndX - dx, mEndY - dy);
                PointF leftBottom = new PointF(mStartX + dx, mStartY + dy);
                PointF rightBottom = new PointF(mStartX - dx, mStartY - dy);
//            Log.d(TAG, "V: " + new PointF(leftTop.x - rightTop.x, leftTop.y - rightTop.y).length());
//            mPoints[0] = leftTop;
//            mPoints[1] = leftBottom;
//            mPoints[2] = rightBottom;
//            mPoints[3] = rightTop;


                mPoints[0] = leftTop;
                mPoints[1] = rightTop;
                mPoints[2] = rightBottom;
                mPoints[3] = leftBottom;
                return true;


//                //point-k formula
//                //y= kx + b
//                float kLine = (mEndY - mStartY) / (mEndX - mStartX);
//                float kVertLine = -1 / kLine;
//                float b = mStartY - (kVertLine * mStartX);
//                if (!caculatePoints(kVertLine, b, mStartX, mStartY, distance, mPoints[0], mPoints[1])) {
//                    String info = String.format(TAG, "startPt, criterion < 0, (%.2f, %.2f)-->(%.2f, %.2f), kLine - %.2f, kVertLine - %.2f, b - %.2f",
//                            mStartX, mStartY, mEndX, mEndY, kLine, kVertLine, b);
//                    Log.i(TAG, info);
//                    return false;
//                }
//                b = mEndY - (kVertLine * mEndX);
//                if (!caculatePoints(kVertLine, b, mEndX, mEndY, distance, mPoints[2], mPoints[3])) {
//                    String info = String.format(TAG, "endPt, criterion < 0, (%.2f, %.2f)-->(%.2f, %.2f), kLine - %.2f, kVertLine - %.2f, b - %.2f",
//                            mStartX, mStartY, mEndX, mEndY, kLine, kVertLine, b);
//                    Log.i(TAG, info);
//                    return false;
//                }

                //TODO: use other ways to reorder unti-clockwise points
//                reorderUntiClockwise(mPoints);
                //reorder points to unti-clockwise
//                if (mStartX < mEndX) {
//                    if (mStartY < mEndY) {
//                        if (mPoints[0].x < mPoints[1].x) {
//                            swapPoint(mPoints[0], mPoints[1]);
//                        }
//                        if (mPoints[2].x > mPoints[3].x) {
//                            swapPoint(mPoints[2], mPoints[3]);
//                        }
//                    } else {
//                        if (mPoints[0].x > mPoints[1].x) {
//                            swapPoint(mPoints[0], mPoints[1]);
//                        }
//                        if (mPoints[2].x < mPoints[3].x) {
//                            swapPoint(mPoints[2], mPoints[3]);
//                        }
//                    }
//                } else {
//                    if (mStartY < mEndY) {
//                        if (mPoints[0].x < mPoints[1].x) {
//                            swapPoint(mPoints[0], mPoints[1]);
//                        }
//                        if (mPoints[2].x > mPoints[3].x) {
//                            swapPoint(mPoints[2], mPoints[3]);
//                        }
//                    } else {
//                        if (mPoints[0].x > mPoints[1].x) {
//                            swapPoint(mPoints[0], mPoints[1]);
//                        }
//                        if (mPoints[2].x < mPoints[3].x) {
//                            swapPoint(mPoints[2], mPoints[3]);
//                        }
//                    }
//                }
            }

            return true;
        }

        // for the first line
        public void updatePath() {
            //update path
            mPath.reset();
            mPath.moveTo(mPoints[0].x, mPoints[0].y);
            mPath.lineTo(mPoints[1].x, mPoints[1].y);
            mPath.lineTo(mPoints[2].x, mPoints[2].y);
            mPath.lineTo(mPoints[3].x, mPoints[3].y);
            mPath.close();
        }

        // for middle line
        public void updatePathWithStartPoints(PointF pt1, PointF pt2) {
            mPath.reset();
            mPath.moveTo(pt1.x, pt1.y);
            mPath.lineTo(pt2.x, pt2.y);
            mPath.lineTo(mPoints[2].x, mPoints[2].y);
            mPath.lineTo(mPoints[3].x, mPoints[3].y);
            mPath.close();
        }

        public float mStartX = -1;
        public float mStartY = -1;
        public float mEndX = -1;
        public float mEndY = -1;
        public Paint mPaint;
        public Path mPath;
        public PointF[] mPoints = new PointF[4]; //path's vertex
        float mPathWidth, mTempPathWidth;

        public int getAlpha() {
            return mAlpha;
        }
    }

    private LineElement mCurrentLine = null;
    private List<LineElement> mLines = null;
    private float mLaserX = 0;
    private float mLaserY = 0;
    final Paint mPaint = new Paint();
    private int mWidth = 0;
    private int mHeight = 0;
    private long mElapsed = 0;
    private float mStrokeWidth = 60;
    private float mCircleRadius = 10;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            DisappearingDoodleView.this.invalidate();
        }
    };

    public DisappearingDoodleView(Context context) {
        super(context);
        initialize(context);
    }

    public DisappearingDoodleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    private void initialize(Context context) {
        mStrokeWidth = convertDipToPx(context, 22);
        mCircleRadius = convertDipToPx(context, 10);
        mPaint.setARGB(255, 255, 0, 0);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(0);
        mPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
        mHeight = h;
        adjustLasterPosition();
    }

    private void adjustLasterPosition() {
        if (mLaserX - mCircleRadius < 0) mLaserX = mCircleRadius;
        else if (mLaserX + mCircleRadius > mWidth) mLaserX = mWidth - mCircleRadius;
        if (mLaserY - mCircleRadius < 0) mLaserY = mCircleRadius;
        else if (mLaserY + mCircleRadius > mHeight) mLaserY = mHeight - mCircleRadius;
    }

    private void updateLaserPosition(float x, float y) {
        mLaserX = x;
        mLaserY = y;
        adjustLasterPosition();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //canvas.drawText("ABCDE", 10, 16, mPaint);
        mElapsed = SystemClock.elapsedRealtime();

        if (mLines != null) {
            updatePaths();
            for (LineElement e : mLines) {
                if (e.mStartX < 0 || e.mEndY < 0 || e.mPath.isEmpty()) continue;
                //canvas.drawLine(e.mStartX, e.mStartY, e.mEndX, e.mEndY, e.mPaint);
                canvas.drawPath(e.mPath, e.mPaint);
            }
            compactPaths();
        }
//        canvas.drawCircle(mLaserX, mLaserY, mCircleRadius, mPaint);
    }

    private boolean isValidLine(float x1, float y1, float x2, float y2) {
        return Math.abs(x1 - x2) > 1 || Math.abs(y1 - y2) > 1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        int action = event.getAction();
        if (action == MotionEvent.ACTION_UP) {// end one line after finger release
            if (isValidLine(mCurrentLine.mStartX, mCurrentLine.mStartY, x, y)) {
                mCurrentLine.mEndX = x;
                mCurrentLine.mEndY = y;
                addToPaths(mCurrentLine);
            }
            //mCurrentLine.updatePathPoints();
            mCurrentLine = null;
            updateLaserPosition(x, y);
            invalidate();
            return true;
        }

        if (action == MotionEvent.ACTION_DOWN) {
            mLines = null;
            mCurrentLine = new LineElement(mStrokeWidth);

            mCurrentLine.mStartX = x;
            mCurrentLine.mStartY = y;
            updateLaserPosition(x, y);
            return true;
        }

        if (action == MotionEvent.ACTION_MOVE) {
            if (isValidLine(mCurrentLine.mStartX, mCurrentLine.mStartY, x, y)) {
                mCurrentLine.mEndX = x;
                mCurrentLine.mEndY = y;
                addToPaths(mCurrentLine);

                mCurrentLine = new LineElement(mStrokeWidth);
                mCurrentLine.mStartX = x;
                mCurrentLine.mStartY = y;

                updateLaserPosition(x, y);
            } else {
                //do nothing, wait next point
            }
        }

        if (mHandler.hasMessages(1)) {
            mHandler.removeMessages(1);
        }
        Message msg = new Message();
        msg.what = 1;
        mHandler.sendMessageDelayed(msg, 0);

        return true;
    }

    private void addToPaths(LineElement element) {
        if (mLines == null) {
            mLines = new ArrayList<LineElement>();
        }
        mLines.add(element);
    }

    private void updatePaths() {
        int size = mLines.size();
        if (size == 0) return;


        LineElement line = null;
        int j = 0;
        for (; j < size; j++) {
            line = mLines.get(j);
//            line.updatePathPoints();
            if (line.updatePathPoints()) break;
        }

        if (j == size) {
            mLines.clear();
            return;
        } else {
            for (j--; j >= 0; j--) {
                mLines.remove(0);
            }
        }

        line.updatePath();
        size = mLines.size();

        LineElement lastLine = null;
        for (int i = 1; i < size; i++) {
            line = mLines.get(i);
            if (line.updatePathPoints()) {
                if (lastLine == null) {
                    lastLine = mLines.get(i - 1);
                }
                line.updatePathWithStartPoints(lastLine.mPoints[3], lastLine.mPoints[2]);
                lastLine = null;
            } else {
                mLines.remove(i);
                size = mLines.size();
            }
        }
    }

    public void compactPaths() {

        int size = mLines.size();
        int index = size - 1;
        if (size == 0) return;
        int baseAlpha = 255 - LineElement.ALPHA_STEP;
        int itselfAlpha;
        LineElement line;
        for (; index >= 0; index--, baseAlpha -= LineElement.ALPHA_STEP) {
            line = mLines.get(index);
            itselfAlpha = line.getAlpha();
            if (itselfAlpha == 255) {
                if (baseAlpha <= 0 || line.mPathWidth < 1) {
                    ++index;
                    break;
                }
                line.setAlpha(baseAlpha);
            } else {
                itselfAlpha -= LineElement.ALPHA_STEP;
                if (itselfAlpha <= 0 || line.mPathWidth < 1) {
                    ++index;
                    break;
                }
                line.setAlpha(itselfAlpha);
            }
        }

        if (index >= size) {
            // all sub-path should disappear
            mLines = null;
        } else if (index >= 0) {
            //Log.i(TAG, "compactPaths from " + index + " to " + (size - 1));
            mLines = mLines.subList(index, size);
        } else {
            // no sub-path should disappear
        }

        long interval = 40 - SystemClock.elapsedRealtime() + mElapsed;
        if (interval < 0) interval = 0;
        Message msg = new Message();
        msg.what = 1;
        mHandler.sendMessageDelayed(msg, interval);
    }
}