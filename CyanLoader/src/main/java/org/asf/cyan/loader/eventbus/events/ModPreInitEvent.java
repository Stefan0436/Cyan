package org.asf.cyan.loader.eventbus.events;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.events.IEventProvider;
import org.asf.cyan.api.modloader.IModloaderComponent;
import org.asf.cyan.api.modloader.TargetModloader;

@TargetModloader(CyanLoader.class)
public class ModPreInitEvent implements IEventProvider, IModloaderComponent {

	@Override
	public String getChannelName() {
		return "mods.preinit";
	}

}
