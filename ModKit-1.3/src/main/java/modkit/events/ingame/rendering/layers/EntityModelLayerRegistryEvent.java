package modkit.events.ingame.rendering.layers;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;

import modkit.events.objects.ingame.rendering.layers.EntityModelLayerRegistryEventObject;

/**
 * 
 * Event called to register custom entity layers (ignored for pre-1.17 clients,
 * <b>NOT YET IMPLEMENTED, MODKIT 1.3</b>)
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 * @deprecated Upcoming feature, unused
 * @since 1.3
 *
 */
@Deprecated
public class EntityModelLayerRegistryEvent extends AbstractExtendedEvent<EntityModelLayerRegistryEventObject> {

	private static EntityModelLayerRegistryEvent implementation;

	@Override
	public String channelName() {
		return "modkit.entity.layers.register";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static EntityModelLayerRegistryEvent getInstance() {
		return implementation;
	}
}
