package org.asf.cyan.minecraft.toolkits.mtk.internal;

import org.asf.cyan.api.events.IEventProvider;
import org.asf.cyan.api.modloader.IModloaderComponent;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.TargetModloader;

@TargetModloader(any = true, value = Modloader.class)
public class MappingsLoadEventProvider implements IEventProvider, IModloaderComponent {
	private static boolean accepted = false;
	
	public static boolean isAccepted() {
		return accepted;
	}
	
	@Override
	public String getChannelName() {
		accepted = true;
		return "mtk.mappings.loaded";
	}

}
