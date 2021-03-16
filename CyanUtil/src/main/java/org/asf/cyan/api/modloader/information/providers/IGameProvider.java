package org.asf.cyan.api.modloader.information.providers;

import org.asf.cyan.api.modloader.information.game.GameSide;

public interface IGameProvider extends IModloaderInfoProvider {
	public String getGameVersion();
	public GameSide getGameSide();
	public String getGameName();
}
