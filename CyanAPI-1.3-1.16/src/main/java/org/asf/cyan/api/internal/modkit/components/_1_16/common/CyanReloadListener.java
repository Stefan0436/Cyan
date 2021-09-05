package org.asf.cyan.api.internal.modkit.components._1_16.common;

import modkit.events.core.ReloadEvent;
import modkit.events.core.ReloadPrepareEvent;
import modkit.events.objects.core.ReloadEventObject;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public class CyanReloadListener extends SimplePreparableReloadListener<ReloadEventObject> {

	@Override
	protected void apply(ReloadEventObject object, ResourceManager resourceManager, ProfilerFiller filler) {
		ReloadEvent.getInstance().dispatch(object).getResult();
	}

	@Override
	protected ReloadEventObject prepare(ResourceManager resourceManager, ProfilerFiller filler) {
		ReloadEventObject ev = new ReloadEventObject(resourceManager, filler);
		ReloadPrepareEvent.getInstance().dispatch(ev).getResult();
		return ev;
	}

}
