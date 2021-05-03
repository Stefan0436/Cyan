package org.asf.cyan.api.internal;

import org.asf.cyan.api.commands.CommandManager;
import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.events.core.ReloadEvent;
import org.asf.cyan.api.events.core.ReloadPrepareEvent;
import org.asf.cyan.api.events.entities.EntityRegistryEvent;
import org.asf.cyan.api.events.entities.EntityRendererRegistryEvent;
import org.asf.cyan.api.events.ingame.commands.CommandManagerStartupEvent;
import org.asf.cyan.api.events.ingame.crafting.RecipeManagerStartupEvent;
import org.asf.cyan.api.events.ingame.tags.TagManagerStartupEvent;
import org.asf.cyan.api.events.objects.core.ReloadEventObject;
import org.asf.cyan.api.events.objects.entities.EntityRegistryEventObject;
import org.asf.cyan.api.events.objects.entities.EntityRendererRegistryEventObject;
import org.asf.cyan.api.events.objects.ingame.commands.CommandManagerEventObject;
import org.asf.cyan.api.events.objects.ingame.crafting.RecipeManagerEventObject;
import org.asf.cyan.api.events.objects.ingame.tags.TagManagerEventObject;
import org.asf.cyan.api.events.objects.resources.LanguageManagerEventObject;
import org.asf.cyan.api.events.objects.resources.ResourceManagerEventObject;
import org.asf.cyan.api.events.objects.resources.ResourcePackEventObject;
import org.asf.cyan.api.events.objects.resources.TextureManagerEventObject;
import org.asf.cyan.api.events.resources.manager.LanguageManagerStartupEvent;
import org.asf.cyan.api.events.resources.manager.ResourceManagerStartupEvent;
import org.asf.cyan.api.events.resources.manager.TextureManagerStartupEvent;
import org.asf.cyan.api.events.resources.modresources.ModResourcePackLoadEvent;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;
import org.asf.cyan.mods.internal.BaseEventController;

import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.server.MinecraftServer;

@CYAN_COMPONENT
public class TestEventListeners extends CyanComponent implements IEventListenerContainer {
	protected static void initComponent() {
		BaseEventController.addEventContainer(new TestEventListeners());

		CommandManager manager = CommandManager.getMain().newInstance();
	}

	@SimpleEvent(ReloadEvent.class)
	public void reload(ReloadEventObject event) {
		event = event; // DONE
	}

	@SimpleEvent(ReloadPrepareEvent.class)
	public void reloadPrepare(ReloadEventObject event) {
		event = event; // DONE
	}

	@SimpleEvent(CommandManagerStartupEvent.class)
	public void test(CommandManagerEventObject event) {
		event = event; // DONE
	}

	@SimpleEvent(RecipeManagerStartupEvent.class)
	public void test(RecipeManagerEventObject event) {
		event = event; // DONE
	}

	@SimpleEvent(TagManagerStartupEvent.class)
	public void test(TagManagerEventObject event) {
		event = event; // DONE
	}

	@SimpleEvent(TextureManagerStartupEvent.class)
	public void test(TextureManagerEventObject event) {
		event = event;
	}

	@SimpleEvent(ResourceManagerStartupEvent.class)
	public void test(ResourceManagerEventObject event) {
		event = event; // DONE
	}

	@SimpleEvent(LanguageManagerStartupEvent.class)
	public void test(LanguageManagerEventObject event) {
		event = event;
	}

	@SimpleEvent(ModResourcePackLoadEvent.class)
	public void test(ResourcePackEventObject event) {
		event = event;
	}

	@SimpleEvent(EntityRendererRegistryEvent.class)
	public void test(EntityRendererRegistryEventObject event) {
		event = event;
	}

	@SimpleEvent(EntityRegistryEvent.class)
	public void test(EntityRegistryEventObject event) {
		event = event; // DONE
	}
}
