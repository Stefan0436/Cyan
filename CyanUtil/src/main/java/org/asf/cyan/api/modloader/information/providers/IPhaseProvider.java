package org.asf.cyan.api.modloader.information.providers;

import org.asf.cyan.api.modloader.information.modloader.LoadPhase;

public interface IPhaseProvider extends IModloaderInfoProvider {
	public LoadPhase getPhase();
}
