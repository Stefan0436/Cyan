package org.asf.cyan;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.fluid.bytecode.sources.FileClassSourceProvider;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftVersionToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.rift.SimpleRift;
import org.asf.cyan.minecraft.toolkits.mtk.rift.SimpleRiftBuilder;

public class RiftCLI extends CyanComponent {
	public static void main(String[] args) throws IOException {
		File riftConf = new File("rift.properties.ccfg");
		if (args.length >= 1 && riftConf.exists()) {
			RiftConfig config = new RiftConfig().readAll(new String(Files.readAllBytes(riftConf.toPath())));
			MtkBootstrap.mtkDir = new File(config.cacheDir);
			MtkBootstrap.strap();

			MinecraftToolkit.initializeMTK();
			Configurator.setLevel("CYAN", Level.INFO);

			File input = new File(args[0]);
			File output = new File(config.outputDir);
			if (!output.exists())
				output.mkdirs();
			for (RiftArtifact artifact : config.riftJars) {
				try {
					System.out.println("Creating rift jar: " + artifact.classifier);
					File jarFile = new File(new File(config.cacheDir, "caches/jars"),
							artifact.side.toLowerCase() + "-" + artifact.gameVersion + "-deobf.jar");
					if (!jarFile.exists())
						throw new IOException("Deobfuscated jar not available");

					SimpleRiftBuilder builder = new SimpleRiftBuilder();
					builder.setIdentifier(artifact.classifier);
					builder.appendSources(new FileClassSourceProvider(input));
					builder.appendRiftProvider(
							SimpleRiftBuilder.getProviderForPlatform(LaunchPlatform.valueOf(artifact.platform),
									MinecraftVersionToolkit.getVersion(artifact.gameVersion),
									GameSide.valueOf(artifact.side), artifact.loaderVersion, artifact.mappingsVersion));

					for (String dep : config.dependencies.keySet()) {
						System.out.println("Processing dependency " + dep + "...");
						File depFile = new File(config.dependencies.get(dep));
						if (depFile.exists()) {
							System.out.println("Adding dependency jar " + depFile.getName() + "...");
							builder.appendRiftDependencyFile(depFile);
						}
					}

					FileInputStream fileIn = new FileInputStream(input);
					ZipInputStream strm = new ZipInputStream(fileIn);
					ZipEntry ent = strm.getNextEntry();
					while (ent != null) {
						String name = ent.getName().replace("\\", "/");

						if (name.endsWith(".class")) {
							name = name.substring(0, name.lastIndexOf(".class"));
							if (name.startsWith("/"))
								name = name.substring(1);
							builder.addClass(name.replace("/", "."));
						}

						ent = strm.getNextEntry();
					}
					strm.close();
					fileIn.close();

					SimpleRift rift = builder.build();
					rift.apply();
					String fileName = input.getName();
					if (fileName.contains(".jar") || fileName.contains(".zip"))
						fileName = fileName.substring(0, fileName.lastIndexOf("."));
					rift.export(new File(output, fileName + "-rift-" + artifact.classifier + ".jar"));
					rift.close();
					builder.close();
					System.out.println("Done.");
					System.out.println();
				} catch (IOException | ClassNotFoundException e) {
					System.err.println("Error: " + e);
					e.printStackTrace();
				}
			}
			return;
		}

		error();
	}

	public static void error() {
		System.err.println("Usage: rift <jarfile>");
		System.err.println();
		System.err.println("Rift requires a rift.properties.ccfg file to work.");
		System.err.println();
		System.err.println("The MTK and its CLI are licensed LGPL v3. Feel free to use these projects, as long");
		System.err.println("as you give credit to the AerialWorks Software Foundation and its contributors.");
		System.err.println("Full license can be found here: https://www.gnu.org/licenses/lgpl-3.0.txt");
		System.exit(1);
		return;
	}

}
