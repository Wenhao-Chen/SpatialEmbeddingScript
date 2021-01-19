package util;

public class P {

	
	public static String pad(String old, int length, String fillWith)
	{
		while (old.length()<length)
		{
			old = fillWith+old;
		}
		return old;
	}
	
	public static String pad(String old, int length)
	{
		return pad(old, length, "0");
	}
	
	
	public static void p(String s) {
		System.out.println(s);
	}
	
	public static void e(String s) {
		System.err.println(s);
	}
	
	public static void pause(String s) {
		p(s);
		try {
			byte[] c = new byte[10];
			System.in.read(c);
		}catch (Exception en ) {}
	}
}
