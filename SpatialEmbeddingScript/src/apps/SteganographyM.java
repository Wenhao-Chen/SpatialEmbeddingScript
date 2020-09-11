package apps;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

import util.Images;
import util.MessageDictionary;
import util.P;

public class SteganographyM extends AppTemplate {
	
	private static final byte[] prefix = new byte[] {0x52, 0x48, 0x43, 0x50};
    private static final byte[] channelLSBs = new byte[] {0,8,16};
    public static final int MaxPasswordLength = 8;
    
    private BufferedImage stego;
    private Random rng;
    private BitSet bitSet;

	public SteganographyM(BufferedImage image, String inputPath) { super(image, inputPath); }
	public SteganographyM(File f) { super(f); }

	@Override
	public String getFullName() { return "SteganographyM"; }

	@Override
	public String getAbbrName() { return "SM"; }

	@Override
	public String getOutputFormat() { return "png"; }

	@Override
	public int getCapacity() {
		return image.getWidth()*image.getHeight()*3;
	}
	

	@Override
	public int getSignatureLengthInBytes() { return 10;}
	
	@Override
	public boolean requirePassword() { return true; }

	@Override
	public void embed(String message, File out) {
        password = MessageDictionary.randomPassword(1, SteganographyM.MaxPasswordLength);
        embed(message, password, out);
	}
	
	public void embed(String message, String password, File out) {
		if (bitSet == null)
			bitSet = new BitSet(image.getWidth()*image.getHeight());
		stego = Images.copyImage(image);
		embedded = changed = 0;
		bitSet.clear();
		rng = initRNG(password);
		embed(prefix);
        embed(getLengthBytes(message.length()));
        embed(message.getBytes());
        Images.saveImage(stego, "png", out);
	}
	
	private void embed(byte[] bytes)
    {
        for (byte b : bytes)
        {
            for (int i = 0; i < 8; i++)
            {
                int payloadBit = b>>i&1;
                int x, y, pixelIndex;
                do
                {
                    x = rng.nextInt(stego.getWidth());    // a/g/a
                    y = rng.nextInt(stego.getHeight());      // a/g/b
                    pixelIndex = x * stego.getWidth() + y;    // the original code might be faulty.
                }
                while (bitSet.get(pixelIndex));
                bitSet.set(pixelIndex);
                int oldColor = stego.getRGB(x, y);
                int channelLSB = channelLSBs[rng.nextInt(3)];

                int newColor = payloadBit==1? (1<<channelLSB)|oldColor : ((1<<channelLSB)^-0x1)&oldColor;
                stego.setRGB(x, y, newColor);
                //P.i("setting ("+x+","+y+") from " + Integer.toHexString(oldColor) + " to " + Integer.toHexString(newColor) +". Bit is " + payloadBit+". Channel is "+channelLSB);
                embedded++;
                if (newColor != oldColor)
                    changed++;
            }
        }
    }

	@Override
	public String extract(String password, int inputLength) {
		return extract(image, password, inputLength);
	}
	
	public static String extract(BufferedImage img, String password, int inputLength)
	{
		BitSet bitSet = new BitSet(img.getWidth()*img.getHeight());
		bitSet.clear();
		Random rng = initRNG(password);
		
		String currentByte = "";
		List<Byte> bytes = new ArrayList<>();
		int x,y,pixelIndex, msgLength = -1;
		while (bitSet.cardinality()<=img.getWidth()*img.getHeight())
		{
			do
            {
                x = rng.nextInt(img.getWidth());    // a/g/a
                y = rng.nextInt(img.getHeight());      // a/g/b
                pixelIndex = x * img.getWidth() + y;
            }
            while (bitSet.get(pixelIndex));
			bitSet.set(pixelIndex);
			
			int color = img.getRGB(x, y);
			int channelLSB = channelLSBs[rng.nextInt(3)];
			int bit = (color>>channelLSB)&1;
			currentByte = bit+currentByte;
			if (currentByte.length()==8)
			{
				bytes.add((byte)Integer.parseInt(currentByte, 2));
				P.p("  [new b] "+bytes.size()+" "+inputLength);
				if (bytes.size()==4 && !compare(bytes, prefix))
				{
					return null;
				}
				else if (bytes.size()==10)
				{
					List<Byte> lengthBytes = new ArrayList<>();
					for (int i = 4; i < bytes.size(); i++)
						lengthBytes.add(bytes.get(i));
					msgLength = (int) Pictograph.parseLength(lengthBytes);
				}
				else if (msgLength>0 && bytes.size()==10+msgLength)
				{
					byte[] msgBytes = new byte[msgLength];
					for (int i = 10; i < bytes.size(); i++)
					{
						msgBytes[i-10] = bytes.get(i);
					}
					return new String(msgBytes);
				}
				currentByte = "";
			}
		}
		return null;
		
	}
	
	private static boolean compare(List<Byte> b1, byte[] b2) {
		if (b1.size()!=b2.length)
			return false;
		for (int i = 0; i < b1.size(); i++)
			if (b1.get(i)!=b2[i])
				return false;
		return true;
	}
	
	private static Random initRNG(String password) {
        byte[] pwBytes = password.getBytes();
        byte[] bytes = new byte[] {0,0,0,0,0,0,0,0};
        for (int i = 0; i < pwBytes.length && i < bytes.length; i++)
        {
            bytes[i] = pwBytes[i];
        }
        int[] bitsToShift = new int[] {0x38, 0x30, 0x28, 0x20, 0x18, 0x10, 0x8, 0};
        long seed = 0;
        for (int i = 0; i < 8; i++)
        {
            long l = (long)(bytes[i] & 0xff);
            seed |= (l<<bitsToShift[i]);
        }
        return new Random(seed);
    }
	
	private byte[] getLengthBytes(int length) {
        byte[] bytes = new byte[] {0,0,0,0,0,0};
        for (int i = 5; i > 1; i--)
        {
            bytes[i] = (byte)(length>>((5-i)*8)&255);
        }

        return bytes;
    }

}
