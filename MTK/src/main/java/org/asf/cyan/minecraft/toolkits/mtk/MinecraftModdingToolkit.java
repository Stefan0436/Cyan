package org.asf.cyan.minecraft.toolkits.mtk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.asf.cyan.fluid.deobfuscation.DeobfuscationTargetMap;
import org.asf.cyan.fluid.remapping.FluidClassRemapper;
import org.asf.cyan.fluid.remapping.FluidMemberRemapper;
import org.asf.cyan.fluid.remapping.MAPTYPE;
import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.jboss.windup.decompiler.api.ClassDecompileRequest;
import org.jboss.windup.decompiler.api.DecompilationListener;
import org.jboss.windup.decompiler.fernflower.FernflowerDecompiler;
import org.objectweb.asm.tree.ClassNode;

/**
 * 
 * Minecraft Modding Toolkit, mainly for deobfuscation
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@CYAN_COMPONENT
public class MinecraftModdingToolkit extends CyanComponent {

	protected static void initComponent() {
		trace("INITIALIZE Minecraft Modding Toolkit, caller: " + CallTrace.traceCallName());
	}

	/**
	 * Deobfuscate a version jar (must have been downloaded first)<br/>
	 * <b>DO NOT DISTRIBUTE THE OUTPUT JARS!</b>
	 * 
	 * @param version Minecraft version
	 * @param side    Which side (server or client)
	 * @return File object of the deobfuscated jar
	 * @throws IOException If the version jar or mappings do not exist
	 */
	public static File deobfuscateJar(MinecraftVersionInfo version, GameSide side) throws IOException {
		return deobfuscateJar(version, side, false);
	}

	/**
	 * Deobfuscate a version jar (must have been downloaded first)<br/>
	 * <b>DO NOT DISTRIBUTE THE OUTPUT JARS!</b>
	 * 
	 * @param version   Minecraft version
	 * @param side      Which side (server or client)
	 * @param overwrite True to overwrite existing files, false to return the
	 *                  existing
	 * @return File object of the deobfuscated jar
	 * @throws IOException If the version jar or mappings do not exist
	 */
	public static File deobfuscateJar(MinecraftVersionInfo version, GameSide side, boolean overwrite)
			throws IOException {
		File input = MinecraftInstallationToolkit.getVersionJar(version, side);
		if (input == null)
			throw new FileNotFoundException(
					"Could not find " + side.toString().toLowerCase() + " jar for version " + version);

		if (!MinecraftMappingsToolkit.areMappingsAvailable(version, side))
			throw new IOException("No mappings are present for the " + version + " " + side.toString().toLowerCase());

		if (MinecraftMappingsToolkit.getLoadedMappingsVersion(side) == null)
			throw new IOException("Mappings not loaded, please load the version mappings before deobfuscating.");

		if (!MinecraftMappingsToolkit.getLoadedMappingsVersion(side).equals(version)) {
			throw new IOException(
					"Mappings version mismatch, please load the right version mappings before deobfuscating.");
		}

		return deobfuscateJar(input, overwrite, MinecraftMappingsToolkit.getMappings(side));
	}

	/**
	 * Deobfuscate a jar by using the specified mappings, outputs in the samme
	 * directory but with '-deobf' at the end of the jarfile's name, returns
	 * existing file, if one is present
	 * 
	 * @param input    Input jarfile
	 * @param mappings Mappings to use for deobfuscation
	 * @return Output file
	 * @throws IOException If loading the input fails
	 */
	public static File deobfuscateJar(File input, Mapping<?>... mappings) throws IOException {
		return deobfuscateJar(input, false, mappings);
	}

	/**
	 * Deobfuscate a jar by using the specified mappings, outputs in the samme
	 * directory but with '-deobf' at the end of the jarfile's name
	 * 
	 * @param input     Input jarfile
	 * @param mappings  Mappings to use for deobfuscation
	 * @param overwrite True to overwrite existing files, false to return the
	 *                  existing
	 * @return Output file
	 * @throws IOException If loading the input fails or if writing fails
	 */
	public static File deobfuscateJar(File input, boolean overwrite, Mapping<?>... mappings) throws IOException {
		String name = input.getName();
		String ext = name.substring(name.lastIndexOf(".") + 1);
		name = name.substring(0, name.lastIndexOf("."));

		File output = new File(input.getParent(), name + "-deobf." + ext);
		trace("CREATE File object for output, destination path: " + output.getAbsolutePath() + ", caller: "
				+ CallTrace.traceCallName());
		if (output.exists() && !overwrite)
			return output;
		else if (output.exists())
			output.delete();
		info("Preparing to deobfuscate " + input.getName() + "...");

		trace("CREATE ZipInputStream object for input, path: " + input.getAbsolutePath() + ", caller: "
				+ CallTrace.traceCallName());
		debug("Opening input jar as ZipInputStream...");
		ZipInputStream jar = new ZipInputStream(new FileInputStream(input));

		trace("CREATE FileOutputStream object for output, path: " + output.getAbsolutePath() + ", caller: "
				+ CallTrace.traceCallName());
		FileOutputStream outputJar = new FileOutputStream(output);

		debug("Creating empty classpool...");
		FluidClassPool pool = FluidClassPool.createEmpty();

		info("Reading jar file...");
		pool.excludeEntry("META-INF/MOJANGCS\\.RSA");
		pool.excludeEntry("META-INF/MOJANGCS\\.SF");
		pool.excludeEntry("META-INF/MANIFEST\\.MF");
		for (Mapping<?> root : mappings) {
			for (Mapping<?> clsMapping : root.mappings) {
				if (clsMapping.mappingType == MAPTYPE.CLASS) {
					pool.addIncludedClass(clsMapping.obfuscated.replaceAll("\\.", "/"));
					pool.addIncludedClass(clsMapping.name.replaceAll("\\.", "/"));
				}
			}
		}
		pool.importArchive(jar);

		ClassNode[] classes = pool.getLoadedClasses();

		info("Creating targets for " + input.getName() + "...");
		DeobfuscationTargetMap targets = Fluid.createTargetMap(classes, pool, mappings);

		info("Deobfuscating " + input.getName() + "...");
		Fluid.deobfuscate(classes, pool, mappings);
		FluidMemberRemapper fluidmemberremapper = Fluid.createMemberRemapper(targets);
		FluidClassRemapper fluidclsremapper = Fluid.createClassRemapper(targets);

		info("Remapping classes of " + input.getName() + "...");
		ClassNode[] classesArray = Stream.of(classes)
				.filter(cls -> targets.keySet().stream().anyMatch(t -> t.equals(cls.name))
						|| targets.values().stream().anyMatch(t -> t.jvmName.equals(cls.name)))
				.toArray(t -> new ClassNode[t]);
		Fluid.remapClasses(fluidmemberremapper, fluidclsremapper, pool, classesArray);

		info("Saving classes to output...");
		pool.transferOutputArchive(outputJar);

		info("Closing " + input.getName() + " and " + output.getName() + " jars...");
		jar.close();
		outputJar.close();
		pool.close();

		trace("RETURN output, path: " + output.getCanonicalPath() + ", caller: " + CallTrace.traceCallName());
		return output;
	}

	static void deleteDir(File dir) {
		for (File f : dir.listFiles()) {
			debug("Deleting " + f.getAbsolutePath());
			if (f.isDirectory()) {
				deleteDir(f);
			} else
				f.delete();
		}
		dir.delete();
	}

	static void zip(File dir, ZipOutputStream strm, String prefix) {
		for (File f : dir.listFiles()) {
			debug("Adding file/folder " + f.getAbsolutePath());
			if (f.isDirectory()) {
				try {
					ZipEntry entry = new ZipEntry(prefix + f.getName() + "/");
					strm.putNextEntry(entry);
					zip(f, strm, prefix + f.getName() + "/");
					strm.closeEntry();
				} catch (IOException e) {
					error("Failed to add directory " + f.getName(), e);
				}
			} else {
				try {
					ZipEntry entry = new ZipEntry(prefix + f.getName());
					strm.putNextEntry(entry);
					FileInputStream fIn = new FileInputStream(f);
					fIn.transferTo(strm);
					fIn.close();
					strm.closeEntry();
					info(f.getName() + " -> <jararchive>!/" + prefix + f.getName());
				} catch (IOException e) {
					error("Failed to add file " + f.getName(), e);
				}
			}
		}
	}

	/**
	 * Create a source jar (decompiles with Windup Fernflower)<br/>
	 * <b>DO NOT DISTRIBUTE THE OUTPUT FILES</b>
	 * 
	 * @param input Input jar
	 * @return Source jar
	 * @throws IOException If creating the jars fails
	 */
	public static File sourcesJar(File input) throws IOException {
		return sourcesJar(input, false);
	}

	/**
	 * Create a source jar (decompiles with Windup Fernflower)<br/>
	 * <b>DO NOT DISTRIBUTE THE OUTPUT FILE</b>
	 * 
	 * @param input     Input jar
	 * @param overwrite True to overwrite output file, false to return existing
	 * @return Source jar
	 * @throws IOException If creating the jars fails
	 */
	public static File sourcesJar(File input, boolean overwrite) throws IOException {
		String name = input.getName();
		FernflowerDecompiler decompiler = new FernflowerDecompiler();
		if (name.contains("."))
			name = name.substring(0, name.lastIndexOf("."));

		File outputSrc = new File(input.getParent(), name + "-sources.jar");
		File outputSrcTmp = new File(input.getParent(), name + "-sources.tmp.jar");

		if (outputSrc.exists() && overwrite)
			outputSrc.delete();
		else if (outputSrc.exists() && !overwrite)
			return outputSrc;

		if (outputSrcTmp.exists())
			outputSrcTmp.delete();

		File outputRecompTmpDir = new File(MinecraftInstallationToolkit.getMinecraftDirectory(),
				"caches/tmp/recomp/" + input.getName());
		File tmpDecomp = new File(MinecraftInstallationToolkit.getMinecraftDirectory(),
				"caches/tmp/decomp/" + input.getName());
		if (tmpDecomp.exists()) {
			info("Deleting temporary archive...");
			deleteDir(tmpDecomp);
		}
		if (outputRecompTmpDir.exists()) {
			info("Deleting temporary archive...");
			deleteDir(outputRecompTmpDir);
		}

		trace("CREATE ArrayList object for holding class files, path: " + input.getAbsolutePath() + ", caller: "
				+ CallTrace.traceCallName());
		ArrayList<File> classFiles = new ArrayList<File>();
		trace("CREATE HashMap object for holding normal files, path: " + input.getAbsolutePath() + ", caller: "
				+ CallTrace.traceCallName());
		HashMap<File, File> files = new HashMap<File, File>();

		trace("CREATE ZipInputStream object for input, path: " + input.getAbsolutePath() + ", caller: "
				+ CallTrace.traceCallName());
		debug("Opening input jar as ZipInputStream...");
		ZipInputStream jar = new ZipInputStream(new FileInputStream(input));
		info("Extracting JAR archive, file: " + input.getName());

		File tmp = new File(MinecraftInstallationToolkit.getMinecraftDirectory(),
				"caches/tmp/extract/" + input.getName());
		if (tmp.exists()) {
			info("Deleting temporary archive...");
			deleteDir(tmp);
		}
		debug("Creating temporary directory... Output: " + tmp.getCanonicalPath());
		tmp.mkdirs();

		ZipEntry entry = jar.getNextEntry();
		while (entry != null) {
			String path = entry.getName();
			path = path.replaceAll("\\\\", "/");
			File output = new File(tmp, path);
			trace("PROCESS " + path + ", caller: " + CallTrace.traceCallName());

			if (entry.isDirectory()) {
				trace("MKDIRS " + output.getCanonicalPath() + ", caller: " + CallTrace.traceCallName());
				output.mkdirs();
			} else {
				trace("MKDIRS parent of " + output.getCanonicalPath() + " if nonexistent, caller: "
						+ CallTrace.traceCallName());
				if (!output.getParentFile().exists())
					output.getParentFile().mkdirs();

				debug("Extracting " + output.getCanonicalPath() + "...");
				trace("CREATE FileOutputStream for " + output.getCanonicalPath() + ", caller: "
						+ CallTrace.traceCallName());
				FileOutputStream strm = new FileOutputStream(output);
				trace("TRANSFER ZipInputStream to FileOutputStream, caller: " + CallTrace.traceCallName());
				jar.transferTo(strm);
				info(path + " -> " + output.getName());
				strm.close();

				if (output.getName().endsWith(".class"))
					classFiles.add(output);
				else
					files.put(output, new File(tmpDecomp, path));
			}

			jar.closeEntry();
			entry = jar.getNextEntry();
		}
		jar.close();

		debug("Extracted " + classFiles.size() + " classes and " + files.size() + " normal files.");
		info("Done, extracted " + (files.size() + classFiles.size()) + " files, decompiling...");

		ArrayList<ClassDecompileRequest> requests = new ArrayList<ClassDecompileRequest>();
		for (File source : classFiles) {
			requests.add(new ClassDecompileRequest(tmp.toPath(), source.toPath(), tmpDecomp.toPath()));
		}
		decompiler.decompileClassFiles(requests, new DecompilationListener() {

			@Override
			public void fileDecompiled(List<String> sourceClassPaths, String outputPath) {
				info(new File(sourceClassPaths.get(0)).getName() + " -> " + new File(outputPath).getName());
			}

			@Override
			public void decompilationFailed(List<String> sourceClassPaths, String message) {
				error("Failed to decompile " + new File(sourceClassPaths.get(0)).getName() + ", message: " + message);
			}

			@Override
			public void decompilationProcessComplete() {
				info("Decompilation completed, cleaning up...");
			}

			@Override
			public boolean isCancelled() {
				return false;
			}

		});

		info("Copying normal files...");
		files.forEach((k, v) -> {
			try {
				if (!v.getParentFile().exists())
					v.getParentFile().mkdirs();

				debug("Copying " + k.getCanonicalPath() + " to " + v.getName());
				Files.copy(k.toPath(), v.toPath());
				v = v.getCanonicalFile();
				info(k.getName() + " -> " + tmpDecomp.getParentFile().toPath().relativize(v.toPath()));
			} catch (IOException e) {
				error("Failed to copy " + k.getName(), e);
			}
		});

		info("Creating JAR archive...");
		trace("CREATE ZipOutputStream object for temporary output source jar, path: " + outputSrcTmp.getAbsolutePath()
				+ ", caller: " + CallTrace.traceCallName());
		debug("Opening output source jar as ZipOutputStream...");
		ZipOutputStream srcOut = new ZipOutputStream(new FileOutputStream(outputSrcTmp));
		zip(tmpDecomp, srcOut, "");
		debug("Closing output source jar...");
		srcOut.close();

		debug("Moving " + outputSrcTmp.getCanonicalPath() + " to " + outputSrc.getName() + "...");
		Files.move(outputSrcTmp.toPath(), outputSrc.toPath());
		info(outputSrcTmp.getName() + " -> " + outputSrc.getName());

		return outputSrc;
	}

	/**
	 * Create a source for a minecraft version (decompiles with Windup
	 * Fernflower)<br/>
	 * <b>DO NOT DISTRIBUTE THE OUTPUT FILE</b>
	 * 
	 * @param version Minecraft version
	 * @param side    Which side (server or client)
	 * @return Source jar
	 * @throws IOException If loading the input fails or if writing fails
	 */
	public static File sourcesJar(MinecraftVersionInfo version, GameSide side) throws IOException {
		File input = MinecraftInstallationToolkit.getVersionJar(version, side);
		if (input != null)
			input = new File(input.getParentFile(),
					input.getName().substring(0, input.getName().lastIndexOf(".jar")) + "-deobf.jar");
		if (input != null && !input.exists())
			input = null;

		if (input == null)
			throw new FileNotFoundException(
					"Could not find deobfuscated " + side.toString().toLowerCase() + " jar for version " + version);

		return sourcesJar(input);
	}

	/**
	 * Create a source for a minecraft version (decompiles with Windup
	 * Fernflower)<br/>
	 * <b>DO NOT DISTRIBUTE THE OUTPUT FILE</b>
	 * 
	 * @param version   Minecraft version
	 * @param side      Which side (server or client)
	 * @param overwrite True to overwrite output file, false to return existing
	 * @return Source jar
	 * @throws IOException If creating the jars fails
	 */
	public static File sourcesJar(MinecraftVersionInfo version, GameSide side, boolean overwrite) throws IOException {
		File input = MinecraftInstallationToolkit.getVersionJar(version, side);
		if (input != null)
			input = new File(input.getParentFile(),
					input.getName().substring(0, input.getName().lastIndexOf(".jar")) + "-deobf.jar");
		if (input != null && !input.exists())
			input = null;

		if (input == null)
			throw new FileNotFoundException(
					"Could not find deobfuscated " + side.toString().toLowerCase() + " jar for version " + version);

		return sourcesJar(input, overwrite);
	}
}
