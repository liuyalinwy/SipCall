package scutbci.lyl.sipcall;


import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;
import android.os.Message;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

public class FlashButton extends AppCompatTextView {

    private int	mState;
    private int	mType;
    private int	mInterval;
    private int[]	mImageId;
    private int[]	mTextColor;
    private int[]	mBackColor;
    private float	mTextSize;
    private Paint	mPaint;
    private Timer	mTimer;

    public FlashButton(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
        mState = 0;
        mType = 0;
        mInterval = 100;
        mPaint = new Paint();
        mTextColor = new int[2];
        mBackColor = new int[2];
        mImageId = new int[2];
        mTextColor[0] = Color.WHITE;
        mTextColor[1] = Color.BLACK;
        mBackColor[0] = Color.BLACK;
        mBackColor[1] = Color.WHITE;
        mImageId[0] = -1;
        mImageId[1] = -1;
        mTextSize = getTextSize();
    }

    public FlashButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
        mState = 0;
        mPaint = new Paint();
        mTextColor = new int[2];
        mBackColor = new int[2];
        mImageId = new int[2];

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.FlashButton);
        mType = array.getInt(R.styleable.FlashButton_type, 0);
        mInterval = array.getInt(R.styleable.FlashButton_interval, 100);
        mTextColor[0] = array.getColor(R.styleable.FlashButton_textColor1, Color.WHITE);
        mTextColor[1] = array.getColor(R.styleable.FlashButton_textColor2, Color.BLACK);
        mBackColor[0] = array.getColor(R.styleable.FlashButton_backColor1, Color.BLACK);
        mBackColor[1] = array.getColor(R.styleable.FlashButton_backColor2, Color.WHITE);
        mTextSize = array.getDimension(R.styleable.FlashButton_textSize, 36);
        mImageId[0] = array.getResourceId(R.styleable.FlashButton_imageId1, R.mipmap.ic_launcher);
        mImageId[1] = array.getResourceId(R.styleable.FlashButton_imageId2, R.mipmap.ic_launcher);
        array.recycle();
    }

    public FlashButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // TODO Auto-generated constructor stub
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // TODO
        int w = getMeasuredWidth()- getPaddingLeft() - getPaddingRight();
        int h = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
        Rect rcPaint = new Rect(getPaddingLeft(),
                getPaddingTop(),
                getPaddingLeft() + w,
                getPaddingTop() + h);
        mTextSize = w > h ? h : w;
        mPaint.setAntiAlias(true);

        if (mType == 0)
        {
            mPaint.setColor(mBackColor[mState]);
            canvas.drawRect(rcPaint, mPaint);
            String mText = (String)getText();
            mPaint.setTextSize(mTextSize);
            Paint.FontMetrics fm = mPaint.getFontMetrics();
            mPaint.setColor(mTextColor[mState]);
            mPaint.setTextAlign(Paint.Align.CENTER);
            mPaint.setTypeface(Typeface.create("Arial", Typeface.BOLD));
            canvas.drawText(mText, rcPaint.left + w/2, (rcPaint.top+rcPaint.bottom-fm.top-fm.bottom)/2, mPaint);
        }
        else if (mType == 1)
        {
            if ((mImageId != null) && (mImageId[0] > 0) && (mImageId[1] > 0))
            {
                InputStream is = getResources().openRawResource(mImageId[mState]);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                canvas.drawBitmap(bitmap, new Rect(0,0,bitmap.getWidth(),bitmap.getHeight()),
                        rcPaint, mPaint);
            }
        }
    }

    public void setState(int state) {
        this.mState = state;
    }

    public void setType(int type) {
        this.mType = type;
    }

    public void setTextColor(int color[]) {
        this.mTextColor = color;
    }

    public void setBackColor(int color[]) {
        this.mBackColor = color;
    }

    public void setImage(int imageid[]) {
        this.mImageId = imageid;
    }

    public void Flash(boolean start)
    {
        if (start)
        {
            mTimer = new Timer(true);
            mTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    Message msg = new Message();
                    msg.what = 1;
                    TimerHandler.sendMessage(msg);
                }
            }, mInterval/2, mInterval/2);
        }
        else
        {
            if (mTimer != null) {
                mTimer.cancel();
                mTimer = null;
            }

            // reset state
            (new Timer(true)).schedule(new TimerTask() {
                @Override
                public void run() {
                    Message msg = new Message();
                    msg.what = 2;
                    TimerHandler.sendMessage(msg);
                }
            }, 1);
        }
    }

    public void FlashOnce(int onset)
    {
        mState = 1;
        mTimer = new Timer(true);
        // onset
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = 3;
                TimerHandler.sendMessage(msg);
            }
        }, 1);
        // delay to reset
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = 2;
                TimerHandler.sendMessage(msg);
            }
        }, onset);
    }

    private Handler TimerHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int msgId = msg.what;
            switch (msgId) {
                case 1: // change state
                    mState = 1 - mState;
                    invalidate();
                    break;
                case 2: // reset
                    mState = 0;
                    invalidate();
                    break;
                case 3: // onset
                    mState = 1;
                    invalidate();
                    break;
                default:
                    break;
            }
        }
    };
}
