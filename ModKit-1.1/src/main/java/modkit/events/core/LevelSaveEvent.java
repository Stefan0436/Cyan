package modkit.events.core;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;

import modkit.events.objects.core.LevelDataEventObject;

/**
 * 
 * Level save event -- called on world save
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class LevelSaveEvent extends AbstractExtendedEvent<LevelDataEventObject> {

	private static LevelSaveEvent implementation;

	@Override
	public String channelName() {
		return "modkit.save.level";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static LevelSaveEvent getInstance() {
		return implementation;
	}

}
