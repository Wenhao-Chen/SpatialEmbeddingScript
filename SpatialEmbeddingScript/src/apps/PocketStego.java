package apps;

import java.awt.image.BufferedImage;
import java.io.File;

import util.Images;

public class PocketStego extends AppTemplate {
	
	private BufferedImage stego;
	

	public PocketStego(BufferedImage image, String inputPath) {
		super(image, inputPath);
	}
	
	public PocketStego(File f) {
		super(f);
	}

	@Override
	public String getFullName()
	{
		return "PocketStego";
	}

	@Override
	public String getAbbrName()
	{
		return "PS";
	}

	@Override
	public String getOutputFormat()
	{
		return "png";
	}

	@Override
	public int getCapacity()
	{
		return image.getWidth()*image.getHeight();
	}

	@Override
	public int getSignatureLengthInBytes() {
		return 0;
	}

	@Override
	public boolean requirePassword()
	{
		return false;
	}

	@Override
	public void embed(String message, File out) {
		embedded = changed = 0;
		stego = Images.copyImage(image);
		message += '\u0000';
		int payloadLength = message.length()*8;
		int payloadBitIndex = 0;
		main: for (int x = 0; x < image.getWidth(); x++)
		for (int y = 0; y < image.getHeight(); y++) {
			int oldColor = image.getRGB(x, y);
			if (payloadBitIndex < payloadLength) {
				int color = oldColor & 0xfffffffe;
				char bits = message.charAt(payloadBitIndex/8);
				int shift = 7-payloadBitIndex%8;
				int bit = bits>>shift & 1;
				color |= bit;
				payloadBitIndex++;
				embedded++;
				if (color != oldColor)
					changed++;
				stego.setRGB(x, y, color);
			}
			else
				break main;
		}
		Images.saveImage(stego, getOutputFormat(), out);
	}

	@Override
	public String extract(String password, int inputLength) {
		int x = 0, y = 0;
		String currentByte = "";
		byte[] bytes = new byte[inputLength];
		for (int i = 0; i < bytes.length*8; i++)
		{
			currentByte += image.getRGB(x, y)&1;
			if (currentByte.length()==8) {
				bytes[i/8] = (byte)Integer.parseInt(currentByte, 2);
				currentByte = "";
			}
			
			y++;
			if (y >= image.getHeight()) {
				y = 0;
				x++;
			}
		}
		return new String(bytes);
	}

}
