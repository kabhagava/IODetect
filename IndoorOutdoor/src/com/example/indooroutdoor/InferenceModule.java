package com.example.indooroutdoor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

import android.location.*;
import android.os.Environment;
import android.util.Log;

public class InferenceModule {
	private static final String TAG = "DecisionManagerActivity";

	protected enum Decision {
		INDOOR, OUTDOOR, AMBIGUOUS
	};// decisions

	private enum Action {
		STILL, WALKING, RUNNING, DRIVING, UNKNOWN
	};

	private enum TimeDay {
		MORNING, AFTERNOON, EVENING, NIGHT
	};

	private enum Light {
		DARK, ROOM, SUNNY
	};

	private static final int LUX_DARK = 20;// less than this (including street
											// lamps)
	private static final int LUX_SUNLIGHT = 2000;// greater than 1000
	private static final double STILL_MOVEMENT = 0.2;// less than this
	private static final double WALKING_MOVEMENT = 8;// less than 8
	private static final double DRIVING_MOVEMENT = 1.5;// btwn .3 and 1.5

	private EnvironmentFingerprint currentFingerprint;
	private EnvironmentFingerprint previousFingerprint;
	private Decision previousDecision;
	private Decision currentDecision;
	private boolean firstTime;
	private Decision conclusion;

	// Fingerprints/decision.
	HashMap<EnvironmentFingerprint, Decision> visitedPlaces;// store sensor
															// states/decision

	public InferenceModule() {
		visitedPlaces = new HashMap<EnvironmentFingerprint, Decision>();
		currentDecision = Decision.AMBIGUOUS;// starting out default
		firstTime = true;
		currentFingerprint = new EnvironmentFingerprint();
		previousFingerprint = new EnvironmentFingerprint();
	}

	// ------------MAIN METHODS

	private boolean repeatingHue() {
		if (Math.abs(currentFingerprint.getColorBin()
				- previousFingerprint.getColorBin()) > 1) {
			return false;
		}
		return true;
	}

	private boolean repeatingLight() {
		if (currentFingerprint.getLight().compareTo(
				previousFingerprint.getLight()) == 0) {
			return true;
		}
		return false;
	}

	private boolean repeatingAPs() {
		return WifiModule.isSameWifiPoint(
				currentFingerprint.getWifiFingerprint(),
				previousFingerprint.getWifiFingerprint());
	}

	private Decision doAnalysis() {
		// this should be invoked once i've already figured out i'm in a new
		// place and need to decide if this place is indoor/outdoor
		if (currentFingerprint.getAction().compareTo(Action.DRIVING) == 0) {
			return Decision.OUTDOOR;
		} else if (currentFingerprint.getAction().compareTo(Action.STILL) == 0
				|| currentFingerprint.getAction() == Action.UNKNOWN) {
			if (currentFingerprint.getTime().compareTo(TimeDay.MORNING) == 0
					|| currentFingerprint.getTime()
							.compareTo(TimeDay.AFTERNOON) == 0) {
				// daytime, expect either room light or sunlight
				if (currentFingerprint.getLight().compareTo(Light.SUNNY) == 0) {
					return Decision.OUTDOOR;
				} else {
					return Decision.INDOOR;
				}
			} else if (currentFingerprint.getTime().compareTo(TimeDay.EVENING) == 0) {
				// intuitively wouldn't expect user to be STILL in the DARK in
				// the evening (too early to sleep?)
				if (currentFingerprint.getLight() != Light.ROOM) {
					return Decision.OUTDOOR;// this breaks if user's sitting in
											// dark theater, corner case
				} else {
					return Decision.INDOOR;
				}
			} else {// night
				if (currentFingerprint.getLight() != Light.SUNNY) {
					return Decision.INDOOR;
				} else {
					return Decision.OUTDOOR;
				}
			}
		} else if (currentFingerprint.getAction().compareTo(Action.WALKING) == 0
				|| currentFingerprint.getAction().compareTo(Action.RUNNING) == 0) {
			if (currentFingerprint.getTime().compareTo(TimeDay.MORNING) == 0
					|| currentFingerprint.getTime()
							.compareTo(TimeDay.AFTERNOON) == 0) {
				// daytime, expect either room light or sunlight
				if (currentFingerprint.getLight().compareTo(Light.SUNNY) == 0) {
					return Decision.OUTDOOR;
				} else {
					return Decision.INDOOR;
				}
			} else {
				if (currentFingerprint.getLight().compareTo(Light.DARK) == 0) {
					return Decision.OUTDOOR;
				} else if (currentFingerprint.getLight().compareTo(Light.SUNNY) == 0) {
					// weird reason they might be out at a stadium or something
					return Decision.OUTDOOR;
				} else {
					return Decision.INDOOR;
				}
			}
		}

		return Decision.AMBIGUOUS;

	}

	private boolean hasEnvChanged() {
		Log.v(TAG, "CHECKING IF ENVIRONMENT HAS CHANGED\n");
		// if movement or color-light has changed significantly
		System.out.println("PREVIOUS ACTION = "
				+ previousFingerprint.getAction().toString());
		System.out.println("PREVIOUS LIGHT = "
				+ previousFingerprint.getLight().toString());
		System.out.println("CURRENT ACTION = "
				+ currentFingerprint.getAction().toString());
		System.out.println("CURRENT LIGHT = "
				+ currentFingerprint.getLight().toString());

		if (previousFingerprint.getAction().compareTo(
				currentFingerprint.getAction()) != 0) {
			return true;
		}
		if (previousFingerprint.getLight().compareTo(
				currentFingerprint.getLight()) != 0) {
			return true;
		}
		if (Math.abs(previousFingerprint.getColorBin()
				- currentFingerprint.getColorBin()) > 1) {
			return true;
		}

		return false;
	}

	// ------------END MAIN METHODS
	protected String infer(Report unseenData) {
		if (firstTime) {
			Log.v(TAG, "FIRST TIME INFERENCE\n");
			setCurrentEnvironment(unseenData);
			//savePreviousPrint();// first time set current and previous to be the
								// same
			conclusion = doAnalysis();
			visitedPlaces.put(currentFingerprint, conclusion);
			firstTime = false;
			return outputDecision(conclusion);
		} else {
			savePreviousPrint();
			setCurrentEnvironment(unseenData);
			// only calculate decision if there has been significant change in
			// place/action/environment
			if (hasEnvChanged()) {
				Log.v(TAG, "CHANGE DETECTED\n");
				boolean found = false;
				// IF this place ALREADY IN HASH, JUST OUTPUT EXISTING DECISION
				for (EnvironmentFingerprint eachPlace : visitedPlaces.keySet()) {
					if (matchEnvFingerprints(currentFingerprint, eachPlace)) {
						// consider this the same place
						Decision previousDecision = visitedPlaces
								.get(eachPlace);
						conclusion = previousDecision;
						found = true;
						break;
					}
				}
				// IF COUND'T FIGURE OUT DECISION BASED ON PREVIOUSLY SAVED
				// DECISIONS THEN DO ANALYSIS and put into hash
				if (!found) {
					conclusion = doAnalysis();
					Log.v(TAG, "PUTTING THIS PLACE IN HASH\n");
					visitedPlaces.put(currentFingerprint, conclusion);
				}
				return outputDecision(conclusion);
			} else {
				// just return existing decision
				Log.v(TAG, "RECOGNIZED PREVIOUS PLACE");
				return outputDecision(previousDecision);
			}
		}
	}

	private void savePreviousPrint() {
		// cannot assign object to each other (will just end up with a pointer
		// reference, not what we want!)
		previousFingerprint.setAction(currentFingerprint.getAction());
		previousFingerprint.setTime(currentFingerprint.getTime());
		previousFingerprint.setLight(currentFingerprint.getLight());
		previousFingerprint.setColorBin(currentFingerprint.getColorBin());
		previousFingerprint.setWifiAccuracy(currentFingerprint
				.getWifiAccuracy());
		previousFingerprint.setWifiFingerprint(currentFingerprint
				.getWifiFingerprint());
		previousFingerprint.setCellTower(currentFingerprint.getCellTower());
		previousDecision = currentDecision;
	}

	private boolean matchEnvFingerprints(EnvironmentFingerprint place1,
			EnvironmentFingerprint place2) {
		if (place1.getTime().compareTo(place2.getTime()) == 0) {
			if (place1.getAction().compareTo(place1.getAction()) == 0) {
				if (place1.getLight().compareTo(place2.getLight()) == 0) {
					if (Math.abs(place1.getColorBin() - place2.getColorBin()) < 2) {
						if (WifiModule.isSameWifiPoint(
								place1.getWifiFingerprint(),
								place2.getWifiFingerprint())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private String outputDecision(Decision dec) {
		switch (dec) {
		case INDOOR:
			currentDecision = Decision.INDOOR;
			return "INDOOR";
		case OUTDOOR:
			currentDecision = Decision.OUTDOOR;
			return "OUTDOOR";
		default:
			currentDecision = Decision.AMBIGUOUS;
			return "AMBIGUOUS";
		}
	}

	private void setCurrentEnvironment(Report unseenData) {
		Log.v(TAG, "SETTING CURRENT ENVIRONMENT\n");

		// ------TIME----------------mainly to narrow down on what type of light
		// ambience to expect
		if (unseenData.getTimestamp().getHours() <= 12
				&& unseenData.getTimestamp().getHours() >= 6) {// if between 6am
																// and 12pm
			currentFingerprint.setTime(TimeDay.MORNING);
		} else if (unseenData.getTimestamp().getHours() >= 12
				&& unseenData.getTimestamp().getHours() <= 18) {// btwn 12pm-6pm
			currentFingerprint.setTime(TimeDay.AFTERNOON);
		} else if (unseenData.getTimestamp().getHours() >= 18
				&& unseenData.getTimestamp().getHours() <= 23) {// 6pm-11pm
			currentFingerprint.setTime(TimeDay.EVENING);
		} else {
			currentFingerprint.setTime(TimeDay.NIGHT);
		}

		// -----------LIGHT--------------------
		if (currentFingerprint.getTime() == TimeDay.MORNING
				|| currentFingerprint.getTime() == TimeDay.AFTERNOON) {
			if (unseenData.getLuxValue() <= LUX_DARK) {
				currentFingerprint.setLight(Light.DARK);
			} else if (unseenData.getLuxValue() >= LUX_DARK
					&& unseenData.getLuxValue() <= LUX_SUNLIGHT) {
				currentFingerprint.setLight(Light.ROOM);
			} else {
				currentFingerprint.setLight(Light.SUNNY);
			}
		} else {
			if (unseenData.getLuxAverage() <= LUX_DARK) {
				currentFingerprint.setLight(Light.DARK);
			} else if (unseenData.getLuxAverage() >= LUX_DARK
					&& unseenData.getLuxAverage() <= LUX_SUNLIGHT) {
				currentFingerprint.setLight(Light.ROOM);
			} else {
				currentFingerprint.setLight(Light.SUNNY);
			}
		}
		// =------------COLOR-------------------------------
		currentFingerprint.setColorBin(unseenData.getHueBin());
		// --------------WIFI VECTOR---------------
		currentFingerprint.setWifiFingerprint(unseenData.getCurrentAP());
		if (unseenData.getProvider().equalsIgnoreCase("wifi")) {
			currentFingerprint.setWifiAccuracy(unseenData.getAccuracy());
		}
		// ----------ACTION(STATE)--------------
		currentFingerprint.setAction(Action.UNKNOWN);// default
		if (firstTime) {
			if (unseenData.getAccelVariance() <= STILL_MOVEMENT) {
				currentFingerprint.setAction(Action.STILL);
			} else if (unseenData.getAccelVariance() >= STILL_MOVEMENT
					&& unseenData.getAccelVariance() <= DRIVING_MOVEMENT) {
				currentFingerprint.setAction(Action.DRIVING);
			} else if (unseenData.getAccelVariance() >= DRIVING_MOVEMENT
					&& unseenData.getAccelVariance() <= WALKING_MOVEMENT) {
				currentFingerprint.setAction(Action.WALKING);
			} else if (unseenData.getAccelVariance() >= WALKING_MOVEMENT) {
				currentFingerprint.setAction(Action.RUNNING);
			}
		} else {
			if (unseenData.getAccelVariance() <= STILL_MOVEMENT) {
				if (repeatingHue() && repeatingLight()) {
				//	if (repeatingAPs() && previousFingerprint.getAction()==Action.DRIVING){//this choice just added march 5th
				//		currentFingerprint.setAction(Action.DRIVING);
						
					//}else 
					if (!repeatingAPs()
							&& previousFingerprint.getAction() == Action.DRIVING) {// used
						// to
						// be
						// OR
						currentFingerprint.setAction(Action.DRIVING);
					}else if(repeatingAPs()){
						currentFingerprint.setAction(Action.STILL);
					}
				}
			} else if (unseenData.getAccelVariance() >= STILL_MOVEMENT
					&& unseenData.getAccelVariance() <= DRIVING_MOVEMENT) {
				if (repeatingHue() || repeatingLight() && !repeatingAPs()) {// used
																			// to
																			// be
																			// hue
																			// OR
																			// light

					// if(previousFingerprint.getAction()==Action.WALKING){
					// currentFingerprint.setAction(Action.WALKING);
					// }else
					currentFingerprint.setAction(Action.DRIVING);
				} else if (repeatingAPs()) {
					if(previousFingerprint.getAction()==Action.DRIVING){//just so driving won't get mistaken as walking
						currentFingerprint.setAction(Action.DRIVING);
					}else{
						currentFingerprint.setAction(Action.WALKING);// slower
					}												// walking
																	// mistaken
																	// as
																	// driving
				}
			} else if (unseenData.getAccelVariance() >= DRIVING_MOVEMENT
					&& unseenData.getAccelVariance() <= WALKING_MOVEMENT) {
				currentFingerprint.setAction(Action.WALKING);
			} else if (unseenData.getAccelVariance() >= WALKING_MOVEMENT) {
				currentFingerprint.setAction(Action.RUNNING);
			}
		}
		writeActionToFile();
	}

	private void writeActionToFile() {
		Utilities.sanityCheckExternalStorage();
		if (Utilities.externalStorageAvailable
				&& Utilities.externalStorageWriteable) {
			File root = Environment.getExternalStorageDirectory();
			File locationDir = new File(root.getAbsolutePath()
					+ "/Android/data/com.example.action/files");
			if (!locationDir.exists()) {
				locationDir.mkdirs();
			}

			try {
				SimpleDateFormat shortFormat = new SimpleDateFormat("yyyyMMMdd");
				String day = shortFormat.format(new Date());

				File file = new File(locationDir, "ActionData" + day + ".txt");
				BufferedWriter pw = new BufferedWriter(new FileWriter(file,
						true));
				pw.write("-------------\n");
				long timeInMillis = (new Date()).getTime()
						+ (System.nanoTime() - System.nanoTime()) / 1000000L;// converting
																				// nanoseconds
																				// to
																				// milliseconds
				String fullLog = "Time: "
						+ (new Timestamp(timeInMillis)).toString();
				fullLog += "\n";
				pw.write(fullLog);

				pw.write(currentFingerprint.getAction().toString());
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

	public class EnvironmentFingerprint {

		private TimeDay time;
		private Light light;// not able to encapsulate field not sure why
		private Action action;
		private int color;
		private Vector wifiFingerprint;
		private double wifiAccuracy;
		private int cellTower;

		public EnvironmentFingerprint() {

		}

		public void setTime(TimeDay t) {
			this.time = t;
		}

		public TimeDay getTime() {
			return time;
		}

		public void setLight(Light l) {
			this.light = l;
		}

		public Light getLight() {
			return light;
		}

		public void setAction(Action a) {
			this.action = a;
		}

		public Action getAction() {
			return action;
		}

		public void setColorBin(int c) {
			this.color = c;
		}

		public int getColorBin() {
			return color;
		}

		private Vector getWifiFingerprint() {
			return wifiFingerprint;
		}

		private void setWifiFingerprint(Vector wifiFingerprint) {
			this.wifiFingerprint = wifiFingerprint;
		}

		private double getWifiAccuracy() {
			return wifiAccuracy;
		}

		private void setWifiAccuracy(double wifiAccuracy) {
			this.wifiAccuracy = wifiAccuracy;
		}

		private int getCellTower() {
			return cellTower;
		}

		private void setCellTower(int cellTower) {
			this.cellTower = cellTower;
		}
	}

}
