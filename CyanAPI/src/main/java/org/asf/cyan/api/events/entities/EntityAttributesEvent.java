package org.asf.cyan.api.events.entities;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;
import org.asf.cyan.api.events.objects.entities.EntityAttributesEventObject;

/**
 * 
 * Entity Attributes Event Object -- Register your entity attribute suppliers
 * with this event
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class EntityAttributesEvent extends AbstractExtendedEvent<EntityAttributesEventObject> {

	private static EntityAttributesEvent implementation;

	@Override
	public String channelName() {
		return "modkit.entity.living.attributes.register";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static EntityAttributesEvent getInstance() {
		return implementation;
	}
}
