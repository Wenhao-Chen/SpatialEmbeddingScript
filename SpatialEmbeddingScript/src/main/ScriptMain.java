package main;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import apps.AppTemplate;
import apps.MobiStego;
import apps.Pictograph;
import apps.PocketStego;
import apps.SteganographyM;
import util.Images;
import util.MessageDictionary;
import util.P;

public class ScriptMain {
	
	public static boolean validate = false;
	
	public static List<Class<? extends AppTemplate>> AllApps = Arrays.asList(
			MobiStego.class,
			Pictograph.class,
			SteganographyM.class,
			PocketStego.class
	);

	public static void main(String[] args) {
		
		if (args.length<2 || args[0].equals("h") || args[0].equals("help") || 
				args[0].equals("-h") || args[0].equals("-help")) {
			printUsage();
			return;
		}
		
		MessageDictionary.dict_dir = args[0];
		List<File> inputImages = collectInputImages(args[1]);
		File outRoot = args.length>1?new File(args[2]):new File("stego_output");
		outRoot.mkdirs();
		
		double rateMin = args.length>3?Double.parseDouble(args[3]):0.05;
		double rateMax = args.length>4?Double.parseDouble(args[4]):0.25;
		double rateInc = args.length>5?Double.parseDouble(args[5]):0.05;
		
		if (args.length>6)
			validate = args[6].equalsIgnoreCase("true");
		
		go(AllApps, inputImages, outRoot, rateMin, rateMax, rateInc);
	}
	
	
	
	public static void go(List<Class<? extends AppTemplate>> apps, List<File> inputImages, File outRoot,
						double rateMin, double rateMax, double rateInc) {
		
		P.p("--- Embedding Options ---");
		P.p("  Number of input images: "+inputImages.size());
		P.p("  Output folder: "+outRoot.getAbsolutePath());
		P.p("  Embedding rates: from "+rateMin+" to "+rateMax+", incrementing by "+rateInc);
		P.p("  Validate stegos: "+validate);
		P.p("-------\n");
		P.p("Start embedding...");
		long time = System.currentTimeMillis();
		int total = inputImages.size();
		int count = 0;
		for (File inputImage : inputImages) {
			System.out.printf("\nProcessing %d/%d: %s...\n", ++count, total, inputImage.getName());
			long imageTime = System.currentTimeMillis();
			P.p("  Loading image...");
			BufferedImage image = Images.loadImage(inputImage);
			P.p("  Embedding...");
			for (Class<? extends AppTemplate> app : apps) {
				try {
					AppTemplate appInstance = 
							app.getConstructor(BufferedImage.class, String.class)
							.newInstance(image, inputImage.getAbsolutePath());
					File dir = new File(outRoot, appInstance.getFullName());
					dir.mkdirs();
					
					appInstance.embedBatch(dir, rateMin, rateMax, rateInc);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			imageTime = System.currentTimeMillis()-imageTime;
			P.p("  Done. Time: "+convertTime(imageTime));
		}
		time = System.currentTimeMillis()-time;
		P.p("\n-- All done. Finished in "+convertTime(time));
	}
	
	private static String convertTime(long millis) {
		long hours = 0, minutes = 0, seconds = 0;
		seconds = millis/1000;
		if (seconds==0)
			return (double)millis/1000+" seconds.";
		if (seconds>60) {
			minutes = seconds/60;
			seconds %= 60;
		}
		if (minutes>60) {
			hours = minutes/60;
			minutes %= 60;
		}
		String runTime = "";
		if (hours>0)
			runTime += hours+" hours";
		if (minutes>0)
			runTime += minutes+" minutes";
		if (seconds>0)
			runTime += seconds+" seconds";
		return runTime;
	}
	
	public static List<File> collectInputImages(String path) {
		File input = new File(path);
		List<File> inputFiles = new ArrayList<>();
		if (input.isFile())
			inputFiles.add(input);
		else if (input.isDirectory()) {
			File[] files = input.listFiles();
			if (files != null)
				for (File file : files)
					if (file.isFile())
						inputFiles.add(file);
		}
		
		List<File> images = new ArrayList<>();
		for (File file : inputFiles)
			if (formatSupported(file))
				images.add(file);
		return images;
	}
	
	static Set<String> SupportedFormats = new HashSet<>(Arrays.asList(
			"png", "jpeg", "jpg", "tif", "tiff"));
	public static boolean formatSupported(File f) {
		String ext = f.getName();
		ext = ext.substring(ext.lastIndexOf(".")+1).toLowerCase();
		return SupportedFormats.contains(ext);
	}
	
	private static void printUsage() {
		String usage = 
				"Usage: java -jar SpatialScript.jar [dict_path] [input_path] [output_path] [rate_min] [rate_max] [rate_delta] [validate_after_embedding]\n" + 
				"\n" + 
				"Parameters:\n" + 
				"   dict_path: the path of the folder that contains dictionary files\n" +
				"	input_path: the path of the input image file or the folder that contains input image files\n" + 
				"	output_path (Optional): the path of the output folder to save the stego images (default: 'stego_output' folder at current dir)\n" + 
				"	rate_min (Optional): the lower bound of the embedding rates (default: 0.05)\n" + 
				"	rate_max (Optional): the upper bound of the embedding rates (default: 0.25)\n" + 
				"	rate_delta (Optional): the interval between each embedding rates (default: 0.05)\n" + 
				"	validate_after_embedding (Optional): either 'true' or 'false' (default: false). If 'true', each stego image will be validated after creation. It could be slow.\n";
		P.p(usage);
	}

}
