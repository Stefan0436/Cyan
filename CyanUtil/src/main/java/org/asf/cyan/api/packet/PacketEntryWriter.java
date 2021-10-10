package org.asf.cyan.api.packet;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.asf.cyan.api.packet.entries.BooleanEntry;
import org.asf.cyan.api.packet.entries.ByteArrayEntry;
import org.asf.cyan.api.packet.entries.ByteEntry;
import org.asf.cyan.api.packet.entries.CharEntry;
import org.asf.cyan.api.packet.entries.DoubleEntry;
import org.asf.cyan.api.packet.entries.FloatEntry;
import org.asf.cyan.api.packet.entries.IntEntry;
import org.asf.cyan.api.packet.entries.LongEntry;
import org.asf.cyan.api.packet.entries.SerializingEntry;
import org.asf.cyan.api.packet.entries.StringEntry;

/**
 * 
 * Packet building system, creates network packets.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class PacketEntryWriter {
	protected ArrayList<PacketEntry<?>> entries = new ArrayList<PacketEntry<?>>();
	protected long version = 1l;

	/**
	 * Adds an entry
	 * 
	 * @param entry Packet entry
	 */
	public PacketEntryWriter add(PacketEntry<?> entry) {
		entries.add(entry);
		return this;
	}

	/**
	 * Adds a string
	 * 
	 * @param entry String entry
	 */
	public PacketEntryWriter add(String entry) {
		return add(new StringEntry(entry));
	}

	/**
	 * Adds a boolean value
	 * 
	 * @param entry Boolean entry
	 */
	public PacketEntryWriter add(boolean entry) {
		return add(new BooleanEntry(entry));
	}

	/**
	 * Adds an integer
	 * 
	 * @param entry Integer entry
	 */
	public PacketEntryWriter add(int entry) {
		return add(new IntEntry(entry));
	}

	/**
	 * Adds a floating-point number
	 * 
	 * @param entry Float entry
	 */
	public PacketEntryWriter add(float entry) {
		return add(new FloatEntry(entry));
	}

	/**
	 * Adds a 64-bit signed integer
	 * 
	 * @param entry Float entry
	 */
	public PacketEntryWriter add(long entry) {
		return add(new LongEntry(entry));
	}

	/**
	 * Adds a double-precision floating-point number
	 * 
	 * @param entry Double entry
	 */
	public PacketEntryWriter add(double entry) {
		return add(new DoubleEntry(entry));
	}

	/**
	 * Adds a byte number (recommended to avoid usage, use arrays instead)
	 * 
	 * @param entry Byte entry
	 */
	public PacketEntryWriter add(byte entry) {
		return add(new ByteEntry(entry));
	}

	/**
	 * Adds a character (recommended to avoid usage, use Strings instead)
	 * 
	 * @param entry Char entry
	 */
	public PacketEntryWriter add(char entry) {
		return add(new CharEntry(entry));
	}

	/**
	 * Adds a byte array
	 * 
	 * @param entry Byte array entry
	 */
	public PacketEntryWriter add(byte[] entry) {
		return add(new ByteArrayEntry(entry));
	}

	/**
	 * Adds a normal object (serializing)
	 * 
	 * @param entry Object entry
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public PacketEntryWriter add(Object entry) {
		if (entry instanceof Byte)
			return add((byte) entry);
		else if (entry instanceof Integer)
			return add((int) entry);
		else if (entry instanceof Long)
			return add((long) entry);
		else if (entry instanceof Float)
			return add((float) entry);
		else if (entry instanceof Double)
			return add((double) entry);
		else if (entry instanceof Character)
			return add((char) entry);
		else if (entry instanceof String)
			return add((String) entry);
		else if (entry instanceof Boolean)
			return add((boolean) entry);
		else if (entry instanceof PacketEntry)
			return add((PacketEntry<?>) entry);
		return add(new SerializingEntry(entry));
	}

	/**
	 * Sets the version needed to parse the packet, default is one.
	 * 
	 * @param version Packet version.
	 */
	public PacketEntryWriter setVersion(long version) {
		this.version = version;
		return this;
	}

	/**
	 * Builds the packet
	 * 
	 * @param destination Destination stream
	 * @throws IOException If writing fails.
	 */
	public void write(OutputStream destination) throws IOException {
		destination.write(ByteBuffer.allocate(8).putLong(version).array());

		destination.write(ByteBuffer.allocate(4).putInt(entries.size()).array());
		for (PacketEntry<?> entry : entries) {
			destination.write(entry.type());
		}

		for (PacketEntry<?> entry : entries) {
			entry.transfer(destination);
		}
	}
}
