package org.asf.cyan.api.internal.modkit.components._1_15_2.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.events.objects.resources.ResourcePackEventObject;
import org.asf.cyan.api.events.resources.modresources.ModResourcePackLoadEvent;
import org.asf.cyan.api.internal.CyanAPIComponent;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.core.CyanInfo;
import org.asf.cyan.mods.IMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;

public class CyanPackResources extends VanillaPackResources {
	private HashMap<String, IMod> mods = new HashMap<String, IMod>();
	private Set<String> modNamespaces;

	public CyanPackResources() {
		modNamespaces = new HashSet<String>(init());
	}

	@Override
	public Set<String> getNamespaces(PackType var1) {
		return modNamespaces;
	}

	private ArrayList<String> init() {
		ArrayList<String> namespaces = new ArrayList<String>();
		namespaces.add("cyan");
		for (Modloader modloader : Modloader.getAllModloaders()) {
			if (modloader instanceof CyanLoader) {
				CyanLoader loader = (CyanLoader) modloader;
				for (IMod mod : loader.getAllModInstances()) {
					mods.put(mod.getManifest().id().replace(":", "."), mod);
					namespaces.add(mod.getManifest().id().replace(":", "."));
				}
				break;
			}
		}

		ModResourcePackLoadEvent.getInstance().dispatch(new ResourcePackEventObject(this)).getResult();
		return namespaces;
	}

	@Override
	protected InputStream getResourceAsStream(PackType type, ResourceLocation location) {
		if (!location.getNamespace().equals("cyan")) {
			for (String namespace : mods.keySet()) {
				if (namespace.equals(location.getNamespace())) {
					IMod mod = mods.get(location.getNamespace());
					InputStream strm = getCyanResourceStream(type, mod, location.getPath());
					if (strm != null)
						return strm;
					else {
						if (location.getPath().equals("mod.mcmeta"))
							return new ByteArrayInputStream(getPackMeta().getBytes());
					}
				}
			}
		} else {
			InputStream strm = getCyanResourceStream(type, location.getPath());
			if (strm != null)
				return strm;
			else {
				if (location.getPath().equals("mod.mcmeta"))
					return new ByteArrayInputStream(getPackMeta().getBytes());
			}
		}
		return null;
	}

	private URL getCyanResource(PackType type, IMod source, String location) {
		String path = "";
		if (type == PackType.SERVER_DATA) {
			path = "server/" + location;
			if (source.getResource(path) != null)
				return source.getResource(path);
		} else if (type == PackType.CLIENT_RESOURCES) {
			path = "client/" + location;
			if (source.getResource(path) != null)
				return source.getResource(path);
		}
		path = "common/" + location;
		if (source.getResource(path) != null)
			return source.getResource(path);
		return null;
	}

	private InputStream getCyanResourceStream(PackType type, IMod source, String location) {
		URL u = getCyanResource(type, source, location);
		if (u != null) {
			try {
				return u.openStream();
			} catch (IOException e) {
			}
		}
		return null;
	}

	private URL getCyanResource(PackType type, String location) {
		String path = "";
		if (type == PackType.SERVER_DATA) {
			path = "server/" + location;
			if (CyanAPIComponent.getResource(path) != null)
				return CyanAPIComponent.getResource(path);
		} else if (type == PackType.CLIENT_RESOURCES) {
			path = "client/" + location;
			if (CyanAPIComponent.getResource(path) != null)
				return CyanAPIComponent.getResource(path);
		}
		path = "common/" + location;
		if (CyanAPIComponent.getResource(path) != null)
			return CyanAPIComponent.getResource(path);
		return null;
	}

	private InputStream getCyanResourceStream(PackType type, String location) {
		URL u = getCyanResource(type, location);
		if (u != null) {
			try {
				return u.openStream();
			} catch (IOException e) {
			}
		}
		return null;
	}

	@Override
	public boolean hasResource(PackType type, ResourceLocation location) {
		if (location.getNamespace().equals("cyan")) {
			if (location.getPath().equals("pack.mcmeta"))
				return true;
			else {
				if (getCyanResource(type, location.getPath()) != null)
					return true;
			}
		}

		for (String namespace : mods.keySet()) {
			if (location.getPath().equals("pack.mcmeta"))
				return true;

			if (namespace.equals(location.getNamespace())) {
				IMod mod = mods.get(location.getNamespace());
				if (getCyanResource(type, mod, location.getPath()) != null)
					return true;
			}
		}

		return false;
	}

	@Override
	protected InputStream getResourceAsStream(String path) {
		if (path.equals("pack.mcmeta")) {
			if (CyanAPIComponent.class.getResource(path) == null)
				return new ByteArrayInputStream(getPackMeta().getBytes());
		}
		return CyanAPIComponent.getResourceAsStream("common/" + path);
	}

	private String getPackMeta() {
		StringBuilder metadata = new StringBuilder();
		metadata.append("{\n");
		metadata.append("\t\"pack\": {\n");
		metadata.append("\t\t\"description\": \"Embedded resource pack for Cyan Mods.\",\n");
		if (Version.fromString(CyanInfo.getMinecraftVersion()).isLessOrEqualTo(Version.fromString("1.16.1"))) {
			metadata.append("\t\t\"pack_format\": 5\n");
		} else if (Version.fromString(CyanInfo.getMinecraftVersion()).isLessOrEqualTo(Version.fromString("1.17"))) {
			metadata.append("\t\t\"pack_format\": 6\n");
		} else {
			metadata.append("\t\t\"pack_format\": 7\n");
		}
		metadata.append("\t}\n");
		metadata.append("}");
		return metadata.toString();
	}

	@Override
	public String getName() {
		return "Cyan Mods";
	}
}
