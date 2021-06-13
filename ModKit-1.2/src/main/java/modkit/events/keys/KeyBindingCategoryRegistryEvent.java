package modkit.events.keys;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;

import modkit.events.objects.keys.KeyBindingCategoryRegistryEventObject;

/**
 * 
 * Event called to register custom key binding categories
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 * @since ModKit 1.1
 *
 */
public class KeyBindingCategoryRegistryEvent extends AbstractExtendedEvent<KeyBindingCategoryRegistryEventObject> {

	private static KeyBindingCategoryRegistryEvent instance;
	
	@Override
	public String channelName() {
		return "modkit.keybindings.categories.register";
	}

	@Override
	public void afterInstantiation() {
		instance = this;
	}
	
	public static AbstractExtendedEvent<KeyBindingCategoryRegistryEventObject> getInstance() {
		return instance;
	}
	
}
