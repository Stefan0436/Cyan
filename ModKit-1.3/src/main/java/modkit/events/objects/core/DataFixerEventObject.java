package modkit.events.objects.core;

import org.asf.cyan.api.events.extended.EventObject;

import com.mojang.datafixers.DataFixerBuilder;

/**
 * 
 * DataFixer Event Object -- Event object for the DataFixer event
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class DataFixerEventObject extends EventObject {

	private DataFixerBuilder builder;

	public DataFixerEventObject(DataFixerBuilder builder) {
		this.builder = builder;
	}

	/**
	 * Retrieves the datafixer builder
	 */
	public DataFixerBuilder getBuilder() {
		return builder;
	}

}
