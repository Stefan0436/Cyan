package org.asf.cyan.internal.modkitimpl.channels.packets;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.modloader.IPostponedComponent;
import org.asf.cyan.api.modloader.TargetModloader;
import org.asf.cyan.api.packet.PacketEntry;
import org.asf.cyan.internal.modkitimpl.channels.flow.FlowPacketBuilder;

import modkit.network.OutputFlow;
import modkit.network.PacketWriter;

@TargetModloader(CyanLoader.class)
public class PacketWriterImplementation extends PacketWriter implements IPostponedComponent {

	private OutputFlow flow;
	private FlowPacketBuilder builder;

	@Override
	public void initComponent() {
		implementation = new PacketWriterImplementation();
	}

	@Override
	protected PacketWriter newInstance() {
		return new PacketWriterImplementation();
	}

	@Override
	protected void init(OutputFlow flow) {
		this.flow = flow;
	}

	@Override
	public <T> PacketWriter writeEntry(PacketEntry<T> entry) {
		if (builder == null) {
			builder = new FlowPacketBuilder(flow);
		}
		builder.add(entry);
		return this;
	}

}
