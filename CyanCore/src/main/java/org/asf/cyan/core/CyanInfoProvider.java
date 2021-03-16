package org.asf.cyan.core;

import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.api.modloader.information.modloader.LoadPhase;
import org.asf.cyan.api.modloader.information.providers.IGameProvider;
import org.asf.cyan.api.modloader.information.providers.ILaunchPlatformProvider;
import org.asf.cyan.api.modloader.information.providers.IVersionProvider;
import org.asf.cyan.api.modloader.information.providers.IPhaseProvider;
import org.asf.cyan.api.modloader.information.providers.IVersionChangelogProvider;
import org.asf.cyan.api.modloader.information.providers.IVersionStatusProvider;
import org.asf.cyan.api.versioning.VersionStatus;

public class CyanInfoProvider implements IVersionProvider, IVersionStatusProvider, IVersionChangelogProvider,
		IGameProvider, ILaunchPlatformProvider, IPhaseProvider {

	@Override
	public LoadPhase getPhase() {
		return CyanInfo.getCurrentPhase();
	}

	@Override
	public LaunchPlatform getPlatform() {
		return CyanInfo.getPlatform();
	}

	@Override
	public GameSide getGameSide() {
		return CyanInfo.getSide();
	}

	@Override
	public String getGameName() {
		return "Minecraft";
	}

	@Override
	public String getCurrentVersionChangelog() {
		return CyanInfo.getVersionChangelog();
	}

	@Override
	public String getUpdateVersionChangelog() {
		return CyanInfo.getVersionUpdateChangelog();
	}

	@Override
	public VersionStatus getModloaderVersionStatus() {
		return CyanInfo.getModloaderVersionStatus();
	}

	@Override
	public String getModloaderVersion() {
		return CyanInfo.getCyanVersion();
	}

	@Override
	public String getGameVersion() {
		return CyanInfo.getMinecraftVersion();
	}

}
