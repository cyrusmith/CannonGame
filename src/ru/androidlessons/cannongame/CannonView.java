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
	private CannonThread cannonThread; // контроль цикла игры
	private Activity activity; // отображение диалогового окна
	// Game Over в потоке GUI
	private boolean dialogIsDisplayed = false;

	// константы, используемые в игре
	public static final int TARGET_PIECES = 7; // секции мишени
	public static final int MISS_PENALTY = 2; // секунды, вычитаемые
	// в случае промаха
	public static final int HIT_REWARD = 3; // секунды, добавляемые
	// при попадании

	// переменные, используемые для цикла игры и отслеживания статистики
	private boolean gameOver; // игра завершена?
	private double timeLeft; // оставшееся время в секундах
	private int shotsFired; // количество выстрелов пользователя
	private double totalTimeElapsed; // количество прошедших секунд

	// переменные, используемые при определении блока и цели
	private Line blocker; // начальная и конечная точки блока
	private int blockerDistance; // расстояние от блока слева
	private int blockerBeginning; // расстояние от блока сверху
	private int blockerEnd; // расстояние от нижнего края блока сверху
	private int initialBlockerVelocity; // множитель начальной
	// скорости блока
	private float blockerVelocity; // множитель скорости блока
	// во время игры

	private Line target; // начальная и конечная точки мишени
	private int targetDistance; // дистанция до мишени слева
	private int targetBeginning; // дистанция до мишени сверху
	private double pieceLength; // длина секции мишени
	private int targetEnd; // дистанция сверху до нижнего края мишени
	private int initialTargetVelocity; // множитель начальной скорости
	// мишени
	private float targetVelocity; // множитель скорости мишени
	// во время игры

	private int lineWidth; // ширина мишени и блока
	private boolean[] hitStates; // попадание во все секции мишени?
	private int targetPiecesHit; // количество пораженных секций
	// мишени (из 7)

	// переменные для пушки и пушечного ядра
	private Point cannonball; // изображение ядра в левом верхнем углу
	private int cannonballVelocityX; // скорость ядра по горизонтали
	private int cannonballVelocityY; // скорость ядра по вертикали
	private boolean cannonballOnScreen; // ядро на экране
	private int cannonballRadius; // радиус ядра
	private int cannonballSpeed; // скорость ядра
	private int cannonBaseRadius; // радиус основания пушки
	private int cannonLength; // длина ствола пушки
	private Point barrelEnd; // конец ствола пушки
	private int screenWidth; // ширина экрана
	private int screenHeight; // высота экрана

	// константы и переменные, используемые для управления звуком
	private static final int TARGET_SOUND_ID = 0;
	private static final int CANNON_SOUND_ID = 1;
	private static final int BLOCKER_SOUND_ID = 2;
	private SoundPool soundPool; // звуковые эффекты
	private SparseIntArray soundMap; // отображение ID
	// на SoundPool

	// переменные Paint, используемые для рисования на экране
	private Paint textPaint; // Paint, используемая для рисования текста
	private Paint cannonballPaint; // Paint, используемая
	// для рисования ядра
	private Paint cannonPaint; // Paint, используемая
	// для рисования пушки
	private Paint blockerPaint; // Paint, используемая
	// для рисования блока
	private Paint targetPaint; // Paint, используемая
	// для рисования мишени
	private Paint backgroundPaint; // Paint, используемая

	// для чистки области рисования

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

		screenWidth = w; // хранение значения ширины
		screenHeight = h; // хранение значения высоты
		cannonBaseRadius = h / 18; // радиус основания пушки, равный
		// 1/18 от высоты экрана
		cannonLength = w / 8; // длина пушки равна 1/8 от ширины экрана

		cannonballRadius = w / 36; // радиус ядра равен 1/36 ширины экрана
		cannonballSpeed = w * 3 / 2; // множитель скорости ядра

		lineWidth = w / 24; // ширина мишени и блока равны 1/24
		// от ширины экрана

		// конфигурирование переменных экземпляра, связанных с блоком
		blockerDistance = w * 5 / 8; // зазор слева от блока равен
		// 5/8 ширины экрана
		blockerBeginning = h / 8; // зазор сверху от блока равен
		// 1/8 от высоты экрана
		blockerEnd = h * 3 / 8; // зазор сверху от блока равен 3/8
		// от высоты экрана
		initialBlockerVelocity = h / 2; // начальный множитель
		// скорости блока
		blocker.start = new Point(blockerDistance, blockerBeginning);
		blocker.end = new Point(blockerDistance, blockerEnd);

		// конфигурирование переменных экземпляра, связанных с мишенью
		targetDistance = w * 7 / 8; // зазор слева от мишень равен
		// 7/8 от ширины экрана
		targetBeginning = h / 8; // зазор сверху от мишени равен
		// 1/8 от высоты экрана
		targetEnd = h * 7 / 8; // зазор сверху равен 7/8 от высоты экрана
		pieceLength = (targetEnd - targetBeginning) / TARGET_PIECES;
		initialTargetVelocity = -h / 4; // множитель начальной
		// скорости мишени
		target.start = new Point(targetDistance, targetBeginning);
		target.end = new Point(targetDistance, targetEnd);

		// конечная точка ствола пушки (изначально направлен по горизонтали)
		barrelEnd = new Point(cannonLength, h / 2);

		// конфигурирование объектов Paint для рисования элементов игры
		textPaint.setTextSize(w / 20); // размер текста равен 1/20
		// от ширины экрана
		textPaint.setAntiAlias(true); // сглаживание текста
		cannonPaint.setStrokeWidth(lineWidth * 1.5f); // настройка
		// толщины линии
		blockerPaint.setStrokeWidth(lineWidth); // настройка толщины
		// линии
		targetPaint.setStrokeWidth(lineWidth); // настройка толщины линии
		backgroundPaint.setColor(Color.WHITE); // настройка фонового цвета

		newGame(); // первоначальная настройка и запуск новой игры
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