package modkit.events.objects.resources;

import org.asf.cyan.api.events.extended.EventObject;

import net.minecraft.client.resources.language.LanguageManager;

/**
 * 
 * Language Manager Event Object -- Event object for all events related to the
 * language manager.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class LanguageManagerEventObject extends EventObject {
	private LanguageManager languageManager;

	public LanguageManagerEventObject(LanguageManager languageManager) {
		this.languageManager = languageManager;
	}

	/**
	 * Retrieves the language manager (always null on servers, only present for the
	 * LangaugeManager event)
	 */
	public LanguageManager getLanguageManager() {
		return languageManager;
	}

}
