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
import java.util.Iterator;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.util.Log;
import java.lang.Math;

public class AccelerationModule {

	private SensorManager sensorManager;
	private Sensor accelSensor;
	private SensorEventListener accelSensorListener;
	// String for logging
	private static final String TAG = "AccelerationModule";
	private AccelReport report;
	private float currentVariance;
	private int SLIDING_WINDOW = 15;
	private ArrayList<Double> window;
	//private int sampledTimes;//to get variance chunks of every 20 samples (sliding window)

	public AccelerationModule(Context managerContext) {
		sensorManager = (SensorManager) managerContext
				.getSystemService(Context.SENSOR_SERVICE);
		accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		accelSensorListener = new SensorEventListener() {

			@Override
			public void onAccuracyChanged(Sensor arg0, int arg1) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onSensorChanged(SensorEvent event) {
				// TODO Auto-generated method stub
				processNewData(event);
				//sampledTimes+=1;
			}

		};

		if (accelSensor == null) {
			Log.v(TAG, "No Accelerometer available on this phone!!\n");
		}
		
		window = new ArrayList<Double>();
	}

	protected void startSensing() {
		//sampledTimes = 0;
		// Register the sensor listeners to start getting updates
		sensorManager.registerListener(accelSensorListener, accelSensor,
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	protected void stopSensing() {
		sensorManager.unregisterListener(accelSensorListener, accelSensor);
	}

	protected AccelReport getReport() {
		return report;
	}

	private void processNewData(SensorEvent event) {
		updateVariance(event);
		setReport();
		String formattedData = formatLatestData(event);
		writeDataToFile(formattedData);
	}

	private void writeDataToFile(String currentReport) {
		Utilities.sanityCheckExternalStorage();
		if (Utilities.externalStorageAvailable
				&& Utilities.externalStorageWriteable) {
			File root = Environment.getExternalStorageDirectory();
			File locationDir = new File(root.getAbsolutePath()
					+ "/Android/data/com.example.rawacceleration/files");
			if (!locationDir.exists()) {
				locationDir.mkdirs();
			}

			try {
			    SimpleDateFormat shortFormat = new SimpleDateFormat("yyyyMMMdd");  
			    String day = shortFormat.format(new Date());  
				
				File file = new File(locationDir, "AccelerationData"+day+".txt");
				BufferedWriter pw = new BufferedWriter(new FileWriter(file,
						true));
				//Log.v(TAG, "WRITING ACCELERATION DATA \n");
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

	private void setReport() {
		AccelReport rep = new AccelReport (currentVariance);
		report = rep;
	}
	private void updateVariance(SensorEvent event){
		float x = event.values[0];
		float y = event.values[1];
		float z = event.values[2];
		
		double magnitude = Math.sqrt( Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2)) - 9.8;//gravity constant
		if(window.size()>= SLIDING_WINDOW){
			window.remove(0);//remove oldest in cyclic manner
		}
		window.add(magnitude);
		if(window.size() == SLIDING_WINDOW){
			currentVariance = computeVariance(window);
		}else{
			currentVariance = 0;//nothing until first window starts
		}
		
	}
	private float computeVariance(ArrayList win){
		int numMagnitudes = win.size();
		float sum=0;
		double mean = calculateMean(win);
		Iterator itr = win.iterator();
		while(itr.hasNext()){
			double eachMag = (Double)itr.next();
			double delta = Math.pow((eachMag-mean), 2);
			sum+=delta;
		}
		return (sum/numMagnitudes);
	}
	
	private double calculateMean(ArrayList l){
		Iterator itr = l.iterator();
		double sum = 0;
		while(itr.hasNext()){
			sum+=(Double)itr.next();
		}
		return (double)sum/l.size();
	}
	
	private String formatLatestData(SensorEvent event) {
		// display coordinates
		String latestMotion = Float.toString(event.values[0]) + ","
				+ Float.toString(event.values[1]) + ","
				+ Float.toString(event.values[2]);
		long timeInMillis = (new Date()).getTime()
				+ (event.timestamp - System.nanoTime()) / 1000000L;// converting
																	// nanoseconds
																	// to
																	// milliseconds
		String fullLog = "Time: " + (new Timestamp(timeInMillis)).toString();
		fullLog += "\n";
		fullLog += latestMotion;

		return fullLog;
	}

	public class AccelReport {

		//private float accelX;
		//private float accelY;
		//private float accelZ;
		
		private float variance;

		public AccelReport(float v) {
			this.variance = v;
			/*this.accelY = y;
			this.accelZ = z;
			*/
			
		}
		/*protected float getAccelX(){
			return accelX;
		}
		protected float getAccelY(){
			return accelY;
		}
		protected float getAccelZ(){
			return accelZ;
		}*/
		protected float getVariance(){
			return variance;
		}
	}

}
