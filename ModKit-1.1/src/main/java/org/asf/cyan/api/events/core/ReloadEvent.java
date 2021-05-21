package org.asf.cyan.api.events.core;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;
import org.asf.cyan.api.events.objects.core.ReloadEventObject;

/**
 * 
 * Reload Event -- Called on game reload.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ReloadEvent extends AbstractExtendedEvent<ReloadEventObject> {

	private static ReloadEvent implementation;

	@Override
	public String channelName() {
		return "modkit.reload.game";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static ReloadEvent getInstance() {
		return implementation;
	}

}
