package org.asf.cyan.api.internal.modkit.transformers._1_15_2.client.entities;

import java.util.Map;

import org.asf.cyan.api.events.entities.EntityRendererRegistryEvent;
import org.asf.cyan.api.events.objects.entities.EntityRendererRegistryEventObject;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.world.entity.EntityType;

@FluidTransformer
@TargetClass(target = "net.minecraft.client.renderer.entity.EntityRenderDispatcher")
public class EntityRenderDispatcherModification {

	private final Font font = null;
	public final TextureManager textureManager = null;
	private final Map<EntityType<?>, EntityRenderer<?>> renderers = null;
	public Options options = null;

	@InjectAt(location = InjectLocation.TAIL)
	private void registerRenderers(
			@TargetType(target = "net.minecraft.client.renderer.entity.ItemRenderer") ItemRenderer var1,
			@TargetType(target = "net.minecraft.server.packs.resources.ReloadableResourceManager") ReloadableResourceManager var2) {
		EntityRendererRegistryEventObject modEntities = new EntityRendererRegistryEventObject(var2, this,
				textureManager, font, options);
		EntityRendererRegistryEvent.getInstance().dispatch(modEntities).getResult();
		for (EntityType<?> type : modEntities.getEntities().keySet()) {
			EntityRenderer<?> renderer = modEntities.getEntities().get(type);
			renderers.put(type, renderer);
		}
	}

}
