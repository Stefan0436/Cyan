package org.asf.cyan.api.internal.modkit.components._1_16.client;

import org.asf.cyan.api.events.objects.network.ClientConnectionEventObject;
import org.asf.cyan.api.internal.IModKitComponent;
import org.asf.cyan.api.network.channels.ClientPacketProcessor;
import org.asf.cyan.internal.modkitimpl.util.ScreenUtil;

import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;

public class ScreenUtilsImpl extends ScreenUtil implements IModKitComponent {

	@Override
	public void setScreenToTitle(ClientConnectionEventObject event) {
		event.getClient().setScreen(new ReceivingLevelScreen());
	}

	@Override
	public void initializeComponent() {
		impl = this;
	}

	@Override
	@SuppressWarnings("resource")
	public void setToReceiveLevelScreenIfNeeded(ClientPacketProcessor processor) {
		if (processor.getClient().screen != null && processor.getClient().screen instanceof ReceivingLevelScreen)
			processor.getClient().setScreen((Screen) null);
	}

}
