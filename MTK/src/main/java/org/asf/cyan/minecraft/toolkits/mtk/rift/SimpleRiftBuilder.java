package org.asf.cyan.minecraft.toolkits.mtk.rift;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipInputStream;

import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.asf.cyan.fluid.bytecode.enums.ComparisonMethod;
import org.asf.cyan.fluid.bytecode.sources.IClassSourceProvider;
import org.asf.cyan.fluid.remapping.MAPTYPE;
import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.rift.providers.IRiftToolchainProvider;
import org.asf.cyan.minecraft.toolkits.mtk.rift.providers.RiftFabricToolchainProvider;
import org.asf.cyan.minecraft.toolkits.mtk.rift.providers.RiftForgeToolchainProvider;
import org.asf.cyan.minecraft.toolkits.mtk.rift.providers.RiftPaperToolchainProvider;
import org.asf.cyan.minecraft.toolkits.mtk.rift.providers.RiftVanillaToolchainProvider;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;

/**
 * 
 * SimpleRiftBuilder - system create instances of the SimpleRift toolchain
 * simplifier.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class SimpleRiftBuilder implements Closeable {

	/**
	 * Gets the toolchain provider for the specified platform, returns null if
	 * remapping is not needer or if the combination of information is unsupported.
	 * 
	 * @param platform         Platform to use
	 * @param version          Game version
	 * @param side             Game side
	 * @param modloaderVersion Modloader version
	 * @param mappingsVersion  Mappings version
	 * @return Toolchain provider for the given platform or null.
	 */
	public static IRiftToolchainProvider getProviderForPlatform(LaunchPlatform platform, MinecraftVersionInfo version,
			GameSide side, String modloaderVersion, String mappingsVersion) {
		switch (platform) {
		case DEOBFUSCATED:
			return null;
		case VANILLA:
			return new RiftVanillaToolchainProvider(version, side);
		case MCP:
			return new RiftForgeToolchainProvider(version, side, modloaderVersion, mappingsVersion);
		case YARN:
			return new RiftFabricToolchainProvider(version, side, modloaderVersion, mappingsVersion);
		case SPIGOT:
			if (side != GameSide.SERVER)
				return null;
			else
				return new RiftPaperToolchainProvider(version, modloaderVersion, mappingsVersion);
		case UNKNOWN:
			return null;
		}
		return null;
	}

	private ArrayList<String> classes = new ArrayList<String>();
	private ArrayList<IClassSourceProvider<?>> sourceProviders = new ArrayList<IClassSourceProvider<?>>();
	private ArrayList<IRiftToolchainProvider> toolchainProviders = new ArrayList<IRiftToolchainProvider>();

	private File outputMappingsFile;
	private String outputIdentifier;

	/**
	 * Sets the binary mappings identifier, create version-based, clear but complex
	 * identifiers. (recommended to use, this caches output and speeds up the
	 * process if run more than once)
	 * 
	 * @param id Identifier to use.
	 */
	public void setIdentifier(String id) {
		if (outputMappingsFile == null)
			outputMappingsFile = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/mappings");

		outputIdentifier = id;
	}

	/**
	 * Sets the directory the mappings are saved in, only use this if using
	 * setIdentifier, default sets to MTK.
	 * 
	 * @param dir Output directory
	 */
	public void setMappingsSaveDir(File dir) {
		outputMappingsFile = dir;
	}

	/**
	 * Appends source class names to the rift builder.
	 * 
	 * @param cls Source class name
	 */
	public void addClass(String cls) {
		if (!classes.contains(cls)) {
			classes.add(cls);
		}
	}

	/**
	 * Appends rift providers to the rift builder.
	 * 
	 * @param provider Provider to append.
	 */
	public void appendRiftProvider(IRiftToolchainProvider provider) {
		if (provider == null)
			return;

		toolchainProviders.add(provider);
	}

	/**
	 * Appends source providers to the rift builder. These providers will provide
	 * the files that will be remapped.
	 * 
	 * @param provider Provider to append.
	 */
	public void appendSources(IClassSourceProvider<?> provider) {
		boolean present = false;
		ArrayList<IClassSourceProvider<?>> backupSources = new ArrayList<IClassSourceProvider<?>>(sourceProviders);
		switch (provider.getComparisonMethod()) {
		case OBJECT_EQUALS:
			present = backupSources.stream().anyMatch(t -> t.getComparisonMethod() == ComparisonMethod.OBJECT_EQUALS
					&& t.providerObject().equals(provider.providerObject()));
		case CLASS_EQUALS:
			present = backupSources.stream()
					.anyMatch(t -> t.getComparisonMethod() == ComparisonMethod.CLASS_EQUALS && t.providerObject()
							.getClass().getTypeName().equals(provider.providerObject().getClass().getTypeName()));
		case CLASS_ISASSIGNABLE:
			present = backupSources.stream()
					.anyMatch(t -> t.getComparisonMethod() == ComparisonMethod.CLASS_ISASSIGNABLE
							&& t.providerObject().getClass().isAssignableFrom(provider.providerObject().getClass()));
		case LOGICAL_EQUALS:
			present = backupSources.stream().anyMatch(t -> t.getComparisonMethod() == ComparisonMethod.LOGICAL_EQUALS
					&& t.providerObject() == provider.providerObject());
		}

		if (!present) {
			sourceProviders.add(provider);
		}
	}

	/**
	 * Builds a SimpleRift instance.
	 * 
	 * @throws ClassNotFoundException If a class cannot be found.
	 * @throws IOException            If helper jar (game jar) cannot be imported.
	 */
	public SimpleRift build() throws ClassNotFoundException, IOException {
		SimpleRift rift = new SimpleRift();

		File output = null;
		if (outputIdentifier != null) {
			output = new File(outputMappingsFile, "rift-" + outputIdentifier + ".mappings.bin");
		}

		FluidClassPool libs = FluidClassPool.create();
		ArrayList<Mapping<?>> mappings = new ArrayList<Mapping<?>>();
		for (IRiftToolchainProvider provider : toolchainProviders) {
			if (output == null || !output.exists())
				mappings.add(provider.getRiftMappings());
		}

		for (Mapping<?> root : mappings) {
			for (Mapping<?> clsMapping : root.mappings) {
				if (clsMapping.mappingType == MAPTYPE.CLASS) {
					libs.addIncludedClass(clsMapping.obfuscated.replaceAll("\\.", "/"));
					libs.addIncludedClass(clsMapping.name.replaceAll("\\.", "/"));
				}
			}
		}

		for (IRiftToolchainProvider provider : toolchainProviders) {
			ZipInputStream strm = new ZipInputStream(new FileInputStream(provider.getJar()));
			libs.importArchive(strm);
			strm.close();
			for (IClassSourceProvider<?> src : provider.getSources()) {
				libs.addSource(src);
			}
		}

		FluidClassPool sources = FluidClassPool.createEmpty();
		for (IClassSourceProvider<?> provider : sourceProviders) {
			sources.addSource(provider);
		}
		for (String clName : classes) {
			sources.getClassNode(clName);
		}

		if (output != null && output.exists()) {
			rift.assign(libs, sources, output, new Mapping<?>[0]);
			return rift;
		}
		rift.assign(libs, sources, output, mappings.toArray(t -> new Mapping[t]));
		return rift;
	}

	@Override
	public void close() throws IOException {
		classes.clear();
		sourceProviders.clear();
		toolchainProviders.clear();
		outputMappingsFile = null;
		outputIdentifier = null;
	}
}
