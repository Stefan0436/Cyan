package org.asf.cyan.api.packet.entries;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.asf.cyan.api.packet.PacketEntry;

public class BooleanEntry implements PacketEntry<Boolean> {

	private boolean val;

	BooleanEntry() {

	}

	public BooleanEntry(boolean value) {
		val = value;
	}

	@Override
	public byte type() {
		return 9;
	}

	@Override
	public boolean isCompatible(byte type) {
		return type == type();
	}

	@Override
	public Boolean get() {
		return val;
	}

	@Override
	public void transfer(OutputStream destination) throws IOException {
		destination.write(val ? 0 : 1);
	}

	@Override
	public PacketEntry<Boolean> importStream(InputStream source) throws IOException {
		return new BooleanEntry(source.read() == 1 ? false : true);
	}

}
