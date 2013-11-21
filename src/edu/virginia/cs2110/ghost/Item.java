package edu.virginia.cs2110.ghost;


import com.google.android.gms.location.Geofence;

/**
 * Based on code from
 * http://developer.android.com/training/location/geofencing.html
 * 
 */
public class Item {
	// Instance variables
	private final String id;
	private final double latitude;
	private final double longitude;
	private final float radius;
	private long expirationDuration;
	private int transitionType;

	/**
	 * @param geofenceId
	 *            The Geofence's request ID, can be up to 100 characters
	 * @param latitude
	 *            Latitude of the Geofence's center in degrees, between -90 and
	 *            +90 inclusive.
	 * @param longitude
	 *            Longitude of the Geofence's center in degrees, between -90 and
	 *            +90 inclusive.
	 * @param radius
	 *            Radius of the geofence circle in meters.
	 * @param expiration
	 *            Geofence expiration duration in milliseconds.
	 * @param transition
	 *            Type of Geofence transition.
	 * @throws IllegalArgumentException
	 *             if any parameters are out of range
	 */
	public Ghost(String geofenceId, double latitude, double longitude,
			float radius, long expiration, int transition) {
		// Set the instance fields from the constructor
		if (geofenceId.length() > 100)
			throw new IllegalArgumentException();
		this.id = geofenceId;
		if (latitude < -90 || latitude > 90)
			throw new IllegalArgumentException();
		this.latitude = latitude;
		if (longitude < -90 || longitude > 90)
			throw new IllegalArgumentException();
		this.longitude = longitude;
		this.radius = radius;
		this.expirationDuration = expiration;
		if (transitionType <= 0
				|| transitionType > (Geofence.GEOFENCE_TRANSITION_DWELL
						| Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT))
			throw new IllegalArgumentException();
		this.transitionType = transition;
	}

	// Instance field getters
	public String getId() {
		return id;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public float getRadius() {
		return radius;
	}

	public long getExpirationDuration() {
		return expirationDuration;
	}

	public int getTransitionType() {
		return transitionType;
	}
