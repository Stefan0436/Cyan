package org.asf.cyan.api.events.objects.ingame.blocks;

import java.util.HashMap;
import java.util.Map;

import org.asf.cyan.api.events.extended.EventObject;

import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * 
 * Block Entity Renderer Registry Event Object -- Register your block entity
 * renderers by using this
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class BlockEntityRendererRegistryEventObject extends EventObject {

	private Font font;
	private TextureManager textureManager;
	private BlockEntityRenderDispatcher dispatcher;
	private Options options;
	private HashMap<BlockEntityType<?>, BlockEntityRenderer<?>> entities = new HashMap<BlockEntityType<?>, BlockEntityRenderer<?>>();

	public BlockEntityRendererRegistryEventObject(Object dispatcher,
			TextureManager textureManager, Font font, Options options) {
		this.dispatcher = (BlockEntityRenderDispatcher) dispatcher;
		this.textureManager = textureManager;
		this.options = options;
		this.font = font;
	}

	/**
	 * Retrieves the block entity dispatcher
	 */
	public BlockEntityRenderDispatcher getDispatcher() {
		return dispatcher;
	}

	/**
	 * Retrieves the texture manager
	 */
	public TextureManager getTextureManager() {
		return textureManager;
	}

	/**
	 * Adds custom block entity renderers to the game
	 * 
	 * @param <T>        Entity Class Type
	 * @param entityType Entity type
	 * @param renderer   Entity renderer
	 */
	public <T extends BlockEntity> void addEntity(BlockEntityType<? extends T> entityType,
			BlockEntityRenderer<? extends T> renderer) {
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
	public Map<BlockEntityType<?>, BlockEntityRenderer<?>> getEntities() {
		return new HashMap<BlockEntityType<?>, BlockEntityRenderer<?>>(entities);
	}

	/**
	 * Retrieves the font
	 */
	public Font getFont() {
		return font;
	}

}
