package org.asf.cyan;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.zip.ZipInputStream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.asf.cyan.fluid.bytecode.sources.FileClassSourceProvider;
import org.asf.cyan.fluid.deobfuscation.DeobfuscationTargetMap;
import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.fluid.remapping.SimpleMappings;
import org.asf.cyan.fluid.remapping.SupertypeRemapper;
import org.asf.cyan.minecraft.toolkits.mtk.DynamicCompatibilityMappings;
import org.asf.cyan.minecraft.toolkits.mtk.FabricCompatibilityMappings;
import org.asf.cyan.minecraft.toolkits.mtk.ForgeCompatibilityMappings;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftMappingsToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftModdingToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftRifterToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftVersionToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.PaperCompatibilityMappings;
import org.asf.cyan.minecraft.toolkits.mtk.auth.AuthenticationInfo;
import org.asf.cyan.minecraft.toolkits.mtk.auth.MinecraftAccountType;
import org.asf.cyan.minecraft.toolkits.mtk.auth.YggdrasilAuthentication;
import org.asf.cyan.minecraft.toolkits.mtk.auth.windowed.YggdrasilAuthenticationWindow;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public class MtkCLI extends CyanComponent {
	public static void main(String[] args) throws IOException {
		MtkBootstrap.strap();
		MinecraftToolkit.initializeMTK();
		Configurator.setLevel("CYAN", Level.INFO);

		if (args.length > 0) {
			if (args[0].equals("mappings")
					&& ((args.length >= 3 && args[1].equalsIgnoreCase("spigot")) || args.length >= 4)) {
				String type = args[1];
				String version = args[2];
				String side = "";
				if (!type.equalsIgnoreCase("spigot"))
					side = args[3].toUpperCase();
				else
					side = "SERVER";
				if (side.equals("SERVER") || side.equals("CLIENT")) {
					GameSide gameSide = GameSide.valueOf(side);
					if (type.equalsIgnoreCase("mojang")) {
						MinecraftInstallationToolkit.saveVersionManifest(MinecraftVersionToolkit.getVersion(version));
						MinecraftMappingsToolkit.downloadVanillaMappings(MinecraftVersionToolkit.getVersion(version),
								gameSide);
						MinecraftMappingsToolkit.saveMappingsToDisk(MinecraftVersionToolkit.getVersion(version),
								gameSide, true);
						info("Downloaded " + version + " mappings to cache.");
						return;
					} else if (type.equalsIgnoreCase("yarn") && args.length >= 5) {
						MinecraftInstallationToolkit.saveVersionManifest(MinecraftVersionToolkit.getVersion(version));
						MinecraftMappingsToolkit.downloadYarnMappings(MinecraftVersionToolkit.getVersion(version),
								gameSide, args[4]);
						MinecraftMappingsToolkit.saveMappingsToDisk("-" + args[4].replaceAll("[^A-Za-z0-9.\\-]", "_"),
								"yarn", MinecraftVersionToolkit.getVersion(version), gameSide, true);
						info("Downloaded " + version + " " + args[4] + " yarn mappings to cache.");
						return;
					} else if (type.equalsIgnoreCase("intermediary") && args.length >= 4) {
						MinecraftInstallationToolkit.saveVersionManifest(MinecraftVersionToolkit.getVersion(version));
						MinecraftMappingsToolkit
								.downloadIntermediaryMappings(MinecraftVersionToolkit.getVersion(version), gameSide);
						MinecraftMappingsToolkit.saveMappingsToDisk("", "intermediary",
								MinecraftVersionToolkit.getVersion(version), gameSide, true);
						info("Downloaded " + version + " intermediary mappings to cache.");
						return;
					} else if (type.equalsIgnoreCase("mcp") && args.length >= 5) {
						MinecraftInstallationToolkit.saveVersionManifest(MinecraftVersionToolkit.getVersion(version));
						MinecraftMappingsToolkit.downloadMCPMappings(MinecraftVersionToolkit.getVersion(version),
								gameSide, args[4]);
						MinecraftMappingsToolkit.saveMappingsToDisk("-" + args[4].replaceAll("[^A-Za-z0-9.\\-]", "_"),
								"mcp", MinecraftVersionToolkit.getVersion(version), gameSide, true);
						info("Downloaded " + version + " " + args[4] + " mcp mappings to cache.");
						return;
					} else if (type.equalsIgnoreCase("spigot") && args.length >= 4) {
						if (!args[3].matches("[a-z0-9]+:[0-9a-zA-Z_]+")) {
							System.err.println(
									"Cannot download spigot mappings: illegal version: expected hash:version.");
							System.exit(1);
							return;
						}
						MinecraftInstallationToolkit.saveVersionManifest(MinecraftVersionToolkit.getVersion(version));
						if (!MinecraftMappingsToolkit.areMappingsAvailable(MinecraftVersionToolkit.getVersion(version),
								gameSide)) {
							System.err.println("Cannot download spigot mappings: missing vanilla server mappings.");
							System.exit(1);
							return;
						}
						MinecraftMappingsToolkit
								.downloadSpigotMappings(
										MinecraftMappingsToolkit
												.loadMappings(MinecraftVersionToolkit.getVersion(version), gameSide),
										MinecraftVersionToolkit.getVersion(version), args[3]);
						MinecraftMappingsToolkit.saveMappingsToDisk("-" + args[3].replaceAll("[^A-Za-z0-9.\\-]", "_"),
								"spigot", MinecraftVersionToolkit.getVersion(version), GameSide.SERVER, true);
						info("Downloaded " + version + " " + args[3] + " spigot mappings to cache.");
						return;
					}
				}
			} else if (args[0].equals("jar") && args.length >= 3) {
				String version = args[1];
				String side = args[2].toUpperCase();
				if (side.equals("SERVER") || side.equals("CLIENT")) {
					GameSide gameSide = GameSide.valueOf(side);
					MinecraftInstallationToolkit.saveVersionManifest(MinecraftVersionToolkit.getVersion(version));
					MinecraftInstallationToolkit.downloadVersionJar(MinecraftVersionToolkit.getVersion(version),
							gameSide);
					info("Downloaded " + version + " " + gameSide + " jar to cache.");
					return;
				}
			} else if (args[0].equals("libraries") && args.length >= 1) {
				String version = args[1];
				MinecraftInstallationToolkit.saveVersionManifest(MinecraftVersionToolkit.getVersion(version));
				MinecraftInstallationToolkit.downloadVersionFiles(MinecraftVersionToolkit.getVersion(version), false);
				info("Downloaded " + version + " client libraries to cache.");
				return;
			} else if (args[0].equals("validate") && args.length >= 1) {
				String version = args[1];
				MinecraftInstallationToolkit.saveVersionManifest(MinecraftVersionToolkit.getVersion(version));
				if (!MinecraftInstallationToolkit.checkInstallation(MinecraftVersionToolkit.getVersion(version), true))
					System.exit(1);
				return;
			} else if (args[0].equals("client") && args.length >= 1) {
				String version = args[1];
				MinecraftInstallationToolkit.saveVersionManifest(MinecraftVersionToolkit.getVersion(version));
				MinecraftInstallationToolkit.downloadVersionFiles(MinecraftVersionToolkit.getVersion(version), true);
				info("Downloaded " + version + " client files to cache.");
				return;
			} else if (args[0].equals("manifest") && args.length >= 1) {
				String version = args[1];
				MinecraftInstallationToolkit.saveVersionManifest(MinecraftVersionToolkit.getVersion(version));
				info("Downloaded " + version + " version manifest to cache.");
				return;
			} else if (args[0].equals("runclient") && args.length >= 4) {
				String version = args[1];
				MinecraftInstallationToolkit.saveVersionManifest(MinecraftVersionToolkit.getVersion(version));
				File authFile = new File(args[3]);
				if (!authFile.exists()) {
					System.err.println("Cannot launch client: missing authentication file.");
					System.exit(1);
					return;
				}
				if (!MinecraftInstallationToolkit.checkInstallation(MinecraftVersionToolkit.getVersion(version),
						true)) {
					System.err.println("Cannot launch client: missing client files.");
					System.exit(1);
					return;
				}
				String[] authdata = Files.readAllLines(authFile.toPath()).stream()
						.filter(t -> !t.isEmpty() && !t.startsWith("#")).toArray(t -> new String[t]);
				if (authdata.length < 5 || (!authdata[4].equals("MOJANG") && !authdata[4].equals("MSA")
						&& !authdata[4].equals("LEGACY"))) {
					System.err.println("Cannot launch client: invalid authentication file.");
					System.err.println("Expected file content:");
					System.err.println("<username>");
					System.err.println("<player name>");
					System.err.println("<session token>");
					System.err.println("<player uuid>");
					System.err.println("<MOJANG/MAS/LEGACY>");
					System.exit(1);
					return;
				}
				AuthenticationInfo account = AuthenticationInfo.create(authdata[0], authdata[1], authdata[2],
						UUID.fromString(authdata[3]), MinecraftAccountType.valueOf(authdata[4]));
				MinecraftInstallationToolkit.extractNatives(MinecraftVersionToolkit.getVersion(version));
				File gameDir = new File(args[2]);
				String last = null;
				if (new File(gameDir, "properties.txt").exists()) {
					for (String argument : Files.readAllLines(new File(gameDir, "properties.txt").toPath())) {
						if (!argument.isEmpty() && !argument.startsWith("#")) {
							if (last != null) {
								MinecraftInstallationToolkit.putVariable(last, argument);
								last = null;
							} else
								last = argument;
						}
					}
				}
				ArrayList<String> jvm = new ArrayList<String>();
				for (String arg : MinecraftInstallationToolkit
						.generateJvmArguments(MinecraftVersionToolkit.getVersion(version)))
					jvm.add(arg);
				if (new File(gameDir, "args.txt").exists()) {
					for (String argument : Files.readAllLines(new File(gameDir, "args.txt").toPath())) {
						if (!argument.isEmpty() && !argument.startsWith("#")) {
							jvm.add(argument);
						}
					}
				}
				System.exit(MinecraftInstallationToolkit.launchInstallation(MinecraftVersionToolkit.getVersion(version),
						gameDir, jvm.toArray(t -> new String[t]), MinecraftInstallationToolkit
								.generateGameArguments(MinecraftVersionToolkit.getVersion(version), account, gameDir)));
				return;
			} else if (args[0].equals("yggdrasil") && args.length >= 2) {
				String username = null;
				File authFile = new File(args[1]);
				if (authFile.exists()) {
					String[] authdata = Files.readAllLines(authFile.toPath()).stream()
							.filter(t -> !t.isEmpty() && !t.startsWith("#")).toArray(t -> new String[t]);
					if (authdata.length < 5 || (!authdata[4].equals("MOJANG") && !authdata[4].equals("MSA")
							&& !authdata[4].equals("LEGACY"))) {
						System.err.println("Cannot authenticate for the client: invalid authentication file.");
						System.err.println("Expected file content:");
						System.err.println("<username>");
						System.err.println("<player name>");
						System.err.println("<session token>");
						System.err.println("<player uuid>");
						System.err.println("<MOJANG/MAS/LEGACY>");
						System.exit(1);
						return;
					} else {
						username = authdata[0];
					}
				}
				if (username == null) {
					AuthenticationInfo account = AuthenticationInfo.authenticate(MinecraftAccountType.MOJANG);
					if (account != null) {
						StringBuilder accountFile = new StringBuilder();
						accountFile.append(account.getUserName()).append("\n");
						accountFile.append(account.getPlayerName()).append("\n");
						accountFile.append(account.getAccessToken()).append("\n");
						accountFile.append(account.getUUID()).append("\n");
						accountFile.append(account.getAccountType().toString()).append("\n");
						Files.write(authFile.toPath(), accountFile.toString().getBytes());
						info("Saved authentication file.");
						return;
					} else {
						System.exit(1);
						return;
					}
				} else {
					AuthenticationInfo account = null;
					try {
						account = AuthenticationInfo.authenticate(username, MinecraftAccountType.MOJANG);
					} catch (IOException e) {
						YggdrasilAuthentication.init();
						YggdrasilAuthenticationWindow window = new YggdrasilAuthenticationWindow(username,
								"Login no longer valid.");
						account = window.getAccount();
					}
					if (account != null) {
						StringBuilder accountFile = new StringBuilder();
						accountFile.append(account.getUserName()).append("\n");
						accountFile.append(account.getPlayerName()).append("\n");
						accountFile.append(account.getAccessToken()).append("\n");
						accountFile.append(account.getUUID()).append("\n");
						accountFile.append(account.getAccountType().toString()).append("\n");
						Files.write(authFile.toPath(), accountFile.toString().getBytes());
						info("Saved authentication file.");
						return;
					} else {
						System.exit(1);
						return;
					}
				}
			} else if (args[0].equals("deobfuscate") && args.length >= 3 && new File(args[1]).exists()
					&& args[1].endsWith(".jar")) {
				File jarfile = new File(args[1]);
				ArrayList<Mapping<?>> mappings = new ArrayList<Mapping<?>>();

				String[] mappingArguments = Arrays.copyOfRange(args, 2, args.length);
				for (String arg : mappingArguments) {
					info("Loading mappings: " + arg);
					mappings.add(new SimpleMappings().loadFile(arg));
				}

				MinecraftModdingToolkit.deobfuscateJar(jarfile, mappings.toArray(t -> new Mapping[t]));
				return;
			} else if (args[0].equals("deobfuscate") && args.length >= 3) {
				String version = args[1];
				String side = args[2].toUpperCase();
				if (side.equals("SERVER") || side.equals("CLIENT")) {
					GameSide gameSide = GameSide.valueOf(side);
					MinecraftInstallationToolkit.saveVersionManifest(MinecraftVersionToolkit.getVersion(version));
					if (!MinecraftMappingsToolkit.areMappingsAvailable(MinecraftVersionToolkit.getVersion(version),
							gameSide)) {
						System.err.println("Cannot deobfuscate the game jar: missing vanilla "
								+ gameSide.toString().toLowerCase() + " mappings.");
						System.exit(1);
						return;
					}
					if (gameSide == GameSide.CLIENT) {
						if (!MinecraftInstallationToolkit.checkInstallation(MinecraftVersionToolkit.getVersion(version),
								false)) {
							System.err.println("Cannot deobfuscate client: missing client files.");
							System.exit(1);
							return;
						}
					}
					MinecraftMappingsToolkit.loadMappings(MinecraftVersionToolkit.getVersion(version), gameSide);
					MinecraftModdingToolkit.deobfuscateJar(MinecraftVersionToolkit.getVersion(version), gameSide, true);
					info("Deobfuscated " + version + " " + gameSide + " jar, written to cache.");
					return;
				}
			} else if (args[0].equals("compat") && args.length >= 7) {
				String type = args[1];
				String gameVersion = args[2];
				String loaderVersion = args[3];
				File outputFile = new File(
						new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/mappings"),
						args[6] + ".mappings.ccfg");

				info("Loading " + gameVersion + " OBFUS mappings from file: " + new File(args[4]).getName() + "...");
				Mapping<?> obfus = new SimpleMappings().loadFile(args[4]);
				info("Loading " + gameVersion + " DEOBF mappings from file: " + new File(args[5]).getName() + "...");
				Mapping<?> deobf = new SimpleMappings().loadFile(args[5]);
				Mapping<?> output = null;

				if (type.equals("spigot")) {
					output = new PaperCompatibilityMappings(deobf, obfus,
							MinecraftVersionToolkit.getVersion(gameVersion), loaderVersion);
				} else if (type.equals("intermediary")) {
					output = new FabricCompatibilityMappings(deobf, obfus,
							MinecraftVersionToolkit.getVersion(gameVersion), loaderVersion);
				} else if (type.equals("mcp")) {
					output = new ForgeCompatibilityMappings(deobf, obfus,
							MinecraftVersionToolkit.getVersion(gameVersion), loaderVersion);
				} else if (type.equals("dynamic")) {
					output = new DynamicCompatibilityMappings(deobf, obfus,
							MinecraftVersionToolkit.getVersion(gameVersion), loaderVersion);
				}

				if (output != null) {
					info("Saving compatibilty mappings...");
					if (!outputFile.getParentFile().exists()) {
						outputFile.getParentFile().mkdirs();
					}
					Files.write(outputFile.toPath(), output.toString().getBytes());
					info("Saved compatibilty mappings to: " + args[6] + ".mappings.ccfg");
					return;
				}
			} else if (args[0].equals("riftmap") && args.length >= 3) {
				File outputFile = new File(
						new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/mappings"),
						args[1] + ".mappings.ccfg");

				ArrayList<Mapping<?>> mappings = new ArrayList<Mapping<?>>();
				for (int i = 2; i < args.length; i++) {
					String arg = args[i];

					info("Loading mappings: " + arg);
					mappings.add(new SimpleMappings().loadFile(arg));
				}

				Mapping<?> rift = MinecraftRifterToolkit.generateRiftTargets(mappings.toArray(t -> new Mapping[t]));

				info("Saving RIFT mappings...");
				if (!outputFile.getParentFile().exists()) {
					outputFile.getParentFile().mkdirs();
				}
				Files.write(outputFile.toPath(), rift.toString().getBytes());
				info("Saved RIFT mappings to: " + args[1] + ".mappings.ccfg");
				return;
			} else if (args[0].equals("binmap") && args.length >= 3) {
				info("Loading mappings: " + args[1]);
				SimpleMappings input = new SimpleMappings().loadFile(args[1]);
				FluidClassPool pool = FluidClassPool.create();

				for (int i = 2; i < args.length; i++) {
					info("Loading helper file: " + args[i]);
					pool.addSource(new FileClassSourceProvider(new File(args[i])));
				}

				info("Finding classes...");
				ArrayList<ClassNode> classes = new ArrayList<ClassNode>();

				int classesN = 0;
				String[] names = input.getObfuscatedClassNames();
				for (String cls : names) {
					try {
						classes.add(pool.getClassNode(cls));
					} catch (ClassNotFoundException e) {
						warn("Missing class file: " + cls);
					}

					classesN++;
					if (names.length != classesN) {
						if (classesN % 100 == 0)
							info("Loaded " + classesN + "/" + names.length + " classes.");
					}
				}
				info("Loaded " + names.length + "/" + names.length + " classes.");

				info("Creating target map...");
				DeobfuscationTargetMap mp = Fluid.createTargetMap(classes.toArray(t -> new ClassNode[t]), pool, input);

				info("Writing binary mappings...");
				File output = new File(args[1].replace(".mappings.ccfg", ".mappings") + ".bin");
				Files.write(output.toPath(), serialize(mp));
				info("Written binary mappings to: " + output.getPath());
				return;
			} else if (args[0].equals("riftengine") && args.length >= 4) {
				DeobfuscationTargetMap mp;
				try {
					info("Loading binmap from: " + args[1] + "...");
					mp = deserialize(Files.readAllBytes(new File(args[1]).toPath()));
				} catch (ClassNotFoundException e) {
					throw new IOException("Unrecognized class", e);
				}

				FluidClassPool outputPool = FluidClassPool.createEmpty();
				for (int i = 3; i < args.length; i++) {
					scanFilesForClasses(new File(args[i]), "/", outputPool);
				}

				info("Applying RIFT...");
				info("Expanding supertype implementations...");
				SupertypeRemapper remapper = new SupertypeRemapper(mp);
				for (ClassNode cls : outputPool.getLoadedClasses()) {
					remapper.remap(cls);
				}
				info("Running FLUID remappers...");
				Fluid.remapClasses(Fluid.createMemberRemapper(mp), Fluid.createClassRemapper(mp), outputPool,
						outputPool.getLoadedClasses());

				info("Writing output archive...");
				FileOutputStream strm = new FileOutputStream(args[2]);
				outputPool.transferOutputArchive(strm);
				strm.close();
				info("Written RIFT archive to: " + new File(args[2]).getPath());
				return;
			}
		}

		error();
	}

	private static void scanFilesForClasses(File file, String pref, FluidClassPool pool) throws IOException {
		if (file.isDirectory()) {
			for (File d : file.listFiles(t -> t.isDirectory())) {
				scanFilesForClasses(d, pref + d.getName() + "/", pool);
			}
			for (File d : file.listFiles(t -> !t.isDirectory())) {
				scanFilesForClasses(d, pref, pool);
			}
		} else {
			if (file.getName().endsWith(".class")) {
				FileInputStream strm = new FileInputStream(file);
				ClassReader reader = new ClassReader(strm);
				ClassNode cls = new ClassNode();
				reader.accept(cls, 0);
				strm.close();
				pool.addClass(cls.name, cls);
			} else {
				FileInputStream strm = new FileInputStream(file);
				if (file.getName().endsWith(".zip") || file.getName().endsWith(".jar")) {
					ZipInputStream strm2 = new ZipInputStream(strm);
					pool.importArchive(strm2);
					strm2.close();
				} else {
					pool.addFile(pref + file.getName(), strm);
				}
				strm.close();
			}
		}
	}

	private static byte[] serialize(Object obj) throws IOException {
		ByteArrayOutputStream strm = new ByteArrayOutputStream();
		ObjectOutputStream serializer = new ObjectOutputStream(strm);
		serializer.writeObject(obj);
		return strm.toByteArray();
	}

	@SuppressWarnings("unchecked")
	private static <T> T deserialize(byte[] data) throws IOException, ClassNotFoundException {
		ByteArrayInputStream strm = new ByteArrayInputStream(data);
		ObjectInputStream deserializer = new ObjectInputStream(strm);
		Object obj = deserializer.readObject();
		strm.close();
		return (T) obj;
	}

	public static void error() {
		System.err.println("Usage: mtk <task> [<arguments>]");
		System.err.println();
		System.err.println("Tasks:");
		System.err.println(
				" - mappings <mojang/spigot/yarn/mcp/intermediary> <version>                                - download mappings");
		System.err.println("   [<client/server> (mojang/yarn/mcp only)] [<mappings-version> (mcp/spigot/yarn only)]");
//		System.err.println(
//				" - convert-mp <tiny-v1/tiny-v2/tsrg/csrg/proguard> <output-filename>                       - converts mappings files to CCFG format"); // TODO
		System.err.println(
				" - compat <spigot/mcp/intermediary/dynamic> <game-version>                                 - manually create compatibility mappings");
		System.err.println("   <loader-version> <obfus-mappings-file> <deobf-mappings-file> <output-filename>");
		System.err.println(
				" - riftmap <output-filename> <input-mappings-file(s)...>                                   - create reverse-targeting mappings (RIFT)");
		System.err.println(
				" - binmap <mappings-file> <main-jarfile> [<library-jarfile(s)...>]                         - create binary mappings");
		System.err.println(
				" - riftengine <binmap-file> <output> <source-file(s)...>                                   - runs the rift remapping engine");
		System.err.println(
				" - jar <version> <client/server>                                                           - download game jars");
		System.err.println(
				" - deobfuscate <jar-file> <mappings-ccfg-files>...                                         - deobfuscate jars");
		System.err.println(
				" - deobfuscate <version> <client/server>                                                   - deobfuscate game jars");
		System.err.println(
				" - libraries <version>                                                                     - download game library jars");
		System.err.println(
				" - client <version>                                                                        - download game libraries and asset files");
		System.err.println(
				" - validate <version>                                                                      - validate client files (returns 1 if it fails)");
		System.err.println(
				" - manifest <version>                                                                      - download game version manifests");
		System.err.println(
				" - yggdrasil <authfile>                                                                    - authenticate a mojang user");
		System.err.println(
				" - runclient <version> <gamedir> <authfile>                                                - runs the client");
		System.err.println();
		System.err.println("The MTK and its CLI are licensed LGPL v3. Feel free to use these projects, as long");
		System.err.println("as you give credit to the AerialWorks Software Foundation and its contributors.");
		System.err.println("Full license can be found here: https://www.gnu.org/licenses/lgpl-3.0.txt");
		System.exit(1);
		return;
	}

}
