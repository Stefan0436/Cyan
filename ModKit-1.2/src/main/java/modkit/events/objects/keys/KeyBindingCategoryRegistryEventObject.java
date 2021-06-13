package modkit.events.objects.keys;

import java.util.HashMap;
import java.util.Map;

import org.asf.cyan.api.events.extended.EventObject;

/**
 * 
 * Event object containing methods to register custom key binding categories
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 * @since ModKit 1.1
 *
 */
public class KeyBindingCategoryRegistryEventObject extends EventObject {
	private HashMap<String, Integer> categories = new HashMap<String, Integer>();

	/**
	 * Retrieves a map of modded key binding categories to be registered
	 * 
	 * @return Map of key binding categories
	 */
	public Map<String, Integer> getCategories() {
		return new HashMap<String, Integer>(categories);
	}

	/**
	 * Registers key binding categories
	 * 
	 * @param id              Category id (language key)
	 * @param sortingPosition Sorting value (default is 2)
	 */
	public void addCategory(String id, int sortingPosition) {
		categories.putIfAbsent(id, sortingPosition);
	}

	/**
	 * Registers key binding categories
	 * 
	 * @param id Category id (language key)
	 */
	public void addCategory(String id) {
		addCategory(id, 2);
	}
}
