package org.asf.cyan.api.packet.entries;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.asf.cyan.api.packet.PacketEntry;

public class StringEntry implements PacketEntry<String> {

	private String val;

	StringEntry() {

	}

	public StringEntry(String value) {
		val = value;
	}

	@Override
	public byte type() {
		return 8;
	}

	@Override
	public boolean isCompatible(long type) {
		return type == type();
	}

	@Override
	public String get() {
		return val;
	}

	@Override
	public void transfer(OutputStream destination) throws IOException {
		destination.write(ByteBuffer.allocate(4).putInt(val.getBytes().length).array());
		destination.write(val.getBytes());
	}

	@Override
	public PacketEntry<String> importStream(InputStream source) throws IOException {
		return new StringEntry(new String(source.readNBytes(ByteBuffer.wrap(source.readNBytes(4)).getInt())));
	}

}
