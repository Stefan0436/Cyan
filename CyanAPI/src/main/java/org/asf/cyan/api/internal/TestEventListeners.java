package org.asf.cyan.api.internal;

import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.events.core.ReloadEvent;
import org.asf.cyan.api.events.ingame.commands.CommandManagerStartupEvent;
import org.asf.cyan.api.events.objects.core.ReloadEventObject;
import org.asf.cyan.api.events.objects.ingame.commands.CommandManagerEventObject;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;
import org.asf.cyan.mods.internal.BaseEventController;

import net.minecraft.commands.Commands;

@CYAN_COMPONENT
public class TestEventListeners extends CyanComponent implements IEventListenerContainer {
	protected static void initComponent() {
		BaseEventController.addEventContainer(new TestEventListeners());
	}

	@SimpleEvent(ReloadEvent.class)
	public void testReload(ReloadEventObject reload) {
		reload = reload;
	}

	@SimpleEvent(CommandManagerStartupEvent.class)
	public void testCommandManager(CommandManagerEventObject event) {
		Commands cmds = event.getCommandManager();
		event = event;
	}
}
