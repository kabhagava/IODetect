package com.example.indooroutdoor;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.highgui.Highgui;

import com.example.indooroutdoor.CameraModule.CameraReport;

import android.content.Context;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;
import java.util.List;

public class Scraps {

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
//	private Mat mIntermediateMat;
//	private Mat hist;
	private MatOfInt channels[];
	private MatOfInt histSize;
	private int histSizeNum = 18; // Quantize the hue to 30 levels
//	private MatOfFloat hRanges;
//	private Scalar colorsRGB[];
//	private Scalar colorsHue[];
	
	protected enum HueColor {Red,Orange,Yellow,YellowGreen,Green, GreenCyan,Cyan, CyanBlue,Blue,Violet,Magenta, WhiteGrayBlack};
	
	private Mat hls; 
	private int hBins = 12;// hue ranges 0-30,30-60,60-90......150-179
	private List<Mat> hlsChannels; 
	private Mat theHueMatrix;
	private Mat theLightMatrix;
	private HashMap histInput; 
	private int hRange = 179;
	private int binWithMostPixels;
	private int mostPixels;
	private int numPix;
	
	
	private int pixelValue;
	private int correspondingBin; 
	private int previousBinVal;
	private int numRows;
	private int numCols;
	
	
	
	
	
	
	/*
	 * Mat labels, centers; int attempts = 5; double epsilon = 0.001;
	 * 
	 * Core.kmeans(hue, 3, labels,
	 * Core.TermCriteria(Core.TermCriteria.COUNT+Core.TermCriteria.EPS, 10,
	 * epsilon), attempts, Core.KMEANS_RANDOM_CENTERS, centers);
	 */
	
	
	/* private void processImageHist(String imgFilename) {  
	  Mat src = Highgui.imread(imgFilename);
	  Mat hls = new Mat();// destination color space 
	  Imgproc.cvtColor(src, hls,Imgproc.COLOR_RGB2HLS);
	  Mat hist = new Mat();
	  Core.split(hls, hlsChannels); 
	  Mat hue =hlsChannels.get(0); 
	  Mat lum = hlsChannels.get(1); 
	  Mat sat = hlsChannels.get(2); // we compute the histogram from the 0-th channel for hue 
	  Imgproc.calcHist(Arrays.asList(hls), channels[0], new Mat(), hist, new MatOfInt(histSizeNum), new MatOfFloat(0f,180f));
	 // Imgproc.calcHist(hue,channels[0], hist, new MatOfInt(8), new MatOfFloat(0f,180f));
	  //Core.normalize(hist, hist, 0,2, Core.NORM_MINMAX, -1, new Mat());
	 
	  //Mat histNorm = hist. / (src.rows() * src.cols()); Core.MinMaxLocResult
	  Core.MinMaxLocResult result = Core.minMaxLoc(hist); // max value should contain the most-recurring Hue value. 
	  double mostOccurringHue = result.maxVal; 
	  double leastOccurringHue = result.minVal; 
	  double mostOccurringX = result.maxLoc.x; 
	  double leastOccurringX = result.minLoc.x;
	  double mostOccurringY = result.maxLoc.y; 
	  double leastOccurringY = result.minLoc.y;
	  
	  // print out for sanity checking 
	  System.out.println("MAX HUE = " +Double.toString(mostOccurringHue) +"\n"); System.out.println("MIN HUE = "
	  + Double.toString(leastOccurringHue) +"\n MAX X ="+Double.toString(mostOccurringX)+ " MIN X = "+ Double.toString(leastOccurringX)
	  + "\n MAX Y = "+Double.toString(mostOccurringY)+ ", MIN Y = "+ Double.toString(leastOccurringY));
	  
	  // setReport after processing CameraReport rep = new CameraReport();
	 // rep.setHue(mostOccurringHue); 
	  }
	  */
	 
	 
		/*private void processImageMine(String imgFilename) {
		Mat src = Highgui.imread(imgFilename);
		Imgproc.cvtColor(src, hls, Imgproc.COLOR_RGB2HLS);
		Core.split(hls, hlsChannels);
		histInput.clear();//make sure empty it after previously processed image
		System.out.println("PROCESSING IMAGE");
		
		theHueMatrix = hlsChannels.get(0);
		numRows = theHueMatrix.rows();
		numCols = theHueMatrix.cols();
		System.out.println("Num Rows = "+Integer.toString(numRows) + ", Num Cols = "+ Integer.toString(numCols));
		double[] intermediate;

		for (int i = 0; i < numRows; i++) {
			for (int j = 0; i < numCols; j++) {
					intermediate = theHueMatrix.get(i, j);
					if( intermediate!=null){
						pixelValue = (int) intermediate[0];
						correspondingBin = (pixelValue * hBins) / hRange;
						previousBinVal = 0;
						if (histInput.containsKey(correspondingBin)) {
							previousBinVal = (Integer) histInput.get(correspondingBin);
						}
						histInput.put(correspondingBin, previousBinVal + 1);
					}
				//}
			}
		}
		System.out.println("FIGUREING OUT HISTOGRAM");
		
		// now see which bin as highest number of pixels
		binWithMostPixels = 0;
		mostPixels = 0;
		Iterator itr = histInput.keySet().iterator();
		while (itr.hasNext()) {
			Integer bin = (Integer) itr.next();
			numPix = (Integer) histInput.get(bin);
			if (numPix > mostPixels) {
				mostPixels = numPix;
				binWithMostPixels = bin;
			}
		}
		CameraReport report = new CameraReport();
		report.setHue(translateBinToColor(binWithMostPixels));
		setReport(report);
		

	}
	*/
	
	
}
