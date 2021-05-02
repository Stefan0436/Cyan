package org.asf.cyan.api.internal.modkit.transformers._1_15_2.client;

import org.asf.cyan.api.events.objects.resources.LanguageManagerEventObject;
import org.asf.cyan.api.events.objects.resources.ResourceManagerEventObject;
import org.asf.cyan.api.events.objects.resources.TextureManagerEventObject;
import org.asf.cyan.api.events.resources.manager.LanguageManagerStartupEvent;
import org.asf.cyan.api.events.resources.manager.ResourceManagerStartupEvent;
import org.asf.cyan.api.events.resources.manager.TextureManagerStartupEvent;
import org.asf.cyan.api.internal.modkit.components._1_15_2.common.CyanReloadListener;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Constructor;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import net.minecraft.client.main.GameConfig;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.server.packs.resources.ReloadableResourceManager;

@FluidTransformer
@TargetClass(target = "net.minecraft.client.Minecraft")
public class MinecraftModification {

	private final ReloadableResourceManager resourceManager = null;
	private final LanguageManager languageManager = null;
	private final TextureManager textureManager = null;

	@Constructor
	@InjectAt(location = InjectLocation.HEAD, targetCall = "reload()", targetOwner = "net.minecraft.server.packs.repository.PackRepository")
	public void init1(@TargetType(target = "net.minecraft.client.main.GameConfig") GameConfig config) {
		ResourceManagerStartupEvent.getInstance().dispatch(new ResourceManagerEventObject(resourceManager)).getResult();
	}

	@Constructor
	@InjectAt(location = InjectLocation.HEAD, targetCall = "registerReloadListener(net.minecraft.server.packs.resources.PreparableReloadListener)", targetOwner = "net.minecraft.server.packs.resources.ReloadableResourceManager")
	public void init2(@TargetType(target = "net.minecraft.client.main.GameConfig") GameConfig config) {
		LanguageManagerStartupEvent.getInstance().dispatch(new LanguageManagerEventObject(languageManager)).getResult();
	}

	@Constructor
	@InjectAt(location = InjectLocation.HEAD, targetCall = "registerReloadListener(net.minecraft.server.packs.resources.PreparableReloadListener)", targetOwner = "net.minecraft.server.packs.resources.ReloadableResourceManager", offset = 1)
	public void init5(@TargetType(target = "net.minecraft.client.main.GameConfig") GameConfig config) {
		TextureManagerStartupEvent.getInstance().dispatch(new TextureManagerEventObject(textureManager)).getResult();
	}

	@Constructor
	@InjectAt(location = InjectLocation.TAIL, targetCall = "<init>(net.minecraft.client.Minecraft)", targetOwner = "net.minecraft.client.gui.Gui")
	public void init4(@TargetType(target = "net.minecraft.client.main.GameConfig") GameConfig config) {
		resourceManager.registerReloadListener(new CyanReloadListener());
	}

}
