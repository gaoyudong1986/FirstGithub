package com.example.bitmaptest;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class FaceView extends View {
	private static final String TAG = "FaceView";
	
	private Context mContext;
	private float mViewWidth;
	private float mViewHeight;
	private float mViewLeft;
	private float mViewTop;
	
	private static final float MAX_SCALE_VALUE = 6.0f;
	
	private int mBackgroundColor =0xFF000000;
	private boolean mNeedFitInImage = false;
    
	private float mFitInScale = 1.0f;
	private float mFitOutScale = 1.0f;
	private float mMaxScale = MAX_SCALE_VALUE;
	
	private Bitmap mFaceBitmap = null;
	private float mFaceBitmapWidth;
	private float mFaceBitmapHeight;
	private float mFaceScale = 1.0f;
	private RectF mFaceRect = new RectF();
	private RectF mDstRect = new RectF();
	
	private static final int STATE_NONE = 0x1000;
	private static final int STATE_INIT = 0x1001;
	private static final int STATE_PAN = 0x1002;
	private static final int STATE_ZOOM = 0x1003;
	private static final int STATE_ADJUST = 0X1004;
	private static final int STATE_ANIMATION = 0x1005;
	private static final int STATE_FLING = 0x1006;
	private int mState = STATE_NONE;
	
	private boolean mEnableScrollX = true;
	private boolean mEnableScrollY = true;
	
	private static final float MIN_FLING_VELOCITY = 300;
	private static final float MAX_FLING_VELOCITY = 3000;
	
	private long mAnimationStartTime;
	private long mAnimationDuration;
	private float mAnimationDeltaX;
	private float mAnimationDeltaY;
	private float mAnimationVelocity = 2.0f;  //pix / ms
	private float mAnimationLastDeltaX = 0.0f;
	private float mAnimationLastDeltaY = 0.0f;
	
	private float mAnimationFlingDeltaX = 0.0f;
	private float mAnimationFlingDeltaY = 0.0f;
	
	private float mAnimationScaleCenterX;
	private float mAnimationScaleCenterY;
	private float mAnimationDstScale;
	private float mAnimationDeltaScale;
	private float mAnimationStartScale;
    
    private float mPreX = 0;
    private float mPreY = 0;
    private float mCurX = 0;
    private float mCurY = 0;
    private float mOffsetX = 0;
    private float mOffsetY = 0;
    
    private boolean mEnableGestureDetector = true;
	
	public FaceView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		mContext = context;
		
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.FaceView, defStyle, 0);
        
        mBackgroundColor = a.getColor(R.styleable.FaceView_Background, 0xFF000000);
        
        mNeedFitInImage = a.getBoolean(R.styleable.FaceView_FitImage, true);
        
        a.recycle();
	}

	public FaceView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		// TODO Auto-generated constructor stub
	}

	public FaceView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		mContext = context;
	}

	public void recycle() {
		if (mHandler != null) {
			mHandler.removeMessages(0);
			mHandler = null;
		}
	}
	
	public void setFaceBitmap(Bitmap bitmap){
		mFaceBitmap = bitmap;
		this.invalidate();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		if(mFaceBitmap != null && mState == STATE_NONE){
			initFaceView();
			createHandler();
		}
		canvas.save();
		canvas.drawColor(mBackgroundColor);
		if(mState != STATE_NONE){
			if(null != mFaceBitmap){
				canvas.drawBitmap(mFaceBitmap, null, mFaceRect, null);
			}
		}
		canvas.restore();
	}
    
    private void initFaceView(){
    	if(mState == STATE_NONE && mFaceBitmap != null){
    		mFaceBitmapWidth = mFaceBitmap.getWidth();
    		mFaceBitmapHeight = mFaceBitmap.getHeight();
    		mViewWidth = this.getWidth();
    		mViewHeight = this.getHeight();
    		int[] location = new int[2];
    		this.getLocationOnScreen(location);
    		mViewLeft = location[0];
    		mViewTop = location[1];
    		
    		mFitInScale = fitView(mFaceBitmap, true);
    		mFitOutScale = fitView(mFaceBitmap, false);
    		
    		if(mNeedFitInImage){
    			mFaceScale = mFitInScale;
    		}
    		else{
    			mFaceScale = mFitOutScale;
    		}
    		
    		mFaceRect.left = mViewWidth/2 - mFaceBitmapWidth/2 * mFaceScale;
    		mFaceRect.top = mViewHeight/2 - mFaceBitmapHeight/2 * mFaceScale;
    		mFaceRect.right = mFaceRect.left + mFaceBitmapWidth * mFaceScale;
    		mFaceRect.bottom = mFaceRect.top + mFaceBitmapHeight * mFaceScale;
            
    		mState = STATE_INIT;
    	}
    }
    
	public void onShowPress(float x, float y){
		if(!mEnableGestureDetector)
			return;
	}
	
	public boolean onDrag(int type, float x, float y){
		return true;
	}
	
	public boolean onScrollStart(float x1, float y1, float x2, float y2, float distanceX, float distanceY){
		if(!mEnableGestureDetector)
			return false;
		if(mState == STATE_INIT){
			mState = STATE_PAN;
			mEnableScrollX = true;
			mEnableScrollY = true;
			
			if(mFaceRect.width() <= mViewWidth){
				mEnableScrollX = false;
			}
			if(mFaceRect.height() <= mViewHeight){
				mEnableScrollY = false;
			}
		}

		return true;
	}
	
	public boolean onScroll(float x1, float y1, float x2, float y2, float distanceX, float distanceY){
		if(!mEnableGestureDetector)
			return false;
		if(mState == STATE_PAN){
			mFaceRect.left -= distanceX;
			mFaceRect.right = mFaceRect.left + mFaceBitmapWidth * mFaceScale;
			mFaceRect.top -= distanceY;
			mFaceRect.bottom = mFaceRect.top + mFaceBitmapHeight * mFaceScale;
			this.invalidate();
		}

		return true;
	}
	
	public void onScrollEnd(float x1, float y1, float x2, float y2, float distanceX, float distanceY){
		if(!mEnableGestureDetector)
			return;
		if(mState == STATE_PAN){
			mDstRect = getDstRect(mFaceRect);
			mAnimationDeltaX = mDstRect.left - mFaceRect.left;
			mAnimationDeltaY = mDstRect.top - mFaceRect.top;

			if(mAnimationDeltaX == 0 && mAnimationDeltaY == 0){
				mState = STATE_INIT;
			}
			else{
				mState = STATE_ANIMATION;
				mAnimationDuration = 
					(long) (Math.sqrt(mAnimationDeltaX * mAnimationDeltaX + mAnimationDeltaY * mAnimationDeltaY)
							/ mAnimationVelocity);
				
				sendMessageToHandler(DO_ANIMATION_PAN);
			}
		}
	}
	
	public boolean onFling(float velocityX, float velocityY){
		if(!mEnableGestureDetector)
			return false;
		if(mState == STATE_PAN){
			mState = STATE_FLING;
			boolean toLeft = false;
			boolean toRight = false;
			boolean toTop = false;
			boolean toBottom = false;
			
			boolean enFlingX = true;
			boolean enFlingY = true;
			
			if(velocityX >0){
				toRight = true;
			}
			else{
				toLeft = true;
			}
			
			if(velocityY >0){
				toBottom = true;
			}
			else{
				toTop =true;
			}
			
			if(Math.abs(velocityX) < MIN_FLING_VELOCITY || !mEnableScrollX){
				enFlingX = false;
			}
			if(Math.abs(velocityY) < MIN_FLING_VELOCITY || !mEnableScrollY){
				enFlingY = false;
			}
			
			if(enFlingX){
				if(Math.abs(velocityX) > MAX_FLING_VELOCITY){
					if(toRight){
						velocityX = MAX_FLING_VELOCITY;
						mAnimationFlingDeltaX = velocityX / 5.0f;
						if(mFaceRect.left + mAnimationFlingDeltaX > 0){
							mAnimationFlingDeltaX = -mFaceRect.left;
						}
					}
					else{
						velocityX = -MAX_FLING_VELOCITY;
						mAnimationFlingDeltaX = velocityX / 5.0f;
						if(mFaceRect.right + mAnimationFlingDeltaX < mViewWidth){
							mAnimationFlingDeltaX = mViewWidth - mFaceRect.right;
						}
					}
				}
			}
			
			if(enFlingY){
				if(Math.abs(velocityY) > MAX_FLING_VELOCITY){
					if(toBottom){
						velocityY = MAX_FLING_VELOCITY;
						mAnimationFlingDeltaY = velocityY / 5.0f;
						if(mFaceRect.top + mAnimationFlingDeltaY > 0){
							mAnimationFlingDeltaY = -mFaceRect.top;
						}
					}
					else{
						velocityY = -MAX_FLING_VELOCITY;
						mAnimationFlingDeltaY = velocityY / 5.0f;
						if(mFaceRect.bottom + mAnimationFlingDeltaY < mViewHeight){
							mAnimationFlingDeltaY = mViewHeight - mFaceRect.bottom;
						}
					}
				}
			}
			
			if(mAnimationFlingDeltaX == 0 && mAnimationFlingDeltaY == 0){
				mState = STATE_PAN;
			}
			else{
				mState = STATE_ANIMATION;
				
				mAnimationDuration = 
					(long) (Math.sqrt(mAnimationFlingDeltaX * mAnimationFlingDeltaX + mAnimationFlingDeltaY * mAnimationFlingDeltaY)
							/ (mAnimationVelocity * 2));
				
				sendMessageToHandler(DO_ANIMATION_FLING);
			}
		}
		return false;
	}
	
	public boolean onClick(float x, float y){
		if(!mEnableGestureDetector)
			return false;
		return false;
	}
	
	public boolean onClickUp(float x, float y){
		if(!mEnableGestureDetector)
			return false;
		return false;
	}
	
	public boolean onDoubleClick(float x, float y){
		if(!mEnableGestureDetector)
			return false;
		if(mState == STATE_INIT){
			float dstscale = 0;

			if(mFaceScale <= mFitInScale){
				dstscale = mFitOutScale;
			}
			else{
				dstscale = mFitInScale;
			}
			showPoint(x, y,
					mFaceScale, dstscale,
					x, y);
		}
		return false;
	}
	
	public boolean onMultiTouchMove(int x0, int y0, int x1, int y1, float scale, double angle){
		if(!mEnableGestureDetector)
			return false;
//		if(mState == STATE_ZOOM){	
			mFaceScale *= scale;
			
			float midX = (x0 + x1) / 2.0f;
			float midY = (y0 + y1) / 2.0f;
			
			float oldW = mFaceRect.width();
			float oldH = mFaceRect.height();
			float newW = mFaceBitmapWidth * mFaceScale;
			float newH = mFaceBitmapHeight * mFaceScale;
			
			float ratioX = ((midX - mFaceRect.left) / oldW);
			float ratioY = ((midY - mFaceRect.top) / oldH);
			
			mFaceRect.left = midX - ratioX * newW;
			mFaceRect.top = midY - ratioY * newH;
			mFaceRect.right = mFaceRect.left + newW;
			mFaceRect.bottom = mFaceRect.top + newH;
			this.invalidate();
//		}

		return true;
	}
	
	public boolean onMultiTouchBegin(int x0, int y0, int x1, int y1){
		if(!mEnableGestureDetector)
			return false;
		if(mState == STATE_INIT){
			mState = STATE_ZOOM;
		}
		
		return true;
	}
	
	public boolean onMultiTouchEnd(int x0, int y0, int x1, int y1){
		if(!mEnableGestureDetector)
			return false;
//		if(mState == STATE_ZOOM){
			float cx = (x0 + x1) / 2.0f;
			float cy = (y0 + y1) / 2.0f;
			float dstscale = 0.0f;
			
			if(mFaceScale > mMaxScale){
				dstscale = mMaxScale;
			}
			else if(mFaceScale < mFitInScale){
				dstscale = mFitInScale;
			}
			else{
				dstscale = mFaceScale;
			}
			
			showPoint(cx, cy,
					mFaceScale, dstscale,
					cx, cy);
//		}
		
		return true;
	}
    
	private float fitView(Bitmap bitmap, boolean isfitin){
		float scale = 1.0f;
		
		float bmpW = bitmap.getWidth();
		float bmpH = bitmap.getHeight();
		float viewW = this.getWidth();
		float viewH = this.getHeight();
		
		if(isfitin){
			scale = ((viewW / bmpW) < (viewH / bmpH)) ? (viewW / bmpW) : (viewH / bmpH);
		}
		else{
			scale = ((viewW / bmpW) > (viewH / bmpH)) ? (viewW / bmpW) : (viewH / bmpH);
		}

		return scale;
	}
	
	void playPanFinishAnimation(){
		long time = SystemClock.uptimeMillis();
		if (mAnimationStartTime == 0){
			mAnimationStartTime = time;
		}
		long duration = time - mAnimationStartTime;
		if(duration < mAnimationDuration){
			float x=((float)duration)/((float)mAnimationDuration);
			float interpolated = getInterpolation(x);
			
			mFaceRect.left += interpolated * mAnimationDeltaX - mAnimationLastDeltaX;
			mFaceRect.top += interpolated * mAnimationDeltaY - mAnimationLastDeltaY;
			mFaceRect.right += interpolated * mAnimationDeltaX - mAnimationLastDeltaX;
			mFaceRect.bottom += interpolated * mAnimationDeltaY - mAnimationLastDeltaY;
			
			mAnimationLastDeltaX = interpolated * mAnimationDeltaX;
			mAnimationLastDeltaY = interpolated * mAnimationDeltaY;
			this.invalidate();
			sendMessageToHandler(DO_ANIMATION_PAN);
		}
		else{
			mFaceRect.left = mDstRect.left;
			mFaceRect.right = mDstRect.right;
			mFaceRect.top = mDstRect.top;
			mFaceRect.bottom = mDstRect.bottom;
			
			mAnimationStartTime = 0;
			mAnimationLastDeltaX = 0.0f;
			mAnimationLastDeltaY = 0.0f;
			mAnimationDeltaX = 0.0f;
			mAnimationDeltaY = 0.0f;
			mState = STATE_INIT;
			this.invalidate();
		}
	}
	
	void playFlingAnimation(){
		long time = SystemClock.uptimeMillis();
		if (mAnimationStartTime == 0){
			mAnimationStartTime = time;
		}
		long duration = time - mAnimationStartTime;
		if(duration < mAnimationDuration){
			float x=((float)duration)/((float)mAnimationDuration);
			float interpolated = getInterpolation(x);
			
			mFaceRect.left += interpolated * mAnimationFlingDeltaX - mAnimationLastDeltaX;
			mFaceRect.top += interpolated * mAnimationFlingDeltaY - mAnimationLastDeltaY;
			mFaceRect.right += interpolated * mAnimationFlingDeltaX - mAnimationLastDeltaX;
			mFaceRect.bottom += interpolated * mAnimationFlingDeltaY - mAnimationLastDeltaY;
			
			mAnimationLastDeltaX = interpolated * mAnimationFlingDeltaX;
			mAnimationLastDeltaY = interpolated * mAnimationFlingDeltaY;
			this.invalidate();
			sendMessageToHandler(DO_ANIMATION_FLING);
		}
		else{
			mFaceRect.left += (mAnimationFlingDeltaX - mAnimationLastDeltaX);
			mFaceRect.top += (mAnimationFlingDeltaY - mAnimationLastDeltaY);
			mFaceRect.right += (mAnimationFlingDeltaX - mAnimationLastDeltaX);
			mFaceRect.bottom += (mAnimationFlingDeltaY - mAnimationLastDeltaY);
			
			mAnimationStartTime = 0;
			mAnimationLastDeltaX = 0.0f;
			mAnimationLastDeltaY = 0.0f;
			mAnimationFlingDeltaX = 0.0f;
			mAnimationFlingDeltaY = 0.0f;
			
			mDstRect = getDstRect(mFaceRect);
			mAnimationDeltaX = mDstRect.left - mFaceRect.left;
			mAnimationDeltaY = mDstRect.top - mFaceRect.top;

			if(mAnimationDeltaX == 0 && mAnimationDeltaY == 0){
				mState = STATE_INIT;
			}
			else{
				mState = STATE_ANIMATION;
				mAnimationDuration = 
					(long) (Math.sqrt(mAnimationDeltaX * mAnimationDeltaX + mAnimationDeltaY * mAnimationDeltaY)
							/ mAnimationVelocity);
				
				sendMessageToHandler(DO_ANIMATION_PAN);
			}
			this.invalidate();
		}
	}
	
	void playScaleAnimation(){
		long time = SystemClock.uptimeMillis();
		if (mAnimationStartTime == 0){
			mAnimationStartTime = time;
		}
		long duration = time - mAnimationStartTime;
		
		if(duration < mAnimationDuration){
			float x=((float)duration)/((float)mAnimationDuration);
			float interpolated = getInterpolation(x);
			
			mFaceScale = mAnimationStartScale + mAnimationDeltaScale * interpolated;
			
			float oldW = mFaceRect.width();
			float oldH = mFaceRect.height();
			float newW = mFaceBitmapWidth * mFaceScale;
			float newH = mFaceBitmapHeight * mFaceScale;
			
			float ratioX = ((mAnimationScaleCenterX + mAnimationLastDeltaX - mFaceRect.left) / oldW);
			float ratioY = ((mAnimationScaleCenterY + mAnimationLastDeltaY - mFaceRect.top) / oldH);
			
			mFaceRect.left = mAnimationScaleCenterX - ratioX * newW + mAnimationDeltaX * interpolated;
			mFaceRect.top = mAnimationScaleCenterY - ratioY * newH + mAnimationDeltaY * interpolated;
			mFaceRect.right = mFaceRect.left + newW;
			mFaceRect.bottom = mFaceRect.top + newH;
			
			mAnimationLastDeltaX = mAnimationDeltaX * interpolated;
			mAnimationLastDeltaY = mAnimationDeltaY * interpolated;
			
			this.invalidate();
			sendMessageToHandler(DO_ANIMATION_SCALE);
		}
		else{
			mFaceScale = mAnimationDstScale;
			
			float oldW = mFaceRect.width();
			float oldH = mFaceRect.height();
			float newW = mFaceBitmapWidth * mFaceScale;
			float newH = mFaceBitmapHeight * mFaceScale;
			
			float ratioX = ((mAnimationScaleCenterX + mAnimationLastDeltaX - mFaceRect.left) / oldW);
			float ratioY = ((mAnimationScaleCenterY + mAnimationLastDeltaY - mFaceRect.top) / oldH);
			
			mFaceRect.left = mAnimationScaleCenterX - ratioX * newW + mAnimationDeltaX;
			mFaceRect.top = mAnimationScaleCenterY - ratioY * newH + mAnimationDeltaY;
			mFaceRect.right = mFaceRect.left + newW;
			mFaceRect.bottom = mFaceRect.top + newH;
			
			mAnimationLastDeltaX = 0;
			mAnimationLastDeltaY = 0;
			mAnimationStartTime = 0;
			mAnimationDeltaX = 0;
			mAnimationDeltaY = 0;
			
			mState = STATE_INIT;
			this.invalidate();
		}
	}
	
	float getInterpolation(float x) 
	{
		float interpolation = (float)(1.0f - Math.pow((1.0f - x), 2 * 1.5)); //����
		if(interpolation<0.0f){
			interpolation=0.0f;
		}
		if(interpolation>1.0f){
			interpolation=1.0f;
		}
		return interpolation;
	}
	
	private RectF getDstRect(RectF src){
		RectF dst = new RectF();
		
		float left = src.left;
		float right = src.right;
		float top = src.top;
		float bottom = src.bottom;
		
		dst.left = src.left;
		dst.right = src.right;
		dst.top = src.top;
		dst.bottom = src.bottom;
		
		if(left > 0){
			dst.left = 0;
			dst.right = src.width();
		}
		if(top >0){
			dst.top = 0;
			dst.bottom = src.height();
		}
		if(right < mViewWidth){
			dst.right = mViewWidth;
			dst.left = dst.right - src.width();
		}
		if(bottom < mViewHeight){
			dst.bottom = mViewHeight;
			dst.top = dst.bottom - src.height();
		}
		
		if(!mEnableScrollX){
			dst.left = (mViewWidth - src.width())/2.0f;
			dst.right = (mViewWidth + src.width())/2.0f;
		}
		if(!mEnableScrollY){
			dst.top = (mViewHeight - src.height())/2.0f;
			dst.bottom = (mViewHeight + src.height())/2.0f;
		}
		
		return dst;
	}
    
	private static class MyHandler extends Handler {
		private final WeakReference<FaceView> mOwner; 
		
		MyHandler(FaceView owner) {
			mOwner = new WeakReference<FaceView>(owner); 
		}
		
		@Override
		public void handleMessage(Message msg) {
			FaceView owner = mOwner.get();
			if (owner != null) {
				owner.handleMessage(msg);
			}
		}
	}
	
	private Handler mHandler;
	private static final int DO_ANIMATION_PAN = 1;
	private static final int DO_ANIMATION_FLING = 2;
	private static final int DO_ANIMATION_SCALE = 3;
	private void createHandler(){
		if (mHandler != null) {
			return;
		}
		
		mHandler = new MyHandler(this);
	}
	
	private void handleMessage(Message msg) {
		switch (msg.what) {
		case DO_ANIMATION_PAN:
			playPanFinishAnimation();
			break;
		case DO_ANIMATION_FLING:
			playFlingAnimation();
			break;
		case DO_ANIMATION_SCALE:
			playScaleAnimation();
			break;
		default:
			break;
		}
	}
		
	private void sendMessageToHandler(int what){
		mHandler.removeMessages(DO_ANIMATION_PAN);
		mHandler.removeMessages(DO_ANIMATION_FLING);
		mHandler.removeMessages(DO_ANIMATION_SCALE);
		Message msg = mHandler.obtainMessage(what);
		mHandler.sendMessage(msg);
	}
	
	private double calcDistance(float x0, float y0, float x1, float y1){
		return Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0));
	}
    
    private float transCoordScreenToViewX(float x){
    	return x - mViewLeft;
    }
    
    private float transCoordScreenToViewY(float y){
    	return y - mViewTop;
    }
    
    private float transCoordViewToOriImageX(float viewx){
    	float orix = (viewx - mFaceRect.left) / mFaceScale;
    	return orix;
    }
    
    private float transCoordViewToOriImageY(float viewy){
    	float oriy = (viewy - mFaceRect.top) / mFaceScale;
    	return oriy;
    }
    
    private float transCoordOriImageToViewX(float orix){
    	float viewx = orix * mFaceScale + mFaceRect.left;
    	return viewx;
    }
    
    private float transCoordOriImageToViewY(float oriy){
    	float viewy = oriy * mFaceScale + mFaceRect.top;
    	return viewy;
    }
	
	public void showRectToFitView(RectF rect){
		if(mState == STATE_INIT){
			RectF viewRect = changeSrcRectToViewRect(rect);
			
			float cx = (viewRect.left + viewRect.right) / 2.0f;
			float cy = (viewRect.top + viewRect.bottom) / 2.0f;
			float dstscale = getFitScaleWithRect(viewRect);
			
			showPoint(cx, cy,
					mFaceScale, dstscale,
					mViewWidth / 2.0f, mViewHeight / 2.0f);
		}
	}
    
	private void showPoint(float x, float y, float startscale, float dstscale, float tox, float toy){
		mAnimationScaleCenterX = x;
		mAnimationScaleCenterY = y;
		
		mAnimationStartScale = startscale;
		mAnimationDstScale = dstscale;
			
		float[] realXY = new float[2];
		getRealPanDistance(mFaceRect, mAnimationDstScale, 
				mAnimationScaleCenterX, mAnimationScaleCenterY,
				tox, toy,
				realXY);
		
		mAnimationDeltaX = realXY[0];
		mAnimationDeltaY = realXY[1];
			
		mAnimationDeltaScale = mAnimationDstScale - mAnimationStartScale;
		
		mAnimationDuration = (long) (mAnimationDeltaScale * 100.0f);
		if(mAnimationDuration < 200){
			mAnimationDuration = 200;
		}
			
		mState = STATE_ANIMATION;
		sendMessageToHandler(DO_ANIMATION_SCALE);
	}
	
	private void getRealPanDistance(RectF oldRect, float scale, float oldx, float oldy, float tox, float toy, float[] realXY){
		float oldW = oldRect.width();
		float oldH = oldRect.height();
		float newW = mFaceBitmapWidth * scale;
		float newH = mFaceBitmapHeight * scale;
		
		float ratioX = ((oldx - oldRect.left) / oldW);
		float ratioY = ((oldy - oldRect.top) / oldH);
		
		RectF newRect = new RectF();
		
		newRect.left = oldx - ratioX * newW;
		newRect.top = oldy - ratioY * newH;
		newRect.right = newRect.left + newW;
		newRect.bottom = newRect.top + newH;
		
		realXY[0] = 0;
		realXY[1] = 0;
		
		if(newRect.width()>= mViewWidth){
			realXY[0] = tox - oldx;
			if(newRect.left + realXY[0] > 0){
				realXY[0] = -newRect.left;
			}
			else if(newRect.right + realXY[0] < mViewWidth){
				realXY[0] = mViewWidth - newRect.right;
			}
		}
		else{
			realXY[0] = (mViewWidth / 2.0f) - newRect.centerX();
		}
		
		if(newRect.height() >= mViewHeight){
			realXY[1] = toy - oldy;
			if(newRect.top + realXY[1] > 0){
				realXY[1] = -newRect.top;
			}
			else if(newRect.bottom + realXY[1] < mViewHeight){
				realXY[1] = mViewHeight - newRect.bottom;
			}
		}
		else{
			realXY[1] = (mViewHeight / 2.0f) - newRect.centerY();
		}
	}
	
	private Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
		bitmap.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(output);

		int color = 0xff424242;
		Paint paint = new Paint();
		Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		int centerx = bitmap.getWidth() / 2;
		int centery = bitmap.getHeight() / 2;
		
		float roundPx = bitmap.getWidth() / 2 - 8;

		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawCircle(centerx, centery, roundPx, paint);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);
		return output;
	}
	
	private float getFitScaleWithRect(RectF rect){
		float dstscale;
		
		if(rect.width() >= mViewWidth || rect.height() >= mViewHeight){
			dstscale = ((mViewWidth / rect.width()) > (mViewHeight / rect.height()))
					? (mViewWidth / rect.width())
					: (mViewHeight / rect.height());
		}
		else{
			dstscale = ((mViewWidth / rect.width()) < (mViewHeight / rect.height()))
					? (mViewWidth / rect.width())
					: (mViewHeight / rect.height());
		}
		
		dstscale *= 0.67f;
		
		if(mMaxScale < dstscale){
			dstscale = mMaxScale;
		}

		return dstscale;
	}
	
	private RectF changeSrcRectToViewRect(RectF rect){
		RectF newRect = new RectF();
		newRect.left = transCoordOriImageToViewX(rect.left);
		newRect.right = transCoordOriImageToViewX(rect.right);
		newRect.top = transCoordOriImageToViewY(rect.top);
		newRect.bottom = transCoordOriImageToViewY(rect.bottom);
		
		return newRect;
	}
	
	public boolean isEnableGestureDetector(){
		return mEnableGestureDetector;
	}
	
	public void setEnableGestureDetector(boolean enable){
		mEnableGestureDetector = enable;
	}
	
	public void setFaceBackground(int color){
		mBackgroundColor = color;
	}
    
	public void setNeedFitInImage(boolean need){
		mNeedFitInImage = need;
	}
	
	public void resetFaceBitmap(Bitmap face){
		mFaceBitmap = face;
		if(mFaceBitmap != null){
    		mFaceBitmapWidth = mFaceBitmap.getWidth();
    		mFaceBitmapHeight = mFaceBitmap.getHeight();
    		mViewWidth = this.getWidth();
    		mViewHeight = this.getHeight();
    		int[] location = new int[2];
    		this.getLocationOnScreen(location);
    		mViewLeft = location[0];
    		mViewTop = location[1];
    		
    		mFitInScale = fitView(mFaceBitmap, true);
    		mFitOutScale = fitView(mFaceBitmap, false);
    		
    		if(mNeedFitInImage){
    			mFaceScale = mFitInScale;
    		}
    		else{
    			mFaceScale = mFitOutScale;
    		}

        	mFaceRect.left = mViewWidth/2 - mFaceBitmapWidth/2 * mFaceScale;
        	mFaceRect.top = mViewHeight/2 - mFaceBitmapHeight/2 * mFaceScale;
        	mFaceRect.right = mFaceRect.left + mFaceBitmapWidth * mFaceScale;
        	mFaceRect.bottom = mFaceRect.top + mFaceBitmapHeight * mFaceScale;
            
//    		mState = STATE_INIT;
		}
		this.invalidate();
	}
}
