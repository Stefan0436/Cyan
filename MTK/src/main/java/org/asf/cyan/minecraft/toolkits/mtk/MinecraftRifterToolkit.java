package org.asf.cyan.minecraft.toolkits.mtk;

import java.io.IOException;
import java.util.ArrayList;

import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.deobfuscation.DeobfuscationTarget;
import org.asf.cyan.fluid.deobfuscation.DeobfuscationTargetMap;
import org.asf.cyan.fluid.remapping.MAPTYPE;
import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.fluid.remapping.SimpleMappings;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;

/**
 * 
 * Minecraft Reverse Obfuscation Targeting Toolkit - Remap code to run in
 * obfuscated environments, based on FLUID.<br />
 * <br />
 * Here follows the execution chain needed to generate proper deobfscation
 * mappings for remapping jars: (separate the client and server chains)<br />
 * <b>1.</b> Make sure the client installation is completely verified and that
 * the server jar is available.<br />
 * <b>2.</b> Download MCP, INTERMEDIARY and the vanilla mappings if not done so.
 * (load the vanilla mappings last)<br />
 * <b>2.</b> Deobfuscate both the client and server jars.<br />
 * <b>3.</b> Generate the RIFT mappings.<br />
 * <b>4.</b> Create a new class pool and somehow load all classes for either the
 * client or server. (including libraries, use importJarArchive for the best
 * result, make sure to use the <b>DEOBFUSCATED</b> jar, not the obfuscated
 * jar)<br />
 * <b>5.</b> Run <code>Fluid.createTargetMap(classesArray, pool,
 * riftMappings)</code> for the jar.<br />
 * <b>6.</b> Serialize and save the mappings for use in the future (binary
 * serialization is the fastest)
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@CYAN_COMPONENT
public class MinecraftRifterToolkit extends CyanComponent {
	protected static void initComponent() {
		trace("INITIALIZE Minecraft Rifter Toolkit, caller: " + CallTrace.traceCall());
	}

	private static FabricCompatibilityMappings fabricMappingsClient = null;
	private static FabricCompatibilityMappings fabricMappingsServer = null;
	private static ForgeCompatibilityMappings forgeMappings = null;

	private static MinecraftVersionInfo fabricMappingsVersionClient = null;
	private static MinecraftVersionInfo fabricMappingsVersionServer = null;
	private static MinecraftVersionInfo forgeMappingsVersion = null;

	private static PaperCompatibilityMappings paperMappings = null;
	private static MinecraftVersionInfo paperMappingsVersion = null;

	public static ForgeCompatibilityMappings getForgeMappings() {
		return forgeMappings;
	}

	public static FabricCompatibilityMappings getFabricServerMappings() {
		return fabricMappingsServer;
	}

	public static FabricCompatibilityMappings getFabricClientMappings() {
		return fabricMappingsClient;
	}

	public static PaperCompatibilityMappings getPaperServerMappings() {
		return paperMappings;
	}

	/**
	 * Generate reverse obfuscation target mappings, needed for rift jars
	 * 
	 * @param mappings Input mappings
	 * @return Mapping object representing the rift targets.
	 */
	public static Mapping<?> generateRiftTargets(Mapping<?>... mappings) {
		SimpleMappings newMappings = new SimpleMappings();

		info("Generating RIFT reverse class targeting mappings...");
		for (Mapping<?> root : mappings) {
			if (root.allowSupertypeFinalOverride())
				newMappings.setAllowSupertypeFinalOverride(true);
			for (Mapping<?> clsMapping : root.mappings) {
				if (clsMapping.mappingType.equals(MAPTYPE.CLASS)) {
					trace("Generating class targets for class " + clsMapping.name);
					newMappings.createClassMapping(clsMapping.obfuscated, clsMapping.name);
				}
			}
		}

		info("Generating RIFT reverse member targeting mappings...");
		for (Mapping<?> root : mappings) {
			for (Mapping<?> clsMapping : root.mappings) {
				if (clsMapping.mappingType.equals(MAPTYPE.CLASS)) {
					debug("Generating targets for class " + clsMapping.name);
					for (Mapping<?> memberMapping : clsMapping.mappings) {
						if (memberMapping.mappingType == MAPTYPE.METHOD) {
							trace("Generating target for method " + memberMapping.name);
							SimpleMappings mapping = newMappings.getClassMapping(clsMapping.obfuscated);

							ArrayList<String> types = new ArrayList<String>();
							for (String type : memberMapping.argumentTypes) {
								types.add(newMappings.mapClassToDeobfuscation(type));
							}

							mapping.createMethod(memberMapping.obfuscated, memberMapping.name,
									newMappings.mapClassToDeobfuscation(memberMapping.type),
									types.toArray(t -> new String[t]));
						} else if (memberMapping.mappingType == MAPTYPE.PROPERTY) {
							trace("Generating target for field " + memberMapping.name);
							SimpleMappings mapping = newMappings.getClassMapping(clsMapping.obfuscated);
							mapping.createField(memberMapping.obfuscated, memberMapping.name,
									newMappings.mapClassToDeobfuscation(memberMapping.type));
						}
					}
				}
			}
		}

		return newMappings;
	}

	/**
	 * Generate reverse obfuscation target mappings for Cyan Vanilla, needed for
	 * rift jars, uses a version and side
	 * 
	 * @param version Minecraft version
	 * @param side    Which side (client or server)
	 * @return Mapping object representing the rift targets.
	 * @throws IOException If the mappings don't match the version given
	 */
	public static Mapping<?> generateCyanRiftTargets(MinecraftVersionInfo version, GameSide side) throws IOException {
		if (!MinecraftMappingsToolkit.areMappingsAvailable(version, side))
			throw new IOException("No mappings are present for the " + version + " " + side.toString().toLowerCase());

		if (MinecraftMappingsToolkit.getLoadedMappingsVersion(side) == null)
			throw new IOException("Mappings not loaded, please load the version mappings before deobfuscating.");

		if (!MinecraftMappingsToolkit.getLoadedMappingsVersion(side).equals(version)) {
			throw new IOException(
					"Mappings version mismatch, please load the right version mappings before deobfuscating.");
		}

		info("Generating RIFT reverse targeting mappings for " + side.toString().toLowerCase() + " version " + version
				+ "...");
		return generateRiftTargets(MinecraftMappingsToolkit.getMappings(side));
	}

	/**
	 * Generate reverse obfuscation target mappings for CyanForge, needed for rift
	 * jars, uses a version and side
	 * 
	 * @param version          Minecraft version
	 * @param modloaderVersion Modloader version
	 * @param mcpVersion       MCP Version
	 * @return Mapping object representing the rift targets.
	 * @throws IOException If the mappings don't match the version given
	 */
	public static Mapping<?> generateCyanForgeRiftTargets(MinecraftVersionInfo version, String modloaderVersion,
			String mcpVersion) throws IOException {
		String mappingsId = "-" + mcpVersion.replaceAll("[!?/:\\\\]", "-") + "-" + modloaderVersion;

		if (!MinecraftMappingsToolkit.areMappingsAvailable(version, GameSide.CLIENT))
			throw new IOException("No CLIENT mappings are present for " + version);

		if (!MinecraftMappingsToolkit.areMappingsAvailable(mappingsId, "mcp", version, GameSide.CLIENT))
			throw new IOException("No MCP mappings are present for " + version);

		if (MinecraftMappingsToolkit.getLoadedMappingsVersion(GameSide.CLIENT) == null)
			throw new IOException("Mappings not loaded, please load the version mappings before deobfuscating.");

		if (!MinecraftMappingsToolkit.getLoadedMappingsVersion(GameSide.CLIENT).equals(version)) {
			throw new IOException(
					"Mappings version mismatch, please load the right version mappings before deobfuscating.");
		}

		if (forgeMappingsVersion == null || !forgeMappingsVersion.equals(version)) {
			forgeMappingsVersion = version;
			forgeMappings = new ForgeCompatibilityMappings(MinecraftMappingsToolkit.getMappings(GameSide.CLIENT),
					modloaderVersion, version, GameSide.CLIENT, false, mcpVersion);
		}

		info("Generating MCP RIFT reverse targeting mappings for version " + version + "...");
		return generateRiftTargets(forgeMappings);
	}

	/**
	 * Generate reverse obfuscation target mappings for CyanFabric, needed for rift
	 * jars, uses a version and side
	 * 
	 * @param version          Minecraft version
	 * @param side             Which side (client or server)
	 * @param modloaderVersion Modloader version
	 * @return Mapping object representing the rift targets.
	 * @throws IOException If the mappings don't match the version given
	 */
	public static Mapping<?> generateCyanFabricRiftTargets(MinecraftVersionInfo version, GameSide side,
			String modloaderVersion) throws IOException {
		String mappingsId = "-" + modloaderVersion;

		if (!MinecraftMappingsToolkit.areMappingsAvailable(version, side))
			throw new IOException("No mappings are present for the " + version + " " + side.toString().toLowerCase());

		if (!MinecraftMappingsToolkit.areMappingsAvailable(mappingsId, "intermediary", version, side))
			throw new IOException(
					"No INTERMEDIARY mappings are present for the " + version + " " + side.toString().toLowerCase());

		if (MinecraftMappingsToolkit.getLoadedMappingsVersion(side) == null)
			throw new IOException("Mappings not loaded, please load the version mappings before deobfuscating.");

		if (!MinecraftMappingsToolkit.getLoadedMappingsVersion(side).equals(version)) {
			throw new IOException(
					"Mappings version mismatch, please load the right version mappings before deobfuscating.");
		}

		if (side == GameSide.CLIENT) {
			if (fabricMappingsVersionClient == null || !fabricMappingsVersionClient.equals(version)) {
				fabricMappingsVersionClient = version;
				fabricMappingsClient = new FabricCompatibilityMappings(MinecraftMappingsToolkit.getMappings(side),
						modloaderVersion, version, side, false);
			}
		} else {
			if (fabricMappingsVersionServer == null || !fabricMappingsVersionServer.equals(version)) {
				fabricMappingsVersionServer = version;
				fabricMappingsServer = new FabricCompatibilityMappings(MinecraftMappingsToolkit.getMappings(side),
						modloaderVersion, version, side, false);
			}
		}

		info("Generating INTERMEDIARY RIFT reverse targeting mappings for " + side.toString().toLowerCase()
				+ " version " + version + "...");
		return generateRiftTargets((side == GameSide.CLIENT ? fabricMappingsClient : fabricMappingsServer));
	}

	/**
	 * Generate reverse obfuscation target mappings for CyanPaper, needed for rift
	 * jars, <b>SERVER ONLY</b>
	 * 
	 * @param version          Minecraft version
	 * @param modloaderVersion Modloader version
	 * @param mappingsVersion  Mappings version
	 * @return Mapping object representing the rift targets.
	 * @throws IOException If the mappings don't match the version given
	 */
	public static Mapping<?> generateCyanPaperRiftTargets(MinecraftVersionInfo version, String modloaderVersion,
			String mappingsVersion) throws IOException {
		String mappingsId = "-" + mappingsVersion.replaceAll("[!?/:\\\\]", "-") + "-" + modloaderVersion;
		if (!MinecraftMappingsToolkit.areMappingsAvailable(version, GameSide.SERVER))
			throw new IOException(
					"No mappings are present for the " + version + " " + GameSide.SERVER.toString().toLowerCase());

		if (!MinecraftMappingsToolkit.areMappingsAvailable(mappingsId, "spigot", version, GameSide.SERVER))
			throw new IOException("No SPIGOT mappings are present for the " + version + " "
					+ GameSide.SERVER.toString().toLowerCase());

		if (MinecraftMappingsToolkit.getLoadedMappingsVersion(GameSide.SERVER) == null)
			throw new IOException("Mappings not loaded, please load the version mappings before deobfuscating.");

		if (!MinecraftMappingsToolkit.getLoadedMappingsVersion(GameSide.SERVER).equals(version)) {
			throw new IOException(
					"Mappings version mismatch, please load the right version mappings before deobfuscating.");
		}

		if (paperMappings == null || !paperMappingsVersion.equals(version)) {
			paperMappingsVersion = version;
			paperMappings = new PaperCompatibilityMappings(MinecraftMappingsToolkit.getMappings(GameSide.SERVER),
					modloaderVersion, version, false, mappingsVersion);
		}

		info("Generating SPIGOT RIFT reverse targeting mappings for server version " + version + "...");
		return generateRiftTargets(paperMappings);
	}

	/**
	 * Converts a deobfuscation map to the SimpleMappings class.
	 * 
	 * @param input Input map
	 * @return New SimpleMappings representing the input.
	 */
	public static SimpleMappings deobfuscationMapToMappings(DeobfuscationTargetMap input) {
		info("Mapping deobufscation map into SimpleMappings...");
		SimpleMappings mappings = new SimpleMappings();

		input.forEach((k, v) -> {
			String className = k.replaceAll("/", ".");
			SimpleMappings cls = mappings.createClassMapping(v.outputName, className);

			v.fields.forEach((inp, outp) -> {
				String[] info = parseInfo(inp);
				String name = info[0];
				String desc = info[1];

				String type = parseType(desc);
				cls.createField(outp, name, type);
			});

			v.methods.forEach((inp, outp) -> {
				String[] info = parseInfo(inp);
				String name = info[0];
				String desc = info[1];

				String type = parseType(desc);
				String[] types = parseParams(desc);
				cls.createMethod(outp, name, type, types);
			});
		});

		return mappings;
	}

	/**
	 * Converts SimpleMappings to a deobfuscation map, only use output generated by
	 * deobfuscationMapToMappings.
	 * 
	 * @param input Input mappings
	 * @return DeobfuscationTargetMap representing the input.
	 */
	public static DeobfuscationTargetMap createDeobfuscationMapFromMappings(SimpleMappings input) {
		info("Mapping SimpleMappings into deobfuscation map...");
		DeobfuscationTargetMap mappings = new DeobfuscationTargetMap();

		for (Mapping<?> clsMapping : input.mappings) {
			DeobfuscationTarget target = new DeobfuscationTarget();
			target.outputName = clsMapping.name;
			target.jvmName = target.outputName.replaceAll("\\.", "/");

			for (Mapping<?> member : clsMapping.mappings) {
				if (member.mappingType == MAPTYPE.METHOD) {
					target.methods.put(member.obfuscated + " (" + Fluid.getDescriptors(member.argumentTypes) + ")"
							+ Fluid.getDescriptor(member.type), member.name);
				} else if (member.mappingType == MAPTYPE.PROPERTY) {
					target.fields.put(member.obfuscated + " " + Fluid.getDescriptor(member.type), member.name);
				}
			}

			mappings.put(clsMapping.obfuscated.replaceAll("\\.", "/"), target);
		}

		return mappings;
	}

	private static String[] parseInfo(String inp) {
		String name = inp;
		String desc = "";
		if (name.contains(" ")) {
			desc = name.substring(name.lastIndexOf(" ") + 1);
			name = name.substring(0, name.lastIndexOf(" "));
		}
		return new String[] { name, desc };
	}

	private static String[] parseParams(String descriptor) {
		String typesStr = "";
		if (descriptor.contains(")"))
			typesStr = descriptor.substring(1, descriptor.lastIndexOf(")"));
		return Fluid.parseMultipleDescriptors(typesStr);
	}

	private static String parseType(String descriptor) {
		String type = descriptor;
		if (descriptor.contains(")"))
			type = type.substring(type.lastIndexOf(")") + 1);
		return Fluid.parseDescriptor(type);
	}
}
