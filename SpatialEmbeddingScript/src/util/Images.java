package util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;


public class Images {
	
	public static void main(String[] args) {
		File dng = new File("E:\\stegodb_March2019\\iPhone6s-1\\originals\\iPhone6s-1_Scene-20190409-212233_RAW-00_I200_E17_o.dng");
		dngToTiff(dng, "E:\\testTiff.tif");
	}

	public static String dngValidatePath;
	
	static {
		String os = System.getProperty("os.name");
		dngValidatePath = os.startsWith("Windows")?"tools/dng_validate.exe":"tools/dng_validate";
	}
	
	public static void dngToTiff(File dng, String tif)
	{
		try
		{
			Runtime.getRuntime().exec(dngValidatePath+" -tif \""+tif+"\" \""+dng.getAbsolutePath()+"\"").waitFor();
		} catch (IOException e)
		{
			e.printStackTrace();
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
	public static BufferedImage copyImage(BufferedImage source)
	{
		return copyImage(source, source.getType());
	}
	
	public static int[] getImageDimension(File imgFile)
	{
		String ext = F.getFileExt(imgFile);
		if (ext == null)
			return null;
		
		Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(ext);
		while (iter.hasNext())
		{
			ImageReader reader = iter.next();
			try
			{
				ImageInputStream stream = new FileImageInputStream(imgFile);
			    reader.setInput(stream);
			    int width = reader.getWidth(reader.getMinIndex());
			    int height = reader.getHeight(reader.getMinIndex());
			    return new int[] {width, height};
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				reader.dispose();
			}
		}
		return null;
	}
	
	public static boolean isJPEG(File original)
	{
		String ext = original.getName().substring(original.getName().lastIndexOf(".")+1);
		return ext.equalsIgnoreCase("jpg");
	}
	
	public static boolean isDNG(File original)
	{
		String ext = original.getName().substring(original.getName().lastIndexOf(".")+1);
		return ext.equalsIgnoreCase("dng");
	}
	
	public static BufferedImage copyImage(BufferedImage source, int type)
	{
	    BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), type);
	    Graphics g = b.getGraphics();
	    g.drawImage(source, 0, 0, null);
	    g.dispose();
	    return b;
	}
	
	public static BufferedImage loadImage(String path)
	{
		return loadImage(new File(path));
	}
	
	public static BufferedImage loadImage(File f)
	{
		try
		{
			return ImageIO.read(f);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.err.println("Error reading file: " + f.getAbsolutePath().replace('\\', '/'));
		}
		return null;
	}
	
	public static void centerCrop(BufferedImage img, File out)
	{
		int x0 = img.getWidth()/2-256;
		int y0 = img.getHeight()/2-256;
		BufferedImage cropped = img.getSubimage(x0, y0, 512, 512);
		saveImage(cropped, "png", out);
	}
	
	public static BufferedImage scale(File original, File saveTo)
	{
		return scale(loadImage(original), saveTo);
	}
	
	public static BufferedImage scale(BufferedImage original, File saveTo)
	{
		BufferedImage outputImage = scale(original, 0.05);
		
		if (saveTo != null)
			Images.saveImage(outputImage, saveTo.getName().substring(saveTo.getName().lastIndexOf(".")+1), saveTo);
		
		return outputImage;
	}
	
	public static BufferedImage scale(BufferedImage source,double ratio)
	{
		  int w = (int) (source.getWidth() * ratio);
		  int h = (int) (source.getHeight() * ratio);
		  BufferedImage bi = getCompatibleImage(w, h);
		  Graphics2D g2d = bi.createGraphics();
		  double xScale = (double) w / source.getWidth();
		  double yScale = (double) h / source.getHeight();
		  AffineTransform at = AffineTransform.getScaleInstance(xScale,yScale);
		  g2d.drawRenderedImage(source, at);
		  g2d.dispose();
		  return bi;
		}

	private static BufferedImage getCompatibleImage(int w, int h) {
		  GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		  GraphicsDevice gd = ge.getDefaultScreenDevice();
		  GraphicsConfiguration gc = gd.getDefaultConfiguration();
		  BufferedImage image = gc.createCompatibleImage(w, h);
		  return image;
		}
	
	public static boolean checkReadability(File f)
	{
		try {
			ImageIO.read(f);
		}
		catch (Exception e)
		{
			System.err.println("Unreadable Image: " + f.getAbsolutePath());
			return false;
		}
		return true;
	}
	
	public static void saveImage(BufferedImage image, String format, File out)
	{
		try
		{
			ImageIO.write(image, format, out);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.err.println("Error when saving file: " + out.getAbsolutePath());
		}
	}
	
	public static boolean compareRGB(File f1, File f2) {
		BufferedImage img1 = Images.loadImage(f1);
		BufferedImage img2 = Images.loadImage(f2);
		
		if (img1.getWidth()!=img2.getWidth())
			return false;
		if (img1.getHeight()!=img2.getHeight())
			return false;
		for (int x = 0; x < img1.getWidth(); x++)
		{
			for (int y = 0; y < img1.getHeight(); y++)
			{
				int p1 = img1.getRGB(x, y);
				int p2 = img2.getRGB(x, y);
				Color c1 = new Color(p1, true);
				Color c2 = new Color(p2, true);
				if (!c1.equals(c2))
					return false;
			}
		}
		return true;
	}
	
	
}
