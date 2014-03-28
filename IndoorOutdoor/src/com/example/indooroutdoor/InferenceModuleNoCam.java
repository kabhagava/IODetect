package com.example.indooroutdoor;

import java.util.HashMap;
import android.location.*;
import android.util.Log;

public class InferenceModuleNoCam {
	private static final String TAG = "DecisionManagerActivity";
	
	protected enum Decision { INDOOR, OUTDOOR, AMBIGUOUS};//decisions
	private enum Action {STILL, WALKING, RUNNING, DRIVING, AMBIGUOUS};
	private enum TimeDay {DAY, NIGHT};
	private enum Signal {GPS, WIFI, CELL};
	
	private static final int ACCURACY_THRESHOLD = 20;// strong good signal if less than 20, weak if greater
	private static final int LUX_DARK = 60;//less than 60 (including street lamps)
	private static final int LUX_SUNLIGHT= 1000;//greater than 1000
	
	HashMap<Location, Decision> recentPlaces;//Instead of location/decision, maybe store Fingerprints/decision. Then, even if person is in 
	//similar location, but decision happens to be different, we can record according to the fingerprint
	
	public InferenceModuleNoCam(){
		recentPlaces = new HashMap<Location, Decision>();
	}
	
	private Decision doAnalysis(Report unseenData){
		TimeDay d;
		if(unseenData.getTimestamp().getHours()<18 && unseenData.getTimestamp().getHours() >6){//if between 6am and 6pm
			d = TimeDay.DAY;
		}else{d = TimeDay.NIGHT;} //mainly to narrow down on what type of light ambience to expect
		Action a;
		if(unseenData.getAccelVariance()<=.5 && unseenData.getLocation().getSpeed() <=1){a = Action.STILL;}
		else if(unseenData.getAccelVariance() >=2 && unseenData.getAccelVariance() <=15 && unseenData.getLocation().getSpeed() >=1.2 && unseenData.getLocation().getSpeed()<=1.5){
			a = Action.WALKING;
		//}else if (unseenData.getLocation().getSpeed() >=1.5 && unseenData.getLocation().getSpeed()<=2.0){
			//a = Action.RUNNING;
		}else if(unseenData.getAccelVariance()>= .5 && unseenData.getLocation().getSpeed() >= 2.0){
			a = Action.DRIVING;
		}else{
			a = Action.AMBIGUOUS;
		}
		
		//confirm driving by checking signal strength
		if(a ==Action.DRIVING && unseenData.getLocation().getAccuracy() <=ACCURACY_THRESHOLD && unseenData.getProvider().equalsIgnoreCase("gps")){
			return Decision.OUTDOOR;
		}
		if(a ==Action.RUNNING){
			if(d == TimeDay.NIGHT){
				if(unseenData.getLuxValue() <=LUX_DARK && unseenData.getLocation().getAccuracy()<=ACCURACY_THRESHOLD && unseenData.getProvider().equalsIgnoreCase("gps")){
					//dark, probably running outside
					return Decision.OUTDOOR;
					//can check air pressure here too
				}else if(unseenData.getLocation().getAccuracy()>= ACCURACY_THRESHOLD){
					//probably inside house/gym on a treadmill
					return Decision.INDOOR;
				}
			}else{
				//day time running
				if(unseenData.getLuxValue()>=LUX_SUNLIGHT && unseenData.getLocation().getAccuracy() <=ACCURACY_THRESHOLD && unseenData.getProvider().equalsIgnoreCase("gps")){
					return Decision.OUTDOOR;
				}else{
					return Decision.INDOOR;
				}
			}
		}
		if(a == Action.STILL || a==Action.WALKING){
			if(d==TimeDay.NIGHT){
				//if dark, either in theater or home or outside in park maybe
				if(unseenData.getLuxValue() <= LUX_DARK){
					if(unseenData.getLocation().getAccuracy() <=ACCURACY_THRESHOLD && unseenData.getProvider().equalsIgnoreCase("gps")){
						return Decision.OUTDOOR;
					}else{
						return Decision.INDOOR;
					}
				}else if(unseenData.getLuxValue() >=LUX_DARK && unseenData.getLuxValue()<= LUX_SUNLIGHT){
					//room light
					if(unseenData.getLocation().getAccuracy()>=ACCURACY_THRESHOLD && unseenData.getProvider().equalsIgnoreCase("gps")){
						return Decision.INDOOR;
					}
				}
			}else{//day time
				if(unseenData.getLuxValue()>=LUX_SUNLIGHT && unseenData.getLocation().getAccuracy() <=ACCURACY_THRESHOLD && unseenData.getProvider().equalsIgnoreCase("gps")){
					return Decision.OUTDOOR;
				}
				else if(unseenData.getLuxValue()<=LUX_SUNLIGHT && unseenData.getLocation().getAccuracy() >=ACCURACY_THRESHOLD && unseenData.getProvider().equalsIgnoreCase("gps")){
					return Decision.INDOOR;
				}	
			}
			
		}
		return Decision.AMBIGUOUS;
		
	}
	
	protected String infer(Report userStatus){
		Log.v(TAG, "STARTING TO INFER\n");
		Decision conclusion = Decision.AMBIGUOUS;//default
		boolean found = false;
		//better for report to return Location type, since then it's easier to calculate radii and closeness
		//GET LOCATION
		Location loc = userStatus.getLocation();
		//IF LOCATION ALREADY IN HASH, JUST OUTPUT EXISTING DECISION
		for(Location eachPlace: recentPlaces.keySet()){
			float distance = eachPlace.distanceTo(loc);
			if(distance < 5){
				//consider this the same place
				Decision previousDecision = recentPlaces.get(eachPlace);
				conclusion = previousDecision;
				found = true;
				break;
			}
		}
		//IF LOCATION NOT IN HASH YET (within 5 meter radius), do analysis and put into hash
		if(!found){
			conclusion = doAnalysis(userStatus);
			recentPlaces.put(loc, conclusion);
		}
		Log.v(TAG, "INFERRED SUCCESSFULLY\n");
		
		switch(conclusion){
		case INDOOR:
			return "INDOOR";
		case OUTDOOR:
			return "OUTDOOR";
		default:
			return "AMBIGUOUS";
		}
		

		//maybe i should only put it in the hash if I've seen it a couple of times? Not just once. Need a variable to keep track of number of count

	}

}
