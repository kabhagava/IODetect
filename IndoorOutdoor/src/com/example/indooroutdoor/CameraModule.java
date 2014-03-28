package com.example.indooroutdoor;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.highgui.Highgui;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;

import java.util.List;

public class CameraModule {
	// String for logging
	private static final String TAG = "CameraModule";
	private CameraReport report;

	// Specific to Camera
	Camera mCamera;
	Camera.PictureCallback mCall;

	// interval for snapping photos
	private final ScheduledExecutorService camScheduler = Executors
			.newScheduledThreadPool(1);
	private ScheduledFuture snapHandle;

	// for image processing OpenCV stuff
	private BaseLoaderCallback mOpenCVCallBack;

	protected enum HueColor {
		Red, Orange, Yellow, YellowGreen, Green, GreenCyan, Cyan, CyanBlue, Blue, Violet, Magenta, WhiteGrayBlack
	};

	private Mat hls;
	private Mat src;
	private int hBins = 12;// hue ranges 0-30,30-60,60-90......150-179
	private List<Mat> hlsChannels;
	private HashMap histInput;
	private Mat theHueMatrix;
	private Mat theLightMatrix;
	private int hRange = 179;
	private int binWithMostPixels;
	private int mostPixels;
	private int numPix;
	private int pixelValue;
	private int correspondingBin;
	private int previousBinVal;
	private byte buff[];
	private byte buffLum[];
	// private double buff[];
	// private double buffLum[];
	private double intermediate;
	private Iterator itr;
	private int backCameraId;
	private SurfaceView view;
	private Context thisContext;

	public CameraModule(Context managerContext) {
		this.thisContext = managerContext;
		// CAMERA callback
		mCall = new Camera.PictureCallback() {

			public void onPictureTaken(byte[] data, Camera camera) {
				Log.v(TAG, "Successfully captured Photo");

				if (data != null) {
					writeImageToFile(data);// this calls another method to
											// process the Hue
				} else {
					Log.v(TAG, "OMG DATA IS NULL WTF");
				}
			}
		};

		mOpenCVCallBack = new BaseLoaderCallback(managerContext) {
			@Override
			public void onManagerConnected(int status) {
				switch (status) {
				case LoaderCallbackInterface.SUCCESS: {
					Log.i(TAG, "OpenCV loaded successfully");
					// now can safely do stuff with opencv
					initImgProcVars();
				}
					break;
				default: {
					super.onManagerConnected(status);
				}
					break;
				}
			}
		};

		if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8,
				managerContext, mOpenCVCallBack)) {
			Log.e(TAG, "Cannot connect to OpenCV Manager");
		}

		backCameraId = findBackFacingCamera();
		view = new SurfaceView(thisContext);
	}

	private void initImgProcVars() {
		report = new CameraReport();
		hls = new Mat();
		hlsChannels = new ArrayList<Mat>();
		histInput = new HashMap<Integer, Integer>();
		buff = new byte[1920000];
		buffLum = new byte[1920000];

	}

	private void writeImageToFile(byte[] data) {
		// first create file path in externals storage
		FileOutputStream outStream = null;
		try {
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
					.format(new Date());
			String imageFileName = timeStamp;
			// create desired directory if it doesn't exist yet
			File root = Environment.getExternalStorageDirectory();
			File storageDir = new File(root.getAbsolutePath()
					+ "/Android/data/com.example.rawCamera/files");
			if (!storageDir.exists()) {
				storageDir.mkdirs();
			}

			outStream = new FileOutputStream(root.getAbsolutePath()
					+ "/Android/data/com.example.rawCamera/files/"
					+ imageFileName + ".jpg");
			// write the actual image data
			outStream.write(data);
			outStream.close();
			// retrieve this written image file to process colors
			processImage(root.getAbsolutePath()
					+ "/Android/data/com.example.rawCamera/files/"
					+ imageFileName + ".jpg");
		} catch (FileNotFoundException e) {
			Log.d(TAG, e.getMessage());
		} catch (IOException e) {
			Log.d(TAG, e.getMessage());
		}
	}

	private int findBackFacingCamera() {
		int cameraId = -1;
		// Search for the front facing camera
		int numberOfCameras = Camera.getNumberOfCameras();
		for (int i = 0; i < numberOfCameras; i++) {
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(i, info);
			if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
				cameraId = i;
				Log.v(TAG, "Back Camera Id = " + cameraId);
				break;
			}
		}
		return cameraId;
	}

	protected void startSensing() {
		
		//	safeCameraOpen(backCameraId);

		// -----every 1 minute
		final Runnable snapPhotos = new Runnable() {
			public void run() {
				capturePhoto();
			}
		};
		snapHandle = camScheduler
				.scheduleAtFixedRate(snapPhotos, 1, 1, MINUTES);

	}

	protected void stopSensing() {
		// stop the fixed interval
		snapHandle.cancel(true);
		releaseCamera();
	}

	private boolean safeCameraOpen(int id) {
		boolean qOpened = false;

		try {
			releaseCamera();
			// mCamera = Camera.open(0);
			mCamera = Camera.open(id);
			qOpened = (mCamera != null);
		} catch (Exception e) {
			Log.e(TAG, "failed to open Camera");
			e.printStackTrace();
		}
		setupPreview();
		return qOpened;
	}
	
	private void setupPreview(){
		try {
			mCamera.setPreviewDisplay(view.getHolder());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Camera.Parameters params = mCamera.getParameters();  
        params.setJpegQuality(100);  
        mCamera.setParameters(params);  
		mCamera.startPreview();
	}

	private void releaseCamera() {
		if (mCamera != null) {
			mCamera.release();
			mCamera = null;
		}
	}

	private void capturePhoto() {
		Log.v(TAG, "Starting to Capture Photo");
		if(safeCameraOpen(0)){
			Log.v(TAG, "Starting to TAke Photo");
			mCamera.takePicture(null, null, mCall);
		}

	}

	private void setReport(CameraReport r) {
		this.report = r;
	}

	protected CameraReport getReport() {
		return report;
	}

	private void processImage(String imgFilename) {
		src = Highgui.imread(imgFilename);
		// Imgproc.cvtColor(src, hls, Imgproc.COLOR_RGB2HLS);
		Imgproc.cvtColor(src, hls, Imgproc.COLOR_BGR2HLS);
		Core.split(hls, hlsChannels);

		theHueMatrix = hlsChannels.get(0);
		theLightMatrix = hlsChannels.get(1);

		// copy Mat into primitive java structure, then do processing on it.
		// System.out.println("BUFF SIZE = "+ (int)
		// theHueMatrix.total()*theHueMatrix.channels());
		// System.out.println("LUM SIZE = "+ (int) theLightMatrix.total() *
		// theLightMatrix.channels());

		// buff = null;
		// buffLum = null;
		// buff = new byte[ (int) theHueMatrix.total()*theHueMatrix.channels()];
		// buffLum = new byte[(int) theLightMatrix.total() *
		// theLightMatrix.channels()];
		theHueMatrix.get(0, 0, buff);
		theLightMatrix.get(0, 0, buffLum);
		// testing-------------
		/*
		 * Utilities.sanityCheckExternalStorage(); if
		 * (Utilities.externalStorageAvailable &&
		 * Utilities.externalStorageWriteable) { File root =
		 * Environment.getExternalStorageDirectory(); File locationDir = new
		 * File(root.getAbsolutePath() +
		 * "/Android/data/com.example.rawPixels/files"); if
		 * (!locationDir.exists()) { locationDir.mkdirs(); }
		 * 
		 * try { SimpleDateFormat shortFormat = new
		 * SimpleDateFormat("yyyyMMMdd"); String day = shortFormat.format(new
		 * Date());
		 * 
		 * File file = new File(locationDir, "PixelData"+day+".txt");
		 * BufferedWriter pw = new BufferedWriter(new FileWriter(file, true));
		 * pw.write("------PROCESSING NEW IMAGE----------\n");
		 * 
		 * //--------end testing
		 */
		for (int p = 0; p < buff.length; p++) {
			// reset correspondingBin just to clear from the last pixel
			correspondingBin = 0;

			intermediate = buff[p];
			if (intermediate > 0.0) {
				pixelValue = (int) intermediate;
				// pw.write(Integer.toString(pixelValue));
				// pw.write("\n");
				correspondingBin = Math.round(((float) pixelValue * hBins)
						/ hRange);
				// correspondingBin = pixelValue/hBins;
			} else {// either black,white,gray: check Lightness matrix
				double lum = buffLum[p];
				if (lum >= 70) {
					// probably white
					correspondingBin = hBins;// 12
				} else if (lum <= 30) {
					// probably black
					correspondingBin = hBins + 1;// 13
				} else if (lum > 30 && lum < 70) {
					correspondingBin = hBins + 2;// 14 gray
				}
			}
			previousBinVal = 0;
			if (histInput.containsKey(correspondingBin)) {
				previousBinVal = (Integer) histInput.get(correspondingBin);
			}
			histInput.put(correspondingBin, previousBinVal + 1);
		}
		/*
		 * pw.flush(); pw.close(); } catch (FileNotFoundException e) {
		 * e.printStackTrace(); } catch (IOException e) { e.printStackTrace(); }
		 * }
		 */

		// now see which bin as highest number of pixels
		binWithMostPixels = 0;
		mostPixels = 0;
		itr = histInput.keySet().iterator();
		while (itr.hasNext()) {
			Integer bin = (Integer) itr.next();
			numPix = (Integer) histInput.get(bin);
			if (numPix > mostPixels) {
				mostPixels = numPix;
				binWithMostPixels = bin;
			}
		}

		report.setHue(binWithMostPixels);
		writeToFile(binWithMostPixels);
		setReport(report);

		histInput.clear();// clear so the next image can start clean
	}

	private void writeToFile(int color) {
		Utilities.sanityCheckExternalStorage();
		if (Utilities.externalStorageAvailable
				&& Utilities.externalStorageWriteable) {
			File root = Environment.getExternalStorageDirectory();
			File locationDir = new File(root.getAbsolutePath()
					+ "/Android/data/com.example.rawHue/files");
			if (!locationDir.exists()) {
				locationDir.mkdirs();
			}

			try {
				SimpleDateFormat shortFormat = new SimpleDateFormat("yyyyMMMdd");
				String day = shortFormat.format(new Date());

				File file = new File(locationDir, "HueData" + day + ".txt");
				BufferedWriter pw = new BufferedWriter(new FileWriter(file,
						true));
				pw.write("----------------\n");
				long timeInMillis = (new Date()).getTime()
						+ (System.nanoTime() - System.nanoTime()) / 1000000L;// converting
																				// nanoseconds
																				// to
																				// milliseconds
				String fullLog = "Time: "
						+ (new Timestamp(timeInMillis)).toString();
				fullLog += "\n";

				pw.write(fullLog);
				pw.write("Hue: " + Integer.toString(color));
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

	/*
	 * private HueColor translateBinToColor(int bin){ switch(bin){ case 11:
	 * System.out.println("Red"); return HueColor.Red; case 0:
	 * System.out.println("Orange"); return HueColor.Orange; case 1:
	 * System.out.println("Yellow"); return HueColor.Yellow; case 2:
	 * System.out.println("YellowGreen"); return HueColor.YellowGreen; case 3:
	 * System.out.println("Green"); return HueColor.Green; case 4:
	 * System.out.println("GreenCyan"); return HueColor.GreenCyan; case 5:
	 * System.out.println("Cyan"); return HueColor.Cyan; case 6:
	 * System.out.println("CyanBlue"); return HueColor.CyanBlue; case 7:
	 * System.out.println("Blue"); return HueColor.Blue; case 8:
	 * System.out.println("Violet"); return HueColor.Violet; case 9:
	 * System.out.println("Magenta"); return HueColor.Magenta; case 10:
	 * System.out.println("Red"); return HueColor.Red; default:
	 * System.out.println("WhiteGrayBlack"); return HueColor.WhiteGrayBlack; } }
	 */

	public class CameraReport {

		private int hue;
		private float lum;
		private float saturation;

		public CameraReport() {
			// this.hue = h;
			// this.lum = l;
			// this.saturation = s;
		}

		protected int getHue() {// returns bin number
			return hue;
		}

		protected float getLum() {
			return lum;
		}

		protected float getSaturation() {
			return saturation;
		}

		protected void setHue(int h) {
			this.hue = h;
		}

		protected void setLum(float l) {
			this.lum = l;
		}

		protected void setSaturation(float s) {
			this.saturation = s;
		}
	}
}
