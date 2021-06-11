package org.asf.cyan.modifications._1_17.common.forge;

import java.nio.file.Path;
import java.util.ArrayList;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.fluid.annotations.PlatformOnly;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Erase;
import org.asf.cyan.fluid.api.transforming.TargetClass;

@FluidTransformer
@PlatformOnly(LaunchPlatform.MCP)
@TargetClass(target = "net.minecraftforge.fml.loading.FMLLoader")
public class FMLLoaderModification {
    
	private static Path[] mcPaths;
    
	@Erase
	public static Path[] getMCPaths() {
        ArrayList<Path> paths = new ArrayList<Path>();
        CyanLoader.addCyanPaths(paths);
        for (Path pth : mcPaths) {
        	paths.add(pth);
        }
        return paths.toArray(t -> new Path[t]);
    }
}
