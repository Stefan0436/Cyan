package modkit.events.objects.ingame.tags;

import org.asf.cyan.api.events.extended.EventObject;

import net.minecraft.tags.TagManager;

/**
 * 
 * Tag Manager Event Object -- Event object for all events related to the tag
 * manager.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class TagManagerEventObject extends EventObject {
	private TagManager tagManager;

	public TagManagerEventObject(TagManager tagManager) {
		this.tagManager = tagManager;
	}

	/**
	 * Retrieves the tag manager
	 */
	public TagManager getRecipeManager() {
		return tagManager;
	}

}
