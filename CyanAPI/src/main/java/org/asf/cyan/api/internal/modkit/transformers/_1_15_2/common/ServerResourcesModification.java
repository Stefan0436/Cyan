package org.asf.cyan.api.internal.modkit.transformers._1_15_2.common;

import org.asf.cyan.api.events.ingame.commands.CommandManagerStartupEvent;
import org.asf.cyan.api.events.ingame.crafting.RecipeManagerStartupEvent;
import org.asf.cyan.api.events.ingame.tags.TagManagerStartupEvent;
import org.asf.cyan.api.events.objects.ingame.commands.CommandManagerEventObject;
import org.asf.cyan.api.events.objects.ingame.crafting.RecipeManagerEventObject;
import org.asf.cyan.api.events.objects.ingame.tags.TagManagerEventObject;
import org.asf.cyan.api.events.objects.resources.ResourceManagerEventObject;
import org.asf.cyan.api.events.resources.manager.ResourceManagerStartupEvent;
import org.asf.cyan.api.internal.modkit.components._1_15_2.common.CyanReloadListener;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Constructor;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import net.minecraft.commands.Commands;
import net.minecraft.commands.Commands.CommandSelection;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.tags.TagManager;
import net.minecraft.world.item.crafting.RecipeManager;

@FluidTransformer
@TargetClass(target = "net.minecraft.server.ServerResources")
public class ServerResourcesModification {

	private final ReloadableResourceManager resources = null;
	private final RecipeManager recipes = null;
	private final TagManager tagManager = null;
	private final Commands commands = null;

	@Constructor
	@InjectAt(location = InjectLocation.HEAD, targetCall = "<init>()", targetOwner = "net.minecraft.world.item.crafting.RecipeManager")
	public void init1(@TargetType(target = "net.minecraft.commands.Commands.CommandSelection") CommandSelection var1,
			int var2) {
		ResourceManagerStartupEvent.getInstance().dispatch(new ResourceManagerEventObject(resources)).getResult();
	}

	@Constructor
	@InjectAt(location = InjectLocation.HEAD, targetCall = "<init>()", targetOwner = "net.minecraft.world.item.crafting.RecipeManager")
	public void init2(@TargetType(target = "net.minecraft.commands.Commands.CommandSelection") CommandSelection var1,
			int var2) {
		RecipeManagerStartupEvent.getInstance().dispatch(new RecipeManagerEventObject(recipes)).getResult();
	}

	@Constructor
	@InjectAt(location = InjectLocation.HEAD, targetCall = "<init>()", targetOwner = "net.minecraft.world.item.crafting.RecipeManager")
	public void init3(@TargetType(target = "net.minecraft.commands.Commands.CommandSelection") CommandSelection var1,
			int var2) {
		TagManagerStartupEvent.getInstance().dispatch(new TagManagerEventObject(tagManager)).getResult();
	}

	@Constructor
	@InjectAt(location = InjectLocation.HEAD, targetCall = "<init>()", targetOwner = "net.minecraft.world.item.crafting.RecipeManager")
	public void init4(@TargetType(target = "net.minecraft.commands.Commands.CommandSelection") CommandSelection var1,
			int var2) {
		CommandManagerStartupEvent.getInstance().dispatch(new CommandManagerEventObject(commands)).getResult();
	}

	@Constructor
	@InjectAt(location = InjectLocation.TAIL)
	public void init5(@TargetType(target = "net.minecraft.commands.Commands.CommandSelection") CommandSelection var1,
			int var2) {
		resources.registerReloadListener(new CyanReloadListener());
	}

}
