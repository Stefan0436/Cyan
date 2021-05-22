package org.asf.cyan.api.protocol;

import java.util.HashMap;
import java.util.Optional;

import org.asf.cyan.api.modloader.Modloader;

/**
 * 
 * ModKit Modloader - Extension interface for the Modloader abstract.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 * @since 1.1
 *
 */
public abstract class ModkitModloader extends Modloader {
	public static ModkitModloader getModKitRootModloader() {
		if (RootModloader.getRoot() == null)
			RootModloader.select();
		return RootModloader.getRoot();
	}

	/**
	 * 
	 * ModKit Protocol Rules - defines the compatible protocols of a ModKit root
	 * modloader
	 * 
	 * @author Stefan0436 - AerialWorks Software Foundation
	 * @since 1.1
	 *
	 */
	public static interface ModkitProtocolRules {
		public double modloaderProtocol();

		public double modloaderMinProtocol();

		public double modloaderMaxProtocol();

		public double modkitProtocolVersion();
	}

	/**
	 * 
	 * Modloader Root Rules - Rules needed to participate for becoming the root
	 * modloader.
	 * 
	 * @author Stefan0436 - AerialWorks Software Foundation
	 * @since 1.1
	 *
	 */
	protected static class RootRules {
		private int level = 10;

		public static RootRules defaultRules() {
			return new RootRules();
		}

		/**
		 * Use if your modloader will malfunction if it is not the root modloader.
		 */
		public RootRules requireRoot() {
			level = 0;
			return this;
		}

		/**
		 * Sets the rquirement level, default is 10
		 * 
		 * @param level Requirement level, the lower, the earlier the loader is selected
		 */
		public RootRules level(int level) {
			this.level = level;
			return this;
		}
	}

	/**
	 * 
	 * ModKit Root Loader Interface
	 * 
	 * @author Stefan0436 - AerialWorks Software Foundation
	 * @since 1.1
	 *
	 */
	protected static class RootModloader {
		private static ModkitModloader root = null;
		private static HashMap<ModkitModloader, RootRules> participants = new HashMap<ModkitModloader, RootRules>();

		/**
		 * Makes the modloader a participant for becoming the ModKit Root Modloader.
		 * 
		 * @param modloader The modloader instance
		 */
		public static <T extends ModkitModloader & ModkitProtocolRules> void participate(T modloader, RootRules rules) {
			if (modloader.modkitProtocolVersion() < ModKitProtocol.MIN_PROTOCOL) {
				throw new IncompatibleProtocolException(modloader.getName() + " uses dated protocol "
						+ modloader.modkitProtocolVersion() + ", ModKit requires " + ModKitProtocol.MIN_PROTOCOL + "+");
			}
			info(modloader.getName() + " participated for becoming the root modloader with selection level "
					+ rules.level + ", modloader protocol version: " + modloader.modloaderProtocol());
			if (rules.level == 0) {
				if (participants.values().stream().anyMatch(t -> t.level == 0)) {
					throw new IncompatibleProtocolException(modloader.getName()
							+ " is required to be the root modloader but another has already received the root modloader slot.");
				}
			}
			participants.put(modloader, rules);
		}

		static void select() {
			Optional<ModkitModloader> root = participants.keySet().stream().sorted((t1, t2) -> {
				int level1 = participants.get(t1).level;
				int level2 = participants.get(t2).level;
				if (participants.get(t1).level != 0 && participants.get(t2).level != 0) {
					if (((ModkitProtocolRules) t1).modkitProtocolVersion() > ((ModkitProtocolRules) t2)
							.modkitProtocolVersion())
						level1--;
					if (((ModkitProtocolRules) t2).modkitProtocolVersion() > ((ModkitProtocolRules) t1)
							.modkitProtocolVersion())
						level2--;
				}
				return Integer.compare(level1, level2);
			}).findFirst();
			if (root.isPresent()) {
				RootModloader.root = root.get();
				info(RootModloader.root.getName() + " has been chosen as root modloader.");
			}
		}

		/**
		 * Retrieves the root modloader
		 * 
		 * @return Root modloader or null
		 */
		public static ModkitModloader getRoot() {
			return root;
		}
	}
}
