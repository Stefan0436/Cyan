package modkit.events.objects.keys;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.asf.cyan.api.events.extended.EventObject;

import com.mojang.blaze3d.platform.InputConstants.Type;

import net.minecraft.client.KeyMapping;

/**
 * 
 * Event object containing methods to register custom key bindings
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 * @since ModKit 1.1
 *
 */
public class KeyBindingRegistryEventObject extends EventObject {

	private HashMap<Supplier<KeyMapping>, Consumer<KeyMapping>> keys = new HashMap<Supplier<KeyMapping>, Consumer<KeyMapping>>();

	/**
	 * Retrieves the array of registered mod key bindings
	 * 
	 * @return Array of key mapping constructors
	 */
	public Map<Supplier<KeyMapping>, Consumer<KeyMapping>> getKeys() {
		return new HashMap<Supplier<KeyMapping>, Consumer<KeyMapping>>(keys);
	}

	/**
	 * Registers a key binding
	 * 
	 * @param id       Key id (language key)
	 * @param key      {@link com.mojang.blaze3d.platform.InputConstants
	 *                 InputConstants} key id.
	 * @param category
	 * @param callback Key registry callback (called after registry)
	 */
	public void registerKey(String id, int key, String category, Consumer<KeyMapping> callback) {
		registerKey(id, Type.KEYSYM, key, category, callback);
	}

	/**
	 * Registers a key binding
	 * 
	 * @param id       Key id (language key)
	 * @param type     Key type (such as keyboard (KEYSYM) or mouse (MOUSE))
	 * @param key      {@link com.mojang.blaze3d.platform.InputConstants
	 *                 InputConstants} key id.
	 * @param category
	 * @param callback Key registry callback (called after registry)
	 */
	public void registerKey(String id, Type type, int key, String category, Consumer<KeyMapping> callback) {
		registerKey(() -> {
			return new KeyMapping(id, key, category);
		}, callback);
	}

	/**
	 * Registers a key binding
	 * 
	 * @param constructor Key mapping constructor (returns a KeyMapping instance)
	 * @param callback    Key registry callback (called after registry)
	 */
	public void registerKey(Supplier<KeyMapping> constructor, Consumer<KeyMapping> callback) {
		keys.put(constructor, callback);
	}
}
