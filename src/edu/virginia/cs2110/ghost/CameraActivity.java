package edu.virginia.cs2110.ghost;

import android.app.Activity;
import android.content.Context;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class CameraActivity extends Activity implements SensorEventListener {

	private CameraView mView;
	private SensorManager mSensorManager;
	private float[] orientation;
	private ImageView overlay;
	
	// Angle between north and ghost, in degrees
	private final float ghostAngle = 300;	// Test value

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		orientation = new float[3];

		setContentView(R.layout.activity_camera);
		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);

		// Create our view and set it as the content of our activity.
		mView = new CameraView(this);
		preview.addView(mView);

		overlay = new ImageView(this);
		overlay.setImageResource(R.drawable.ghost);
		overlay.setScaleType(ScaleType.MATRIX);
		preview.addView(overlay);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Register this class as a listener for the orientation sensor
		Sensor orientationSensor = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		mSensorManager.registerListener(this, orientationSensor,
				SensorManager.SENSOR_DELAY_GAME);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Nothing to do
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		synchronized (this) {
			if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {

				float ghostWidth = overlay.getDrawable().getBounds().width();
				float ghostHeight = overlay.getDrawable().getBounds().height();

				orientation = event.values;

				Matrix matrix = new Matrix();
				matrix.postRotate(orientation[2], ghostWidth / 2,
						ghostHeight / 2);
				// Scale should be based on distance to ghost
				matrix.postScale(.75f, .75f, ghostWidth / 2, ghostHeight / 2);
				matrix.postTranslate(
						(((ghostAngle - orientation[0] + 180) % 360f) - 180)
								* ghostWidth / 30, (((-90
								- orientation[1] + 180) % 360f) - 180)
								* ghostHeight / 40);
				overlay.setImageMatrix(matrix);
			}
		}
	}
}