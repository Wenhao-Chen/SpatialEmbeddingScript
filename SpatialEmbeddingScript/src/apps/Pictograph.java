package apps;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import util.Images;
import util.P;

public class Pictograph extends AppTemplate {
	
	private BufferedImage stego;
	private int x, y;

	public Pictograph(BufferedImage image, String inputPath) { super(image, inputPath); }
	public Pictograph(File f) { super(f); }

	@Override
	public String getFullName() { return "Pictograph"; }

	@Override
	public String getAbbrName() { return "PG"; }

	@Override
	public String getOutputFormat() { return "png"; }

	@Override
	public int getCapacity() { return image.getWidth()*image.getHeight()*2; }

	@Override
	public int getSignatureLengthInBytes() { return 18; }

	@Override
	public boolean requirePassword() { return false; }
	
	@Override
	public void embed(String message, File out) {
		stego = Images.copyImage(image, BufferedImage.TYPE_INT_RGB);
		x = y = embedded = changed = 0;
		boolean[] payload = getPayload(message);
		
		for (int i = 0; i < payload.length/2; i++)
		{
			boolean bit1 = payload[i*2];
			boolean bit2 = payload[i*2+1];
			int twobits = 0;
			if (bit1)
				twobits+=2;
			if (bit2)
				twobits+=1;
			
			int rgb = stego.getRGB(x, y);
			Color c = new Color(rgb);
			int r = c.getRed();
			int g = c.getGreen();
			int b = c.getBlue();
			
			int newG = (g&0xfc) + twobits;
			Color newC = new Color(r, newG, b);
			
			stego.setRGB(x, y, newC.getRGB());
			embedded+=2;
			recordChange(g, newG);
			
			x++;
			if (x >= stego.getWidth()) {
				x = 0;
				y++;
			}
		}
		Images.saveImage(stego, "png", out);
	}
	
	private boolean[] getPayload(String message) {
		boolean[] sigs = getArray(0, 16);
		boolean[] length_p = getArray(message.length()*8, 64);
		boolean[] length_i = getArray(0, 64);
		List<Boolean> messages = toArray(message);
		List<Boolean> payload = new ArrayList<>();
		for (boolean b : sigs)
			payload.add(b);
		for (boolean b: length_p)
			payload.add(b);
		for (boolean b : length_i)
			payload.add(b);
		for (boolean b : messages)
			payload.add(b);
		boolean[] r = new boolean[payload.size()];
		for (int i = 0; i < r.length; i++)
			r[i] = payload.get(i);
		return r;
	}
	
	private List<Boolean> toArray(String m) {
		byte[] bytes = m.getBytes();
		List<Boolean> result = new ArrayList<>();
		for (byte b : bytes)
		{
			boolean[] a = getArray(b, 8);
			for (boolean bb : a)
				result.add(bb);
		}
		return result;
	}

	
	private boolean[] getArray(int l, int arrayLength) {
		boolean[] result = new boolean[arrayLength];
		String binary = P.pad(Integer.toBinaryString(l), arrayLength);
		for (int i = 0; i < binary.length(); i++)
		{
			result[i] = binary.charAt(i)=='1';
		}
		return result;
	}
	
	private void recordChange(int i1, int i2) {
        if ((i1&1) != (i2&1))
            changed++;
        if ((i1&2) != (i2&2))
            changed++;
    }

	@Override
	public String extract(String password, int inputLength) {
		x = y = 0;
		long length = -1;
		String currentByte = "";
		List<Byte> bytes = new ArrayList<>();
		while (x < image.getWidth() && y < image.getHeight()) {
			String next2Bits = extract2Bits(image, x, y);
			currentByte += next2Bits;
			if (currentByte.length()==8) {
				byte currentB = (byte)Integer.parseInt(currentByte, 2);
				bytes.add(currentB);
				// first 2 bytes should be 0.
				if (bytes.size()==2)
				{
					byte b0 = bytes.get(0);
					byte b1 = bytes.get(1);
					if (b0!=0 || b1!=0) {
						//System.err.println("first 2 bytes of payload is " + b0 +" and " + b1+". Should both be 0.");
						return null;
					}
				}
				// the next 8 bytes is length of the plaintext
				else if (bytes.size()==10)
				{
					List<Byte> lengthBytes = new ArrayList<>();
					for (int i = 2; i < 10; i++)
						lengthBytes.add(bytes.get(i));
					length = parseLength(lengthBytes)/8;
				}
				else if (length > 0  && bytes.size()==18+length)
				{
					byte[] payloadBytes = new byte[(int) length];
					for (int i = 18; i < bytes.size(); i++)
					{
						payloadBytes[i-18] = bytes.get(i);
					}
					String payload = new String(payloadBytes);
					return payload;
				}
				
				currentByte = "";
			}
			x++;
			if (x >= image.getWidth())
			{
				x = 0;
				y++;
			}
		}
		return null;
	}
	
	private static String extract2Bits(BufferedImage img, int x, int y)
	{
		Color c = new Color(img.getRGB(x, y));
		int green = c.getGreen();
		return P.pad(Integer.toBinaryString(green&3), 2, "0");
	}
	
	public static long parseLength(List<Byte> bytes)
	{
		String s = "";
		long l = 0;
		for (int i = 0; i < bytes.size(); i++)
		{
			String ss = Integer.toBinaryString(bytes.get(i));
			if (ss.length()>8)
				ss = ss.substring(ss.length()-8);
			s += P.pad(ss, 8);
		}
		l = Long.parseLong(s, 2);
		return l;
	}

}
