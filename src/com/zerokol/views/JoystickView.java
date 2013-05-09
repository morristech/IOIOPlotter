package com.zerokol.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class JoystickView extends View implements Runnable {
	// Constants
	public final static long DEFAULT_LOOP_INTERVAL = 100; // 100 ms
	public final static int FRONT = 3;
	public final static int FRONT_RIGHT = 4;
	public final static int RIGHT = 5;
	public final static int RIGHT_BOTTOM = 6;
	public final static int BOTTOM = 7;
	public final static int BOTTOM_LEFT = 8;
	public final static int LEFT = 1;
	public final static int LEFT_FRONT = 2;
	// Variables
	private OnJoystickMoveListener onJoystickMoveListener; // Listener
	private Thread thread = new Thread(this);
	private long loopInterval = DEFAULT_LOOP_INTERVAL;
	private int xPosition = 0; // Touch x position
	private int yPosition = 0; // Touch y position
	private int centerX = 0; // Center view x position
	private int centerY = 0; // Center view y position
	private Paint mainCircle;
	private Paint button;
	private Paint axisLine;
	private int joystickRadius;
	private int buttonRadius;

	public JoystickView(Context context) {
		super(context);
	}

	public JoystickView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initJoystickView();
	}

	public JoystickView(Context context, AttributeSet attrs, int defaultStyle) {
		super(context, attrs, defaultStyle);
		initJoystickView();
	}

	protected void initJoystickView() {
		mainCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
		mainCircle.setColor(Color.LTGRAY);
		mainCircle.setStyle(Paint.Style.FILL_AND_STROKE);

		axisLine = new Paint();
		axisLine.setStrokeWidth(2);
		axisLine.setColor(Color.BLACK);

		button = new Paint(Paint.ANTI_ALIAS_FLAG);
		button.setColor(Color.GRAY);
		button.setStyle(Paint.Style.FILL);
	}

	@Override
	protected void onFinishInflate() {
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// setting the measured values to resize the view to a certain width and
		// height
		int d = Math.min(measure(widthMeasureSpec), measure(heightMeasureSpec));

		setMeasuredDimension(d, d);

		// before measure, get the center of view
		xPosition = (int) getWidth() / 2;
		yPosition = (int) getWidth() / 2;

		buttonRadius = (int) (d / 2 * 0.25);
		joystickRadius = (int) (d / 2 * 0.75);
	}

	private static int measure(int measureSpec) {
		int result = 0;

		// Decode the measurement specifications.
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);

		if (specMode == MeasureSpec.UNSPECIFIED) {
			// Return a default size of 200 if no bounds are specified.
			result = 200;
		} else {
			// As you want to fill the available space
			// always return the full available bounds.
			result = specSize;
		}
		return result;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		centerX = (getWidth()) / 2;
		centerY = (getHeight()) / 2;

		// painting the main circle
		canvas.drawCircle((int) centerX, (int) centerY, joystickRadius + buttonRadius, mainCircle);
		canvas.drawLine((float) (centerX - joystickRadius), (float) centerY, (float) (centerX + joystickRadius),
				(float) centerY, axisLine);
		canvas.drawLine((float) centerX, (float) (centerY - joystickRadius), (float) centerX,
				(float) (centerY + joystickRadius), axisLine);

		// painting the move button
		canvas.drawCircle(xPosition, yPosition, buttonRadius, button);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		xPosition = (int) event.getX();
		yPosition = (int) event.getY();
		double abs = Math.sqrt((xPosition - centerX) * (xPosition - centerX) + (yPosition - centerY)
				* (yPosition - centerY));
		if (abs > joystickRadius) {
			xPosition = (int) ((xPosition - centerX) * joystickRadius / abs + centerX);
			yPosition = (int) ((yPosition - centerY) * joystickRadius / abs + centerY);
		}
		invalidate();
		if (event.getAction() == MotionEvent.ACTION_UP) {
			xPosition = (int) centerX;
			yPosition = (int) centerY;
			thread.interrupt();
			if (onJoystickMoveListener != null) {
				onJoystickMoveListener.onValueChanged(posX(), posY());
			}
		}
		if (onJoystickMoveListener != null && event.getAction() == MotionEvent.ACTION_DOWN) {
			if (thread != null && thread.isAlive()) {
				thread.interrupt();
			}
			thread = new Thread(this);
			thread.start();
			onJoystickMoveListener.onValueChanged(posX(), posY());
		}
		return true;
	}

	private float posX() {
		return (float) (xPosition - centerX) / joystickRadius;
	}

	private float posY() {
		return (float) (yPosition - centerY) / joystickRadius;
	}

	public void setOnJoystickMoveListener(OnJoystickMoveListener listener, long repeatInterval) {
		this.onJoystickMoveListener = listener;
		this.loopInterval = repeatInterval;
	}

	public static interface OnJoystickMoveListener {
		public void onValueChanged(float x, float y);
	}

	@Override
	public void run() {
		while (!Thread.interrupted()) {
			post(new Runnable() {
				@Override
				public void run() {
					onJoystickMoveListener.onValueChanged(posX(), posY());
				}
			});
			try {
				Thread.sleep(loopInterval);
			} catch (InterruptedException e) {
				break;
			}
		}
	}
}