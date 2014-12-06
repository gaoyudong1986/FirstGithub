package com.example.bitmaptest;

import android.os.Bundle;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;

public class MainActivity extends Activity {
	
	private FaceView mFaceView;
	private ComplexGestureDetector mComplexGestureDetector;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mFaceView = (FaceView) findViewById(R.id.faceview);
		
		Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.sample1);
		
		mFaceView.setFaceBitmap(bitmap);
		
		MyGestureDetector detector = new MyGestureDetector(mFaceView);
		mComplexGestureDetector = new ComplexGestureDetector(this, detector);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mComplexGestureDetector.onTouchEvent(event);

		return true;
	}

}
