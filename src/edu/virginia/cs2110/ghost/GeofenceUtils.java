package edu.virginia.cs2110.ghost;

/**
 * This class defines constants used by location sample apps. Taken from
 * https://
 * code.google.com/p/nononsense-notes/source/browse/NoNonsenseNotes/src/com
 * /nononsenseapps/util/GeofenceUtils.java?name=notes2013_ui&r=
 * fa81e31081460ecefc768bb1eb1acd023c5717de
 */
public final class GeofenceUtils {

	// Used to track what type of geofence removal request was made.
	public enum REMOVE_TYPE {
		INTENT, LIST
	}

	// Used to track what type of request is in process
	public enum REQUEST_TYPE {
		ADD, REMOVE
	}

	// Invalid values, used to test geofence storage when retrieving geofences
	public static final long INVALID_LONG_VALUE = -999l;

	public static final float INVALID_FLOAT_VALUE = -999.0f;

	public static final int INVALID_INT_VALUE = -999;

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

	public static final CharSequence GEOFENCE_ID_DELIMITER = ",";

}