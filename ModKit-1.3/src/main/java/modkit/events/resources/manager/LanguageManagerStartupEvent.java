package modkit.events.resources.manager;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;

import modkit.events.objects.resources.LanguageManagerEventObject;

/**
 * 
 * Language Manager Startup Event -- Called on language manager startup.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class LanguageManagerStartupEvent extends AbstractExtendedEvent<LanguageManagerEventObject> {

	private static LanguageManagerStartupEvent implementation;

	@Override
	public String channelName() {
		return "modkit.start.language.manager";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static LanguageManagerStartupEvent getInstance() {
		return implementation;
	}

}
