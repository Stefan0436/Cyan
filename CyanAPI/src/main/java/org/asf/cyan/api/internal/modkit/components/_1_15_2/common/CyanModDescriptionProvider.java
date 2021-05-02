package org.asf.cyan.api.internal.modkit.components._1_15_2.common;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.events.core.ReloadEvent;
import org.asf.cyan.api.events.objects.core.ReloadEventObject;
import org.asf.cyan.api.internal.IModKitComponent;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.mods.IMod;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;
import org.asf.cyan.mods.internal.BaseEventController;

import net.minecraft.locale.Language;

public class CyanModDescriptionProvider implements IModKitComponent, IEventListenerContainer {

	@Override
	public void initializeComponent() {
		BaseEventController.addEventContainer(this);
	}

	@SimpleEvent(ReloadEvent.class)
	private void reload(ReloadEventObject event) {
		if (Modloader.getModloaderGameSide() == GameSide.CLIENT) {
			for (Modloader loader : Modloader.getAllModloaders()) {
				if (loader instanceof CyanLoader) {
					CyanLoader modloader = (CyanLoader) loader;
					for (IMod mod : modloader.getAllModInstances()) {
						mod.setDefaultDescription();
						if (mod.getDescriptionLanguageKey() != null) {
							String desc = Language.getInstance().getOrDefault(mod.getDescriptionLanguageKey());
							if (!desc.equals(mod.getDescriptionLanguageKey()))
								mod.setLanguageBasedDescription(desc);
						}
					}
				}
			}
		}
	}

}
