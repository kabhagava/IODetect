package com.example.indooroutdoor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import android.content.Context;
import android.os.Environment;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

public class CellInfoModule {
	TelephonyManager telephony;
	CellListener phoneListener;
	//int signal_strength;
	private CellInfoReport report;
	int phoneType;

	public CellInfoModule(Context managerContext) {
		telephony = (TelephonyManager) managerContext
				.getSystemService(Context.TELEPHONY_SERVICE);
		phoneListener = new CellListener();
		phoneType = telephony.getPhoneType();
		report = new CellInfoReport();

	}

	public void startSensing() {
		telephony
				.listen(phoneListener, PhoneStateListener.LISTEN_CELL_LOCATION);// LISTEN_CELL_INFO);
	}

	public void stopSensing() {
		telephony.listen(null, PhoneStateListener.LISTEN_NONE);
	}

	CellInfoReport getReport() {
		return report;
	}

	void setReport(CellInfoReport report) {
		this.report = report;
	}

	public class CellListener extends PhoneStateListener {

		public void onCellInfoChanged(List<CellInfo> cellInfo) {
			// here we're notified that cell info has changed
			System.out.println("COMING TO CELL INFO CHANGED");
			if (cellInfo != null) {
				System.out.println("OMG CELL INFO NOT NULL YAY!");
				Iterator<CellInfo> itr = cellInfo.iterator();
				while (itr.hasNext()) {
					CellInfo cellTower = (CellInfo) itr.next();

				}
			}
		}

		public void onCellLocationChanged(CellLocation loc) {
			if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
				System.out.println("CDMA!!");
				report.setCellId( ((CdmaCellLocation) telephony.getCellLocation()).getBaseStationId());
			} else if (phoneType == TelephonyManager.PHONE_TYPE_GSM) {
				System.out.println("GSM!!");
				report.setCellId( ((GsmCellLocation) telephony.getCellLocation()).getCid());
			}
		}

	}

	public class CellInfoReport {
		int cellId;

		public CellInfoReport() {
			//set initial cell tower
			if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
				System.out.println("CDMA!!");
				setCellId( ((CdmaCellLocation) telephony.getCellLocation()).getBaseStationId());
			} else if (phoneType == TelephonyManager.PHONE_TYPE_GSM) {
				System.out.println("GSM!!");
				setCellId( ((GsmCellLocation) telephony.getCellLocation()).getCid());
			}
		}

		public void setCellId(int c) {
			cellId = c;
			writeToFile(cellId);
		}

		public int getCellId() {
			return cellId;
		}
		
	}
	
	private void writeToFile(int c){
		Utilities.sanityCheckExternalStorage();
		if (Utilities.externalStorageAvailable
				&& Utilities.externalStorageWriteable) {
			File root = Environment.getExternalStorageDirectory();
			File locationDir = new File(root.getAbsolutePath()
					+ "/Android/data/com.example.rawCell/files");
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
			String fullLog = "Time: " + (new Timestamp(timeInMillis)).toString();
			fullLog += "\n";
			pw.write(fullLog);
			
			pw.write("CellTowerId: "+ c);
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
