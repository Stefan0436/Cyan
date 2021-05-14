package org.asf.cyan;

import org.asf.cyan.api.config.Configuration;
import org.asf.cyan.api.modloader.information.game.GameSide;

public class KickStartConfig extends Configuration<KickStartConfig> {

	@Override
	public String filename() {
		return null;
	}

	@Override
	public String folder() {
		return null;
	}

	public KickStartInstallation[] installations = new KickStartInstallation[0];

	public static class KickStartInstallation extends Configuration<KickStartInstallation> {

		@Override
		public String filename() {
			return null;
		}

		@Override
		public String folder() {
			return null;
		}

		public GameSide side;
		
		public String cyanData;
		public String gameVersion;

		public String loaderVersion;
		public String platform;
		public String platformVersion;

	}

}
