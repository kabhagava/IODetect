package com.example.indooroutdoor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

public class LocationModule implements LocationListener {

	// String for logging
	private static final String TAG = "LocationModule";

	// Specific to Location
	private LocationManager locationManager;
	private boolean firstTime = true;
	private static final int LOCATION_INTERVAL = 1000 * 60 * 1;// ONE MINUTE
	private static final int MIN_DISTANCE = 5;// only update location if user has moved atleast this many meters
	private Location oldLocation;
	private LocationReport report;

	public LocationModule(Context managerContext) {
		// Acquire a reference to the system Location Manager
		locationManager = (LocationManager) managerContext
				.getSystemService(Context.LOCATION_SERVICE);
	}

	protected void startSensing() {
		// Register the listener with the Location Manager to receive location
		// updates. Both GPS and Network providers
		locationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, MIN_DISTANCE, this);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				LOCATION_INTERVAL, MIN_DISTANCE, this);
	}

	protected void stopSensing() {
		locationManager.removeUpdates(this);
	}

	protected LocationReport getReport() {
		return report;
	}

	private void processNewData(Location newlocation) {
		String latestLocation = null;

		if (firstTime) {
			latestLocation = formatLatestData(newlocation);
			firstTime = false;
			oldLocation = newlocation;
		} else {
			if (isBetterLocation(newlocation, oldLocation)) {
				latestLocation = formatLatestData(newlocation);
			}
		}
		if (latestLocation != null) {// write only if the new location is better
										// than old
			writeDataToFile(latestLocation);
			setReport(newlocation);
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		processNewData(location);

	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix
	 * 
	 * @param location
	 *            The new Location that you want to evaluate
	 * @param currentBestLocation
	 *            The current Location fix, to which you want to compare the new
	 *            one
	 */
	protected boolean isBetterLocation(Location location,
			Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > LOCATION_INTERVAL;
		boolean isSignificantlyOlder = timeDelta < LOCATION_INTERVAL;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
				.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	private void setReport(Location currentLocation) {
		String prov="";
		if(currentLocation.getProvider().matches("gps")){prov="gps";}
		else if(currentLocation.getProvider().matches("network")){
			Bundle extras = currentLocation.getExtras();
			if (extras.containsKey("networkLocationType")) {
			    String type = extras.getString("networkLocationType");
			    if (type.equals("cell")) {
			        // Cell (2G/3G/LTE) is used.
			    	prov="cell";
			    } else if (type.equals("wifi")) {
			        // Wi-Fi is used.
			    	prov="wifi";
			    } else{
			    	prov=type;
			    }
			}
		}
		LocationReport locRep = new LocationReport(currentLocation, prov);
		report = locRep;
	}
	
	private void writeDataToFile(String currentReport) {
		// write data to storage
		Utilities.sanityCheckExternalStorage();
		if (Utilities.externalStorageAvailable
				&& Utilities.externalStorageWriteable) {
			File root = Environment.getExternalStorageDirectory();
			File locationDir = new File(root.getAbsolutePath()
					+ "/Android/data/com.example.rawlocation/files");
			if (!locationDir.exists()) {
				locationDir.mkdirs();
			}

			try {
				 SimpleDateFormat shortFormat = new SimpleDateFormat("yyyyMMMdd"); 
				    String day = shortFormat.format(new Date());  
				File file = new File(locationDir, "LocationData"+day+".txt");
				BufferedWriter pw = new BufferedWriter(new FileWriter(file,
						true));
				Log.v(TAG, "WRITING LOCATION DATA \n");
				pw.write("-------------\n");
				pw.write(currentReport);
				pw.write("\n");
				pw.flush();
				pw.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	
	private String formatLatestData(Location currentLocation) {
		Log.v(TAG, "LAT=" + currentLocation.getLatitude() + ", LNG= "
				+ currentLocation.getLongitude() + "\n");
		// Return the latitude and longitude as strings
		String outputData = "Time in Millis: " + currentLocation.getTime();
		/* already in milliseconds so don't need to convert it*/
																			
		outputData += "\nFormatted Time: "
				+ (new Timestamp(currentLocation.getTime())).toString();
		outputData += "\nLatitude: "
				+ Double.toString(currentLocation.getLatitude())
				+ " , Longitude: "
				+ Double.toString(currentLocation.getLongitude());
		
		if(currentLocation.getProvider() =="gps"){
			outputData += "\nProvider: gps";
			outputData += "\nSatellites: " + Integer.toString(currentLocation.getExtras().getInt("satellites"));
		}else{
			Bundle extras = currentLocation.getExtras();
			if (extras.containsKey("networkLocationType")) {
			    String type = extras.getString("networkLocationType");
			    if (type.equals("cell")) {
			        // Cell (2G/3G/LTE) is used.
			    	outputData += "\nProvider: cell";
			    } else if (type.equals("wifi")) {
			        // Wi-Fi is used.
			    	outputData += "\nProvider: wifi";
			    } else{
			    	outputData += "\nProvider: "+ type;
			    }
			}else{
				outputData += "\nProvider: "+ currentLocation.getProvider();//since there's no detail on network type
			}
		}	
		outputData += "\nAccuracy: " + currentLocation.getAccuracy();
		outputData += "\nSpeed: " + currentLocation.getSpeed();
		
		return outputData;
	}
	
	public class LocationReport {
		private Location location;
		private double latitude;
		private double longitude;
		private String provider;
		private float accuracy;
		private float speed;//need to use variance?

		public LocationReport(Location l, String prov) {
			this.setLocation(l);
			this.latitude = l.getLatitude();
			this.longitude = l.getLongitude();
			this.provider = prov;
			this.accuracy = l.getAccuracy();
			this.speed = l.getSpeed();
		}

		protected double getLatitude() {
			return latitude;
		}

		protected double getLongitude() {
			return longitude;
		}

		protected String getProvider() {
			return provider;
		}

		protected float getAccuracy() {
			return accuracy;
		}
		protected float getSpeed(){
			return speed;
		}

		protected Location getLocation() {
			return location;
		}

		private void setLocation(Location location) {
			this.location = location;
		}
	}


}
