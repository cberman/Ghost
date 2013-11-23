// http://developer.android.com/google/play-services/setup.html
package edu.virginia.cs2110.ghost;

//testing changes 
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationClient.OnAddGeofencesResultListener;
import com.google.android.gms.location.LocationClient.OnRemoveGeofencesResultListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationStatusCodes;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

public class MainActivity extends Activity implements
		GooglePlayServicesClient.ConnectionCallbacks,
		OnConnectionFailedListener, LocationListener,
		OnAddGeofencesResultListener, OnRemoveGeofencesResultListener {
	private LocationClient mLocationClient;
	// Global variable to hold the current location
	Location mCurrentLocation;
	// Define an object that holds accuracy and frequency parameters
	LocationRequest mLocationRequest;
	private boolean mUpdatesRequested;
	private SharedPreferences mPrefs;
	private SharedPreferences.Editor mEditor;
	// Internal List of Geofence objects
	private List<Geofence> mGeofences;
	// Persistent storage for ghosts
	private GhostStore mGhosts;
	// Persistent storage for items
	private ItemStore mItems;
	// Stores the PendingIntent used to request geofence monitoring
	private PendingIntent mTransitionPendingIntent;
	// Flag that indicates if a request is underway.
	private boolean mInProgress;
	// Used to generate ghost locations
	private Random random;

	private GoogleMap map;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Open the shared preferences
		mPrefs = getSharedPreferences(Constants.SHARED_PREFERENCES,
				Context.MODE_PRIVATE);
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
		// Start with updates turned on
		mUpdatesRequested = true;
		mEditor.putBoolean("KEY_UPDATES_ON", mUpdatesRequested);
		mEditor.commit();
		// Instantiate a new geofence storage area
		mGhosts = new GhostStore(this);
		mItems = new ItemStore(this);
		// Instantiate the current List of geofences
		mGeofences = new ArrayList<Geofence>();
		// Start with the request flag set to false
		mInProgress = false;
		random = new Random(System.currentTimeMillis());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.action_camera:
			Intent i = new Intent(this, CameraActivity.class);
			Bundle bundle = new Bundle();
			bundle.putDouble("latitude", mCurrentLocation.getLatitude());
			bundle.putDouble("longitude", mCurrentLocation.getLongitude());
			double[] ghostLats = new double[mGhosts.getIds().size()];
			double[] ghostLongs = new double[mGhosts.getIds().size()];
			int index = 0;
			for(String id : mGhosts.getIds()) {
				Ghost g = mGhosts.getGhost(id);
				ghostLats[index] = g.getLatitude();
				ghostLongs[index] = g.getLongitude();
				index++;
			}
			bundle.putDoubleArray("ghostLats", ghostLats);
			bundle.putDoubleArray("ghostLongs", ghostLongs);
			i.putExtras(bundle);
			startActivity(i);
			return true;
		case R.id.action_settings:
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
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
		if (mLocationClient.isConnected()) {
			/*
			 * After disconnect() is called, the client is considered "dead".
			 */
			mLocationClient.disconnect();
		}
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

	@Override
	protected void onDestroy() {
		super.onDestroy();
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

		map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
				.getMap();
		map.setMyLocationEnabled(true);
		map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
		LatLng myLocation = new LatLng(mCurrentLocation.getLatitude(),
				mCurrentLocation.getLongitude());
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 18));

		// Display the connection status
		Toast.makeText(this, "Connected to Location Services",
				Toast.LENGTH_LONG).show();
		// If already requested, start periodic updates
		createGhost();
		addGeofences();
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
		/*
		 * Remove location updates for a listener. The current Activity is the
		 * listener, so the argument is "this".
		 */
		mLocationClient.removeLocationUpdates(this);
		removeGeofences(new ArrayList<String>(mGhosts.getIds()));
		// Turn off the request flag
		mInProgress = false;
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
		mCurrentLocation = location;
		// Report to the UI that the location was updated
		String msg = "Updated Location: "
				+ Double.toString(mCurrentLocation.getLatitude()) + ","
				+ Double.toString(mCurrentLocation.getLongitude());
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

	private void createGhost() {
		/*
		 * Find an id that isn't in use
		 */
		int id = mGeofences.size() + 1;
		while (mGhosts.getIds().contains(Integer.toString(id)))
			id++;
		/*
		 * Generate a random position
		 */
		double latitude = mCurrentLocation.getLatitude();
		double longitude = mCurrentLocation.getLongitude();
		double radius = (random.nextGaussian() * 10 + 100)
				/ Constants.METERS_PER_DEGREE;
		double theta = random.nextDouble() * 2 * Math.PI;
		latitude += radius * Math.cos(theta);
		longitude += radius * Math.sin(theta) * Math.cos(latitude);
		Log.d("ghostGeneration", "id: " + id + "; lat: " + latitude
				+ "; long: " + longitude);
		Ghost ghost = new Ghost(Integer.toString(id), latitude, longitude,
				Constants.GHOST_RADIUS, Constants.GHOST_EXPIRATION_TIME,
				// This geofence records only entry transitions
				Geofence.GEOFENCE_TRANSITION_ENTER, false);
		// Store this flat version
		mGhosts.saveGhost(Integer.toString(id), ghost);
		mGeofences.add(ghost.toGeofence());
	}

	private void createItem() {
		/*
		 * Find an id that isn't in use
		 */
		int id = mGeofences.size()+1;
		while (mItems.getIds().contains(Integer.toString(id)))
			id++;
		/*
		 * Generate a random position
		 */
		double latitude = mCurrentLocation.getLatitude();
		double longitude = mCurrentLocation.getLongitude();
		double radius = (random.nextGaussian() * 10 + 100)/Constants.METERS_PER_DEGREE;
		double theta = random.nextDouble() * 2 * Math.PI;
		latitude += radius * Math.cos(theta);
		longitude += radius * Math.sin(theta) * Math.cos(latitude);
		Log.d("itemGeneration", "id: "+id+"; lat: "+latitude+"; long: "+longitude);
		Item item = new Item(Integer.toString(id), latitude, longitude,
				Constants.GHOST_RADIUS, Constants.GHOST_EXPIRATION_TIME,
				// This geofence records only entry transitions
				Geofence.GEOFENCE_TRANSITION_ENTER);
		// Store this flat version
		mItems.saveItem(Integer.toString(id), item);
		mGeofences.add(item.toGeofence());
	}

	/**
	 * Start a request for geofence monitoring by calling
	 * LocationClient.connect().
	 */
	public boolean addGeofences() {
		/*
		 * Test for Google Play services after setting the request type. If
		 * Google Play services isn't present, the proper request can be
		 * restarted.
		 */
		if (!servicesConnected()) {
			return false;
		}
		if (mGeofences.size() == 0)
			return true;
		// If a request is not already underway
		if (!mInProgress) {
			// Indicate that a request is underway
			mInProgress = true;
			// Send a request to add the current geofences
			// Get the PendingIntent for the request
			mTransitionPendingIntent = getTransitionPendingIntent();
			// Send a request to add the current geofences
			mLocationClient.addGeofences(mGeofences, mTransitionPendingIntent,
					this);
		} else {
			/*
			 * A request is already underway. You can handle this situation by
			 * disconnecting the client, re-setting the flag, and then re-trying
			 * the request.
			 */
			return false;
		}
		return true;
	}

	/**
	 * Start a request to remove geofences by calling LocationClient.connect()
	 */
	public boolean removeGeofences(PendingIntent requestIntent) {
		/*
		 * Test for Google Play services after setting the request type. If
		 * Google Play services isn't present, the request can be restarted.
		 */
		if (!servicesConnected()) {
			return false;
		}
		// If a request is not already underway
		if (!mInProgress) {
			// Indicate that a request is underway
			mInProgress = true;
			mLocationClient.removeGeofences(requestIntent, this);
		} else {
			/*
			 * A request is already underway. You can handle this situation by
			 * disconnecting the client, re-setting the flag, and then re-trying
			 * the request.
			 */
			return false;
		}
		return true;
	}

	/**
	 * Start a request to remove monitoring by calling LocationClient.connect()
	 * 
	 */
	public boolean removeGeofences(List<String> geofenceIds) {
		/*
		 * Test for Google Play services after setting the request type. If
		 * Google Play services isn't present, the request can be restarted.
		 */
		if (!servicesConnected()) {
			return false;
		}
		// If a request is not already underway
		if (!mInProgress) {
			// Indicate that a request is underway
			mInProgress = true;
			mLocationClient.removeGeofences(geofenceIds, this);
		} else {
			/*
			 * A request is already underway. You can handle this situation by
			 * disconnecting the client, re-setting the flag, and then re-trying
			 * the request.
			 */
			return false;
		}
		return true;
	}

	/*
	 * Create a PendingIntent that triggers an IntentService in your app when a
	 * geofence transition occurs.
	 */
	private PendingIntent getTransitionPendingIntent() {
		// No functionality yet
		// Create an explicit Intent
		Intent intent = new Intent();
		/*
		 * Return the PendingIntent
		 */
		return PendingIntent.getService(this, 0, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
	}

	@Override
	public void onAddGeofencesResult(int statusCode, String[] geofenceRequestIds) {
		// If adding the geofences was successful
		if (statusCode == LocationStatusCodes.SUCCESS) {
			/*
			 * Handle successful addition of geofences here. You can send out a
			 * broadcast intent or update the UI. geofences into the Intent's
			 * extended data.
			 */
			for (String id : geofenceRequestIds) {
				Ghost ghost = mGhosts.getGhost(id);
				double latitude = ghost.getLatitude();
				double longitude = ghost.getLongitude();
				// Add the ghost to the map
				Bitmap bm = BitmapFactory.decodeResource(getResources(),
						R.drawable.ghost);
				Bitmap scaled = Bitmap.createScaledBitmap(bm,
						bm.getWidth() / 2, bm.getHeight() / 2, false);
				map.addMarker(new MarkerOptions()
						.icon(BitmapDescriptorFactory.fromBitmap(scaled))
						.anchor(0.5f, 0.5f)
						.position(new LatLng(latitude, longitude)));
			}
		} else {
			// If adding the geofences failed
			/*
			 * Report errors here. You can log the error using Log.e() or update
			 * the UI.
			 */
			Log.e("Geofence", "unable to add geofence");
		}
		// Turn off the in progress flag
		mInProgress = false;

	}

	/**
	 * When the request to remove geofences by PendingIntent returns, handle the
	 * result.
	 * 
	 * @param statusCode
	 *            the code returned by Location Services
	 * @param requestIntent
	 *            The Intent used to request the removal.
	 */
	@Override
	public void onRemoveGeofencesByPendingIntentResult(int statusCode,
			PendingIntent requestIntent) {
		// If removing the geofences was successful
		if (statusCode == LocationStatusCodes.SUCCESS) {
			/*
			 * Handle successful removal of geofences here. You can send out a
			 * broadcast intent or update the UI. geofences into the Intent's
			 * extended data.
			 */
		} else {
			// If adding the geocodes failed
			/*
			 * Report errors here. You can log the error using Log.e() or update
			 * the UI.
			 */
			Log.e("Geofence", "unable to remove geofences by intent");
		}
		// Indicate that a request is no longer in progress
		mInProgress = false;
	}

	/**
	 * When the request to remove geofences by IDs returns, handle the result.
	 * 
	 * @param statusCode
	 *            The code returned by Location Services
	 * @param geofenceRequestIds
	 *            The IDs removed
	 */
	@Override
	public void onRemoveGeofencesByRequestIdsResult(int statusCode,
			String[] geofenceRequestIds) {
		// If removing the geocodes was successful
		if (LocationStatusCodes.SUCCESS == statusCode) {
			/*
			 * Handle successful removal of geofences here. You can send out a
			 * broadcast intent or update the UI. geofences into the Intent's
			 * extended data.
			 */
			for (String id : geofenceRequestIds) {
				for (Geofence g : mGeofences)
					if (id != null && id.equals(g.getRequestId()))
						mGeofences.remove(g);
				Ghost ghost = mGhosts.getGhost(id);
				double latitude = ghost.getLatitude();
				double longitude = ghost.getLongitude();
				// Remove the ghost from the map
				mGhosts.clearGhost(id);
			}
		} else {
			// If removing the geofences failed
			/*
			 * Report errors here. You can log the error using Log.e() or update
			 * the UI.
			 */
			Log.e("Geofence", "unable to remove geofences by id");
		}
		// Indicate that a request is no longer in progress
		mInProgress = false;
	}
}
