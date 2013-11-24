package edu.virginia.cs2110.ghost;

import android.os.AsyncTask;

public class EventGenerator extends AsyncTask<MainActivity, Void, Void> {

	@Override
	protected Void doInBackground(MainActivity... args) {
		MainActivity main = args[0];
		boolean updated;
		for (int elapsed = 0;; elapsed++) {
			updated = false;
			if(elapsed % (60 - 20 * main.difficulty) == 0) {
				main.createGhost();
				updated = true;
			}
			if(updated)
				main.addGeofences();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
