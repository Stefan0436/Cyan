package org.asf.cyan.api.internal.modkit.transformers._1_16.common.world.storage;

public interface LevelModDataReader {
	public String[] cyanGetAllLoaders();
	public ModloaderMeta cyanGetLoader(String loader);
}
