package edu.virginia.cs2110.ghost;

import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * Based on code from
 * http://developer.android.com/training/location/geofencing.html
 * 
 */
public class GhostStore {
	// Keys for flattened ghost stored in SharedPreferences
	private static final String KEY_LATITUDE = "edu.virginia.cs2110.ghost.KEY_LATITUDE";
	private static final String KEY_LONGITUDE = "edu.virginia.cs2110.ghost.KEY_LONGITUDE";
	private static final String KEY_RADIUS = "edu.virginia.cs2110.ghost.KEY_RADIUS";
	private static final String KEY_EXPIRATION_DURATION = "edu.virginia.cs2110.ghost.KEY_EXPIRATION_DURATION";
	private static final String KEY_TRANSITION_TYPE = "edu.virginia.cs2110.ghost.KEY_TRANSITION_TYPE";
	private static final String KEY_VULNERABLE = "edu.virginia.cs2110.ghost.KEY_VULNERABLE";
	// The prefix for flattened ghost keys
	private static final String KEY_PREFIX = "edu.virginia.cs2110.ghost.KEY";
	// The SharedPreferences object in which ghosts are stored
	private final SharedPreferences preferences;
	// The Set of all the ghost ids
	private Set<String> ids;

	// Create the SharedPreferences storage with private access only
	public GhostStore(Context context) {
		preferences = context.getSharedPreferences(
				Constants.SHARED_PREFERENCES, Context.MODE_PRIVATE);
		ids = new HashSet<String>();
	}

	/**
	 * Returns a stored ghost by its id, or returns null if it's not found.
	 * 
	 * @param id
	 *            The ID of a stored ghost
	 * @return A ghost defined by its center and radius
	 */
	public Ghost getGhost(String id) {
		/*
		 * Get the latitude for the ghost identified by id, or
		 * INVALID_FLOAT_VALUE if it doesn't exist
		 */
		double lat = preferences.getFloat(getGhostFieldKey(id, KEY_LATITUDE),
				Constants.INVALID_FLOAT_VALUE);
		/*
		 * Get the longitude for the ghost identified by id, or
		 * INVALID_FLOAT_VALUE if it doesn't exist
		 */
		double lng = preferences.getFloat(getGhostFieldKey(id, KEY_LONGITUDE),
				Constants.INVALID_FLOAT_VALUE);
		/*
		 * Get the radius for the ghost identified by id, or
		 * INVALID_FLOAT_VALUE if it doesn't exist
		 */
		float radius = preferences.getFloat(getGhostFieldKey(id, KEY_RADIUS),
				Constants.INVALID_FLOAT_VALUE);
		/*
		 * Get the expiration duration for the ghost identified by id, or
		 * INVALID_LONG_VALUE if it doesn't exist
		 */
		long expirationDuration = preferences.getLong(
				getGhostFieldKey(id, KEY_EXPIRATION_DURATION),
				Constants.INVALID_LONG_VALUE);
		/*
		 * Get the transition type for the ghost identified by id, or
		 * INVALID_INT_VALUE if it doesn't exist
		 */
		int transitionType = preferences.getInt(
				getGhostFieldKey(id, KEY_TRANSITION_TYPE),
				Constants.INVALID_INT_VALUE);
		/*
		 * Get the vulnerability for the ghost identified by id, or
		 * INVALID_INT_VALUE if it doesn't exist
		 */
		boolean vulnerable = preferences.getBoolean(
				getGhostFieldKey(id, KEY_VULNERABLE),
				Constants.INVALID_BOOLEAN_VALUE);
		// If none of the values is incorrect, return the object
		if (lat != Constants.INVALID_FLOAT_VALUE
				&& lng != Constants.INVALID_FLOAT_VALUE
				&& radius != Constants.INVALID_FLOAT_VALUE
				&& expirationDuration != Constants.INVALID_LONG_VALUE
				&& transitionType != Constants.INVALID_INT_VALUE) {

			// Return a true ghost object
			return new Ghost(id, lat, lng, radius, expirationDuration,
					transitionType, vulnerable);
			// Otherwise, return null.
		} else {
			return null;
		}
	}

	/**
	 * Save a ghost.
	 * 
	 * @param geofence
	 *            The SimpleGeofence containing the values you want to save in
	 *            SharedPreferences
	 */
	public void saveGhost(String id, Ghost ghost) {
		/*
		 * Get a SharedPreferences editor instance. Among other things,
		 * SharedPreferences ensures that updates are atomic and non-concurrent
		 */
		Editor editor = preferences.edit();
		// Write the ghost's values to SharedPreferences
		editor.putFloat(getGhostFieldKey(id, KEY_LATITUDE),
				(float) ghost.getLatitude());
		editor.putFloat(getGhostFieldKey(id, KEY_LONGITUDE),
				(float) ghost.getLongitude());
		editor.putFloat(getGhostFieldKey(id, KEY_RADIUS), ghost.getRadius());
		editor.putLong(getGhostFieldKey(id, KEY_EXPIRATION_DURATION),
				ghost.getExpirationDuration());
		editor.putInt(getGhostFieldKey(id, KEY_TRANSITION_TYPE),
				ghost.getTransitionType());
		// Commit the changes
		editor.commit();
		ids.add(id);
	}

	public void clearGhost(String id) {
		/*
		 * Remove a flattened ghost object from storage by removing all of
		 * its keys
		 */
		Editor editor = preferences.edit();
		editor.remove(getGhostFieldKey(id, KEY_LATITUDE));
		editor.remove(getGhostFieldKey(id, KEY_LONGITUDE));
		editor.remove(getGhostFieldKey(id, KEY_RADIUS));
		editor.remove(getGhostFieldKey(id, KEY_EXPIRATION_DURATION));
		editor.remove(getGhostFieldKey(id, KEY_TRANSITION_TYPE));
		editor.commit();
		ids.remove(id);
	}
	
	public Set<String> getIds() {
		return ids;
	}

	/**
	 * Given a ghost's ID and the name of a field (for example,
	 * KEY_LATITUDE), return the key name of the object's values in
	 * SharedPreferences.
	 * 
	 * @param id
	 *            The ID of a Ghost object
	 * @param fieldName
	 *            The field represented by the key
	 * @return The full key name of a value in SharedPreferences
	 */
	private String getGhostFieldKey(String id, String fieldName) {
		return KEY_PREFIX + "_" + id + "_" + fieldName;
	}
}
