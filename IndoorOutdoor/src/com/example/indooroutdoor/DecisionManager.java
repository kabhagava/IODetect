package com.example.indooroutdoor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.StrictMode;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import static java.util.concurrent.TimeUnit.*;

public class DecisionManager extends Activity {

	// String for logging
	private static final String TAG = "DecisionManagerActivity";
	/*
	 * Handle to SharedPreferences for this app used to keep track of whether
	 * app should be sampling or not, through the bools stored in the prefs
	 */
	SharedPreferences thePrefs;
	// Handle to a SharedPreferences editor
	SharedPreferences.Editor theEditor;

	// Handles to UI widgets (buttons and text on screen)
	private TextView lifecycleStage;
	private TextView onOffStatus;
	private boolean currentlySampling = false;// off by default
	private Button startStopButton;

	// Wake lock (to ensure that app processes keep running in the background)
	PowerManager pm;
	PowerManager.WakeLock wl;

	// SENSORS
	private AccelerationModule accelMod;
	private LuxModule luxMod;
	private LocationModule locationMod;
	private CameraModule camMod;
	private WifiModule wifiMod;
	private CellInfoModule cellMod;
	
	//Inference Module
	private InferenceModule infMod;

	// for pulling sensor data
	private Report latestReport;
	private final ScheduledExecutorService scheduler = Executors
			.newScheduledThreadPool(1);
	private ScheduledFuture pullerHandle;
	private StrictMode.ThreadPolicy myoldpolicy;

	// --------------------------------------

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "ENTERING LIFECYCLE CREATING\n");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_decision_manager);

		// Open Shared Preferences
		thePrefs = getSharedPreferences(Utilities.SHARED_PREFERENCES,
				Context.MODE_PRIVATE);
		// Get an editor
		theEditor = thePrefs.edit();

		lifecycleStage = (TextView) findViewById(R.id.lifecycleStage);
		lifecycleStage.setText("Current Lifecycle: CREATE");
		onOffStatus = (TextView) findViewById(R.id.onOffStatus);

		// app will only start sampling via this start/stop button
		startStopButton = (Button) findViewById(R.id.startStopButton);
		startStopButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Perform action on click
				if (currentlySampling) {
					Log.v(TAG, "CLICKING STOP\n");
					stopSamplingButton();
				} else {
					Log.v(TAG, "CLICKING START\n");
					startSamplingButton();
				}
			}
		});

		/* INSTANTIATE SENSOR CLASSES HERE */
		luxMod = new LuxModule(this);
		accelMod = new AccelerationModule(this);
		locationMod = new LocationModule(this);
		camMod = new CameraModule(this);
		infMod = new InferenceModule();
		wifiMod = new WifiModule(this);
		cellMod = new CellInfoModule(this);

		// instantiate the wake lock
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
				| PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);

		checkButtons();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.decision_manager, menu);
		return true;
	}

	/*
	 * Called when the Activity is restarted, even before it becomes visible.
	 */
	@Override
	public void onStart() {
		super.onStart();
		Log.v(TAG, "LIFECYCLE STARTING \n");
		lifecycleStage.setText("Current Lifecycle: START");
	}

	/*
	 * Called when the system detects that this Activity is now visible.
	 */
	@Override
	public void onResume() {
		super.onResume();
		Log.v(TAG, "LIFECYCLE RESUMING\n");
		lifecycleStage.setText("Current Lifecycle: RESUME");

		// if no setting, add it into prefs
		if (!thePrefs.contains(Utilities.SENSOR_UPDATES_REQUESTED)) {
			theEditor.putBoolean(Utilities.SENSOR_UPDATES_REQUESTED, false);// default
																			// to
																			// false
																			// if
																			// nothing
			theEditor.commit();
		}
		// If the app already has a setting for getting sensor updates,get it
		currentlySampling = thePrefs.getBoolean(
				Utilities.SENSOR_UPDATES_REQUESTED, false);
		checkButtons();
	}

	/*
	 * Called when the Activity is going into the background. Parts of the UI
	 * may be visible, but the Activity is inactive.
	 */
	@Override
	public void onPause() {
		Log.v(TAG, "PAUSING\n");
		lifecycleStage.setText("Current Lifecycle: PAUSE");
		// Save the current setting for updates, so that it knows what state to
		// be in when resuming
		theEditor.putBoolean(Utilities.SENSOR_UPDATES_REQUESTED,
				currentlySampling);
		theEditor.commit();
		checkButtons();

		super.onPause();
	}

	/*
	 * Called when the Activity is no longer visible at all. Stop updates and
	 * disconnect.
	 */
	@Override
	public void onStop() {
		Log.v(TAG, "LIFECYCLE STOPPING \n");
		lifecycleStage.setText("Current Lifecycle: STOP");
		// Save the current setting for updates, so that it knows what state to
		// be in when resuming
		theEditor.putBoolean(Utilities.SENSOR_UPDATES_REQUESTED,
				currentlySampling);
		theEditor.commit();
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy(); // Always call the superclass
		Log.v(TAG, "LIFECYCLE DESTROYING \n");
	}

	public void startSamplingButton() {
		myoldpolicy=StrictMode.allowThreadDiskWrites();
		
		
		
		Log.v(TAG, "STARTING DETECTING\n");
		currentlySampling = true;
		// start sensors
		luxMod.startSensing();
		accelMod.startSensing();
		locationMod.startSensing();
		camMod.startSensing();
		wifiMod.startSensing();
		cellMod.startSensing();

		checkButtons();
		// acquire wake lock
		if (!wl.isHeld()) {
			wl.acquire();
		}
		// Save the current setting for updates, so that it knows what state to
		// be in when resuming
		theEditor.putBoolean(Utilities.SENSOR_UPDATES_REQUESTED,
				currentlySampling);
		theEditor.commit();

		// every 5 mins, pull sensor data reports
		// definitions for puller thread scheduling

		final Runnable puller = new Runnable() {
			public void run() {
				
				makeDecision();
			}
		};
		pullerHandle = scheduler.scheduleAtFixedRate(puller, 2, 1, MINUTES);//every minute

	}

	public void stopSamplingButton() {
		Log.v(TAG, "STOPPING DETECTION\n");
		currentlySampling = false;
		checkButtons();
		// stop sensors
		luxMod.stopSensing();
		accelMod.stopSensing();
		locationMod.stopSensing();
		camMod.stopSensing();
		wifiMod.stopSensing();
		cellMod.stopSensing();

		// release wake lock
		if (wl.isHeld()) {
			wl.release();
		}
		// Save the current setting for updates, so that it knows what state to
		// be in when resuming
		theEditor.putBoolean(Utilities.SENSOR_UPDATES_REQUESTED,
				currentlySampling);
		theEditor.commit();

		// stop pulling sensor data reports
		pullerHandle.cancel(true);
		
		StrictMode.setThreadPolicy(myoldpolicy);
	}

	private void checkButtons() {
		if (currentlySampling) {
			startStopButton.setText("Stop Detection");
		} else {
			startStopButton.setText("Start Detection");
		}
		onOffStatus.setText(((currentlySampling) ? "ON" : "OFF"));
	}

	private void makeDecision(){
		Log.v(TAG, "STARTING TO MAKE DECISION \n");
		updateReport();
		Log.v(TAG, "UPDATED REPORT. STARTING TO INFER \n");
		String dec = infMod.infer(latestReport);
		Log.v(TAG, "FINISHED INFERRING \n");
		writeDataToFile(dec);
		Log.v(TAG, "DECISION MADE SUCCESSFULLY\n");
		
	}
	
	private void updateReport() {
		Report r = new Report(luxMod.getReport(), accelMod.getReport(),locationMod.getReport(), camMod.getReport(), wifiMod.getReport(), cellMod.getReport());
		this.latestReport = r;
	}
	
	private void writeDataToFile(String currentReport) {
		Utilities.sanityCheckExternalStorage();
		if (Utilities.externalStorageAvailable
				&& Utilities.externalStorageWriteable) {
			File root = Environment.getExternalStorageDirectory();
			File locationDir = new File(root.getAbsolutePath()
					+ "/Android/data/com.example.decision/files");
			if (!locationDir.exists()) {
				locationDir.mkdirs();
			}

			try {
			    SimpleDateFormat shortFormat = new SimpleDateFormat("yyyyMMMdd");  
			    String day = shortFormat.format(new Date());  
				
				File file = new File(locationDir, "DecisionData"+day+".txt");
				BufferedWriter pw = new BufferedWriter(new FileWriter(file,
						true));
				Log.v(TAG, "WRITING DECISION DATA \n");
				pw.write("----------------\n");
				long timeInMillis = (new Date()).getTime();
				String time = "\nFormatted Time: "+ (new Timestamp(timeInMillis)).toString();
				pw.write(time);
				pw.write("\n");
				pw.write(currentReport);
				pw.write("\n");
				if(locationMod.getReport().getAccuracy()<20){
					pw.write("ACTUAL: OUTDOOR");
				}else{
					pw.write("ACTUAL: INDOOR");
				}
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

}
