package org.asf.cyan.mixin.providers;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.spongepowered.asm.launch.platform.container.IContainerHandle;

public class CyanContainerHandle implements IContainerHandle {
	public HashMap<String, Object> attributes = new HashMap<String, Object>();

	@Override
	public String getAttribute(String name) {
		return (attributes.get(name) == null ? null : attributes.get(name).toString());
	}

	public void setAttribute(String name, Object value) {
		attributes.put(name, value);
	}

	@Override
	public Collection<IContainerHandle> getNestedContainers() {
		return Arrays.asList();
	}

}
