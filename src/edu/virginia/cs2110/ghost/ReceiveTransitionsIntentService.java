package edu.virginia.cs2110.ghost;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class ReceiveTransitionsIntentService extends IntentService {
	/**
	 * Sets an identifier for the service
	 */
	public ReceiveTransitionsIntentService() {
		super("ReceiveTransitionsIntentService");
	}

	/**
	 * Handles incoming intents
	 * 
	 * @param intent
	 *            The Intent sent by Location Services. This Intent is provided
	 *            to Location Services (inside a PendingIntent) when you call
	 *            addGeofences()
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		// First check for errors
		if (LocationClient.hasError(intent)) {
			// Get the error code with a static method
			int errorCode = LocationClient.getErrorCode(intent);
			// Log the error
			Log.e("ReceiveTransitionsIntentService",
					"Location Services error: " + Integer.toString(errorCode));
			/*
			 * You can also send the error code to an Activity or Fragment with
			 * a broadcast Intent
			 */
			/*
			 * If there's no error, get the transition type and the IDs of the
			 * geofence or geofences that triggered the transition
			 */
		} else {
			// Get the type of transition (entry or exit)
			int transitionType = LocationClient.getGeofenceTransition(intent);
			// Test that a valid transition was reported
			if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER
					|| transitionType == Geofence.GEOFENCE_TRANSITION_EXIT
					|| transitionType == Geofence.GEOFENCE_TRANSITION_DWELL) {
				List<Geofence> triggerList = LocationClient
						.getTriggeringGeofences(intent);

				Set<String> triggerIds = new HashSet<String>();

				for (int i = 0; i < triggerList.size(); i++) {
					if (transitionType == Geofence.GEOFENCE_TRANSITION_DWELL)
						triggerIds.add("DEATH");
					else
						// Store the Id of each geofence
						triggerIds.add(triggerList.get(i).getRequestId());
				}

				// Open the shared preferences
				SharedPreferences prefs = getSharedPreferences(
						Constants.SHARED_PREFERENCES, Context.MODE_PRIVATE);
				// Get a SharedPreferences editor
				Editor editor = prefs.edit();
				editor.putBoolean(Constants.TRANSITION_UPDATE, true);
				editor.putStringSet(Constants.TRANSITION_IDS, triggerIds);
				editor.commit();
			} else {
				// An invalid transition was reported
				Log.e("ReceiveTransitionsIntentService",
						"Geofence transition error: "
								+ Integer.toString(transitionType));
			}
		}
	}
}
