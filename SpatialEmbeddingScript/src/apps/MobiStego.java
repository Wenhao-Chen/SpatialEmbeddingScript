package apps;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import util.Images;
import util.P;

/**
 * MobiStego embedding process:
 * 1. cut the image into 512*512 regions lexicographically
 * 2. add signature strings to the message:
 * 		payload = "@!#" + message + "#!@"
 * 3. embed payload bits to regions in lexicographical order
 * 4. within each region: visit pixels in lexicographical order
 * 5. within each pixel: embed 6 payload bits - first 2 in R, next 2 in G, last 2 in B
 * */

public class MobiStego extends AppTemplate {
	
	private int mIndex, shiftIndex;
	private byte[] messageBytes;
	private BufferedImage stego;

	public MobiStego(File f) { super(f); }
	public MobiStego(BufferedImage image, String inputPath) {super(image, inputPath);}

	@Override
	public void embed(String message, File out) {
		embedded = changed = 0;
		messageBytes = ("@!#"+message+"#!@").getBytes(Charset.forName("UTF-8"));
		mIndex = shiftIndex = 0;
		stego = Images.copyImage(image, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < stego.getHeight(); y++)
        {
        	for (int x = 0; x < stego.getWidth(); x++)
        	{
        		int pixel = stego.getRGB(x, y);
        		Color color = new Color(pixel);
        		int oldRed = color.getRed();
        		int oldGreen = color.getGreen();
        		int oldBlue = color.getBlue();
        		
        		int newRed = oldRed;
        		int newGreen = oldGreen;
        		int newBlue = oldBlue;
        		
        		if (!allEmbedded()) {
        			newRed = oldRed&0xfc|getNext2bitsMessage();
        			recordChange(oldRed, newRed);
        		}
        		
        		if (!allEmbedded()) {
        			newGreen = oldGreen&0xfc|getNext2bitsMessage();
        			recordChange(oldGreen, newGreen);
        		}
        		
        		if (!allEmbedded()) {
        			newBlue = oldBlue&0xfc|getNext2bitsMessage();
        			recordChange(oldBlue, newBlue);
        		}
        		int newPixel = new Color(newRed, newGreen, newBlue, 0xff).getRGB();
        		stego.setRGB(x, y, newPixel);
        	}
        }
        embedded = messageBytes.length*8;
        Images.saveImage(stego, "png", out);
	}

	@Override
	public String extract(String password, int inputLength) {
		int x = 0, y = 0;
		List<Byte> bytes = new ArrayList<>();
		String currentByte = "";
		// ("@!#"+message+"#!@")
		while (x < image.getWidth() && y < image.getHeight())
		{
			currentByte += extract2BitsR(image, x, y);
			if (currentByte.length()==8)
			{
				byte currentB = (byte)Integer.parseInt(currentByte, 2);
				bytes.add(currentB);
				if (!matchHeader(bytes))
					return null;
				if (matchTail(bytes))
					return carveMessage(bytes);
				currentByte = "";
			}
			currentByte += extract2BitsG(image, x, y);
			if (currentByte.length()==8)
			{
				byte currentB = (byte)Integer.parseInt(currentByte, 2);
				bytes.add(currentB);
				if (!matchHeader(bytes))
					return null;
				if (matchTail(bytes))
					return carveMessage(bytes);
				currentByte = "";
			}
			currentByte += extract2BitsB(image, x, y);
			if (currentByte.length()==8)
			{
				byte currentB = (byte)Integer.parseInt(currentByte, 2);
				bytes.add(currentB);
				if (!matchHeader(bytes))
					return null;
				if (matchTail(bytes))
					return carveMessage(bytes);
				currentByte = "";
			}

			x++;
			if (x >= image.getWidth()) {
				x = 0;
				y++;
			}
		}
		return null;
	}

	@Override
	public String getFullName() {
		return "MobiStego";
	}

	@Override
	public String getAbbrName() {
		return "MS";
	}
	
	@Override
	public String getOutputFormat()
	{
		return "png";
	}
	
	@Override
	public int getCapacity()
	{
		// each pixel can hold 6 payload bits
		return image.getHeight()*image.getWidth()*6;
	}
	
	@Override
	public int getSignatureLengthInBytes() {
		// payload = "@!#" + message + "#!@"
		return 6;
	}
	
	@Override
	public boolean requirePassword() { return false; }
	
	private boolean allEmbedded() {
        return mIndex>=messageBytes.length;
    }
    
    private void recordChange(int i1, int i2) {
        if ((i1&1) != (i2&1))
            changed++;
        if ((i1&2) != (i2&2))
            changed++;
    }
    
    
    private byte getNext2bitsMessage() {
        byte result = messageBytes[mIndex];
        if (shiftIndex==0)
            result = (byte) ((result>>6)&0x3);
        else if (shiftIndex == 1)
            result = (byte) ((result>>4)&0x3);
        else if (shiftIndex == 2)
            result = (byte) ((result>>2)&0x3);
        else if (shiftIndex == 3)
            result = (byte) (result &0x3);
        shiftIndex++;
        if (shiftIndex>3)
        {
            mIndex++;
            shiftIndex = 0;
        }
        return result;
    }
    
    private static String carveMessage(List<Byte> bytes) {
		byte[] bb = new byte[bytes.size()-6];
		for (int i = 0; i < bb.length; i++)
		{
			bb[i] = bytes.get(i+3);
		}
		return new String(bb);
	}
	
	private static boolean matchHeader(List<Byte> bytes) {
		if (bytes.size()>2)
		{
			byte[] bb = new byte[3];
			for (int i = 0; i < 3; i++)
				bb[i] = bytes.get(i);
			String s = new String(bb);
			return s.equals("@!#");
		}
		return true;
	}
	
	private static boolean matchTail(List<Byte> bytes) {
		if (bytes.size()>6)
		{
			byte[] bb = new byte[3];
			for (int i = 0; i < 3; i++)
				bb[i] = bytes.get(bytes.size()-3+i);
			String s = new String(bb);
			return s.equals("#!@");
		}
		return false;
	}
	
	private static String extract2BitsR(BufferedImage img, int x, int y)
	{
		Color c = new Color(img.getRGB(x, y));
		return P.pad(Integer.toBinaryString(c.getRed()&3), 2);
	}
	private static String extract2BitsG(BufferedImage img, int x, int y)
	{
		Color c = new Color(img.getRGB(x, y));
		return P.pad(Integer.toBinaryString(c.getGreen()&3), 2);
	}
	private static String extract2BitsB(BufferedImage img, int x, int y)
	{
		Color c = new Color(img.getRGB(x, y));
		return P.pad(Integer.toBinaryString(c.getBlue()&3), 2);
	}
	

}
