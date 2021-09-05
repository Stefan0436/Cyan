package modkit.events.core;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;

import modkit.events.objects.core.ReloadEventObject;

/**
 * 
 * Reload Event -- Called on game reload. (prepare function)
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ReloadPrepareEvent extends AbstractExtendedEvent<ReloadEventObject> {

	private static ReloadPrepareEvent implementation;

	@Override
	public String channelName() {
		return "modkit.reload.game.prepare";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static ReloadPrepareEvent getInstance() {
		return implementation;
	}

}
