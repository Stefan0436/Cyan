package modkit.events.ingame.tags;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;

import modkit.events.objects.ingame.tags.TagManagerEventObject;

/**
 * 
 * Tag Manager Startup Event -- Called on recipe manager startup.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class TagManagerStartupEvent extends AbstractExtendedEvent<TagManagerEventObject> {

	private static TagManagerStartupEvent implementation;

	@Override
	public String channelName() {
		return "modkit.start.nbt.tag.manager";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static TagManagerStartupEvent getInstance() {
		return implementation;
	}

}
