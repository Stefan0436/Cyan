package org.asf.cyan.api.internal.modkit.transformers._1_16.common.resources;

import modkit.events.objects.resources.ResourceManagerEventObject;
import modkit.events.resources.manager.ResourceManagerStartupEvent;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public class CyanResourceEventListener extends SimplePreparableReloadListener<Void> {

	private boolean reloaded = false;
	
	@Override
	protected void apply(Void arg0, ResourceManager arg1, ProfilerFiller arg2) {
		if (reloaded)
			return;
		reloaded = true;
		ResourceManagerStartupEvent.getInstance().dispatch(new ResourceManagerEventObject(arg1)).getResult();
	}

	@Override
	protected Void prepare(ResourceManager arg0, ProfilerFiller arg1) {
		return null;
	}

}
