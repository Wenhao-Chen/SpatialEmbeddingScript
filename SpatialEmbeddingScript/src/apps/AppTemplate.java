package apps;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;

import main.ScriptMain;
import util.Images;
import util.MessageDictionary;
import util.MessageDictionary.InputMessage;
import util.P;
import util.StegoStats;

public abstract class AppTemplate {

	/** The input image. Treated as cover when doing embedding.
	 	Treated as stego when doing extraction. */
	public BufferedImage image;
	
	/** Capacity: the max number of cover "locations" available for embedding*/
	public int capacity;
	
	/** Embedded: the number of payload bits actually embedded */
	public int embedded;
	
	/** Changed: the number of cover image values that are changed */
	public int changed;
	
	public String inputPath;
	
	protected String password; // leave it as null if app doesn't require password
	
	public AppTemplate(File imageFile) {
		this(Images.loadImage(imageFile), imageFile.getAbsolutePath());
	}
	
	public AppTemplate(BufferedImage image, String inputPath) {
		this.image = image;
		this.inputPath = inputPath;
		capacity = getCapacity();
	}

	
	public void embedBatch(File outDir, double from, double to, double increment) {
		embedBatch(outDir, from, to, increment, false);
	}
	
	public void embedBatch(File outDir, double from, double to, double increment, boolean redo) {
		double minRate = Math.min(from, to);
		double maxRate = Math.max(from, to);
		double inc = Math.abs(increment);
		if (minRate>1.0) {
			P.e("bad parameter: embedding rates upper bound > 1.0");
			System.exit(-1);
		}
		if (maxRate<0.01) {
			P.e("bad parameter: embedding rates lower bound < 0");
			System.exit(-1);
		}
		if (inc < 0.01) {
			P.e("bad parameter: embedding rates delta must be at least 1%");
			System.exit(-1);
		}
		
		File input = new File(inputPath);
		File cover = new File(outDir, getStegoName(input.getName(), 0)+"."+getOutputFormat());
		if (redo || !cover.exists())
			Images.saveImage(image, getOutputFormat(), cover);
		for (double rate=minRate; rate <= maxRate; rate += inc) {
			System.out.printf("    - %s with rate %.2f\n", getFullName(), rate);
			int numBytes = (int)((getCapacity()*rate)/8) - getSignatureLengthInBytes();
			InputMessage msg = MessageDictionary.randomMessage(numBytes);
			String stegoName = getStegoName(input.getName(), (int)(rate*100));
			File out = new File(outDir, stegoName+"."+getOutputFormat());
			File statsFile = new File(outDir, stegoName+".csv");
			if (redo || !out.exists() || !statsFile.exists()) {
				long time = System.currentTimeMillis();
				embed(msg.message, out);
				time = System.currentTimeMillis()-time;
				StegoStats stats = new StegoStats();
				stats.inputImageName = inputPath;
				stats.coverImageName = cover.getAbsolutePath();
				stats.stegoApp = getFullName();
				stats.capacity = getCapacity();
				stats.embedded = embedded;
				stats.embeddingRate = (float)embedded/(float)getCapacity();
				stats.changed = changed;
				stats.dictionary = msg.dictName;
				stats.dictStartLine = msg.lineIndex;
				stats.messageLength = msg.message.length();
				stats.password = requirePassword()?password:"N/A";
				stats.time = time;
				stats.saveToFile(statsFile.getAbsolutePath());
				
				if (ScriptMain.validate) {
					boolean good = validate(this.getClass(), out, statsFile);
					if (!good) {
						P.p("    Failed validation: extracted message different from embedded in stego: "+out.getAbsolutePath());
					}
				}
			}
		}
	}
	
	public String getStegoName(String coverName, int rate) {
		return coverName+"_s_"+this.getAbbrName()+"_rate-"+rate;
	}
	
	public abstract String getFullName();
	public abstract String getAbbrName();
	public abstract String getOutputFormat();
	public abstract int getCapacity();
	public abstract int getSignatureLengthInBytes();
	public abstract boolean requirePassword();
	public abstract void embed(String message, File out);
	public abstract String extract(String password, int inputLength);
	
	
	public static boolean validate(Class<? extends AppTemplate> stegoApp, File stego, File infoF) {
		Map<String, String> info = StegoStats.load(infoF);
		File dict = new File(MessageDictionary.dict_dir, info.get("Input Dictionary"));
		int startLine = Integer.parseInt(info.get("Dictionary Starting Line"));
		int inputlength = Integer.parseInt(info.get("Input Message Length"));
		
		String recordMessage = MessageDictionary.getMessage(dict, startLine, inputlength);
		String password = info.get("Password");
		return validate(stegoApp, stego, recordMessage, password, inputlength);
	}
	
	public static boolean validate(Class<? extends AppTemplate> stegoApp, File stego, String recordedMessage, String password, int inputLength) {
		try {
			AppTemplate app = stegoApp.getConstructor(File.class).newInstance(stego);
			String extractedMessage = app.extract(password, inputLength);
			return recordedMessage.equals(extractedMessage);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	
}
