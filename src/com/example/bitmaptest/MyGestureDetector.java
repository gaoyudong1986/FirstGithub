package com.example.bitmaptest;

import com.example.bitmaptest.ComplexGestureDetector.SimpleOnGestureListener;

import android.graphics.Point;
import android.view.MotionEvent;

public class MyGestureDetector extends SimpleOnGestureListener {
	
	private FaceView mFaceView;
	
	public MyGestureDetector(FaceView workSpace){
		mFaceView = workSpace;
	}
	
	@Override
	public void onShowPress(MotionEvent e) {
		mFaceView.onShowPress(e.getRawX(), e.getRawY());
	}
	
	@Override
	public boolean onLongPressEvent(MotionEvent e) {
		int type = 2;
		int act = e.getAction();
		if (act == MotionEvent.ACTION_DOWN)
			type = 0;
		else if (act == MotionEvent.ACTION_MOVE)
			type = 1;
		return mFaceView.onDrag(type, e.getRawX(), e.getRawY());
	}

	@Override
	public boolean onScrollStart(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		return mFaceView.onScrollStart(e1.getRawX(), e1.getRawY(), e2.getRawX(), e2.getRawY(), distanceX, distanceY);
	}
	
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		return mFaceView.onScroll(e1.getRawX(), e1.getRawY(), e2.getRawX(), e2.getRawY(), distanceX, distanceY);
	}

	@Override
	public void onScrollEnd(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		mFaceView.onScrollEnd(e1.getRawX(), e1.getRawY(), e2.getRawX(), e2.getRawY(), distanceX, distanceY);
	}
	
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		return mFaceView.onFling(velocityX, velocityY);
	}
	
	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		return mFaceView.onClick(e.getRawX(), e.getRawY());
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return mFaceView.onClickUp(e.getRawX(), e.getRawY());
	}
	
	@Override
	public boolean onDoubleTap(MotionEvent e) {
		return mFaceView.onDoubleClick(e.getRawX(), e.getRawY());
	}
	
	@Override
	public boolean onMultiTouchMove(ComplexGestureDetector detector) {
		Point pt0 = detector.getCurrentPoint0();
		Point pt1 = detector.getCurrentPoint1();
		return mFaceView.onMultiTouchMove(pt0.x, pt0.y, pt1.x, pt1.y, detector.getScaleFactor(), detector.getRotateAngle());
	}

	@Override
	public boolean onMultiTouchBegin(ComplexGestureDetector detector) {
		Point pt0 = detector.getCurrentPoint0();
		Point pt1 = detector.getCurrentPoint1();
		return mFaceView.onMultiTouchBegin(pt0.x, pt0.y, pt1.x, pt1.y);
	}

	@Override
	public void onMultiTouchEnd(ComplexGestureDetector detector) {
		Point pt0 = detector.getCurrentPoint0();
		Point pt1 = detector.getCurrentPoint1();
		mFaceView.onMultiTouchEnd(pt0.x, pt0.y, pt1.x, pt1.y);
	}
}
