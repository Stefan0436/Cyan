package org.asf.cyan.api.util;

import java.util.function.Supplier;

/**
 *
 * Event Manager Utility -- Adds event containers based on conditions
 *
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class EventUtil {
	protected static EventUtil implementation;

	/**
	 * Registers the container if the conditions apply
	 * 
	 * @param conditions    Container conditions
	 * @param containerType Container type name supplier
	 */
	public static void registerContainer(ContainerConditions conditions, Supplier<String> containerType) {
		implementation.registerLater(conditions, containerType);
	}

	protected abstract void registerLater(ContainerConditions conditions, Supplier<String> containerType);

}
