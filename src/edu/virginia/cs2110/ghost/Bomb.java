package edu.virginia.cs2110.ghost;

public class Bomb extends Item {

	private double blastRadius;

	public Bomb(String geofenceId, double latitude, double longitude,
			float radius, long expiration, int transition) {
		super(geofenceId, latitude, longitude, radius, expiration, transition);
		// TODO Auto-generated constructor stub
	}

	// feed constructor current location of user.
	public Bomb(String geofenceId, double latitude, double longitude,
			float radius, long expiration, int transition, double blastRadius) {
		super(geofenceId, latitude, longitude, radius, expiration, transition);
		this.setBlastRadius(blastRadius);
		// TODO Auto-generated constructor stub
	}

	public GhostStore explode(GhostStore mGhosts, double myLat, double myLong) {
		for (String id : mGhosts.getIds()) {
			Ghost g = mGhosts.getGhost(id);
			double distance = Math.pow(
					Math.pow(Math.abs(g.getLatitude() - myLat), 2)
							+ Math.pow(Math.abs(g.getLongitude() - myLong), 2),
					0.5);
			if ( distance <= blastRadius) {
				mGhosts.clearGhost(id);
				/**
				 * need to remove ghost from the map
				 * need to set blastRadius
				 */
				
			}
		}

		return mGhosts;

	}

	public double getBlastRadius() {
		return blastRadius;
	}

	public void setBlastRadius(double blastRadius) {
		this.blastRadius = blastRadius;
	}

}
