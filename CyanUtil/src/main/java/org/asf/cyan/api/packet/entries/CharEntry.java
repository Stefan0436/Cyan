package org.asf.cyan.api.packet.entries;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.asf.cyan.api.packet.PacketEntry;

public class CharEntry implements PacketEntry<Character> {

	private Character val;

	CharEntry() {
	}

	public CharEntry(Character value) {
		val = value;
	}

	@Override
	public long length() {
		return 2;
	}

	@Override
	public long type() {
		return 115201330l;
	}

	@Override
	public boolean isCompatible(long type) {
		return type == type();
	}

	@Override
	public Character get() {
		return val;
	}

	@Override
	public void transfer(OutputStream destination) throws IOException {
		destination.write(ByteBuffer.allocate(2).putChar(val).array());
	}

	@Override
	public PacketEntry<Character> importStream(InputStream source, long amount) throws IOException {
		return new CharEntry(ByteBuffer.wrap(source.readNBytes((int)amount)).getChar());
	}

}
