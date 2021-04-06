package org.asf.cyan;

import org.asf.cyan.api.modloader.TargetModloader;
import org.asf.cyan.mods.AbstractCoremod;

@TargetModloader(value = CyanLoader.class, any = true)
public class TestCoremod extends AbstractCoremod {
	@Override
	public String executionKey() {
		return "5644545814586435465416976854";
	}
}
