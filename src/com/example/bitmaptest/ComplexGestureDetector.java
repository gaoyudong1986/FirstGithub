package com.example.bitmaptest;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

public class ComplexGestureDetector {
	@SuppressWarnings("unused")
	private static final String LOG_TAG = ComplexGestureDetector.class.getSimpleName();

	public interface OnGestureListener {

		boolean onDown(MotionEvent e);

		void onShowPress(MotionEvent e);

		boolean onSingleTapUp(MotionEvent e);

		boolean onScrollStart(MotionEvent e1, MotionEvent e2, float distanceX,
				float distanceY);
		
		boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
				float distanceY);

		void onScrollEnd(MotionEvent e1, MotionEvent e2, float distanceX,
				float distanceY);
		
		void onLongPress(MotionEvent e);

		boolean onLongPressEvent(MotionEvent e);

		boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY);
	}

	public interface OnDoubleTapListener {
		boolean onSingleTapConfirmed(MotionEvent e);

		boolean onDoubleTap(MotionEvent e);

		boolean onDoubleTapEvent(MotionEvent e);
	}

	public interface OnMultiTouchGestureListener {
		public boolean onMultiTouchMove(ComplexGestureDetector detector);

		public boolean onMultiTouchBegin(ComplexGestureDetector detector);

		public void onMultiTouchEnd(ComplexGestureDetector detector);
	}

	public static class SimpleOnGestureListener implements OnGestureListener,
			OnDoubleTapListener, OnMultiTouchGestureListener {
		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			return false;
		}

		@Override
		public void onLongPress(MotionEvent e) {
		}

		@Override
		public boolean onLongPressEvent(MotionEvent e) {
			return false;
		}

		@Override
		public boolean onScrollStart(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			return false;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			return false;
		}

		@Override
		public void onScrollEnd(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			
		}
		
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			return false;
		}

		@Override
		public void onShowPress(MotionEvent e) {
		}

		@Override
		public boolean onDown(MotionEvent e) {
			return false;
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			return false;
		}

		@Override
		public boolean onDoubleTapEvent(MotionEvent e) {
			return false;
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			return false;
		}

		@Override
		public boolean onMultiTouchMove(ComplexGestureDetector detector) {
			return false;
		}

		@Override
		public boolean onMultiTouchBegin(ComplexGestureDetector detector) {
			return false;
		}

		@Override
		public void onMultiTouchEnd(ComplexGestureDetector detector) {

		}
	}

	private int mBiggerTouchSlopSquare = 20 * 20;

	private int mTouchSlopSquare;
	private int mDoubleTapSlopSquare;
	private int mMinimumFlingVelocity;
	private int mMaximumFlingVelocity;

	private static final int LONGPRESS_TIMEOUT = ViewConfiguration
			.getLongPressTimeout();
	private static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();
	private static final int DOUBLE_TAP_TIMEOUT = ViewConfiguration
			.getDoubleTapTimeout();

	private static final int SHOW_PRESS = 1;
	private static final int LONG_PRESS = 2;
	private static final int TAP = 3;

	private final Handler mHandler;
	private final OnGestureListener mListener;
	private OnDoubleTapListener mDoubleTapListener;

	private boolean mStillDown;
	private boolean mInLongPress;
	private boolean mAlwaysInTapRegion;
	private boolean mInScroll;
	private boolean mAlwaysInBiggerTapRegion;

	private MotionEvent mCurrentDownEvent;
	private MotionEvent mPreviousUpEvent;

	private boolean mIsDoubleTapping;

	private float mLastMotionY;
	private float mLastMotionX;

	private boolean mIsLongpressEnabled;

	private boolean mIgnoreMultitouch;

	private VelocityTracker mVelocityTracker;

	// multi touch
	private static final float PRESSURE_THRESHOLD = 0.67f;

	private boolean mInMultiTouch;
	private final Context mContext;
	private OnMultiTouchGestureListener mMultiTouchGestureListener;
	private boolean mGestureInProgress;

	private MotionEvent mPrevEvent;
	private MotionEvent mCurrEvent;

	private float mFocusX;
	private float mFocusY;
	private float mPrevFingerDiffX;
	private float mPrevFingerDiffY;
	private float mCurrFingerDiffX;
	private float mCurrFingerDiffY;
	private float mCurrLen;
	private float mPrevLen;
	private double mCurrAngle;
	private double mPrevAngle;
	private Point mCurrPt0 = new Point();
	private Point mCurrPt1 = new Point();
	private float mScaleFactor;
	private float mCurrPressure;
	private float mPrevPressure;
	private long mTimeDelta;

	private float mEdgeSlop;
	private float mRightSlopEdge;
	private float mBottomSlopEdge;
	private boolean mSloppyGesture;
	private boolean mInvalidGesture;

	private int mActiveId0;
	private int mActiveId1;
	private boolean mActive0MostRecent;

	private class GestureHandler extends Handler {
		GestureHandler() {
			super();
		}

		GestureHandler(Handler handler) {
			super(handler.getLooper());
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SHOW_PRESS:
				mListener.onShowPress(mCurrentDownEvent);
				break;

			case LONG_PRESS:
				dispatchLongPress();
				break;

			case TAP:
				if (mDoubleTapListener != null && !mStillDown) {
					mDoubleTapListener.onSingleTapConfirmed(mCurrentDownEvent);
				}
				break;

			default:
				throw new RuntimeException("Unknown message " + msg); // never
			}
		}
	}

	public ComplexGestureDetector(Context context, OnGestureListener listener) {
		this(context, listener, null);
	}

	public ComplexGestureDetector(Context context, OnGestureListener listener,
			Handler handler) {
		this(
				context,
				listener,
				handler,
				context != null
						&& context.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.FROYO);
	}

	public ComplexGestureDetector(Context context, OnGestureListener listener,
			Handler handler, boolean ignoreMultitouch) {
		mContext = context;
		if (handler != null) {
			mHandler = new GestureHandler(handler);
		} else {
			mHandler = new GestureHandler();
		}
		mListener = listener;
		if (listener instanceof OnDoubleTapListener) {
			setOnDoubleTapListener((OnDoubleTapListener) listener);
		}
		if (listener instanceof OnMultiTouchGestureListener) {
			ignoreMultitouch = false;
			setOnMultiTouchGestureListener((OnMultiTouchGestureListener) listener);
		}
		init(context, ignoreMultitouch);
	}

	private void init(Context context, boolean ignoreMultitouch) {
		if (mListener == null) {
			throw new NullPointerException("OnGestureListener must not be null");
		}
		mIsLongpressEnabled = true;
		mIgnoreMultitouch = ignoreMultitouch;
		mInMultiTouch = false;

		int touchSlop, doubleTapSlop;
		if (context == null) {
			touchSlop = ViewConfiguration.getTouchSlop();
			doubleTapSlop = 100;// ViewConfiguration.getDoubleTapSlop();
			mMinimumFlingVelocity = ViewConfiguration.getMinimumFlingVelocity();
			mMaximumFlingVelocity = ViewConfiguration.getMaximumFlingVelocity();

			mEdgeSlop = ViewConfiguration.getEdgeSlop();
		} else {
			final ViewConfiguration configuration = ViewConfiguration
					.get(context);
			touchSlop = configuration.getScaledTouchSlop();
			doubleTapSlop = configuration.getScaledDoubleTapSlop();
			mMinimumFlingVelocity = configuration
					.getScaledMinimumFlingVelocity();
			mMaximumFlingVelocity = configuration
					.getScaledMaximumFlingVelocity();

			mEdgeSlop = configuration.getScaledEdgeSlop();
		}
		mTouchSlopSquare = touchSlop * touchSlop;
		mDoubleTapSlopSquare = doubleTapSlop * doubleTapSlop;
	}

	public void setOnDoubleTapListener(OnDoubleTapListener onDoubleTapListener) {
		mDoubleTapListener = onDoubleTapListener;
	}

	public void setOnMultiTouchGestureListener(
			OnMultiTouchGestureListener onMultiTouchGestureListener) {
		mMultiTouchGestureListener = onMultiTouchGestureListener;
	}

	public void setIsLongpressEnabled(boolean isLongpressEnabled) {
		mIsLongpressEnabled = isLongpressEnabled;
	}

	public boolean isLongpressEnabled() {
		return mIsLongpressEnabled;
	}

	public void setSupportMultiTouch(boolean supported) {
		mIgnoreMultitouch = !supported;
	}

	public boolean isSupportedMultiTouch() {
		return !mIgnoreMultitouch;
	}

	public boolean onTouchEvent(MotionEvent ev) {
		if (mInMultiTouch) {
			return precessMultiTouch(ev);
		}

		final int action = ev.getAction();
		final float y = ev.getY();
		final float x = ev.getX();

		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(ev);

		boolean handled = false;

		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_POINTER_DOWN:
			if (mIgnoreMultitouch) {
				cancel(ev);
			} else {
				cancel(ev);
				mInMultiTouch = true;
				return precessMultiTouch(ev);
			}
			break;

		case MotionEvent.ACTION_POINTER_UP:
			if (mIgnoreMultitouch && ev.getPointerCount() == 2) {
				int index = (((action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT) == 0) ? 1
						: 0;
				mLastMotionX = ev.getX(index);
				mLastMotionY = ev.getY(index);
				mVelocityTracker.recycle();
				mVelocityTracker = VelocityTracker.obtain();
			}
			break;

		case MotionEvent.ACTION_DOWN:
			resetMultiTouch();
			mActiveId0 = ev.getPointerId(0);
			mActive0MostRecent = true;

			if (mDoubleTapListener != null) {
				boolean hadTapMessage = mHandler.hasMessages(TAP);
				if (hadTapMessage)
					mHandler.removeMessages(TAP);
				if ((mCurrentDownEvent != null)
						&& (mPreviousUpEvent != null)
						&& hadTapMessage
						&& isConsideredDoubleTap(mCurrentDownEvent,
								mPreviousUpEvent, ev)) {

					mIsDoubleTapping = true;

					handled |= mDoubleTapListener
							.onDoubleTap(mCurrentDownEvent);
					handled |= mDoubleTapListener.onDoubleTapEvent(ev);
				} else {
					mHandler.sendEmptyMessageDelayed(TAP, DOUBLE_TAP_TIMEOUT);
				}
			}

			mLastMotionX = x;
			mLastMotionY = y;
			if (mCurrentDownEvent != null) {
				mCurrentDownEvent.recycle();
			}
			mCurrentDownEvent = MotionEvent.obtain(ev);
			mAlwaysInTapRegion = true;
			mAlwaysInBiggerTapRegion = true;
			mStillDown = true;
			mInLongPress = false;
			mInScroll = false;

			if (mIsLongpressEnabled) {
				mHandler.removeMessages(LONG_PRESS);
				mHandler.sendEmptyMessageAtTime(LONG_PRESS,
						mCurrentDownEvent.getDownTime() + TAP_TIMEOUT
								+ LONGPRESS_TIMEOUT);
			}
			mHandler.sendEmptyMessageAtTime(SHOW_PRESS,
					mCurrentDownEvent.getDownTime() + TAP_TIMEOUT);
			handled |= mListener.onDown(ev);
			break;

		case MotionEvent.ACTION_MOVE:
			if (mIgnoreMultitouch && ev.getPointerCount() > 1) {
				break;
			}

			final float scrollX = mLastMotionX - x;
			final float scrollY = mLastMotionY - y;
			if (mIsDoubleTapping) {
				handled |= mDoubleTapListener.onDoubleTapEvent(ev);
			} else if (mInLongPress) {
				handled |= mListener.onLongPressEvent(ev);
			} else if (mAlwaysInTapRegion) {
				final int deltaX = (int) (x - mCurrentDownEvent.getX());
				final int deltaY = (int) (y - mCurrentDownEvent.getY());
				int distance = (deltaX * deltaX) + (deltaY * deltaY);
				if (distance > mTouchSlopSquare) {
					handled = mListener.onScrollStart(mCurrentDownEvent, ev,
							scrollX, scrollY);
					mLastMotionX = x;
					mLastMotionY = y;
					mAlwaysInTapRegion = false;
					mInScroll = true;
					mHandler.removeMessages(TAP);
					mHandler.removeMessages(SHOW_PRESS);
					mHandler.removeMessages(LONG_PRESS);
				}
				if (distance > mBiggerTouchSlopSquare) {
					mAlwaysInBiggerTapRegion = false;
				}
			} else if ((Math.abs(scrollX) >= 1) || (Math.abs(scrollY) >= 1)) {
				handled = mListener.onScroll(mCurrentDownEvent, ev, scrollX,
						scrollY);
				mLastMotionX = x;
				mLastMotionY = y;
			}
			break;

		case MotionEvent.ACTION_UP:
			resetMultiTouch();

			mStillDown = false;
			MotionEvent currentUpEvent = MotionEvent.obtain(ev);
			if (mIsDoubleTapping) {
				handled |= mDoubleTapListener.onDoubleTapEvent(ev);
			} else if (mInLongPress) {
				handled |= mListener.onLongPressEvent(ev);
				mHandler.removeMessages(TAP);
				mInLongPress = false;
			} else if (mAlwaysInTapRegion) {
				handled = mListener.onSingleTapUp(ev);
			} else {

				final VelocityTracker velocityTracker = mVelocityTracker;
				velocityTracker.computeCurrentVelocity(1000,
						mMaximumFlingVelocity);
				final float velocityY = velocityTracker.getYVelocity();
				final float velocityX = velocityTracker.getXVelocity();

				if ((Math.abs(velocityY) > mMinimumFlingVelocity)
						|| (Math.abs(velocityX) > mMinimumFlingVelocity)) {
					handled = mListener.onFling(mCurrentDownEvent, ev,
							velocityX, velocityY);
				}
				
				if (mInScroll) {
					mListener.onScrollEnd(mCurrentDownEvent, ev, mLastMotionX - x,
							mLastMotionY - y);
				}
			}
			if (mPreviousUpEvent != null) {
				mPreviousUpEvent.recycle();
				mPreviousUpEvent = null;
			}

			if (mIsDoubleTapping) {
			} else {
				mPreviousUpEvent = currentUpEvent;
			}
			mVelocityTracker.recycle();
			mVelocityTracker = null;
			mIsDoubleTapping = false;
			mHandler.removeMessages(SHOW_PRESS);
			mHandler.removeMessages(LONG_PRESS);
			break;

		case MotionEvent.ACTION_CANCEL:
			cancel(ev);
			break;
		}

		return handled;
	}

	private boolean precessMultiTouch(MotionEvent event) {
		final int action = event.getActionMasked();

		boolean handled = true;
		if (mInvalidGesture) {
			handled = false;
		} else if (!mGestureInProgress) {
			switch (action) {
			case MotionEvent.ACTION_POINTER_DOWN: {
				DisplayMetrics metrics = mContext.getResources()
						.getDisplayMetrics();
				mRightSlopEdge = metrics.widthPixels - mEdgeSlop;
				mBottomSlopEdge = metrics.heightPixels - mEdgeSlop;

				if (mPrevEvent != null)
					mPrevEvent.recycle();
				mPrevEvent = MotionEvent.obtain(event);
				mTimeDelta = 0;

				int index1 = event.getActionIndex();
				int index0 = event.findPointerIndex(mActiveId0);
				mActiveId1 = event.getPointerId(index1);
				if (index0 < 0 || index0 == index1) {
					index0 = findNewActiveIndex(event, index0 == index1 ? -1
							: mActiveId1, index0);
					mActiveId0 = event.getPointerId(index0);
				}
				mActive0MostRecent = false;

				setContext(event);

				final float edgeSlop = mEdgeSlop;
				final float rightSlop = mRightSlopEdge;
				final float bottomSlop = mBottomSlopEdge;
				float x0 = getRawX(event, index0);
				float y0 = getRawY(event, index0);
				float x1 = getRawX(event, index1);
				float y1 = getRawY(event, index1);

				boolean p0sloppy = x0 < edgeSlop || y0 < edgeSlop
						|| x0 > rightSlop || y0 > bottomSlop;
				boolean p1sloppy = x1 < edgeSlop || y1 < edgeSlop
						|| x1 > rightSlop || y1 > bottomSlop;

				if (p0sloppy && p1sloppy) {
					mFocusX = -1;
					mFocusY = -1;
					mSloppyGesture = true;
				} else if (p0sloppy) {
					mFocusX = event.getX(index1);
					mFocusY = event.getY(index1);
					mSloppyGesture = true;
				} else if (p1sloppy) {
					mFocusX = event.getX(index0);
					mFocusY = event.getY(index0);
					mSloppyGesture = true;
				} else {
					mSloppyGesture = false;
					if (mMultiTouchGestureListener != null) {
						mGestureInProgress = mMultiTouchGestureListener
								.onMultiTouchBegin(this);
					}
				}
			}
				break;

			case MotionEvent.ACTION_MOVE:
				if (mSloppyGesture) {
					final float edgeSlop = mEdgeSlop;
					final float rightSlop = mRightSlopEdge;
					final float bottomSlop = mBottomSlopEdge;
					int index0 = event.findPointerIndex(mActiveId0);
					int index1 = event.findPointerIndex(mActiveId1);

					float x0 = getRawX(event, index0);
					float y0 = getRawY(event, index0);
					float x1 = getRawX(event, index1);
					float y1 = getRawY(event, index1);

					boolean p0sloppy = x0 < edgeSlop || y0 < edgeSlop
							|| x0 > rightSlop || y0 > bottomSlop;
					boolean p1sloppy = x1 < edgeSlop || y1 < edgeSlop
							|| x1 > rightSlop || y1 > bottomSlop;

					if (p0sloppy) {
						int index = findNewActiveIndex(event, mActiveId1,
								index0);
						if (index >= 0) {
							index0 = index;
							mActiveId0 = event.getPointerId(index);
							x0 = getRawX(event, index);
							y0 = getRawY(event, index);
							p0sloppy = false;
						}
					}

					if (p1sloppy) {
						int index = findNewActiveIndex(event, mActiveId0,
								index1);
						if (index >= 0) {
							index1 = index;
							mActiveId1 = event.getPointerId(index);
							x1 = getRawX(event, index);
							y1 = getRawY(event, index);
							p1sloppy = false;
						}
					}

					if (p0sloppy && p1sloppy) {
						mFocusX = -1;
						mFocusY = -1;
					} else if (p0sloppy) {
						mFocusX = event.getX(index1);
						mFocusY = event.getY(index1);
					} else if (p1sloppy) {
						mFocusX = event.getX(index0);
						mFocusY = event.getY(index0);
					} else {
						mSloppyGesture = false;
						if (mMultiTouchGestureListener != null) {
							mGestureInProgress = mMultiTouchGestureListener.onMultiTouchBegin(this);
						}
					}
				}
				break;

			case MotionEvent.ACTION_POINTER_UP:
				if (mSloppyGesture) {
					final int pointerCount = event.getPointerCount();
					final int actionIndex = event.getActionIndex();
					final int actionId = event.getPointerId(actionIndex);

					if (pointerCount > 2) {
						if (actionId == mActiveId0) {
							final int newIndex = findNewActiveIndex(event,
									mActiveId1, actionIndex);
							if (newIndex >= 0)
								mActiveId0 = event.getPointerId(newIndex);
						} else if (actionId == mActiveId1) {
							final int newIndex = findNewActiveIndex(event,
									mActiveId0, actionIndex);
							if (newIndex >= 0)
								mActiveId1 = event.getPointerId(newIndex);
						}
					} else {
						// Set focus point to the remaining finger
						final int index = event
								.findPointerIndex(actionId == mActiveId0 ? mActiveId1
										: mActiveId0);
						if (index < 0) {
							mInvalidGesture = true;
							
							if (mGestureInProgress && mMultiTouchGestureListener != null) {
								mMultiTouchGestureListener.onMultiTouchEnd(this);
							}
							return false;
						}

						exitMultiTouch(event);
					}
				} else {
					if (event.getPointerCount() == 2) {
						exitMultiTouch(event);
					}
				}
				break;
			}
		} else {
			switch (action) {
			case MotionEvent.ACTION_POINTER_DOWN: {
				if (mMultiTouchGestureListener != null) {
					mMultiTouchGestureListener.onMultiTouchEnd(this);
				}
				
				final int oldActive0 = mActiveId0;
				final int oldActive1 = mActiveId1;
				
				resetMultiTouch();

				mPrevEvent = MotionEvent.obtain(event);
				mActiveId0 = mActive0MostRecent ? oldActive0 : oldActive1;
				mActiveId1 = event.getPointerId(event.getActionIndex());
				mActive0MostRecent = false;

				int index0 = event.findPointerIndex(mActiveId0);
				if (index0 < 0 || mActiveId0 == mActiveId1) {
					index0 = findNewActiveIndex(event,
							mActiveId0 == mActiveId1 ? -1 : mActiveId1, index0);
					mActiveId0 = event.getPointerId(index0);
				}

				setContext(event);

				if (mMultiTouchGestureListener != null) {
					mGestureInProgress = mMultiTouchGestureListener.onMultiTouchBegin(this);
				}
			}
				break;

			case MotionEvent.ACTION_POINTER_UP: {
				final int pointerCount = event.getPointerCount();
				final int actionIndex = event.getActionIndex();
				final int actionId = event.getPointerId(actionIndex);

				boolean gestureEnded = false;
				if (pointerCount > 2) {
					if (actionId == mActiveId0) {
						final int newIndex = findNewActiveIndex(event,
								mActiveId1, actionIndex);
						if (newIndex >= 0) {
							if (mMultiTouchGestureListener != null) {
								mMultiTouchGestureListener.onMultiTouchEnd(this);
							}
							
							mActiveId0 = event.getPointerId(newIndex);
							mActive0MostRecent = true;
							mPrevEvent = MotionEvent.obtain(event);
							setContext(event);
							
							if (mMultiTouchGestureListener != null) {
								mGestureInProgress = mMultiTouchGestureListener.onMultiTouchBegin(this);
							}
						} else {
							gestureEnded = true;
						}
					} else if (actionId == mActiveId1) {
						final int newIndex = findNewActiveIndex(event,
								mActiveId0, actionIndex);
						if (newIndex >= 0) {
							if (mMultiTouchGestureListener != null) {
								mMultiTouchGestureListener.onMultiTouchEnd(this);
							}
							
							mActiveId1 = event.getPointerId(newIndex);
							mActive0MostRecent = false;
							mPrevEvent = MotionEvent.obtain(event);
							setContext(event);
							
							if (mMultiTouchGestureListener != null) {
								mGestureInProgress = mMultiTouchGestureListener.onMultiTouchBegin(this);
							}
						} else {
							gestureEnded = true;
						}
					}
					mPrevEvent.recycle();
					mPrevEvent = MotionEvent.obtain(event);
					setContext(event);
				} else {
					gestureEnded = true;
				}

				if (gestureEnded) {
					setContext(event);

					if (mMultiTouchGestureListener != null) {
						mMultiTouchGestureListener.onMultiTouchEnd(this);
					}
					
					// Set focus point to the remaining finger
					final int activeId = actionId == mActiveId0 ? mActiveId1
							: mActiveId0;
					final int index = event.findPointerIndex(activeId);
					mFocusX = event.getX(index);
					mFocusY = event.getY(index);

					exitMultiTouch(event);
				}
			}
				break;

			case MotionEvent.ACTION_CANCEL:
				if (mMultiTouchGestureListener != null) {
					mMultiTouchGestureListener.onMultiTouchEnd(this);
				}
				
				mInMultiTouch = false;
				resetMultiTouch();
				break;

			case MotionEvent.ACTION_MOVE: {
				setContext(event);

				if (mCurrPressure / mPrevPressure > PRESSURE_THRESHOLD) {
					if (mMultiTouchGestureListener != null) {
						final boolean updatePrevious = mMultiTouchGestureListener.onMultiTouchMove(this);
	
						if (updatePrevious) {
							mPrevEvent.recycle();
							mPrevEvent = MotionEvent.obtain(event);
						}
					}
				}
			}
				break;
			}
		}

		return handled;
	}

	private void exitMultiTouch(MotionEvent ev) {
		final int action = ev.getAction();
		int index = (((action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT) == 0) ? 1
				: 0;
		mLastMotionX = ev.getX(index);
		mLastMotionY = ev.getY(index);
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		} else{
			mVelocityTracker.recycle();
		}
		mVelocityTracker = VelocityTracker.obtain();
		
		resetMultiTouch();
		mActiveId0 = ev.getPointerId(index);
		mActive0MostRecent = true;
		
		mFocusX = ev.getX(index);
		mFocusY = ev.getY(index);
		
		mInMultiTouch = false;
	}
	
	private void cancel(MotionEvent ev) {
		// temporary
		if (mIsDoubleTapping) {
			mDoubleTapListener.onDoubleTapEvent(ev);
		} else if (mInLongPress) {
			MotionEvent currentEvent = MotionEvent.obtain(ev);
			currentEvent.setAction(MotionEvent.ACTION_CANCEL);
			mListener.onLongPressEvent(currentEvent);
			currentEvent.recycle();
		} else if (mAlwaysInTapRegion) {
			mListener.onSingleTapUp(ev);
		} else {
			if (mInScroll) {
				mListener.onScrollEnd(mCurrentDownEvent, ev, 0, 0);
			}
		}

		if (mPreviousUpEvent != null) {
			mPreviousUpEvent.recycle();
			mPreviousUpEvent = null;
		}
		
		mHandler.removeMessages(SHOW_PRESS);
		mHandler.removeMessages(LONG_PRESS);
		mHandler.removeMessages(TAP);
		mVelocityTracker.recycle();
		mVelocityTracker = null;
		mIsDoubleTapping = false;
		mStillDown = false;
		mAlwaysInTapRegion = false;
		mAlwaysInBiggerTapRegion = false;
		if (mInLongPress) {
			mInLongPress = false;
		}
	}

	private boolean isConsideredDoubleTap(MotionEvent firstDown,
			MotionEvent firstUp, MotionEvent secondDown) {
		if (!mAlwaysInBiggerTapRegion) {
			return false;
		}

		if (secondDown.getEventTime() - firstUp.getEventTime() > DOUBLE_TAP_TIMEOUT) {
			return false;
		}

		int deltaX = (int) firstDown.getX() - (int) secondDown.getX();
		int deltaY = (int) firstDown.getY() - (int) secondDown.getY();
		return (deltaX * deltaX + deltaY * deltaY < mDoubleTapSlopSquare);
	}

	private void dispatchLongPress() {
		mHandler.removeMessages(TAP);
		
		if (mPreviousUpEvent != null) {
			mPreviousUpEvent.recycle();
			mPreviousUpEvent = null;
		}
		mIsDoubleTapping = false;
		
		mInLongPress = true;
		mListener.onLongPress(mCurrentDownEvent);
		mListener.onLongPressEvent(mCurrentDownEvent);
	}

	private int findNewActiveIndex(MotionEvent ev, int otherActiveId,
			int oldIndex) {
		final int pointerCount = ev.getPointerCount();

		final int otherActiveIndex = ev.findPointerIndex(otherActiveId);
		int newActiveIndex = -1;

		for (int i = 0; i < pointerCount; i++) {
			if (i != oldIndex && i != otherActiveIndex) {
				final float edgeSlop = mEdgeSlop;
				final float rightSlop = mRightSlopEdge;
				final float bottomSlop = mBottomSlopEdge;
				float x = getRawX(ev, i);
				float y = getRawY(ev, i);
				if (x >= edgeSlop && y >= edgeSlop && x <= rightSlop
						&& y <= bottomSlop) {
					newActiveIndex = i;
					break;
				}
			}
		}

		return newActiveIndex;
	}

	private static float getRawX(MotionEvent event, int pointerIndex) {
		if (pointerIndex < 0)
			return Float.MIN_VALUE;
		if (pointerIndex == 0)
			return event.getRawX();
		float offset = event.getRawX() - event.getX();
		return event.getX(pointerIndex) + offset;
	}

	private static float getRawY(MotionEvent event, int pointerIndex) {
		if (pointerIndex < 0)
			return Float.MIN_VALUE;
		if (pointerIndex == 0)
			return event.getRawY();
		float offset = event.getRawY() - event.getY();
		return event.getY(pointerIndex) + offset;
	}

	private void setContext(MotionEvent curr) {
		if (mCurrEvent != null) {
			mCurrEvent.recycle();
		}
		mCurrEvent = MotionEvent.obtain(curr);

		mCurrLen = -1;
		mPrevLen = -1;
		mScaleFactor = -1;
		mCurrAngle = 0.0;
		mPrevAngle = 0.0;
		mCurrPt0.set(-1, -1);
		mCurrPt1.set(-1, -1);
		
		final MotionEvent prev = mPrevEvent;

		final int prevIndex0 = prev.findPointerIndex(mActiveId0);
		final int prevIndex1 = prev.findPointerIndex(mActiveId1);
		final int currIndex0 = curr.findPointerIndex(mActiveId0);
		final int currIndex1 = curr.findPointerIndex(mActiveId1);

		if (prevIndex0 < 0 || prevIndex1 < 0 || currIndex0 < 0
				|| currIndex1 < 0) {
			mInvalidGesture = true;
			if (mGestureInProgress && mMultiTouchGestureListener != null) {
				mMultiTouchGestureListener.onMultiTouchEnd(this);
			}
			return;
		}

		final float px0 = prev.getX(prevIndex0);
		final float py0 = prev.getY(prevIndex0);
		final float px1 = prev.getX(prevIndex1);
		final float py1 = prev.getY(prevIndex1);
		final float cx0 = curr.getX(currIndex0);
		final float cy0 = curr.getY(currIndex0);
		final float cx1 = curr.getX(currIndex1);
		final float cy1 = curr.getY(currIndex1);

		final float pvx = px1 - px0;
		final float pvy = py1 - py0;
		final float cvx = cx1 - cx0;
		final float cvy = cy1 - cy0;
		mPrevFingerDiffX = pvx;
		mPrevFingerDiffY = pvy;
		mCurrFingerDiffX = cvx;
		mCurrFingerDiffY = cvy;

		mFocusX = cx0 + cvx * 0.5f;
		mFocusY = cy0 + cvy * 0.5f;
		mTimeDelta = curr.getEventTime() - prev.getEventTime();
		mCurrPressure = curr.getPressure(currIndex0)
				+ curr.getPressure(currIndex1);
		mPrevPressure = prev.getPressure(prevIndex0)
				+ prev.getPressure(prevIndex1);
		
		mCurrAngle = getAngle(px0, py0, px1, py1);
		mPrevAngle = getAngle(cx0, cy0, cx1, cy1);
		
		mCurrPt0.set((int)getRawX(curr, currIndex0), (int)getRawY(curr, currIndex0));
		mCurrPt1.set((int)getRawX(curr, currIndex1), (int)getRawY(curr, currIndex1));
	}

	private double getAngle(float x0, float y0, float x1, float y1) {
		float disX = x1 - x0;
		float disY = y1 - y0;
		
		if (disX == 0 && disY == 0) {
			return 0.0;
		} else if (disY == 0) {
			if (x1 > x0)
				return 0.0;
			else
				return 180.0;
		} else {
			double d = -disY/disX;
			double angle = Math.atan(d)*180/Math.PI;
			if (angle >= 0) {
				if (x1 > x0)
					return angle;
				else
					return (angle + 180);
			} else {
				if (x1 > x0)
					return (angle + 360);
				else
					return (angle + 180);
			}
		}
	}
	
	private void resetMultiTouch() {
		if (mPrevEvent != null) {
			mPrevEvent.recycle();
			mPrevEvent = null;
		}
		if (mCurrEvent != null) {
			mCurrEvent.recycle();
			mCurrEvent = null;
		}
		mSloppyGesture = false;
		mGestureInProgress = false;
		mActiveId0 = -1;
		mActiveId1 = -1;
		mInvalidGesture = false;
	}

	public boolean isInProgress() {
		return mGestureInProgress;
	}

	public float getFocusX() {
		return mFocusX;
	}

	public float getFocusY() {
		return mFocusY;
	}

	public float getCurrentSpan() {
		if (mCurrLen == -1) {
			final float cvx = mCurrFingerDiffX;
			final float cvy = mCurrFingerDiffY;
			mCurrLen = FloatMath.sqrt(cvx * cvx + cvy * cvy);
		}
		return mCurrLen;
	}

	public float getCurrentSpanX() {
		return mCurrFingerDiffX;
	}

	public float getCurrentSpanY() {
		return mCurrFingerDiffY;
	}

	public float getPreviousSpan() {
		if (mPrevLen == -1) {
			final float pvx = mPrevFingerDiffX;
			final float pvy = mPrevFingerDiffY;
			mPrevLen = FloatMath.sqrt(pvx * pvx + pvy * pvy);
		}
		return mPrevLen;
	}

	public float getPreviousSpanX() {
		return mPrevFingerDiffX;
	}

	public float getPreviousSpanY() {
		return mPrevFingerDiffY;
	}

	public float getScaleFactor() {
		if (mScaleFactor == -1) {
			mScaleFactor = getCurrentSpan() / getPreviousSpan();
		}
		return mScaleFactor;
	}

	public long getTimeDelta() {
		return mTimeDelta;
	}

	public long getEventTime() {
		return mCurrEvent.getEventTime();
	}
	
	public double getCurrentAngle() {
		return mCurrAngle;
	}
	
	public double getPreviousAngle() {
		return mPrevAngle;
	}
	
	public double getRotateAngle() {
		double rotate = mPrevAngle - mCurrAngle;
		if (rotate > 270)
			return (rotate - 360);
		else if (rotate < -270)
			return (rotate + 360);
		else
			return rotate;
	}
	
	public Point getCurrentPoint0() {
		return mCurrPt0;
	}
	
	public Point getCurrentPoint1() {
		return mCurrPt1;
	}
}
