package ru.androidlessons.cannongame;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CannonView extends SurfaceView implements SurfaceHolder.Callback {
	private CannonThread cannonThread; // �������� ����� ����
	private Activity activity; // ����������� ����������� ����
	// Game Over � ������ GUI
	private boolean dialogIsDisplayed = false;

	// ���������, ������������ � ����
	public static final int TARGET_PIECES = 7; // ������ ������
	public static final int MISS_PENALTY = 2; // �������, ����������
	// � ������ �������
	public static final int HIT_REWARD = 3; // �������, �����������
	// ��� ���������

	// ����������, ������������ ��� ����� ���� � ������������ ����������
	private boolean gameOver; // ���� ���������?
	private double timeLeft; // ���������� ����� � ��������
	private int shotsFired; // ���������� ��������� ������������
	private double totalTimeElapsed; // ���������� ��������� ������

	// ����������, ������������ ��� ����������� ����� � ����
	private Line blocker; // ��������� � �������� ����� �����
	private int blockerDistance; // ���������� �� ����� �����
	private int blockerBeginning; // ���������� �� ����� ������
	private int blockerEnd; // ���������� �� ������� ���� ����� ������
	private int initialBlockerVelocity; // ��������� ���������
	// �������� �����
	private float blockerVelocity; // ��������� �������� �����
	// �� ����� ����

	private Line target; // ��������� � �������� ����� ������
	private int targetDistance; // ��������� �� ������ �����
	private int targetBeginning; // ��������� �� ������ ������
	private double pieceLength; // ����� ������ ������
	private int targetEnd; // ��������� ������ �� ������� ���� ������
	private int initialTargetVelocity; // ��������� ��������� ��������
	// ������
	private float targetVelocity; // ��������� �������� ������
	// �� ����� ����

	private int lineWidth; // ������ ������ � �����
	private boolean[] hitStates; // ��������� �� ��� ������ ������?
	private int targetPiecesHit; // ���������� ���������� ������
	// ������ (�� 7)

	// ���������� ��� ����� � ��������� ����
	private Point cannonball; // ����������� ���� � ����� ������� ����
	private int cannonballVelocityX; // �������� ���� �� �����������
	private int cannonballVelocityY; // �������� ���� �� ���������
	private boolean cannonballOnScreen; // ���� �� ������
	private int cannonballRadius; // ������ ����
	private int cannonballSpeed; // �������� ����
	private int cannonBaseRadius; // ������ ��������� �����
	private int cannonLength; // ����� ������ �����
	private Point barrelEnd; // ����� ������ �����
	private int screenWidth; // ������ ������
	private int screenHeight; // ������ ������

	// ��������� � ����������, ������������ ��� ���������� ������
	private static final int TARGET_SOUND_ID = 0;
	private static final int CANNON_SOUND_ID = 1;
	private static final int BLOCKER_SOUND_ID = 2;
	private SoundPool soundPool; // �������� �������
	private SparseIntArray soundMap; // ����������� ID
	// �� SoundPool

	// ���������� Paint, ������������ ��� ��������� �� ������
	private Paint textPaint; // Paint, ������������ ��� ��������� ������
	private Paint cannonballPaint; // Paint, ������������
	// ��� ��������� ����
	private Paint cannonPaint; // Paint, ������������
	// ��� ��������� �����
	private Paint blockerPaint; // Paint, ������������
	// ��� ��������� �����
	private Paint targetPaint; // Paint, ������������
	// ��� ��������� ������
	private Paint backgroundPaint; // Paint, ������������

	// ��� ������ ������� ���������

	public CannonView(Context context, AttributeSet attrs) {
		super(context, attrs);

		activity = (Activity) context;

		getHolder().addCallback(this);

		blocker = new Line();
		target = new Line();
		cannonball = new Point();

		hitStates = new boolean[TARGET_PIECES];

		soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);

		soundMap = new SparseIntArray();
		soundMap.put(TARGET_SOUND_ID,
				soundPool.load(context, R.raw.target_hit, 1));
		soundMap.put(CANNON_SOUND_ID,
				soundPool.load(context, R.raw.cannon_fire, 1));
		soundMap.put(BLOCKER_SOUND_ID,
				soundPool.load(context, R.raw.blocker_hit, 1));

		textPaint = new Paint();
		cannonPaint = new Paint();
		cannonballPaint = new Paint();
		blockerPaint = new Paint();
		targetPaint = new Paint();
		backgroundPaint = new Paint();

	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		screenWidth = w; // �������� �������� ������
		screenHeight = h; // �������� �������� ������
		cannonBaseRadius = h / 18; // ������ ��������� �����, ������
		// 1/18 �� ������ ������
		cannonLength = w / 8; // ����� ����� ����� 1/8 �� ������ ������

		cannonballRadius = w / 36; // ������ ���� ����� 1/36 ������ ������
		cannonballSpeed = w * 3 / 2; // ��������� �������� ����

		lineWidth = w / 24; // ������ ������ � ����� ����� 1/24
		// �� ������ ������

		// ���������������� ���������� ����������, ��������� � ������
		blockerDistance = w * 5 / 8; // ����� ����� �� ����� �����
		// 5/8 ������ ������
		blockerBeginning = h / 8; // ����� ������ �� ����� �����
		// 1/8 �� ������ ������
		blockerEnd = h * 3 / 8; // ����� ������ �� ����� ����� 3/8
		// �� ������ ������
		initialBlockerVelocity = h / 2; // ��������� ���������
		// �������� �����
		blocker.start = new Point(blockerDistance, blockerBeginning);
		blocker.end = new Point(blockerDistance, blockerEnd);

		// ���������������� ���������� ����������, ��������� � �������
		targetDistance = w * 7 / 8; // ����� ����� �� ������ �����
		// 7/8 �� ������ ������
		targetBeginning = h / 8; // ����� ������ �� ������ �����
		// 1/8 �� ������ ������
		targetEnd = h * 7 / 8; // ����� ������ ����� 7/8 �� ������ ������
		pieceLength = (targetEnd - targetBeginning) / TARGET_PIECES;
		initialTargetVelocity = -h / 4; // ��������� ���������
		// �������� ������
		target.start = new Point(targetDistance, targetBeginning);
		target.end = new Point(targetDistance, targetEnd);

		// �������� ����� ������ ����� (���������� ��������� �� �����������)
		barrelEnd = new Point(cannonLength, h / 2);

		// ���������������� �������� Paint ��� ��������� ��������� ����
		textPaint.setTextSize(w / 20); // ������ ������ ����� 1/20
		// �� ������ ������
		textPaint.setAntiAlias(true); // ����������� ������
		cannonPaint.setStrokeWidth(lineWidth * 1.5f); // ���������
		// ������� �����
		blockerPaint.setStrokeWidth(lineWidth); // ��������� �������
		// �����
		targetPaint.setStrokeWidth(lineWidth); // ��������� ������� �����
		backgroundPaint.setColor(Color.WHITE); // ��������� �������� �����

		newGame(); // �������������� ��������� � ������ ����� ����
	} // end method onSizeChanged

	public void newGame() {
		for(int i=0; i < TARGET_PIECES; ++i) {			
			hitStates[i] = false;			
		}
		
		targetPiecesHit = 0;
		
		blockerVelocity = initialBlockerVelocity;		
		targetVelocity = initialTargetVelocity;
		
		timeLeft = 10;
		
		cannonballOnScreen = false;
		
		shotsFired = 0;
		totalTimeElapsed = 0.0;
		
		blocker.start.set(blockerDistance, blockerBeginning);
		blocker.end.set(blockerDistance, blockerEnd);
		
		target.start.set(targetDistance, blockerBeginning);
		target.end.set(targetDistance, blockerEnd);
		
		if(gameOver) {
			gameOver = false;
			cannonThread = new CannonThread(getHolder());
			cannonThread.start();
		}
		
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
	}

	private class CannonThread extends Thread {
		public CannonThread(SurfaceHolder holder) {
			
		}
		@Override
		public void run() {
			super.run();
		}
	}

}