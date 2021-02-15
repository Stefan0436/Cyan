package org.asf.cyan.mixin;

import java.util.Arrays;
import java.util.Collection;

import org.spongepowered.asm.launch.platform.container.IContainerHandle;

public class CyanContainerHandle implements IContainerHandle {

	
	//FIXME: Proper implementation
	
	
	@Override
	public String getAttribute(String name) {
		return "";
	}

	@Override
	public Collection<IContainerHandle> getNestedContainers() {
		return Arrays.asList();
	}

}
