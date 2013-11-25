// http://developer.android.com/google/play-services/setup.html
package edu.virginia.cs2110.ghost;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
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
	// Internal List of Geofence objects, temporary storage
	private List<Geofence> mGeofences;
	// Persistent storage for ghosts
	private GhostStore mGhosts;
	// Persistent storage for items
	private ItemStore mItems;
	private EventGenerator generator;
	// Stores the PendingIntent used to request geofence monitoring
	private PendingIntent mTransitionPendingIntent;
	// Used to generate ghost locations
	private Random random;

	private GoogleMap map;
	private Map<String, Marker> mapMarkers;

	private int bombs = 10, money = 6, ghostsKilled;
	public int difficulty;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		((TextView) findViewById(R.id.textViewGhost)).setText(Integer
				.toString(ghostsKilled));
		((TextView) findViewById(R.id.textViewDollar)).setText(Integer
				.toString(money));
		((TextView) findViewById(R.id.textViewBomb)).setText(Integer
				.toString(bombs));

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
		mapMarkers = new HashMap<String, Marker>();
		random = new Random(System.currentTimeMillis());
		// Default difficulty is easy
		difficulty = Constants.DIFFICULTY_EASY;
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
			if (CameraView.checkCameraHardware(this)) {
				Intent i = new Intent(this, CameraActivity.class);
				Bundle bundle = new Bundle();
				bundle.putDouble("latitude", mCurrentLocation.getLatitude());
				bundle.putDouble("longitude", mCurrentLocation.getLongitude());
				double[] ghostLats = new double[mGhosts.getIds().size()];
				double[] ghostLongs = new double[mGhosts.getIds().size()];
				int index = 0;
				for (String id : mGhosts.getIds()) {
					Ghost g = mGhosts.getGhost(id);
					ghostLats[index] = g.getLatitude();
					ghostLongs[index] = g.getLongitude();
					index++;
				}
				bundle.putDoubleArray("ghostLats", ghostLats);
				bundle.putDoubleArray("ghostLongs", ghostLongs);
				i.putExtras(bundle);
				startActivity(i);
			} else
				Toast.makeText(this, "No camera on this device",
						Toast.LENGTH_LONG).show();
			return true;
		case R.id.action_settings:
			return true;
		case R.id.difficulty_easy:
			difficulty = Constants.DIFFICULTY_EASY;
			return true;
		case R.id.difficulty_medium:
			difficulty = Constants.DIFFICULTY_MEDIUM;
			return true;
		case R.id.difficulty_hard:
			difficulty = Constants.DIFFICULTY_HARD;
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
		generator.cancel(true);

		if (mLocationClient != null && mLocationClient.isConnected()) {
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
		do {
			mCurrentLocation = mLocationClient.getLastLocation();
		} while (mCurrentLocation == null);

		map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
				.getMap();
		map.setMyLocationEnabled(true);
		map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
		LatLng myLocation = new LatLng(mCurrentLocation.getLatitude(),
				mCurrentLocation.getLongitude());
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 18));

		// If already requested, start periodic updates
		if (mUpdatesRequested) {
			mLocationClient.requestLocationUpdates(mLocationRequest, this);
		}

		generator = new EventGenerator();
		generator.execute(this);
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
	}

	public void createGhost() {
		/*
		 * Find an id that isn't in use
		 */
		int id = mGhosts.getIds().size();
		while (mGhosts.getIds().contains("G" + id))
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
		Log.d("ghostGeneration", "id: G" + id + "; lat: " + latitude
				+ "; long: " + longitude);
		Ghost ghost = new Ghost("G" + id, latitude, longitude,
				Constants.GHOST_RADIUS, Constants.GHOST_EXPIRATION_TIME,
				// This geofence records only entry transitions
				Geofence.GEOFENCE_TRANSITION_ENTER, false);
		// Store this flat version
		mGhosts.saveGhost("G" + id, ghost);
		mGeofences.add(ghost.toGeofence());
	}

	public void createItem() {
		/*
		 * Find an id that isn't in use
		 */
		int id = mItems.getIds().size();
		while (mItems.getIds().contains("B" + id))
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
		Log.d("itemGeneration", "id: B" + id + "; lat: " + latitude
				+ "; long: " + longitude);
		Item item = new Item("B" + id, latitude, longitude,
				Constants.PICKUP_RADIUS, Constants.GHOST_EXPIRATION_TIME,
				// This geofence records only entry transitions
				Geofence.GEOFENCE_TRANSITION_ENTER);
		// Store this flat version
		mItems.saveItem("B" + id, item);
		mGeofences.add(item.toGeofence());
	}

	private void createMoney(double lat, double lon) {

		int id = mItems.getIds().size();
		while (mItems.getIds().contains("M" + id))
			id++;

		Item item = new Item("M" + id, lat, lon, Constants.PICKUP_RADIUS,
				Constants.GHOST_EXPIRATION_TIME,
				// This geofence records only entry transitions
				Geofence.GEOFENCE_TRANSITION_ENTER);
		// Store this flat version
		mItems.saveItem("M" + id, item);
		mGeofences.add(item.toGeofence());
	}

	public void buyBomb(View v) {
		if (money < 5) {
			Toast.makeText(this, "You need $5 to buy a bomb",
					Toast.LENGTH_SHORT).show();
		} else {
			money -= 5;
			((TextView) findViewById(R.id.textViewDollar)).setText(Integer
					.toString(money));
			bombs++;
			((TextView) findViewById(R.id.textViewBomb)).setText(Integer
					.toString(bombs));
		}
	}

	public void useBomb(View v) {
		if (bombs == 0) {
			Toast.makeText(this, "You don't have any bombs", Toast.LENGTH_SHORT)
					.show();
			return;
		}
		bombs--;
		((TextView) findViewById(R.id.textViewBomb)).setText(Integer
				.toString(bombs));
		Bitmap bm = BitmapFactory.decodeResource(getResources(),
				R.drawable.boom);
		Bitmap scaled = Bitmap.createScaledBitmap(bm, bm.getWidth() / 2,
				bm.getHeight() / 2, false);
		final double latitude = mCurrentLocation.getLatitude();
		final double longitude = mCurrentLocation.getLongitude();
		final Marker bomb = map.addMarker(new MarkerOptions()
				.icon(BitmapDescriptorFactory.fromBitmap(scaled))
				.anchor(0.5f, 0.5f).position(new LatLng(latitude, longitude)));
		new Handler().postDelayed(new Runnable() {

			public void run() {
				bomb.remove();
			}
		}, 500);
		new Handler().post(new Runnable() {
			public void run() {
				List<String> killed = new ArrayList<String>();
				for (String id : mGhosts.getIds()) {
					Ghost g = mGhosts.getGhost(id);
					double ghostLat = g.getLatitude(), ghostLong = g
							.getLongitude();
					Log.d("location", latitude + ", " + longitude);
					Log.d("ghostLoc", ghostLat + ", " + ghostLong);
					Log.d("distance",
							haversine(latitude, longitude, ghostLat, ghostLong)
									+ "");
					if (haversine(latitude, longitude, ghostLat, ghostLong) <= Constants.BOMB_RADIUS) {
						killed.add(id);
						ghostsKilled++;
						createMoney(ghostLat, ghostLong);
					}
				}
				((TextView) findViewById(R.id.textViewGhost)).setText(Integer
						.toString(ghostsKilled));
				// Add the money
				addGeofences();
				removeGeofences(killed);
			}
		});
	}

	/**
	 * Uses the Haversine Formula expressed in terms of a two-argument inverse
	 * tangent function to calculate the great circle distance between two
	 * points on the Earth. From
	 * http://andrew.hedges.name/experiments/haversine/
	 * 
	 */
	private double haversine(double lat1, double lon1, double lat2, double lon2) {
		lat1 = Math.toRadians(lat1);
		lon1 = Math.toRadians(lon1);
		lat2 = Math.toRadians(lat2);
		lon2 = Math.toRadians(lon2);
		double dlon = lon2 - lon1;
		double dlat = lat2 - lat1;
		double a = Math.pow(Math.sin(dlat / 2), 2) + Math.cos(lat1)
				* Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double d = 6373000 * c; // Optimized for locations around 39 degrees
		return d;
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
		// Send a request to add the current geofences
		// Get the PendingIntent for the request
		mTransitionPendingIntent = getTransitionPendingIntent();
		// Send a request to add the current geofences
		mLocationClient
				.addGeofences(mGeofences, mTransitionPendingIntent, this);
		mGeofences.clear();
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
		mLocationClient.removeGeofences(requestIntent, this);
		return true;
	}

	/**
	 * Start a request to remove monitoring by calling LocationClient.connect()
	 * 
	 */
	public boolean removeGeofences(List<String> geofenceIds) {
		Log.d("bombing", geofenceIds.toString());
		if (geofenceIds.isEmpty())
			return true;
		/*
		 * Test for Google Play services after setting the request type. If
		 * Google Play services isn't present, the request can be restarted.
		 */
		if (!servicesConnected()) {
			return false;
		}
		mLocationClient.removeGeofences(geofenceIds, this);
		return true;
	}

	/*
	 * Create a PendingIntent that triggers an IntentService in your app when a
	 * geofence transition occurs.
	 */
	private PendingIntent getTransitionPendingIntent() {
		// Create an explicit Intent
		Intent intent = new Intent(this, ReceiveTransitionsIntentService.class);
		Bundle bundle = new Bundle();
		intent.putExtras(bundle);
		/*
		 * Return the PendingIntent
		 */
		return PendingIntent.getService(this, 0, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
	}

	public void proximityAlert() {
		runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(getApplicationContext(), "Look out, you're near a ghost!",
						Toast.LENGTH_LONG).show();
			}
		});
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
				double latitude = 0, longitude = 0;
				Bitmap scaled = null;
				if (id.charAt(0) == 'G') {
					Ghost ghost = mGhosts.getGhost(id);
					latitude = ghost.getLatitude();
					longitude = ghost.getLongitude();
					// Add the ghost to the map
					Bitmap bm = BitmapFactory.decodeResource(getResources(),
							R.drawable.ghost);
					scaled = Bitmap.createScaledBitmap(bm, bm.getWidth() / 2,
							bm.getHeight() / 2, false);
				} else {
					Item item = mItems.getItem(id);
					latitude = item.getLatitude();
					longitude = item.getLongitude();
					// Add the ghost to the map
					Bitmap bm = null;

					if (id.charAt(0) == 'B') {
						bm = BitmapFactory.decodeResource(getResources(),
								R.drawable.bomb);
					} else {
						bm = BitmapFactory.decodeResource(getResources(),
								R.drawable.dollarsign);
					}

					scaled = Bitmap.createScaledBitmap(bm, bm.getWidth() / 4,
							bm.getHeight() / 4, false);
				}
				Marker marker = map.addMarker(new MarkerOptions()
						.icon(BitmapDescriptorFactory.fromBitmap(scaled))
						.anchor(0.5f, 0.5f)
						.position(new LatLng(latitude, longitude)));
				mapMarkers.put(id, marker);

			}
		} else {
			// If adding the geofences failed
			/*
			 * Report errors here. You can log the error using Log.e() or update
			 * the UI.
			 */
			Log.e("Geofence", "unable to add geofence");
		}
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
				// Remove the ghost from the map
				Log.d("bombing", "Removing ghost " + id);
				mapMarkers.get(id).remove();
				mapMarkers.remove(id);
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
	}
}
