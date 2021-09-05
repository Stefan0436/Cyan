package modkit.events.ingame.rendering.blockentity;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;

import modkit.events.objects.ingame.rendering.blockentity.BlockEntityRendererRegistryEventObject;

/**
 * 
 * Event called to register custom block entity renderers
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class BlockEntityRendererRegistryEvent extends AbstractExtendedEvent<BlockEntityRendererRegistryEventObject> {

	private static BlockEntityRendererRegistryEvent implementation;

	@Override
	public String channelName() {
		return "modkit.block.entity.renderer.register";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static BlockEntityRendererRegistryEvent getInstance() {
		return implementation;
	}
}
