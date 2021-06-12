package modkit.events.core;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;

import modkit.events.objects.core.LevelDataEventObject;

/**
 * 
 * Level load event -- called on world load
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class LevelLoadEvent extends AbstractExtendedEvent<LevelDataEventObject> {

	private static LevelLoadEvent implementation;

	@Override
	public String channelName() {
		return "modkit.load.level";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static LevelLoadEvent getInstance() {
		return implementation;
	}

}
