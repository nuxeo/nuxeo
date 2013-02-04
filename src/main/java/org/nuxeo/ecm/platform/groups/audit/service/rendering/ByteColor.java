package org.nuxeo.ecm.platform.groups.audit.service.rendering;
public class ByteColor{
	public static ByteColor BLACK = new ByteColor((byte) 0x00, (byte) 0x00,
			(byte) 0x00);
	public static ByteColor WHITE = new ByteColor((byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF);
	public static ByteColor GREEN = new ByteColor((byte) 0x00, (byte) 0xFF,
			(byte) 0x00);
	public static ByteColor RED = new ByteColor((byte) 0xFF, (byte) 0x00,
			(byte) 0x00);
	public static ByteColor BLUE = new ByteColor((byte) 0x00, (byte) 0x00,
			(byte) 0xFF);

	public ByteColor(byte r, byte g, byte b) {
		this.r = r;
		this.g = g;
		this.b = b;
	}
	public byte r;
	public byte g;
	public byte b;
}
