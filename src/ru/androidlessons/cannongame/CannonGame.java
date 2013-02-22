package ru.androidlessons.cannongame;

import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

public class CannonGame extends Activity {
	
	GestureDetector gestureDetector;
	CannonView cannonView;
	
	SimpleOnGestureListener gestureListener = new SimpleOnGestureListener() {
		public boolean onDoubleTap(MotionEvent e) {
			cannonView.fireCannonball(e);
			return true;
		};
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		cannonView = (CannonView)findViewById(R.id.cannonView);
		gestureDetector = new GestureDetector(this, gestureListener);
		
		setVolumeControlStream(AudioManager.STREAM_MUSIC);		
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		cannonView.stopGame();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		cannonView.releaseResources();
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		 
		if(action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
			cannonView.alignCannon(event);
		}
		
		return super.onTouchEvent(event);
	}
}
