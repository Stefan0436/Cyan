package org.asf.cyan.api.internal.test;

import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.internal.test.sides.ClientEvents;
import org.asf.cyan.api.internal.test.sides.ServerEvents;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.util.server.language.ClientLanguage;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.AttachEvent;
import org.asf.cyan.mods.internal.BaseEventController;

@CYAN_COMPONENT
public class TestEventListeners extends CyanComponent implements IEventListenerContainer {

	protected static void initComponent() {
		BaseEventController.addEventContainer(new TestEventListeners());
		BaseEventController.addEventContainer(new ServerEvents());
		if (Modloader.getModloaderGameSide() == GameSide.CLIENT) {
			BaseEventController.addEventContainer(new ClientEvents());
		} else {
			ClientLanguage.registerLanguageKey("test.test", "hello world");
		}
	}

	@AttachEvent("mods.preinit")
	private void preInit() {
		this.equals(this); // OK 2
	}

	@AttachEvent("mods.init")
	private void init() {
		this.equals(this); // OK 2
	}

	@AttachEvent("mods.postinit")
	private void postInit() {
		this.equals(this); // OK 2
	}

	@AttachEvent("mods.runtimestart")
	private void runtime() {
		this.equals(this); // OK 2
	}

}
