package com.example.indooroutdoor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.util.Log;

public class LuxModule {
	// String for logging
	private static final String TAG = "LightModule";

	private SensorManager sensorManager;
	private Sensor lightSensor;
	private SensorEventListener lightSensorListener;
	private static final int LIGHT_INTERVAL = 1000 * 60;// one minute
	private LuxReport report;
	private int AVERAGE_WINDOW = 10;
	private ArrayList<Integer> rollingLuxValues;
	/*
	 * to read the Lux values from internal hardware file and bring to this app
	 */
	public static Scanner st;

	public LuxModule(Context managerContext) {
		
		sensorManager = (SensorManager) managerContext
				.getSystemService(Context.SENSOR_SERVICE);
		lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		lightSensorListener = new SensorEventListener() {

			@Override
			public void onAccuracyChanged(Sensor arg0, int arg1) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onSensorChanged(SensorEvent event) {
				// TODO: Figure out how to interpret these light values
				processNewData(event);

				// also capture camera photo here
				/*
				 */
			}

		};
		if (lightSensor == null) {
			Log.v(TAG, "No Lux available on this phone!!\n");
		}
		
		rollingLuxValues = new ArrayList<Integer>();

	}

	protected void startSensing() {
		// 30 second intervals for light and camera
		sensorManager.registerListener(lightSensorListener, lightSensor,
				LIGHT_INTERVAL);
	}

	protected void stopSensing() {
		sensorManager.unregisterListener(lightSensorListener, lightSensor);
	}

	protected LuxReport getReport() {
		return report;
	}

	private void processNewData(SensorEvent event) {
		String formattedData = formatLatestData(event);
		writeDataToFile(formattedData);
	}

	private void writeDataToFile(String currentReport) {
		Utilities.sanityCheckExternalStorage();

		if (Utilities.externalStorageAvailable
				&& Utilities.externalStorageWriteable) {
			File root = Environment.getExternalStorageDirectory();
			File locationDir = new File(root.getAbsolutePath()
					+ "/Android/data/com.example.rawlux/files");
			if (!locationDir.exists()) {
				locationDir.mkdirs();
			}

			try {
				 SimpleDateFormat shortFormat = new SimpleDateFormat("yyyyMMMdd");  
				    String day = shortFormat.format(new Date());  
				File file = new File(locationDir, "LuxData"+day+".txt");
				BufferedWriter pw = new BufferedWriter(new FileWriter(file,
						true));
				pw.write("----------------\n");
				pw.write(currentReport);
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

	private void setReport(int rawLux) {
		LuxReport r = new LuxReport(rawLux);
		r.setLuxAverage(addToAverage(rawLux));
		report = r;
	}
	
	private double addToAverage(int latestLux){
		
		if(rollingLuxValues.size() <AVERAGE_WINDOW-1){
			rollingLuxValues.add(latestLux);
		}else{
			rollingLuxValues.remove(0);//shift window over
			rollingLuxValues.add(latestLux);
		}
		//compute average
		double sum = 0.0;
		for(int i=0;i<rollingLuxValues.size();i++){
			sum+= rollingLuxValues.get(i);
		}
		return (sum/rollingLuxValues.size());
		
	}
	
	private String formatLatestData(SensorEvent event) {

		// Lower lux = darker, higher lux = brighter

		long timeInMillis = (new Date()).getTime()
				+ (event.timestamp - System.nanoTime()) / 1000000L;// converting
																	// nanoseconds
																	// to
																	// milliseconds

		String fullReport = "Time in millis: " + timeInMillis;
		fullReport += "\nFormatted Time: "
				+ (new Timestamp(timeInMillis)).toString();
		fullReport += "\nAccuracy: " + Integer.toString(event.accuracy) + "\n";
		try {
			st = new Scanner(
					new File(
							"/sys/devices/virtual/lightsensor/switch_cmd/lightsensor_file_state"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (st.hasNext()) {
			int lux = st.nextInt();
			setReport(lux);
			fullReport += "Lux: " + Integer.toString(lux);
			Log.v(TAG, "WRITING LUX "+ fullReport);
		}
		st.close();
		return fullReport;
	}

	public class LuxReport {

		private int luxValue;
		private double luxAverage;

		public LuxReport(int l) {
			this.luxValue = l;
		}

		protected int getLuxValue() {
			return luxValue;
		}

		protected double getLuxAverage() {
			return luxAverage;
		}

		private void setLuxAverage(double luxAverage) {
			this.luxAverage = luxAverage;
		}
	}
}
