package modkit.events.core;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;

import modkit.events.objects.core.DataFixerEventObject;

/**
 * 
 * DataFixer Event -- called on DataFixer load
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class DataFixerEvent extends AbstractExtendedEvent<DataFixerEventObject> {

	private static DataFixerEvent implementation;

	@Override
	public String channelName() {
		return "modkit.reload.datafixers";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static DataFixerEvent getInstance() {
		return implementation;
	}

}
