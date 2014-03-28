package com.example.indooroutdoor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import com.example.indooroutdoor.AccelerationModule.AccelReport;
import com.example.indooroutdoor.CameraModule.CameraReport;
import com.example.indooroutdoor.CameraModule.HueColor;
import com.example.indooroutdoor.CellInfoModule.CellInfoReport;
import com.example.indooroutdoor.LocationModule.LocationReport;
import com.example.indooroutdoor.LuxModule.LuxReport;
import com.example.indooroutdoor.WifiModule.WifiReport;

import android.location.Location;
import android.net.wifi.ScanResult;
import android.os.Environment;

public class Report {
	
	private Date timestamp;
	private double luxValue;
	private double luxAverage;
	private float accelVariance;
	
	private Location location;
	private double latitude;
	private double longitude;
	private String provider;
	private float accuracy;
	
	private int hueBin;
	private Vector wifiFingerprint;
	
	private int cellTowerId;

	public Report(LuxReport l, AccelReport a, LocationReport loc, CameraReport cam, WifiReport wifi, CellInfoReport cell){
		this.luxValue=l.getLuxValue();
		this.setLuxAverage(l.getLuxAverage());
		this.accelVariance=a.getVariance();
		this.setLocation(loc.getLocation());
		this.latitude = loc.getLatitude();
		this.longitude = loc.getLongitude();
		this.provider = loc.getProvider();
		this.accuracy = loc.getAccuracy();
		this.hueBin= cam.getHue();
		this.wifiFingerprint = wifi.getCurrentWifiFingerprint();
		this.setCellTowerId(cell.getCellId());
		
		this.timestamp = new Date();
	}

	protected Date getTimestamp() {
		return timestamp;
	}

	private void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	protected double getLuxValue() {
		return luxValue;
	}

	private void setLuxValue(double luxValue) {
		this.luxValue = luxValue;
	}

	protected double getLatitude() {
		return latitude;
	}

	private void setLatitude(float latitude) {
		this.latitude = latitude;
	}

	protected double getLongitude() {
		return longitude;
	}

	private void setLongitude(float longitude) {
		this.longitude = longitude;
	}

	protected String getProvider() {
		return provider;
	}

	private void setProvider(String provider) {
		this.provider = provider;
	}

	protected double getAccuracy() {
		return accuracy;
	}

	private void setAccuracy(float locationAccuracy) {
		this.accuracy = locationAccuracy;
	}

	protected int getHueBin() {
		return hueBin;
	}

	private void setHueBin(int hue) {
		this.hueBin = hue;
	}

	protected float getAccelVariance() {
		return accelVariance;
	}

	private void setAccelVariance(float variance) {
		this.accelVariance = variance;
	}

	protected Location getLocation() {
		return location;
	}

	private void setLocation(Location location) {
		this.location = location;
	}

	protected Vector getCurrentAP() {
		return wifiFingerprint;
	}

	private void setCurrentAP(Vector currentAP) {
		this.wifiFingerprint = currentAP;
	}

	protected int getCellTowerId() {
		return cellTowerId;
	}

	private void setCellTowerId(int cellTowerId) {
		this.cellTowerId = cellTowerId;
	}

	protected double getLuxAverage() {
		return luxAverage;
	}

	private void setLuxAverage(double luxAverage) {
		this.luxAverage = luxAverage;
	}
	
	/*protected void writeDataToFile(){
		Utilities.sanityCheckExternalStorage();
		if (Utilities.externalStorageAvailable
				&& Utilities.externalStorageWriteable) {
			File root = Environment.getExternalStorageDirectory();
			File locationDir = new File(root.getAbsolutePath()
					+ "/Android/data/com.example.report/files");
			if (!locationDir.exists()) {
				locationDir.mkdirs();
			}
		
		try {
		    SimpleDateFormat shortFormat = new SimpleDateFormat("yyyyMMMdd");  
		    String day = shortFormat.format(new Date());  
			
			File file = new File(locationDir, "CellData"+day+".txt");
			BufferedWriter pw = new BufferedWriter(new FileWriter(file,true));
			pw.write("-------------\n");
			long timeInMillis = (new Date()).getTime()
					+ (System.nanoTime() - System.nanoTime()) / 1000000L;// converting
																		// nanoseconds
																		// to
																		// milliseconds
			String time = "Time: " + (new Timestamp(timeInMillis)).toString();
			time += "\n";
			pw.write(time);
			pw.write(formatReportData());
			pw.write("\n");
			pw.flush();
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		}
	}
	*/

}
