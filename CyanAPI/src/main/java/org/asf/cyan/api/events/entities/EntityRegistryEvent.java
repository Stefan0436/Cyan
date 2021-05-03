package org.asf.cyan.api.events.entities;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;
import org.asf.cyan.api.events.objects.entities.EntityRegistryEventObject;

/**
 * 
 * Event called to register custom entities
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class EntityRegistryEvent extends AbstractExtendedEvent<EntityRegistryEventObject> {

	private static EntityRegistryEvent implementation;

	@Override
	public String channelName() {
		return "modkit.entity.register";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static EntityRegistryEvent getInstance() {
		return implementation;
	}
}
