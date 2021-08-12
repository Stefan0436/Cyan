package org.asf.cyan.minecraft.toolkits.mtk;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.asf.aos.util.service.extra.slib.util.ArrayUtil;
import org.asf.cyan.api.config.annotations.Comment;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.core.CyanInfo;
import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.remapping.MAPTYPE;
import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.fluid.remapping.SimpleMappings;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionType;

/**
 * 
 * Remaps the Spigot mappings to allow Paper to run Cyan, credits to PaperMC and
 * SpigotMC
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@Comment("Spigot Compatibility Mappings, automatically generated by Cyan MTK on first startup.")
@Comment("These mappings were generated by Cyan Spigot Support Extension to allow Cyan to run on Spigot.")
@Comment("Cyan downloads the official and Spigot mappings on first launch and converts them to CCFG so FLUID can read them.")
@Comment("The mappings have not been included in cyan's jar files and may NOT be distributed!")
public class PaperCompatibilityMappings extends CompatibilityMappings {
	public PaperCompatibilityMappings(Mapping<?> mappings, String mappingsVersion) {
		this(mappings, CyanInfo.getModloaderVersion(),
				MinecraftVersionToolkit.createOrGetVersion(CyanInfo.getMinecraftVersion(), MinecraftVersionType.UNKNOWN,
						null, CyanInfo.getReleaseDate()),
				true, mappingsVersion);
	}

	public PaperCompatibilityMappings(Mapping<?> deobf, Mapping<?> paper, MinecraftVersionInfo gameVersion,
			String loaderVersion) {
		try {
			create(deobf, paper, gameVersion, loaderVersion);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void create(Mapping<?> deobf, Mapping<?> paper, MinecraftVersionInfo gameVersion, String loaderVersion)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException, IOException {
		if (Version.fromString(gameVersion.getVersion()).isGreaterOrEqualTo(Version.fromString("1.17"))) {
			ignoredTypes.add("net.minecraft.network.protocol.status.ServerStatus$Serializer");
			combine("PAPER", deobf, paper, true);
		} else {
			ignoredTypes.add("net.minecraft.network.protocol.status.ServerStatus$Serializer");
			combine("SPIGOT", deobf, paper, true);
		}

		applyInconsistencyMappings(gameVersion, "paper", loaderVersion);
	}

	public void applyCsrgMemberMappings(List<String> linesMembers, Mapping<?> mappings) {
		for (String line : linesMembers) {
			if (line.startsWith("#"))
				continue;

			String[] entries = line.split(" ");
			String owner = entries[0];
			owner = owner.replaceAll("\\.", "");
			owner = owner.replaceAll("/", ".");

			String obfus = entries[1];

			Mapping<?> ownerClass = null;
			for (Mapping<?> mp : mappings.mappings) {
				if (mp.obfuscated.equals(owner) && mp.mappingType == MAPTYPE.CLASS) {
					ownerClass = mp;
					break;
				}
			}
			if (ownerClass == null) {
				ownerClass = new SimpleMappings();
				ownerClass.mappingType = MAPTYPE.CLASS;
				ownerClass.name = owner;
				ownerClass.obfuscated = owner;
				mappings.mappings = ArrayUtil.append(mappings.mappings, new Mapping[] { ownerClass });
			}

			Mapping<?> map = new SimpleMappings();

			String name = "";
			if (entries.length == 3) {
				name = entries[2];
				map.mappingType = MAPTYPE.PROPERTY;
			} else {
				name = entries[3];

				String[] types = Fluid.parseMultipleDescriptors(entries[2].substring(1, entries[2].lastIndexOf(")")));
				type = Fluid.parseDescriptor(entries[2].substring(entries[2].lastIndexOf(")") + 1));
				map.argumentTypes = types;
				map.mappingType = MAPTYPE.METHOD;
			}

			map.type = type;
			map.name = obfus;
			map.obfuscated = name;

			for (Mapping<?> mp : ownerClass.mappings) {
				if (mp.mappingType == map.mappingType && mp.obfuscated.equals(obfus)) {
					mp.name = name;
					mp.type = type;
					map = null;
					break;
				}
			}
			if (map != null)
				ownerClass.mappings = ArrayUtil.append(ownerClass.mappings, new Mapping<?>[] { map });
		}
	}

	@Override
	public boolean allowSupertypeFinalOverride() {
		return true;
	}

	public PaperCompatibilityMappings(Mapping<?> mappings, String modloader, MinecraftVersionInfo info, boolean msg,
			String mappingsVersion) {
		String mappingsId = "-" + mappingsVersion.replaceAll("[!?/:\\\\]", "-")
				+ (modloader.isEmpty() ? "" : "-" + modloader);

		setBuild(5);

		try {
			if (loadWhenPossible("paper", mappingsId, info, GameSide.SERVER))
				return;

			Mapping<?> paperMappings;

			if (Version.fromString(info.getVersion()).isGreaterOrEqualTo(Version.fromString("1.17"))) {
				MinecraftToolkit.infoLog("Loading paper support... Preparing PAPER mappings for compatibility...");
				if (!MinecraftMappingsToolkit.areMappingsAvailable(mappingsId, "spigot", info, GameSide.SERVER)) {

					if (msg)
						MinecraftToolkit.infoLog("First time loading with paper support for version " + modloader
								+ ", downloading PAPER mappings...");

					MinecraftMappingsToolkit.downloadSpigotMappings(mappings, info, mappingsVersion);
					MinecraftMappingsToolkit.saveMappingsToDisk(mappingsId, "spigot", info, GameSide.SERVER);
				}

				paperMappings = MinecraftMappingsToolkit.loadMappings(mappingsId, "spigot", info, GameSide.SERVER);
			} else {
				MinecraftToolkit.infoLog("Loading paper support... Preparing SPIGOT mappings for compatibility...");
				if (!MinecraftMappingsToolkit.areMappingsAvailable(mappingsId, "spigot", info, GameSide.SERVER)) {

					if (msg)
						MinecraftToolkit.infoLog("First time loading with paper support for version " + modloader
								+ ", downloading SPIGOT mappings...");

					MinecraftMappingsToolkit.downloadSpigotMappings(mappings, info, mappingsVersion);
					MinecraftMappingsToolkit.saveMappingsToDisk(mappingsId, "spigot", info, GameSide.SERVER);
				}

				paperMappings = MinecraftMappingsToolkit.loadMappings(mappingsId, "spigot", info, GameSide.SERVER);
			}

			create(mappings, paperMappings, info, modloader);
			saveToDisk("paper", mappingsId, info, GameSide.SERVER);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException | IOException e) {
			throw new RuntimeException(e);
		}
	}

}
