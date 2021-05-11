package org.asf.cyan.internal.modkitimpl.channels.packets;

import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.network.ByteFlow;
import org.asf.cyan.api.network.PacketReader;
import org.asf.cyan.api.packet.PacketEntry;
import org.asf.cyan.internal.modkitimpl.channels.flow.FlowPacketParser;

@CYAN_COMPONENT
public class PacketReaderImplementation extends PacketReader {

	private ByteFlow flow;
	private FlowPacketParser parser = null;

	protected static void initComponent() {
		implementation = new PacketReaderImplementation();
	}

	@Override
	protected PacketReader newInstance() {
		return new PacketReaderImplementation();
	}

	@Override
	protected void init(ByteFlow flow) {
		this.flow = flow;
	}

	@Override
	public <T> PacketEntry<T> readEntry() {
		if (parser == null) {
			parser = new FlowPacketParser(flow);
		}
		return parser.nextEntry();
	}

}
