package org.asf.cyan.api.packet;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

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
public class PacketBuilder {
	protected ArrayList<PacketEntry<?>> entries = new ArrayList<PacketEntry<?>>();
	protected long version = 1l;

	/**
	 * Adds an entry
	 * 
	 * @param entry Packet entry
	 */
	public PacketBuilder add(PacketEntry<?> entry) {
		entries.add(entry);
		return this;
	}

	/**
	 * Adds a string
	 * 
	 * @param entry String entry
	 */
	public PacketBuilder add(String entry) {
		return add(new StringEntry(entry));
	}

	/**
	 * Adds an integer
	 * 
	 * @param entry Integer entry
	 */
	public PacketBuilder add(int entry) {
		return add(new IntEntry(entry));
	}

	/**
	 * Adds a floating-point number
	 * 
	 * @param entry Float entry
	 */
	public PacketBuilder add(float entry) {
		return add(new FloatEntry(entry));
	}

	/**
	 * Adds a 64-bit signed integer
	 * 
	 * @param entry Float entry
	 */
	public PacketBuilder add(long entry) {
		return add(new LongEntry(entry));
	}

	/**
	 * Adds a double-precision floating-point number
	 * 
	 * @param entry Double entry
	 */
	public PacketBuilder add(double entry) {
		return add(new DoubleEntry(entry));
	}

	/**
	 * Adds a byte number (recommended to avoid usage, use arrays instead)
	 * 
	 * @param entry Byte entry
	 */
	public PacketBuilder add(byte entry) {
		return add(new ByteEntry(entry));
	}

	/**
	 * Adds a character (recommended to avoid usage, use Strings instead)
	 * 
	 * @param entry Char entry
	 */
	public PacketBuilder add(char entry) {
		return add(new CharEntry(entry));
	}

	/**
	 * Adds a byte array
	 * 
	 * @param entry Byte array entry
	 */
	public PacketBuilder add(byte[] entry) {
		return add(new ByteArrayEntry(entry));
	}

	/**
	 * Adds a normal object (serializing)
	 * 
	 * @param entry Object entry
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public PacketBuilder add(Object entry) {
		return add(new SerializingEntry(entry));
	}

	/**
	 * Sets the version needed to parse the packet, default is one.
	 * 
	 * @param version Packet version.
	 */
	public PacketBuilder setVersion(long version) {
		this.version = version;
		return this;
	}

	/**
	 * Builds the packet
	 * 
	 * @param destination Destination stream
	 * @throws IOException If writing fails.
	 */
	public void build(OutputStream destination) throws IOException {
		destination.write(ByteBuffer.allocate(8).putLong(version).array());

		destination.write(ByteBuffer.allocate(4).putInt(entries.size()).array());
		for (PacketEntry<?> entry : entries) {
			destination.write(ByteBuffer.allocate(8).putLong(entry.type()).array());
			destination.write(ByteBuffer.allocate(8).putLong(entry.length()).array());
		}

		for (PacketEntry<?> entry : entries) {
			entry.transfer(destination);
		}
	}
}
