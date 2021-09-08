package org.asf.cyan.internal.modkitimpl.channels.flow;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.asf.cyan.api.packet.PacketEntry;
import org.asf.cyan.api.packet.PacketParser;

import modkit.network.ByteFlow;

public class FlowPacketParser extends PacketParser {

	public class ByteFlowStream extends InputStream {

		private ByteFlow flow;

		public ByteFlowStream(ByteFlow flow) {
			this.flow = flow;
		}

		@Override
		public int read() throws IOException {
			return flow.read() & 0xFF;
		}

	}

	public static class FlowUtil {
		/**
		 * Reads the next byte
		 * 
		 * @param flow Input flow
		 * @return Byte
		 */
		public static int readRawByte(ByteFlow flow) {
			return flow.read();
		}

		/**
		 * Reads the given amount of bytes
		 * 
		 * @param flow Input flow
		 * @param count Byte count
		 * @return Byte array
		 */
		public static byte[] readNBytes(ByteFlow flow, int count) {
			byte[] buffer = new byte[count];
			for (int i = 0; i < count; i++) {
				if (!flow.hasNext())
					break;
				int b = flow.read();
				buffer[i] = (byte) b;
			}
			return buffer;
		}

		/**
		 * Reads all available bytes (stops if size reaches integer max or if the end of
		 * the byte flow is reached)
		 * 
		 * @param flow Input flow
		 * @return Byte array
		 */
		public static byte[] readAllBytes(ByteFlow flow) {
			ArrayList<Byte> bytes = new ArrayList<Byte>();
			long count = 0;
			while (true) {
				if (count + 1l > Integer.MAX_VALUE)
					break;

				int b = flow.read();
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
	}

	private ByteFlow flow;
	private ByteFlowStream stream;

	public FlowPacketParser(ByteFlow input) {
		flow = input;
		stream = new ByteFlowStream(input);
		version = ByteBuffer.wrap(FlowUtil.readNBytes(flow, 8)).getLong();
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <T> PacketEntry<T> nextEntry() {
		if (!flow.hasNext())
			return null;
		else {
			long type = ByteBuffer.wrap(FlowUtil.readNBytes(flow, 8)).getLong();
			long length = ByteBuffer.wrap(FlowUtil.readNBytes(flow, 8)).getLong();
			try {
				Constructor<? extends PacketEntry> ctor = entryTypes.get(type).getDeclaredConstructor();
				ctor.setAccessible(true);
				PacketEntry<T> ent = (PacketEntry<T>) ctor.newInstance();
				ent = ent.importStream(stream, length);
				return ent;
			} catch (Exception e) {
				return null;
			}
		}
	}

}
