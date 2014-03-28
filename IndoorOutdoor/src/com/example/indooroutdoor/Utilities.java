package com.example.indooroutdoor;

import android.os.Environment;

public class Utilities {

	// Name of shared preferences repository that stores persistent state
	public static final String SHARED_PREFERENCES = "com.example.rawlux.SHARED_PREFERENCES";

	// Key for storing the "updates requested" flag in shared preferences
	public static final String SENSOR_UPDATES_REQUESTED = "com.example.rawlux.SENSOR_UPDATES_REQUESTED";

	// For writing data to external storage
	public static boolean externalStorageAvailable = false;
	public static boolean externalStorageWriteable = false;

	public static void sanityCheckExternalStorage() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			externalStorageAvailable = externalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			externalStorageAvailable = true;
			externalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need
			// to know is we can neither read nor write
			externalStorageAvailable = externalStorageWriteable = false;
		}
	}

}
