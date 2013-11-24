package edu.virginia.cs2110.ghost;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class CameraActivity extends Activity implements SensorEventListener {

	private CameraView mView;
	private SensorManager mSensorManager;
	private float[] orientation;
	private ImageView[] overlay;

	private double latitude, longitude;
	private double[] ghostLats, ghostLongs;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		orientation = new float[3];

		Bundle extras = getIntent().getExtras();
		latitude = Math.toRadians(extras.getDouble("latitude"));
		longitude = Math.toRadians(extras.getDouble("longitude"));
		ghostLats = extras.getDoubleArray("ghostLats");
		ghostLongs = extras.getDoubleArray("ghostLongs");

		setContentView(R.layout.activity_camera);
		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);

		// Create our view and set it as the content of our activity.
		mView = new CameraView(this);
		preview.addView(mView);

		overlay = new ImageView[ghostLongs.length];
		for (int i = 0; i < overlay.length; i++) {
			overlay[i] = new ImageView(this);
			overlay[i].setImageResource(R.drawable.ghost);
			overlay[i].setScaleType(ScaleType.MATRIX);
			preview.addView(overlay[i]);
		}
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
		// when the app is paused, it stops updating to conserve battery
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
				orientation = event.values;

				for (int i = 0; i < overlay.length; i++) {
					float ghostWidth = overlay[i].getDrawable().getBounds()
							.width();
					float ghostHeight = overlay[i].getDrawable().getBounds()
							.height();

					Matrix matrix = new Matrix();
					matrix.postRotate(orientation[2], ghostWidth / 2,
							ghostHeight / 2);
					// Scale should be based on distance to ghost
					matrix.postScale(.75f, .75f, ghostWidth / 2,
							ghostHeight / 2);
					float angle = ((ghostAngle(i) - orientation[0] + 540f) % 360f) - 180f ;
					Log.d("ghostView", i + ": " + ghostAngle(i) + " - " + orientation[0] +" = "+angle);
					matrix.postTranslate(angle * ghostWidth / 30f, (((-90
							- orientation[1] + 180) % 360f) - 180)
							* ghostHeight / 40);
					overlay[i].setImageMatrix(matrix);
				}
			}
		}
	}

	/**
	 * When the back button is pressed, return to map
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// activity is done and should be closed
		finish();
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Get angle between two locations, from
	 * http://www.movable-type.co.uk/scripts/latlong.html
	 * 
	 */
	private float ghostAngle(int ghost) {
		double ghostLat = Math.toRadians(ghostLats[ghost]);
		double ghostLong = Math.toRadians(ghostLongs[ghost]);
		double dLat = ghostLat - latitude;
		double dLon = ghostLong - longitude;
		double y = Math.sin(dLon) * Math.cos(ghostLat);
		double x = Math.cos(latitude) * Math.sin(ghostLat) - Math.sin(latitude)
				* Math.cos(ghostLat) * Math.cos(dLon);
		double bearing = Math.toDegrees(Math.atan2(y, x));
		if (!mView.facingBack)
			bearing -= 180;
		if(bearing < 0)
			bearing += 360;
		return (float) bearing;
	}
}