package modkit.events.objects.ingame.blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.asf.cyan.api.events.extended.EventObject;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.versioning.Version;

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

	@SuppressWarnings("rawtypes")
	private HashMap<BlockEntityType<?>, Function> entities = new HashMap<BlockEntityType<?>, Function>();
	private HashMap<BlockEntityType<?>, BlockEntityRenderer<?>> rendererInstances = new HashMap<BlockEntityType<?>, BlockEntityRenderer<?>>();

	public BlockEntityRendererRegistryEventObject() {
	}

	/**
	 * @deprecated Does not work in 1.17+, see addEntity.
	 */
	@Deprecated
	public BlockEntityRendererRegistryEventObject(Object dispatcher, TextureManager textureManager, Font font,
			Options options) {
		this.dispatcher = (BlockEntityRenderDispatcher) dispatcher;
		this.textureManager = textureManager;
		this.options = options;
		this.font = font;
	}

	/**
	 * Retrieves the block entity dispatcher
	 * 
	 * @deprecated Does not work in 1.17+, see addEntity.
	 */
	@Deprecated
	public BlockEntityRenderDispatcher getDispatcher() {
		if (Version.fromString(Modloader.getModloaderGameVersion()).isGreaterOrEqualTo(Version.fromString("1.17")))
			throw new RuntimeException("BlockEntityRendererRegistryEventObject.getDispatcher() does not work in 1.17+");
		warn("DEPRECATION NOTICE: A mod called BlockEntityRendererRegistryEventObject.getDispatcher(), it has been deprecated since Minecraft 1.17 was released, it should not be used.");

		return dispatcher;
	}

	/**
	 * Retrieves the texture manager
	 * 
	 * @deprecated Does not work in 1.17+, see addEntity.
	 */
	@Deprecated
	public TextureManager getTextureManager() {
		if (Version.fromString(Modloader.getModloaderGameVersion()).isGreaterOrEqualTo(Version.fromString("1.17")))
			throw new RuntimeException(
					"BlockEntityRendererRegistryEventObject.getTextureManager() does not work in 1.17+");
		warn("DEPRECATION NOTICE: A mod called BlockEntityRendererRegistryEventObject.getTextureManager(), it has been deprecated since Minecraft 1.17 was released, it should not be used.");

		return textureManager;
	}

	/**
	 * Adds custom block entity renderers to the game
	 * 
	 * @param <T>        Entity Class Type
	 * @param entityType Entity type
	 * @param renderer   Entity renderer
	 * @deprecated Please use <code>addEntity(type, constructor)</code>
	 */
	@Deprecated
	public <T extends BlockEntity> void addEntity(BlockEntityType<? extends T> entityType,
			BlockEntityRenderer<? extends T> renderer) {
		if (Version.fromString(Modloader.getModloaderGameVersion()).isGreaterOrEqualTo(Version.fromString("1.17")))
			throw new RuntimeException(
					"BlockEntityRendererRegistryEventObject.addEntity(type, renderer) does not work in 1.17+");
		warn("DEPRECATION NOTICE: A mod called BlockEntityRendererRegistryEventObject.addEntity(type, renderer), it has been deprecated since Minecraft 1.17 was released, it should not be used.");

		this.<T>addEntity(entityType, (ctx) -> {
			return renderer;
		});
	}

	/**
	 * Adds custom entity renderers to the game
	 * 
	 * @param <T>         Entity Class Type
	 * @param entityType  Entity type
	 * @param constructor Entity constructor function, receives the context,
	 *                    supplies the new renderer instance.
	 * @since ModKit 1.1
	 */
	public <T extends BlockEntity> void addEntity(BlockEntityType<? extends T> entityType,
			Function<BlockEntityRendererContext<T>, BlockEntityRenderer<? extends T>> constructor) {
		this.entities.put(entityType, constructor);
	}

	/**
	 * Retrieves the client options
	 * 
	 * @deprecated Does not work in 1.17+, see addEntity.
	 */
	@Deprecated
	public Options getClientOptions() {
		if (Version.fromString(Modloader.getModloaderGameVersion()).isGreaterOrEqualTo(Version.fromString("1.17")))
			throw new RuntimeException(
					"BlockEntityRendererRegistryEventObject.getClientOptions() does not work in 1.17+");
		warn("DEPRECATION NOTICE: A mod called BlockEntityRendererRegistryEventObject.getClientOptions(), it has been deprecated since Minecraft 1.17 was released, it should not be used.");

		return options;
	}

	/**
	 * Retrieves the map of registered mod entity renderers
	 * 
	 * @deprecated Will not work properly in 1.17+, avoid usage, forces construction
	 *             of all entity renderers will forged context.
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	public Map<BlockEntityType<?>, BlockEntityRenderer<?>> getEntities() {
		if (Version.fromString(Modloader.getModloaderGameVersion()).isGreaterOrEqualTo(Version.fromString("1.17")))
			throw new RuntimeException("BlockEntityRendererRegistryEventObject.getEntities() does not work in 1.17+");

		entities.forEach((type, constr) -> {
			@SuppressWarnings("rawtypes")
			BlockEntityRendererContext ctx = new BlockEntityRendererContext(font, textureManager, dispatcher, options,
					type, new Object[0]);
			BlockEntityRenderer<?> inst = (BlockEntityRenderer<?>) constr.apply(ctx);
			rendererInstances.put(type, inst);
		});
		entities.clear();
		return new HashMap<BlockEntityType<?>, BlockEntityRenderer<?>>(rendererInstances);
	}

	/**
	 * Retrieves the map of block entity constructors
	 * 
	 * @since ModKit 1.1
	 */
	@SuppressWarnings({ "unchecked" })
	public Map<BlockEntityType<?>, Function<BlockEntityRendererContext<?>, BlockEntityRenderer<?>>> getEntityConstructors() {
		HashMap<BlockEntityType<?>, Function<BlockEntityRendererContext<?>, BlockEntityRenderer<?>>> constructors = new HashMap<BlockEntityType<?>, Function<BlockEntityRendererContext<?>, BlockEntityRenderer<?>>>();
		entities.forEach((type, func) -> {
			constructors.put(type, func);
		});
		entities.clear();
		return constructors;
	}

	/**
	 * Retrieves the font
	 * 
	 * @deprecated Does not work in 1.17+, see addEntity.
	 */
	@Deprecated
	public Font getFont() {
		if (Version.fromString(Modloader.getModloaderGameVersion()).isGreaterOrEqualTo(Version.fromString("1.17")))
			throw new RuntimeException("BlockEntityRendererRegistryEventObject.getFont() does not work in 1.17+");
		warn("DEPRECATION NOTICE: A mod called BlockEntityRendererRegistryEventObject.getFont(), it has been deprecated since Minecraft 1.17 was released, it should not be used.");

		return font;
	}

}
