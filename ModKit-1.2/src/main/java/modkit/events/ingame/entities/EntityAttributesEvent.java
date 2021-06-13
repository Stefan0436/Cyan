package modkit.events.ingame.entities;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;

import modkit.events.objects.ingame.entities.EntityAttributesEventObject;

/**
 * 
 * Entity Attributes Event -- Register your entity attribute suppliers with this
 * event
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class EntityAttributesEvent extends AbstractExtendedEvent<EntityAttributesEventObject> {

	private static EntityAttributesEvent implementation;

	@Override
	public boolean requiresSynchronizedListeners() {
		return true;
	}

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
