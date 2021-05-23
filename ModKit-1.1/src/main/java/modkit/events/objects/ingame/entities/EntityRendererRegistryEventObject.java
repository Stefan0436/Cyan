package modkit.events.objects.ingame.entities;

import java.util.HashMap;
import java.util.Map;

import org.asf.cyan.api.events.extended.EventObject;

import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * 
 * Entity Renderer Registry Event Object -- Register your entity renderers by
 * using this
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class EntityRendererRegistryEventObject extends EventObject {

	private Font font;
	private ReloadableResourceManager resourceManager;
	private TextureManager textureManager;
	private EntityRenderDispatcher dispatcher;
	private Options options;
	private HashMap<EntityType<?>, EntityRenderer<?>> entities = new HashMap<EntityType<?>, EntityRenderer<?>>();

	public EntityRendererRegistryEventObject(ReloadableResourceManager resourceManager, Object dispatcher,
			TextureManager textureManager, Font font, Options options) {
		this.resourceManager = resourceManager;
		this.dispatcher = (EntityRenderDispatcher) dispatcher;
		this.textureManager = textureManager;
		this.options = options;
		this.font = font;
	}

	/**
	 * Retrieves the resource manager
	 */
	public ReloadableResourceManager getResourceManager() {
		return resourceManager;
	}

	/**
	 * Retrieves the entity dispatcher
	 */
	public EntityRenderDispatcher getDispatcher() {
		return dispatcher;
	}

	/**
	 * Retrieves the texture manager
	 */
	public TextureManager getTextureManager() {
		return textureManager;
	}

	/**
	 * Adds custom entity renderers to the game
	 * 
	 * @param <T>        Entity Class Type
	 * @param entityType Entity type
	 * @param renderer   Entity renderer
	 */
	public <T extends Entity> void addEntity(EntityType<? extends T> entityType, EntityRenderer<? extends T> renderer) {
		this.entities.put(entityType, renderer);
	}

	/**
	 * Retrieves the client options
	 */
	public Options getClientOptions() {
		return options;
	}

	/**
	 * Retrieves the map of registered mod entity renderers
	 */
	public Map<EntityType<?>, EntityRenderer<?>> getEntities() {
		return new HashMap<EntityType<?>, EntityRenderer<?>>(entities);
	}

	/**
	 * Retrieves the font
	 */
	public Font getFont() {
		return font;
	}

}
