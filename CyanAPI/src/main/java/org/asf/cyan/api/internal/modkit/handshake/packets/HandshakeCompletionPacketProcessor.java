package org.asf.cyan.api.internal.modkit.handshake.packets;

import org.asf.cyan.api.events.network.CyanClientHandshakeEvent;
import org.asf.cyan.api.events.objects.network.ClientConnectionEventObject;
import org.asf.cyan.api.network.PacketReader;
import org.asf.cyan.api.network.channels.ClientPacketProcessor;

import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;

public class HandshakeCompletionPacketProcessor extends ClientPacketProcessor {

	@Override
	public String id() {
		return "handshake.finish";
	}

	@Override
	@SuppressWarnings("resource")
	protected void process(PacketReader reader) {
		if (getClient().screen != null && getClient().screen instanceof ReceivingLevelScreen)
			getClient().setScreen((Screen) null);
		CyanClientHandshakeEvent.getInstance()
				.dispatch(new ClientConnectionEventObject(getConnection(), getClient(), getServerBrand()));
	}

}
