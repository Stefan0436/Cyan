package org.asf.cyan.api.internal.modkit.transformers._1_17.client.entities;

import java.util.Map;

import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Constructor;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import modkit.events.ingame.entities.EntityRendererRegistryEvent;
import modkit.events.objects.ingame.entities.EntityRendererRegistryEventObject;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

@FluidTransformer
@TargetClass(target = "net.minecraft.client.renderer.entity.EntityRenders")
public class EntityRenderDispatcherModification {

	private final Font font = null;
	public final TextureManager textureManager = null;
	private final Map<EntityType<?>, EntityRendererProvider<?>> PROVIDERS = null;
	public Options options = null;

	@Constructor(clinit = true)
	@SuppressWarnings("unchecked")
	@InjectAt(location = InjectLocation.TAIL)
	private void registerRenderers() {
		EntityRendererRegistryEventObject modEntities = new EntityRendererRegistryEventObject(null, this,
				textureManager, font, options);
		EntityRendererRegistryEvent.getInstance().dispatch(modEntities).getResult();
		for (EntityType<?> type : modEntities.getEntities().keySet()) {
			EntityRenderer<?> renderer = modEntities.getEntities().get(type);
			PROVIDERS.put(type, (ctx) -> {
				return (EntityRenderer<Entity>) renderer;
			});
		}
	}

}
