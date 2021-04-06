package org.asf.cyan.api.packet.entries;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.asf.cyan.api.packet.PacketEntry;

public class StringEntry implements PacketEntry<String> {

	private String val;

	StringEntry() {

	}

	public StringEntry(String value) {
		val = value;
	}

	@Override
	public long length() {
		return val.getBytes().length;
	}

	@Override
	public long type() {
		return 1313230212619l;
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
		destination.write(val.getBytes());
	}

	@Override
	public PacketEntry<String> importStream(InputStream source, long amount) throws IOException {
		return new StringEntry(new String(source.readNBytes((int)amount)));
	}

}
