package org.asf.cyan.minecraft.toolkits.mtk.rift.providers;

import java.io.File;
import java.io.IOException;

import org.asf.cyan.fluid.remapping.Mapping;

public interface IRiftToolchainProvider {
	public File getJar() throws IOException;
	public File[] getLibraries() throws IOException;
	public Mapping<?> getRiftMappings() throws IOException; 
}
