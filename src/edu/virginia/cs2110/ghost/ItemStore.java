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
public class ItemStore {
	// Keys for flattened ghost stored in SharedPreferences
	private static final String KEY_LATITUDE = "edu.virginia.cs2110.ghost.KEY_LATITUDE";
	private static final String KEY_LONGITUDE = "edu.virginia.cs2110.ghost.KEY_LONGITUDE";
	private static final String KEY_RADIUS = "edu.virginia.cs2110.ghost.KEY_RADIUS";
	private static final String KEY_EXPIRATION_DURATION = "edu.virginia.cs2110.ghost.KEY_EXPIRATION_DURATION";
	private static final String KEY_TRANSITION_TYPE = "edu.virginia.cs2110.ghost.KEY_TRANSITION_TYPE";
	// The prefix for flattened ghost keys
	private static final String KEY_PREFIX = "edu.virginia.cs2110.ghost.KEY";
	// The SharedPreferences object in which ghosts are stored
	private final SharedPreferences preferences;
	// The Set of all the ghost ids
	private Set<String> ids;

	// Create the SharedPreferences storage with private access only
	public ItemStore(Context context) {
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
	public Item getItem(String id) {
		/*
		 * Get the latitude for the ghost identified by id, or
		 * INVALID_FLOAT_VALUE if it doesn't exist
		 */
		double lat = preferences.getFloat(getItemFieldKey(id, KEY_LATITUDE),
				Constants.INVALID_FLOAT_VALUE);
		/*
		 * Get the longitude for the ghost identified by id, or
		 * INVALID_FLOAT_VALUE if it doesn't exist
		 */
		double lng = preferences.getFloat(getItemFieldKey(id, KEY_LONGITUDE),
				Constants.INVALID_FLOAT_VALUE);
		/*
		 * Get the radius for the ghost identified by id, or
		 * INVALID_FLOAT_VALUE if it doesn't exist
		 */
		float radius = preferences.getFloat(getItemFieldKey(id, KEY_RADIUS),
				Constants.INVALID_FLOAT_VALUE);
		/*
		 * Get the expiration duration for the ghost identified by id, or
		 * INVALID_LONG_VALUE if it doesn't exist
		 */
		long expirationDuration = preferences.getLong(
				getItemFieldKey(id, KEY_EXPIRATION_DURATION),
				Constants.INVALID_LONG_VALUE);
		/*
		 * Get the transition type for the ghost identified by id, or
		 * INVALID_INT_VALUE if it doesn't exist
		 */
		int transitionType = preferences.getInt(
				getItemFieldKey(id, KEY_TRANSITION_TYPE),
				Constants.INVALID_INT_VALUE);

		// If none of the values is incorrect, return the object
		if (lat != Constants.INVALID_FLOAT_VALUE
				&& lng != Constants.INVALID_FLOAT_VALUE
				&& radius != Constants.INVALID_FLOAT_VALUE
				&& expirationDuration != Constants.INVALID_LONG_VALUE
				&& transitionType != Constants.INVALID_INT_VALUE) {

			// Return a true ghost object
			return new Item(id, lat, lng, radius, expirationDuration,
					transitionType);
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
	public void saveItem(String id, Item item) {
		/*
		 * Get a SharedPreferences editor instance. Among other things,
		 * SharedPreferences ensures that updates are atomic and non-concurrent
		 */
		Editor editor = preferences.edit();
		// Write the item's values to SharedPreferences
		editor.putFloat(getItemFieldKey(id, KEY_LATITUDE),
				(float) item.getLatitude());
		editor.putFloat(getItemFieldKey(id, KEY_LONGITUDE),
				(float) item.getLongitude());
		editor.putFloat(getItemFieldKey(id, KEY_RADIUS), item.getRadius());
		editor.putLong(getItemFieldKey(id, KEY_EXPIRATION_DURATION),
				item.getExpirationDuration());
		editor.putInt(getItemFieldKey(id, KEY_TRANSITION_TYPE),
				item.getTransitionType());
		// Commit the changes
		editor.commit();
		ids.add(id);
	}

	public void clearItem(String id) {
		/*
		 * Remove a flattened ghost object from storage by removing all of
		 * its keys
		 */
		Editor editor = preferences.edit();
		editor.remove(getItemFieldKey(id, KEY_LATITUDE));
		editor.remove(getItemFieldKey(id, KEY_LONGITUDE));
		editor.remove(getItemFieldKey(id, KEY_RADIUS));
		editor.remove(getItemFieldKey(id, KEY_EXPIRATION_DURATION));
		editor.remove(getItemFieldKey(id, KEY_TRANSITION_TYPE));
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
	 *            The ID of a Item object
	 * @param fieldName
	 *            The field represented by the key
	 * @return The full key name of a value in SharedPreferences
	 */
	private String getItemFieldKey(String id, String fieldName) {
		return KEY_PREFIX + "_" + id + "_" + fieldName;
	}
}
