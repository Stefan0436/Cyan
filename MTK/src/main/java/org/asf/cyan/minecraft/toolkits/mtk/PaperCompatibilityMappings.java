package org.asf.cyan.minecraft.toolkits.mtk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.asf.aos.util.service.extra.slib.util.ArrayUtil;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.core.CyanInfo;
import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.remapping.MAPTYPE;
import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.fluid.remapping.SimpleMappings;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionType;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 
 * Remaps the Spigot mappings to allow Paper to run Cyan, credits to PaperMC and
 * SpigotMC
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class PaperCompatibilityMappings extends CompatibilityMappings {
	public PaperCompatibilityMappings(Mapping<?> mappings, String mappingsVersion) {
		this(mappings, CyanInfo.getModloaderVersion(),
				MinecraftVersionToolkit.createOrGetVersion(CyanInfo.getMinecraftVersion(), MinecraftVersionType.UNKNOWN,
						null, CyanInfo.getReleaseDate()),
				true, mappingsVersion);
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

	public PaperCompatibilityMappings(Mapping<?> mappings, String modloader, MinecraftVersionInfo info, boolean msg,
			String mappingsVersion) {
		String mappingsId = "-" + mappingsVersion.replaceAll("[!?/:\\\\]", "-")
				+ (modloader.isEmpty() ? "" : "-" + modloader);

		try {
			MinecraftToolkit.infoLog("Loading paper support... Preparing SPIGOT mappings for compatibility...");
			if (!MinecraftMappingsToolkit.areMappingsAvailable(mappingsId, "spigot", info, GameSide.SERVER)) {

				if (msg)
					MinecraftToolkit.infoLog("First time loading with paper support for version " + modloader
							+ ", downloading SPIGOT mappings...");

				MinecraftMappingsToolkit.downloadSpigotMappings(mappings, info, mappingsVersion);
				MinecraftMappingsToolkit.saveMappingsToDisk(mappingsId, "spigot", info, GameSide.SERVER);
			}

			Mapping<?> spigotMappings = MinecraftMappingsToolkit.loadMappings(mappingsId, "spigot", info,
					GameSide.SERVER);
			ignoredTypes.add("net.minecraft.network.protocol.status.ServerStatus$Serializer");
			combine("SPIGOT", mappings, spigotMappings, true);

			if (Version.fromString(info.getVersion()).isGreaterOrEqualTo(Version.fromString("1.17"))) {
				MinecraftToolkit.infoLog("Applying paper mappings patches...");
				URL u = new URL("https://papermc.io/api/v2/projects/paper/versions/" + info.getVersion() + "/builds/"
						+ modloader);
				try {
					String commitHash = null;

					File mappingsPatches = new File(MinecraftInstallationToolkit.getMinecraftDirectory(),
							"caches/mappings/paper-" + info.getVersion() + "-" + modloader);
					if (!mappingsPatches.exists())
						mappingsPatches.mkdirs();

					File commitData = new File(mappingsPatches, "commit.dat");
					if (commitData.exists()) {
						commitHash = new String(Files.readAllBytes(commitData.toPath()));
					}

					File tinyPatch = new File(mappingsPatches, "main.patch.tiny");
					File extraMembers = new File(mappingsPatches, "extra.members.csrg");
					File reobfMappings = new File(mappingsPatches, "reobf.patch.tiny");

					if (!tinyPatch.exists()) {
						commitHash = getData(u, commitHash, commitData);
						URL url = new URL(getUrl(commitHash, "mappings-patch.tiny"));
						InputStream strm = url.openStream();
						FileOutputStream fileOut = new FileOutputStream(tinyPatch);
						strm.transferTo(fileOut);
						fileOut.close();
						strm.close();
					}

					if (!extraMembers.exists()) {
						commitHash = getData(u, commitHash, commitData);
						URL url = new URL(getUrl(commitHash, "additional-spigot-member-mappings.csrg"));
						InputStream strm = url.openStream();
						FileOutputStream fileOut = new FileOutputStream(extraMembers);
						strm.transferTo(fileOut);
						fileOut.close();
						strm.close();
					}

					if (!reobfMappings.exists()) {
						commitHash = getData(u, commitHash, commitData);
						try {
							URL url = new URL(getUrl(commitHash, "reobf-mappings-patch.tiny"));
							InputStream strm = url.openStream();
							FileOutputStream fileOut = new FileOutputStream(reobfMappings);
							strm.transferTo(fileOut);
							fileOut.close();
							strm.close();
						} catch (IOException e) {
						}
					}

					if (tinyPatch.exists() && reobfMappings.exists()) {
						SimpleMappings reobf = new SimpleMappings().parseTinyV2Mappings(
								new String(Files.readAllBytes(reobfMappings.toPath())), "spigot", "mojang+yarn");
						SimpleMappings patch = new SimpleMappings().parseTinyV2Mappings(
								new String(Files.readAllBytes(tinyPatch.toPath())), "spigot", "mojang+yarn");
						translate(patch, this, reobf);
						applyMappingsPatch(reobf);
					}

					if (extraMembers.exists()) {
						applyCsrgMemberMappings(Files.readAllLines(extraMembers.toPath()), this);
					}
				} catch (IOException e) {
					MinecraftToolkit.warnLog(
							"Cannot apply paper mappings patches, assuming that the system is running in the debug environment!");
				}
			}

			applyInconsistencyMappings(info, "paper", modloader);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void translate(SimpleMappings patch, Mapping<?> paperCompatibilityMappings, SimpleMappings reobf) {
		for (Mapping<?> reobfCls : reobf.mappings) {
			if (reobfCls.mappingType == MAPTYPE.CLASS) {
				Mapping<?> target = null;
				for (Mapping<?> cls : patch.mappings) {
					if (cls.mappingType == MAPTYPE.CLASS && cls.name.equals(reobfCls.name)) {
						target = cls;
						break;
					}
				}
				if (target != null) {
					reobfCls.name = mapClass(this, target.obfuscated);

					for (Mapping<?> member : target.mappings) {
						if (member.mappingType == MAPTYPE.METHOD) {
							for (Mapping<?> member2 : reobf.mappings) {
								if (member2.mappingType == member.mappingType && member2.name.equals(member.name)
										&& Arrays.equals(member2.argumentTypes, member.argumentTypes)) {
									member2.name = mapMethod(this, reobfCls.name, member2.obfuscated, true,
											member2.argumentTypes);
								}
							}
						} else if (member.mappingType == MAPTYPE.PROPERTY) {
							for (Mapping<?> member2 : reobf.mappings) {
								if (member2.mappingType == member.mappingType && member2.name.equals(member.name)) {
									member2.name = mapProperty(this, reobfCls.name, member2.obfuscated, true);
								}
							}
						}
					}
				}

				for (Mapping<?> member2 : reobf.mappings) {
					if (member2.mappingType == MAPTYPE.PROPERTY) {
						for (Mapping<?> cls : patch.mappings) {
							if (cls.mappingType == MAPTYPE.CLASS && cls.name.equals(member2.type)) {
								member2.type = cls.name;
								break;
							}
						}
					} else if (member2.mappingType == MAPTYPE.METHOD) {
						for (Mapping<?> cls : patch.mappings) {
							if (cls.mappingType == MAPTYPE.CLASS && cls.name.equals(member2.type)) {
								member2.type = cls.name;
								break;
							}
						}
						int i = 0;
						for (String type : member2.argumentTypes) {
							for (Mapping<?> cls : patch.mappings) {
								if (cls.mappingType == MAPTYPE.CLASS && cls.name.equals(type)) {
									member2.argumentTypes[i] = cls.name;
									break;
								}
							}
							i++;
						}
					}
				}
			}
		}
	}

	private String mapClass(Mapping<?> mp, String input) {
		Mapping<?> map = mp.mapClassToMapping(input, t -> true, true);
		if (map != null)
			return map.name;
		return input;
	}

	private String getUrl(String commitHash, String file) {
		return "https://raw.githubusercontent.com/PaperMC/Paper/" + commitHash + "/build-data/" + file;
	}

	private String getData(URL u, String commitHash, File commitData) throws IOException {
		if (commitHash != null)
			return commitHash;

		InputStream strm = u.openStream();
		JsonObject obj = JsonParser.parseString(new String(strm.readAllBytes())).getAsJsonObject();
		strm.close();

		commitHash = obj.get("changes").getAsJsonArray().get(0).getAsJsonObject().get("commit").getAsString();
		Files.write(commitData.toPath(), commitHash.getBytes());

		return commitHash;
	}
}
