package edu.virginia.cs2110.ghost;

import java.util.HashSet;
import java.util.Set;

public final class Constants {

	// Invalid values, used to test geofence storage when retrieving geofences
	public static final long INVALID_LONG_VALUE = -999l;

	public static final float INVALID_FLOAT_VALUE = -999.0f;

	public static final int INVALID_INT_VALUE = -999;
	public static final boolean INVALID_BOOLEAN_VALUE = false;

	/*
	 * Constants used in verifying the correctness of input values
	 */
	public static final double MAX_LATITUDE = 90.d;

	public static final double MIN_LATITUDE = -90.d;

	public static final double MAX_LONGITUDE = 180.d;

	public static final double MIN_LONGITUDE = -180.d;

	public static final float MIN_RADIUS = 1f;

	/*
	 * Define a request code to send to Google Play services This code is
	 * returned in Activity.onActivityResult
	 */
	public final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

	// A string of length 0, used to clear out input fields
	public static final String EMPTY_STRING = new String();
	public static final Set<String> EMPTY_STRING_SET = new HashSet<String>();

	public static final CharSequence GEOFENCE_ID_DELIMITER = ",";

	// The name of the SharedPreferences
	public static final String SHARED_PREFERENCES = "SharedPreferences";
	public static final String TRANSITION_UPDATE = "TRANSITION_UPDATE";
	public static final String TRANSITION_IDS = "TRANSITION_IDS";

	/*
	 * Timing constants
	 */
	public static final int MILLISECONDS_PER_SECOND = 1000;
	public static final long SECONDS_PER_HOUR = 60;
	public static final int UPDATE_INTERVAL_IN_SECONDS = 5;
	// Update frequency in milliseconds
	public static final long UPDATE_INTERVAL = MILLISECONDS_PER_SECOND
			* UPDATE_INTERVAL_IN_SECONDS;
	public static final int FASTEST_INTERVAL_IN_SECONDS = 1;
	public static final long FASTEST_INTERVAL = MILLISECONDS_PER_SECOND
			* FASTEST_INTERVAL_IN_SECONDS;

	/*
	 * Ghost constants
	 */
	public static final long GHOST_EXPIRATION_IN_HOURS = 12;
	public static final long GHOST_EXPIRATION_TIME = GHOST_EXPIRATION_IN_HOURS
			* SECONDS_PER_HOUR * MILLISECONDS_PER_SECOND;
	public static final float GHOST_RADIUS = 50; // in meters
	public static final int GHOST_KILL_TIME = 10000; 	// in ms
	
	
	public static final double METERS_PER_DEGREE = 111111;

	public static final double BOMB_RADIUS = 50;
	
	public static final float PICKUP_RADIUS = 10;

	public static final int DIFFICULTY_EASY = 0;
	public static final int DIFFICULTY_MEDIUM = 1;
	public static final int DIFFICULTY_HARD = 2;

}
