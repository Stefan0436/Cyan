package org.asf.cyan.api.internal.modkit.components._1_17.common.network;

import org.asf.cyan.api.internal.IModKitComponent;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;
import org.asf.cyan.mods.internal.BaseEventController;

import modkit.events.network.ServerSideConnectedEvent;
import modkit.events.objects.network.ServerConnectionEventObject;
import modkit.util.server.language.ClientLanguage;

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
