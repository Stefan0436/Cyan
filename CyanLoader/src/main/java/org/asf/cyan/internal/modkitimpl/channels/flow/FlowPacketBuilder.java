package org.asf.cyan.internal.modkitimpl.channels.flow;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.asf.cyan.api.packet.PacketBuilder;
import org.asf.cyan.api.packet.PacketEntry;

import modkit.network.OutputFlow;

public class FlowPacketBuilder extends PacketBuilder {

	private OutputFlow flow;
	private FlowOutputStream stream;

	public static class FlowOutputStream extends OutputStream {
		private OutputFlow flow;

		public FlowOutputStream(OutputFlow flow) {
			this.flow = flow;
		}

		@Override
		public void write(int arg0) throws IOException {
			flow.write(arg0);
		}

	}

	public static class FlowUtil {
		public static void write(OutputFlow flow, String data) {
			for (byte b : data.getBytes())
				flow.write(b);
		}

		public static void write(OutputFlow flow, byte[] data) {
			for (byte b : data)
				flow.write(b);
		}

		public static void write(OutputFlow flow, long data) {
			for (byte b : ByteBuffer.allocate(8).putLong(data).array())
				flow.write(b);
		}

		public static void write(OutputFlow flow, int data) {
			for (byte b : ByteBuffer.allocate(4).putInt(data).array())
				flow.write(b);
		}
	}

	public FlowPacketBuilder(OutputFlow output) {
		flow = output;
		stream = new FlowOutputStream(flow);
	}

	private boolean setVersion = false;

	@Override
	public PacketBuilder setVersion(long version) {
		if (setVersion)
			throw new IllegalStateException("Version already set, or a packet has already been written");
		super.setVersion(version);
		FlowUtil.write(flow, version);
		setVersion = true;
		return this;
	}

	@Override
	public PacketBuilder add(PacketEntry<?> entry) {
		if (!setVersion) {
			FlowUtil.write(flow, version);
			setVersion = true;
		}

		FlowUtil.write(flow, entry.type());
		FlowUtil.write(flow, entry.length());

		try {
			entry.transfer(stream);
		} catch (IOException e) {
		}

		return this;
	}
}
