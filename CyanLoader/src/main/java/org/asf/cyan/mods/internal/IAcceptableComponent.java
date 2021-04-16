package org.asf.cyan.mods.internal;

import org.asf.cyan.api.modloader.IModloaderComponent;

public interface IAcceptableComponent extends IModloaderComponent {
	public String[] providers();
	public Object provide(String provider);
	
	public default String[] infoRequests() {
		return new String[0];
	}
	
	public default void provideInfo(Object data, String name) {
	}
}
