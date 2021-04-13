package org.asf.cyan.mods.internal;

import org.asf.cyan.api.modloader.IModloaderComponent;

public interface IAcceptableComponent extends IModloaderComponent {
	public String[] providers();
	
	public default String[] earlyInfoRequests() {
		return new String[0];
	}
	
	public default void provideInfo(String name, Object data) {
	}
	
	public String executionKey();

	public Object provide(String provider);
}
