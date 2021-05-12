package org.asf.cyan.internal.modkitimpl;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.events.network.EarlyCyanClientHandshakeEvent;
import org.asf.cyan.api.events.objects.network.ClientConnectionEventObject;
import org.asf.cyan.api.modloader.IPostponedComponent;
import org.asf.cyan.api.modloader.TargetModloader;
import org.asf.cyan.api.util.ContainerConditions;
import org.asf.cyan.api.util.EventUtil;
import org.asf.cyan.internal.modkitimpl.handshake.CyanHandshakePacketChannel;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeProtocolPacket;
import org.asf.cyan.internal.modkitimpl.info.Protocols;
import org.asf.cyan.internal.modkitimpl.util.HandshakeUtils;
import org.asf.cyan.internal.modkitimpl.util.ScreenUtil;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;

@TargetModloader(CyanLoader.class)
public class HandshakeComponent implements IEventListenerContainer, IPostponedComponent {

	@Override
	public void initComponent() {
		EventUtil.registerContainer(ContainerConditions.CLIENT, () -> getClass().getTypeName());
	}

	@SimpleEvent(value = EarlyCyanClientHandshakeEvent.class, synchronize = true)
	public void handshakeStartClient(ClientConnectionEventObject event) throws InterruptedException {
		ScreenUtil.getImpl().setScreenToTitle(event);
		CyanHandshakePacketChannel channel = HandshakeUtils.getImpl().getChannel(CyanHandshakePacketChannel.class,
				event);
		HandshakeProtocolPacket packet = new HandshakeProtocolPacket();
		packet.protocolVersion = Protocols.MODKIT_PROTOCOL;
		packet.write(channel);
	}

}
