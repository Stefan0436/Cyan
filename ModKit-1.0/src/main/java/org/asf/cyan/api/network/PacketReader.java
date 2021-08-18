package org.asf.cyan.api.network;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.asf.cyan.api.packet.PacketEntry;
import org.asf.cyan.api.packet.entries.ByteArrayEntry;
import org.asf.cyan.api.packet.entries.CharEntry;
import org.asf.cyan.api.packet.entries.DoubleEntry;
import org.asf.cyan.api.packet.entries.FloatEntry;
import org.asf.cyan.api.packet.entries.IntEntry;
import org.asf.cyan.api.packet.entries.LongEntry;
import org.asf.cyan.api.packet.entries.SerializingEntry;
import org.asf.cyan.api.packet.entries.StringEntry;
import org.asf.cyan.api.versioning.Version;

/**
 * 
 * Packet Content Reader
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class PacketReader {

	public static class RawReader {
		public PacketReader reader;

		public RawReader(PacketReader reader) {
			this.reader = reader;
		}

		public byte[] readNBytes(int num) {
			return reader.readNBytes(num);
		}

		public byte[] readAllBytes() {
			return reader.readAllBytes();
		}

		public int readByte() {
			return reader.readRawByte();
		}

		public String readString() {
			return new String(readNBytes(readInt()));
		}

		public char readChar() {
			return ByteBuffer.wrap(readNBytes(2)).getChar();
		}

		public int readInt() {
			return ByteBuffer.wrap(readNBytes(4)).getInt();
		}

		public boolean readBoolean() {
			int data = readByte();
			if (data == 0)
				return true;
			else
				return false;
		}

		public long readLong() {
			return ByteBuffer.wrap(readNBytes(8)).getLong();
		}

		public short readShort() {
			return ByteBuffer.wrap(readNBytes(2)).getShort();
		}

		public float readFloat() {
			return ByteBuffer.wrap(readNBytes(4)).getFloat();
		}

		public double readDouble() {
			return ByteBuffer.wrap(readNBytes(8)).getDouble();
		}

		public PacketReader getReader() {
			return reader;
		}
	}

	private ByteFlow flow;

	protected int readByteInternal() {
		return flow.read();
	}

	protected static PacketReader implementation;

	protected abstract PacketReader newInstance();

	protected abstract void init(ByteFlow flow);

	/**
	 * Reads the next byte
	 * 
	 * @return Byte number
	 */
	public int readRawByte() {
		return flow.read();
	}

	/**
	 * Reads the given amount of bytes
	 * 
	 * @param count Byte count
	 * @return Byte array
	 */
	public byte[] readNBytes(int count) {
		byte[] buffer = new byte[count];
		for (int i = 0; i < count; i++) {
			int b = readRawByte();
			if (!flow.hasNext())
				break;
			buffer[i] = (byte) b;
		}
		return buffer;
	}

	/**
	 * Reads all available bytes (stops if size reaches integer max or if the end of
	 * the byte flow is reached)
	 * 
	 * @return Byte array
	 */
	public byte[] readAllBytes() {
		ArrayList<Byte> bytes = new ArrayList<Byte>();
		long count = 0;
		while (true) {
			if (count + 1l > Integer.MAX_VALUE)
				break;

			int b = readRawByte();
			if (!flow.hasNext())
				break;

			bytes.add((byte) b);
			count++;
		}

		int i = 0;
		byte[] buffer = new byte[bytes.size()];
		for (Byte b : bytes)
			buffer[i++] = b;

		return buffer;
	}

	/**
	 * Creates a new packet reader
	 * 
	 * @param flow Input byte flow
	 */
	public static PacketReader create(ByteFlow flow) {
		PacketReader reader = implementation.newInstance();
		reader.flow = flow;
		reader.init(flow);
		return reader;
	}

	/**
	 * Reads the next entry
	 * 
	 * @param <T> Entry type
	 * @return Packet entry
	 */
	public abstract <T> PacketEntry<T> readEntry();

	/**
	 * Reads the next String entry
	 * 
	 * @return String value or null
	 */
	public String readString() {
		PacketEntry<?> entry = readEntry();
		if (entry instanceof StringEntry)
			return entry.get().toString();
		else
			return null;
	}

	/**
	 * Reads the next integer entry
	 * 
	 * @return Integer value or -1
	 */
	public int readInt() {
		PacketEntry<?> entry = readEntry();
		if (entry instanceof IntEntry)
			return (int) entry.get();
		else
			return -1;
	}

	/**
	 * Reads the next long entry
	 * 
	 * @return Long value or -1
	 */
	public long readLong() {
		PacketEntry<?> entry = readEntry();
		if (entry instanceof LongEntry)
			return (long) entry.get();
		else
			return -1;
	}

	/**
	 * Reads the next floating-point entry
	 * 
	 * @return Float value or -1
	 */
	public float readFloat() {
		PacketEntry<?> entry = readEntry();
		if (entry instanceof FloatEntry)
			return (float) entry.get();
		else
			return -1;
	}

	/**
	 * Reads the next double-precision floating-point entry
	 * 
	 * @return Double value or -1
	 */
	public double readDouble() {
		PacketEntry<?> entry = readEntry();
		if (entry instanceof DoubleEntry)
			return (double) entry.get();
		else
			return -1;
	}

	/**
	 * Reads the next character entry (not recommended, use strings instead)
	 * 
	 * @return Character value or -1
	 */
	public char readChar() {
		PacketEntry<?> entry = readEntry();
		if (entry instanceof CharEntry)
			return (char) entry.get();
		else
			return (char) -1;
	}

	/**
	 * Reads the next byte array entry
	 * 
	 * @return Character value or null
	 */
	public byte[] readBytes() {
		PacketEntry<?> entry = readEntry();
		if (entry instanceof ByteArrayEntry)
			return (byte[]) entry.get();
		else
			return null;
	}

	/**
	 * Reads the next boolean
	 * 
	 * @return Boolean value or false
	 */
	public boolean readBoolean() {
		PacketEntry<?> entry = readEntry();
		if (entry instanceof SerializingEntry) {
			Object obj = entry.get();
			if (obj instanceof Boolean)
				return (boolean) obj;
			else
				return false;
		} else
			return false;
	}

	/**
	 * Reads the next object value
	 * 
	 * @param <T>  Value type
	 * @param type Value class
	 * @return Value or null
	 */
	@SuppressWarnings("unchecked")
	public <T> T readObject(Class<T> type) {
		PacketEntry<?> entry = readEntry();
		if (entry instanceof SerializingEntry) {
			Object obj = entry.get();
			if (type.isAssignableFrom(obj.getClass()))
				return (T) obj;
			else
				return null;
		} else
			return null;
	}

	/**
	 * Reads the next Version value
	 * 
	 * @return Value or null
	 */
	public Version readVersion() {
		String str = readString();
		if (str == null)
			return null;
		return Version.fromString(str);
	}

}
