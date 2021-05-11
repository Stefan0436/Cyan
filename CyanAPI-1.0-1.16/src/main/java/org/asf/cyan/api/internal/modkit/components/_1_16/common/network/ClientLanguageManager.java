package org.asf.cyan.api.internal.modkit.components._1_16.common.network;

import org.asf.cyan.api.events.network.ServerSideConnectedEvent;
import org.asf.cyan.api.events.objects.network.ServerConnectionEventObject;
import org.asf.cyan.api.internal.IModKitComponent;
import org.asf.cyan.api.util.server.language.ClientLanguage;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;
import org.asf.cyan.mods.internal.BaseEventController;

public class ClientLanguageManager implements IModKitComponent, IEventListenerContainer {

	@Override
	public void initializeComponent() {
		BaseEventController.addEventContainer(this);
	}

	@SimpleEvent(ServerSideConnectedEvent.class)
	public void connectPlayer(ServerConnectionEventObject event) {
		ClientLanguage.startPlayerWatcher(event.getServer());
	}

}
