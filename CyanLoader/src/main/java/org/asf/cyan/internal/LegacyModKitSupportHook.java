package org.asf.cyan.internal;

import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.cyan.core.CyanCore;
import org.asf.cyan.fluid.api.ClassLoadHook;
import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;

public class LegacyModKitSupportHook extends ClassLoadHook {

	public static class ModKitClassRemapper extends Remapper {

		private boolean ran = false;
		private Logger log;

		private HashMap<String, String> packages;
		private HashMap<String, String> types;

		private String name;

		public ModKitClassRemapper(HashMap<String, String> packages, HashMap<String, String> types, String name) {
			this.packages = packages;
			this.types = types;

			this.name = name;
			log = LogManager.getLogger("ModKit " + name + " Support");
		}

		@Override
		public String map(String internal) {
			if (types.containsKey(internal.replace("/", "."))) {
				if (!ran) {
					log.warn("Applying support remappers for ModKit " + name + " Mods (LEGACY MODS PRESENT)");
				}
				ran = true;
				internal = types.get(internal.replace("/", ".")).replace(".", "/");
			}
			String pkg = internal.substring(0, internal.lastIndexOf("/")).replace("/", ".");
			if (packages.keySet().stream().anyMatch(t -> t.equals(pkg) || pkg.startsWith(t + "."))) {
				if (!ran) {
					log.warn("Applying support remappers for ModKit " + name + " Mods (LEGACY MODS PRESENT)");
				}
				ran = true;

				for (String remap : packages.keySet()) {
					if (remap.equals(pkg) || pkg.startsWith(remap + ".")) {
						internal = packages.get(remap).replace(".", "/") + "/" + internal.substring(remap.length() + 1);
						break;
					}
				}
			}
			return internal;
		}

	}

	private ArrayList<ModKitClassRemapper> remappers = new ArrayList<ModKitClassRemapper>();

	@Override
	public String targetPath() {
		return "@ANY";
	}

	@Override
	public boolean isSilent() {
		return true;
	}

	@Override
	public void build() {
		HashMap<String, String> packages = new HashMap<String, String>();
		HashMap<String, String> types = new HashMap<String, String>();
		packages.put("org.asf.cyan.api.advanced", "modkit.advanced");
		packages.put("org.asf.cyan.api.commands", "modkit.commands");
		types.put("org.asf.cyan.api.config.ConfigManager", "modkit.config.ConfigManager");
		types.put("org.asf.cyan.api.config.ModConfiguration", "modkit.config.ModConfiguration");
		types.put("org.asf.cyan.api.events.core.DataFixerEvent", "modkit.events.core.DataFixerEvent");
		types.put("org.asf.cyan.api.events.core.ReloadEvent", "modkit.events.core.ReloadEvent");
		types.put("org.asf.cyan.api.events.core.ReloadPrepareEvent", "modkit.events.core.ReloadPrepareEvent");
		types.put("org.asf.cyan.api.events.core.ReloadShutdownEvent", "modkit.events.core.ReloadShutdownEvent");
		packages.put("org.asf.cyan.api.events.ingame", "modkit.events.ingame");
		packages.put("org.asf.cyan.api.events.network", "modkit.events.network");
		packages.put("org.asf.cyan.api.events.objects", "modkit.events.objects");
		packages.put("org.asf.cyan.api.events.resources", "modkit.events.resources");
		packages.put("org.asf.cyan.api.network", "modkit.network");
		packages.put("org.asf.cyan.api.permissions", "modkit.permissions");
		packages.put("org.asf.cyan.api.resources", "modkit.resources");
		packages.put("org.asf.cyan.api.threading", "modkit.threading");
		packages.put("org.asf.cyan.api.util", "modkit.util");
		types.put("org.asf.cyan.api.ApiComponent", "modkit.ApiComponent");
		types.put("org.asf.cyan.api.events.network.CyanClientHandshakeEvent",
				"org.asf.cyan.api.events.network.ModKitClientHandshakeEvent");
		types.put("org.asf.cyan.api.events.network.CyanServerHandshakeEvent",
				"org.asf.cyan.api.events.network.ModKitServerHandshakeEvent");
		remappers.add(new ModKitClassRemapper(packages, types, "1.0"));
	}

	@Override
	public void apply(ClassNode cc, FluidClassPool cp, ClassLoader loader, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws ClassNotFoundException {
		for (ModKitClassRemapper remapper : remappers) {
			if (cc.superName != null) {
				cc.superName = remapper.map(cc.superName);
				CyanCore.setSupertype(cc.name, cc.superName);
			}
			if (cc.interfaces != null) {
				ArrayList<String> interfaces = new ArrayList<String>();
				for (String inter : cc.interfaces) {
					interfaces.add(remapper.map(inter));
					CyanCore.setSupertype(cc.name, remapper.map(inter));
				}
				cc.interfaces = interfaces;
			}
			ClassWriter writer = new ClassWriter(0);
			ClassVisitor clremapper = new ClassRemapper(writer, remapper);
			cc.accept(clremapper);
			cp.rewriteClass(cc.name, writer.toByteArray());
		}
	}

}
