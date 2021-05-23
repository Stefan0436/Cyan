package org.asf.cyan.internal.modkitimpl.channels.packets;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.modloader.IPostponedComponent;
import org.asf.cyan.api.modloader.TargetModloader;
import org.asf.cyan.api.packet.PacketEntry;
import org.asf.cyan.internal.modkitimpl.channels.flow.FlowPacketParser;

import modkit.network.ByteFlow;
import modkit.network.PacketReader;

@TargetModloader(CyanLoader.class)
public class PacketReaderImplementation extends PacketReader implements IPostponedComponent {

	private ByteFlow flow;
	private FlowPacketParser parser = null;

	@Override
	public void initComponent() {
		implementation = this;
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
