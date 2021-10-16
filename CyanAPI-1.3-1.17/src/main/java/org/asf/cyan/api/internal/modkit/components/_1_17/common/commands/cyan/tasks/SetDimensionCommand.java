package org.asf.cyan.api.internal.modkit.components._1_17.common.commands.cyan.tasks;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import modkit.commands.Command;
import modkit.commands.CommandManager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class SetDimensionCommand implements Command {

	@Override
	public String getPermission() {
		return "cyan.commands.admin.cyan.shift";
	}

	@Override
	public String getId() {
		return "shift";
	}

	@Override
	public String getDisplayName() {
		return null;
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public String getUsage() {
		return "<dimension> [<x> <y> <z>] [<yaw> <pitch>]";
	}

	@Override
	public ArgumentBuilder<CommandSourceStack, ?> setupCommand(CommandManager manager,
			LiteralArgumentBuilder<CommandSourceStack> cmd) {

		CommandContainer container = CommandContainer.getFor(this);
		container.add(Commands.argument("dimension", DimensionArgument.dimension()));
		container.attachPermission();
		container.attachExecutionEngine();
		container.closeEntry();

		container.add(Commands.argument("dimension", DimensionArgument.dimension()));
		container.add(Commands.argument("coordinates", Vec3Argument.vec3()));
		container.attachPermission();
		container.attachExecutionEngine();
		container.closeEntry();

		container.add(Commands.argument("dimension", DimensionArgument.dimension()));
		container.add(Commands.argument("coordinates", Vec3Argument.vec3()));
		container.add(Commands.argument("rotation", RotationArgument.rotation()));
		container.attachPermission();
		container.attachExecutionEngine();

		return container.build(cmd);
	}

	@Override
	public int execute(CommandExecutionContext context) {
		if (context.getPlayer() != null) {
			if (context.getArgument("coordinates", Coordinates.class) == null)
				context.getPlayer().teleportTo(
						context.getServer()
								.getLevel(ResourceKey.create(Registry.DIMENSION_REGISTRY,
										context.getArgument("dimension", ResourceLocation.class))),
						context.getPlayer().position().x, context.getPlayer().position().y,
						context.getPlayer().position().z, context.getPlayer().getYRot(), context.getPlayer().getXRot());
			else {
				Vec3 v = context.getArgument("coordinates", Coordinates.class)
						.getPosition(context.toGameType().getSource());

				float y = context.getPlayer().getYRot();
				float p = context.getPlayer().getXRot();
				if (context.getArgument("rotation", Coordinates.class) != null) {
					Vec2 r = context.getArgument("rotation", Coordinates.class)
							.getRotation(context.toGameType().getSource());
					y = r.y;
					p = r.x;
				}

				context.getPlayer().teleportTo(
						context.getServer()
								.getLevel(ResourceKey.create(Registry.DIMENSION_REGISTRY,
										context.getArgument("dimension", ResourceLocation.class))),
						v.x, v.y, v.z, y, p);
			}

			return 0;
		}
		return 1;
	}

}
