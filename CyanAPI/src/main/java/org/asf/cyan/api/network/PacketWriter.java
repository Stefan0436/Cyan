package org.asf.cyan.api.network;

import java.io.Closeable;
import java.nio.ByteBuffer;

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
 * Packet Content Writer
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class PacketWriter implements Closeable {

	public static class RawWriter {
		public PacketWriter writer;

		public RawWriter(PacketWriter writer) {
			this.writer = writer;
		}

		public RawWriter writeBytes(byte[] data) {
			writer.writeRawArray(data);
			return this;
		}

		public RawWriter writeByte(byte data) {
			writer.writeRawByte(data);
			return this;
		}

		public RawWriter writeString(String str) {
			byte[] buff = str.getBytes();
			writeInt(buff.length);
			writeBytes(buff);
			return this;
		}

		public RawWriter writeChar(char ch) {
			writer.writeRawArray(ByteBuffer.allocate(2).putChar(ch).array());
			return this;
		}

		public RawWriter writeInt(int data) {
			writer.writeRawArray(ByteBuffer.allocate(4).putInt(data).array());
			return this;
		}

		public RawWriter writeBoolean(boolean data) {
			writer.writeRawByte(data ? 0 : 1);
			return this;
		}

		public RawWriter writeLong(long data) {
			writer.writeRawArray(ByteBuffer.allocate(8).putLong(data).array());
			return this;
		}

		public RawWriter writeShort(short data) {
			writer.writeRawArray(ByteBuffer.allocate(2).putShort(data).array());
			return this;
		}

		public RawWriter writeFloat(long data) {
			writer.writeRawArray(ByteBuffer.allocate(4).putFloat(data).array());
			return this;
		}

		public RawWriter writeDouble(double data) {
			writer.writeRawArray(ByteBuffer.allocate(8).putDouble(data).array());
			return this;
		}

		public PacketWriter getWriter() {
			return writer;
		}
	}

	private OutputFlow flow;

	protected void writeByteInternal(int data) {
		flow.write(data);
	}

	protected static PacketWriter implementation;

	protected abstract PacketWriter newInstance();

	protected abstract void init(OutputFlow flow);

	public OutputFlow getOutput() {
		return flow;
	}

	/**
	 * Closes the output flow
	 */
	public void close() {
		flow.close();
	}

	/**
	 * Writes the next byte
	 * 
	 * @param data Data to write
	 */
	public void writeRawByte(int data) {
		flow.write(data);
	}

	/**
	 * Writes all bytes
	 * 
	 * @param data Data to write
	 */
	public void writeRawArray(byte[] data) {
		for (byte dat : data)
			writeRawByte(dat);
	}

	/**
	 * Creates a new packet writer
	 * 
	 * @param flow Output byte flow
	 */
	public static PacketWriter create(OutputFlow flow) {
		PacketWriter writer = implementation.newInstance();
		writer.flow = flow;
		writer.init(flow);
		return writer;
	}

	/**
	 * Writes the next entry
	 * 
	 * @param <T>   Entry type
	 * @param entry Packet entry
	 */
	public abstract <T> PacketWriter writeEntry(PacketEntry<T> entry);

	/**
	 * Writes the next String entry
	 * 
	 * @param data Packet entry data
	 */
	public PacketWriter writeString(String data) {
		return writeEntry(new StringEntry(data));
	}

	/**
	 * Writes the next integer entry
	 * 
	 * @param data Packet entry data
	 */
	public PacketWriter writeInt(int data) {
		return writeEntry(new IntEntry(data));
	}

	/**
	 * Writes the next long entry
	 * 
	 * @param data Packet entry data
	 */
	public PacketWriter writeLong(long data) {
		return writeEntry(new LongEntry(data));
	}

	/**
	 * Writes the next floating-point entry
	 * 
	 * @param data Packet entry data
	 */
	public PacketWriter writeFloat(float data) {
		return writeEntry(new FloatEntry(data));
	}

	/**
	 * Writes the next double-precision floating-point entry
	 * 
	 * @param data Packet entry data
	 */
	public PacketWriter writeDouble(double data) {
		return writeEntry(new DoubleEntry(data));
	}

	/**
	 * Writes the next character entry (not recommended, use strings instead)
	 * 
	 * @param data Packet entry data
	 */
	public PacketWriter writeChar(char data) {
		return writeEntry(new CharEntry(data));
	}

	/**
	 * Writes the next byte array entry
	 * 
	 * @param data Packet entry data
	 */
	public PacketWriter writeBytes(byte[] data) {
		return writeEntry(new ByteArrayEntry(data));
	}

	/**
	 * Writes the next boolean
	 * 
	 * @param data Packet entry data
	 */
	public PacketWriter writeBoolean(boolean data) {
		return writeEntry(new SerializingEntry<Boolean>(data));
	}

	/**
	 * Writes the next version value
	 * 
	 * @param data Packet entry data
	 */
	public PacketWriter writeVersion(Version data) {
		return writeEntry(new StringEntry(data.toString()));
	}

	/**
	 * Writes the next object value
	 * 
	 * @param <T>  Value type
	 * @param data Packet entry data
	 */
	public <T> PacketWriter writeObject(T data) {
		return writeEntry(new SerializingEntry<T>(data));
	}

}
