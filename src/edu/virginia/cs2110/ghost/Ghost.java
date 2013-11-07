package edu.virginia.cs2110.ghost;

import com.google.android.gms.location.Geofence;

public class Ghost {
	// Instance variables
	private final String id;
	private final double latitude;
	private final double longitude;
	private final float radius;
	private long expirationDuration;
	private int transitionType;

	/**
	 * @param geofenceId
	 *            The Geofence's request ID
	 * @param latitude
	 *            Latitude of the Geofence's center.
	 * @param longitude
	 *            Longitude of the Geofence's center.
	 * @param radius
	 *            Radius of the geofence circle.
	 * @param expiration
	 *            Geofence expiration duration
	 * @param transition
	 *            Type of Geofence transition.
	 */
	public Ghost(String geofenceId, double latitude, double longitude,
			float radius, long expiration, int transition) {
		// Set the instance fields from the constructor
		this.id = geofenceId;
		this.latitude = latitude;
		this.longitude = longitude;
		this.radius = radius;
		this.expirationDuration = expiration;
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

	/**
	 * Creates a Location Services Geofence object from a SimpleGeofence.
	 * 
	 * @return A Geofence object
	 */
	public Geofence toGeofence() {
		// Build a new Geofence object
		return new Geofence.Builder().setRequestId(getId())
				.setTransitionTypes(transitionType)
				.setCircularRegion(getLatitude(), getLongitude(), getRadius())
				.setExpirationDuration(expirationDuration).build();
	}
}