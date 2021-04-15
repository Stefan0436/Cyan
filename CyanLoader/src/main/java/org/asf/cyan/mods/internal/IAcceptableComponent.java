package org.asf.cyan.mods.internal;

import org.asf.cyan.api.modloader.IModloaderComponent;

public interface IAcceptableComponent extends IModloaderComponent {
	public String[] providers();
	public Object provide(String provider);
}
