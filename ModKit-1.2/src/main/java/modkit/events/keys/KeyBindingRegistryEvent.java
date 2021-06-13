package modkit.events.keys;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;

import modkit.events.objects.keys.KeyBindingRegistryEventObject;

/**
 * 
 * Event called to register custom key bindings
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 * @since ModKit 1.1
 *
 */
public class KeyBindingRegistryEvent extends AbstractExtendedEvent<KeyBindingRegistryEventObject> {

	private static KeyBindingRegistryEvent instance;
	
	@Override
	public String channelName() {
		return "modkit.keybindings.register";
	}

	@Override
	public void afterInstantiation() {
		instance = this;
	}
	
	public static AbstractExtendedEvent<KeyBindingRegistryEventObject> getInstance() {
		return instance;
	}
	
}
