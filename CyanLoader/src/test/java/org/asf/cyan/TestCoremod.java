package org.asf.cyan;

import org.asf.cyan.api.modloader.TargetModloader;
import org.asf.cyan.mods.AbstractCoremod;

@TargetModloader(value = CyanLoader.class, any = true)
public class TestCoremod extends AbstractCoremod {
	
	@Override
	protected void setupCoremod() {
	}

}
