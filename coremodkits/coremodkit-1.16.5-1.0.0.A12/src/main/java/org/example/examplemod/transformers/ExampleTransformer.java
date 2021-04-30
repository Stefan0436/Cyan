package org.example.examplemod.transformers;

import org.asf.cyan.api.fluid.annotations.PlatformExclude;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;

import org.apache.logging.log4j.Logger;

// Lets create a FLUID transformer class, class names don't matter but it is recommended to create a clear one
// For injections, you should use an abstract class.
// 
// This example reflects the mixin example of our dated forge mixin template repository:
// https://github.com/Stefan0436/ForgeMixinModTemplate/blob/main/src/main/java/com/example/examplemod/mixins/ExampleMixin.java
//
// @TargetClass specifies the target class
// @PlatformExclude prevents this version from loading on spigot platforms because it is incompatible
@FluidTransformer
@TargetClass(target = "net.minecraft.server.MinecraftServer")
@PlatformExclude(LaunchPlatform.SPIGOT)
public abstract class ExampleTransformer {

	// Gets the server logger, only reflects, does not assing values.
	private static Logger LOGGER = null;

	// The method to get the overworld dimension, reflecting the server method
	public abstract ServerLevel overworld();

	//
	// Lets inject code at the end (TAIL) of the prepareLevels method
	//
	// First you create a method with the same modifiers, the name must match the
	// name of the method you are injecting in.
	//
	// If you want to inject into the same method more than once,
	// use @TargetName(target = "<target method>") to set the target.
	//
	// When using that annotation, the method name does not matter.
	//
	//
	// Add the original method parameters, naming does not matter.
	// You will need to specify @TargetType for game types or FLUID won't be
	// able to figure out what is what.
	//
	// Then you add the @InjectAt annotation with the information:
	// @Inject(location = InjectLocation.TAIL) - Inject at end of method
	// @Inject(location = InjectLocation.HEAD) - Inject at beginning of method
	//
	// After that, in the new method, you can write the code you want to inject.
	@InjectAt(location = InjectLocation.TAIL)
	private void prepareLevels(
			@TargetType(target = "net.minecraft.server.level.progress.ChunkProgressListener") ChunkProgressListener listener) {
		ServerLevel world = this.overworld(); // See MinecraftServer#prepareLevels (first line)
		BlockPos spawn = world.getSharedSpawnPos(); // Get the world spawn
		String loc = world.dimension().location().toString(); // Get the dimension name

		// Lets print the world and spawn to the console:
		LOGGER.info("loadInitialChunks finished, World: " + loc + ", SPAWN: " + spawn.getX() + ":" + spawn.getY());
		
		
		// IMPORTANT NOTIVE:
		// FLUID is still in the alpha development stage and REALLY buggy.
		//
		// You can make modifications to game code, but it is recommended to have complicated
		// modification code in separate classes. (non-transformer classes)
	}
}
