package edu.virginia.cs2110.ghost;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.util.Log;

public class EventGenerator extends AsyncTask<MainActivity, Void, Void> {

	@Override
	protected Void doInBackground(MainActivity... args) {
		MainActivity main = args[0];
		// Open the shared preferences
		SharedPreferences prefs = main.getSharedPreferences(
				Constants.SHARED_PREFERENCES, Context.MODE_PRIVATE);
		// Get a SharedPreferences editor
		Editor editor = prefs.edit();
		// Store whether a new item/ghost was created
		boolean updated;
		for (int elapsed = 0;; elapsed++) {
			updated = false;
			if (elapsed % (60 - 20 * main.difficulty) == 0) {
				Log.d("generation", "Generating ghost");
				main.createGhost();
				updated = true;
			}
			if (elapsed % (20 * (main.difficulty + 1)) == 0) {
				Log.d("generation", "Generating bomb");
				main.createItem();
				updated = true;
			}
			if (updated)
				main.addGeofences();
			if (prefs.getBoolean(Constants.TRANSITION_UPDATE, false)) {
				Log.d("transition",
						prefs.getStringSet(Constants.TRANSITION_IDS,
								Constants.EMPTY_STRING_SET).toString());
				boolean nearGhost = false;
				List<String> bombs = new ArrayList<String>();
				List<String> money = new ArrayList<String>();
				for (String id : prefs.getStringSet(Constants.TRANSITION_IDS,
						Constants.EMPTY_STRING_SET)) {
					switch (id.charAt(0)) {
					case 'G':
						Log.d("transition", "ghost");
						nearGhost = true;
						break;
					case 'B':
						Log.d("transition", "bomb");
						bombs.add(id);
						break;
					case 'M':
						Log.d("transition", "money");
						money.add(id);
						break;
					}
				}
				if(nearGhost)
					main.proximityAlert();
				if(!bombs.isEmpty())
					main.pickupBomb(bombs);
				if(!money.isEmpty())
					main.pickupMoney(money);
				editor.putBoolean(Constants.TRANSITION_UPDATE, false);
				editor.commit();
			}

			// Wait another second
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
