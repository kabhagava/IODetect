package com.example.indooroutdoor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;


public class WifiModule {

	private static final String TAG = "WifiModule";
	
	WifiManager wifi;
	List<ScanResult> results;
	WifiScanReceiver wifiReceiver;
	ScanResult bestSignal;
	Context managerContext;
	private WifiReport report;
	Vector wifiFingerprint;

	public WifiModule(Context managerContext) {
		this.managerContext = managerContext;
		wifi = (WifiManager) managerContext
				.getSystemService(Context.WIFI_SERVICE);
		if (!wifi.isWifiEnabled()) {// if wifi not enabled yet, enable it now
			wifi.setWifiEnabled(true);
		}
		wifiReceiver = new WifiScanReceiver();
		
		//to hold my data
		wifiFingerprint = new Vector<Integer>();//will hold the signal levels of each ScanResult
		report = new WifiReport();
	}

	protected void startSensing() {
		managerContext.registerReceiver(wifiReceiver, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		wifi.startScan();
	}

	protected void stopSensing() {
		managerContext.unregisterReceiver(wifiReceiver);
	}

	WifiReport getReport() {
		return report;
	}

	void setReport(WifiReport report) {
		this.report = report;
	}

	public class WifiScanReceiver extends BroadcastReceiver {

		@Override
	public void onReceive(Context arg0, Intent arg1) {
		try{	
		if(arg0!=null && arg1!=null){//to avoid weird RunTime Exception
		// TODO Auto-generated method stub
		 results = wifi.getScanResults();
		 if(results!=null && results.size()!=0){
		    bestSignal = null;
		    for (ScanResult result : results) {
		      if (bestSignal == null){
		        bestSignal = result;
		      }else if(WifiManager.compareSignalLevel(bestSignal.level, result.level) < 0){
		    	  bestSignal = result;
		      }
		      wifiFingerprint.add(result.level);
		    }
		    writeToFile(wifiFingerprint);
		    if(report==null){
		    	report = new WifiReport();
		    }
		    report.setCurrentWifiFingerprint(wifiFingerprint);
		    String message = String.format("%s networks found. %s is the strongest.",
		        results.size(), bestSignal.SSID); 
		 }
		}
		}catch(Exception e){
			Log.v(TAG, "Caught an Exception from onReceive");//atleast this will prevent it from crashing the whole app
		}
		
	}
}
	
	private void writeToFile(Vector fingerprint){
		Utilities.sanityCheckExternalStorage();
		if (Utilities.externalStorageAvailable
				&& Utilities.externalStorageWriteable) {
			File root = Environment.getExternalStorageDirectory();
			File locationDir = new File(root.getAbsolutePath()
					+ "/Android/data/com.example.rawWifi/files");
			if (!locationDir.exists()) {
				locationDir.mkdirs();
			}
		
		try {
		    SimpleDateFormat shortFormat = new SimpleDateFormat("yyyyMMMdd");  
		    String day = shortFormat.format(new Date());  
			
			File file = new File(locationDir, "WifiData"+day+".txt");
			BufferedWriter pw = new BufferedWriter(new FileWriter(file,true));
			pw.write("-------------\n");
			long timeInMillis = (new Date()).getTime()
					+ (System.nanoTime() - System.nanoTime()) / 1000000L;// converting
																		// nanoseconds
																		// to
																		// milliseconds
			String fullLog = "Time: " + (new Timestamp(timeInMillis)).toString();
			fullLog += "\n";
			pw.write(fullLog);
			
			for(int i=0;i<fingerprint.size();i++){
				pw.write(Integer.toString((Integer)fingerprint.get(i)));
				pw.write(" ");
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
	
	protected static boolean isSameWifiPoint(Vector F1, Vector F2){
		//first make sure they're the same length; else pad with zeros
		int F1OrigSize = F1.size();
		int F2OrigSize = F2.size();
		if(F1OrigSize!=F2OrigSize){
			if(F1OrigSize<F2OrigSize){
				//pad F1
				while(F1.size()<F2OrigSize){
					F1.add(0);
				}
			}else{
				//pad F2
				while(F2.size()<F1OrigSize){
					F2.add(0);
				}
				
			}
		}
		if(tanimotoCoefficient(F1,F2) >=.72){//similar Wifi Access Point
			return true;
		}
		return false;
	}
	
	private static double tanimotoCoefficient(Vector F1, Vector F2){
		double vectorProduct=0;
		double magnitudeF1NotRooted=0;
		double magnitudeF2NotRooted=0;
		
		for(int i=0;i<F1.size();i++){
			vectorProduct += (Integer)F1.get(i)* (Integer)F2.get(i);
			magnitudeF1NotRooted+= Math.pow(((Integer)F1.get(i)),2);
			magnitudeF2NotRooted+=Math.pow(((Integer)F2.get(i)),2);
		}
		
		double tanimoto = vectorProduct/(magnitudeF1NotRooted+ magnitudeF2NotRooted -vectorProduct);
		return tanimoto;
	}

	public class WifiReport{
		private Vector currentWifiFingerprint;
		
		public WifiReport(){
			
		}

		protected Vector getCurrentWifiFingerprint() {
			return currentWifiFingerprint;
		}

		protected void setCurrentWifiFingerprint(Vector currentAP) {
			this.currentWifiFingerprint = currentAP;
		}
		
	}


}
