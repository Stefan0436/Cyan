package org.asf.cyan.api;

import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.mods.events.IEventListenerContainer;

@CYAN_COMPONENT
public class ApiComponent implements IEventListenerContainer {
	protected static void initComponent() {
		Modloader.getModloader().dispatchEvent("modloader.register.path", ApiComponent.class);
	}
}
