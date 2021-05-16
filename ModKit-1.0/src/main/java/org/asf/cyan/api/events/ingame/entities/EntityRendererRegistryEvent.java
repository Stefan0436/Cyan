package org.asf.cyan.api.events.ingame.entities;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;
import org.asf.cyan.api.events.objects.ingame.entities.EntityRendererRegistryEventObject;

/**
 * 
 * Event called to register custom entity renderers
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class EntityRendererRegistryEvent extends AbstractExtendedEvent<EntityRendererRegistryEventObject> {

	private static EntityRendererRegistryEvent implementation;

	@Override
	public String channelName() {
		return "modkit.entity.renderer.register";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static EntityRendererRegistryEvent getInstance() {
		return implementation;
	}
}
