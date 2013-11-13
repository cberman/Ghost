// http://developer.android.com/google/play-services/setup.html
package edu.virginia.cs2110.ghost;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;

public class MainActivity extends MapActivity implements
		GooglePlayServicesClient.ConnectionCallbacks,
		OnConnectionFailedListener, LocationListener {
	private LocationClient mLocationClient;
	// Global variable to hold the current location
	private Location mCurrentLocation;
	// Define an object that holds accuracy and frequency parameters
	LocationRequest mLocationRequest;
	private boolean mUpdatesRequested;
	private SharedPreferences mPrefs;
	private SharedPreferences.Editor mEditor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//enables zoom
		//won't let me create mapview, so i'm commenting it out for now
		/*
		 * MapView mapView = (MapView) findViewById(R.id.mapview);
		 * mapView.setBuiltInZoomControls(true);
		*/
		// Open the shared preferences
		mPrefs = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
		// Get a SharedPreferences editor
		mEditor = mPrefs.edit();
		/*
		 * Create a new location client, using the enclosing class to handle
		 * callbacks.
		 */
		mLocationClient = new LocationClient(this, this, this);
		// Create the LocationRequest object
		mLocationRequest = LocationRequest.create();
		// Use high accuracy
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		// Set the update interval to 5 seconds
		mLocationRequest.setInterval(Constants.UPDATE_INTERVAL);
		// Set the fastest update interval to 1 second
		mLocationRequest.setFastestInterval(Constants.FASTEST_INTERVAL);
		// Start with updates turned off
		mUpdatesRequested = false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/*
	 * Called when the Activity becomes visible.
	 */
	@Override
	protected void onStart() {
		super.onStart();
		if (servicesConnected()) {
			// Connect the client.
			mLocationClient.connect();
		}
	}

	/*
	 * Called when the Activity is no longer visible at all. Stop updates and
	 * disconnect.
	 */
	@Override
	protected void onStop() {
		// If the client is connected
		if (mLocationClient.isConnected()) {
			/*
			 * Remove location updates for a listener. The current Activity is
			 * the listener, so the argument is "this".
			 */
			mLocationClient.removeLocationUpdates(this);
		}
		/*
		 * After disconnect() is called, the client is considered "dead".
		 */
		mLocationClient.disconnect();
		super.onStop();
	}

	@Override
	protected void onPause() {
		// Save the current setting for updates
		mEditor.putBoolean("KEY_UPDATES_ON", mUpdatesRequested);
		mEditor.commit();
		super.onPause();
	}

	@Override
	protected void onResume() {
		/*
		 * Get any previous setting for location updates Gets "false" if an
		 * error occurs
		 */
		if (mPrefs.contains("KEY_UPDATES_ON")) {
			mUpdatesRequested = mPrefs.getBoolean("KEY_UPDATES_ON", false);

			// Otherwise, turn off location updates
		} else {
			mEditor.putBoolean("KEY_UPDATES_ON", false);
			mEditor.commit();
		}
		super.onResume();
	}

	// Define a DialogFragment that displays the error dialog
	public static class ErrorDialogFragment extends DialogFragment {
		// Global field to contain the error dialog
		private Dialog mDialog;

		// Default constructor. Sets the dialog field to null
		public ErrorDialogFragment() {
			super();
			mDialog = null;
		}

		// Set the dialog to display
		public void setDialog(Dialog dialog) {
			mDialog = dialog;
		}

		// Return a Dialog to the DialogFragment.
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return mDialog;
		}
	}

	/*
	 * Handle results returned to the FragmentActivity by Google Play services
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Decide what to do based on the original request code
		switch (requestCode) {
		// ...
		case Constants.CONNECTION_FAILURE_RESOLUTION_REQUEST:
			/*
			 * If the result code is Activity.RESULT_OK, try to connect again
			 */
			switch (resultCode) {
			case Activity.RESULT_OK:
				/*
				 * Try the request again
				 */
				// ...
				break;
			}
			// ...
		}
	}

	private void showErrorDialog(int errorCode) {
		// Get the error dialog from Google Play services
		Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode,
				this, Constants.CONNECTION_FAILURE_RESOLUTION_REQUEST);

		// If Google Play services can provide an error dialog
		if (errorDialog != null) {
			// Create a new DialogFragment for the error dialog
			ErrorDialogFragment errorFragment = new ErrorDialogFragment();
			// Set the dialog in the DialogFragment
			errorFragment.setDialog(errorDialog);
			// Show the error dialog in the DialogFragment
			errorFragment.show(getFragmentManager(), "Location Updates");
		}
	}

	private boolean servicesConnected() {
		// If this doesn't compile, setup Services Google Play SDK
		// http://developer.android.com/google/play-services/setup.html

		// Check that Google Play services is available
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		// If Google Play services is available
		if (ConnectionResult.SUCCESS == resultCode) {
			// In debug mode, log the status
			Log.d("Location Updates", "Google Play services is available.");
			// Continue
			return true;
			// Google Play services was not available for some reason
		} else {
			showErrorDialog(resultCode);
			return false;
		}
	}

	/*
	 * Called by Location Services when the request to connect the client
	 * finishes successfully. At this point, you can request the current
	 * location or start periodic updates
	 */
	@Override
	public void onConnected(Bundle dataBundle) {
		mCurrentLocation = mLocationClient.getLastLocation();
		// Display the connection status
		Toast.makeText(this, "Connected to Location Services",
				Toast.LENGTH_LONG).show();
		// If already requested, start periodic updates
		if (mUpdatesRequested) {
			mLocationClient.requestLocationUpdates(mLocationRequest, this);
		}
	}

	/*
	 * Called by Location Services if the connection to the location client
	 * drops because of an error.
	 */
	@Override
	public void onDisconnected() {
		// Display the connection status
		Toast.makeText(this,
				"Disconnected from Location Services. Please re-connect.",
				Toast.LENGTH_SHORT).show();
	}

	/*
	 * Called by Location Services if the attempt to Location Services fails.
	 */
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		/*
		 * Google Play services can resolve some errors it detects. If the error
		 * has a resolution, try sending an Intent to start a Google Play
		 * services activity that can resolve error.
		 */
		if (connectionResult.hasResolution()) {
			try {
				// Start an Activity that tries to resolve the error
				connectionResult.startResolutionForResult(this,
						Constants.CONNECTION_FAILURE_RESOLUTION_REQUEST);
				/*
				 * Thrown if Google Play services canceled the original
				 * PendingIntent
				 */
			} catch (IntentSender.SendIntentException e) {
				// Log the error
				e.printStackTrace();
			}
		} else {
			/*
			 * If no resolution is available, display a dialog to the user with
			 * the error.
			 */
			showErrorDialog(connectionResult.getErrorCode());
		}
	}

	// Define the callback method that receives location updates
	@Override
	public void onLocationChanged(Location location) {
		// Report to the UI that the location was updated
		String msg = "Updated Location: "
				+ Double.toString(location.getLatitude()) + ","
				+ Double.toString(location.getLongitude());
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

}
