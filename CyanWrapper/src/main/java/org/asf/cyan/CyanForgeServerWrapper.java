package org.asf.cyan;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Scanner;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.core.CyanCore;
import org.asf.cyan.core.CyanInfo;
import org.asf.cyan.fluid.FluidAgent;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit.OsInfo;

public class CyanForgeServerWrapper extends CyanComponent {

	public CyanForgeServerWrapper() {
		assignImplementation();
	}

	/**
	 * Main server initialization method
	 * 
	 * @param args Arguments
	 * @throws IllegalAccessException    If starting fails
	 * @throws IllegalArgumentException  If starting fails
	 * @throws InvocationTargetException If starting fails
	 * @throws NoSuchMethodException     If starting fails
	 * @throws SecurityException         If starting fails
	 * @throws ClassNotFoundException    If starting fails
	 * @throws IOException               If closing the class loader fails
	 * @throws InterruptedException      If starting fails
	 * @throws URISyntaxException        If FLUID cannot be loaded
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException, NoSuchMethodException,
			SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			InterruptedException, URISyntaxException {
		boolean found = false;
		for (String arg : args) {
			if (arg.equals("--launchTarget")) {
				found = true;
				break;
			}
		}
		CyanCore.initLoader();
		
		if (!found && Version.fromString(CyanInfo.getMinecraftVersion())
				.isGreaterOrEqualTo(Version.fromString("1.17.1"))) {
			CyanCore.simpleInit();
			String plat = "unix";
			if (OsInfo.getCurrent() == OsInfo.windows)
				plat = "win";

			URL url = CyanLoader.class.getResource("/log4j2-server.xml");
			if (System.getProperty("ideMode") != null) {
				url = CyanLoader.class.getResource("/log4j2-server-ide.xml");
			}
			System.setProperty("log4j2.configurationFile", url.toString());
			System.setProperty("log4j.configurationFile", url.toString());

			File argdoc = new File("libraries/net/minecraftforge/forge/" + CyanInfo.getMinecraftVersion() + "-"
					+ CyanInfo.getModloaderVersion() + "/" + plat + "_args.txt");
			if (!argdoc.exists()) {
				error("Missing JVM command line document " + argdoc.getPath());
				System.exit(1);
			}

			info("Forking new JVM for the Forge server...");
			ArrayList<String> arguments = new ArrayList<String>();

			File userFile = new File("user_jvm_args.txt");
			if (userFile.exists()) {
				String[] forgeArgs = new String(Files.readAllBytes(userFile.toPath())).replace("\r", "").split("\n");
				for (String arg : forgeArgs) {
					if (arg.isEmpty() || arg.startsWith("#"))
						continue;
					for (String a : arg.split(" "))
						arguments.add(a);
				}
			}

			arguments.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
			String[] forgeArgs = new String(Files.readAllBytes(argdoc.toPath())).replace(" ", "\n").replace("\r", "")
					.split("\n");
			boolean skip = false;
			int i = 0;
			for (String arg : forgeArgs) {
				if (skip) {
					skip = false;
					i++;
					continue;
				}
				if (arg.equals("-cp")) {
					skip = true;

					arguments.add("-cp");
					ArrayList<String> paths = new ArrayList<String>();
					for (String pth : System.getProperty("java.class.path").split(File.pathSeparator)) {
						paths.add(pth);
					}
					for (String pth : forgeArgs[i + 1].split(File.pathSeparator)) {
						paths.add(pth);
					}

					arg = "";
					for (String pth : paths)
						arg += File.pathSeparator + pth;
					if (!arg.isEmpty())
						arg = arg.substring(1);
					arguments.add(arg);

					i++;
					continue;
				} else if (arg.equals("-p")) {
					skip = true;

					arguments.add("-p");
					ArrayList<String> paths = new ArrayList<String>();
					for (String pth : forgeArgs[i + 1].split(File.pathSeparator)) {
						if (new File(pth).exists())
							paths.add(pth);
					}

					arg = "";
					for (String pth : paths)
						arg += File.pathSeparator + pth;
					if (!arg.isEmpty())
						arg = arg.substring(1);
					arguments.add(arg);

					i++;
					continue;
				} else if (arg.startsWith("-DlegacyClassPath=")) {
					String cp = arg.substring("-DlegacyClassPath=".length());

					ArrayList<String> paths = new ArrayList<String>();
					for (String pth : cp.split(File.pathSeparator)) {
						if (new File(pth).exists())
							paths.add(pth);
					}

					arg = "";
					for (String pth : paths)
						arg += File.pathSeparator + pth;
					if (!arg.isEmpty())
						arg = arg.substring(1);
					arguments.add("-DlegacyClassPath=" + arg);
				} else if (arg.equals("cpw.mods.bootstraplauncher.BootstrapLauncher")) {
					arguments.add(CyanForgeServerWrapper.class.getTypeName());
				} else
					arguments.add(arg);

				i++;
			}

			String path = new File(FluidAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI())
					.getCanonicalPath();
			debug("Path: " + path);
			if (System.getProperty("cyanAgentJar") != null && !path.endsWith(".jar")) {
				path = System.getProperty("cyanAgentJar");
				debug("OVERRIDE FROM COMMAND LINE, New path: " + path);
			}
			arguments.add(0, "--add-exports=cpw.mods.bootstraplauncher/cpw.mods.bootstraplauncher=ALL-UNNAMED");
			arguments.add(0, "-javaagent:" + path);

			if (!arguments.contains("-cp") && !arguments.contains("-classpath")) {
				arguments.add(0, "-cp");
				arguments.add(1, System.getProperty("java.class.path"));
			}

			for (String arg : args) {
				arguments.add(arg);
			}

			String argumentsPretty = "";
			for (String arg : arguments) {
				argumentsPretty += " " + (arg.contains(" ") ? "\"" + arg + "\"" : arg);
			}

			String jvm = ProcessHandle.current().info().command().get();
			arguments.add(0, jvm);
			info("JVM: " + jvm);
			info("Arguments:" + argumentsPretty);

			ProcessBuilder builder = new ProcessBuilder(arguments.toArray(t -> new String[t]));
			builder.inheritIO();
			builder.directory(new File("."));
			Process proc = builder.start();
			proc.waitFor();
			System.exit(proc.exitValue());
			return;
		}

		URL info = CyanClientWrapper.class.getResource("/wrapper.info");
		StringBuilder builder = new StringBuilder();
		Scanner sc = new Scanner(info.openStream());
		while (sc.hasNext())
			builder.append(sc.nextLine());
		sc.close();

		info = CyanClientWrapper.class.getResource("/mappings.info");
		StringBuilder mcp = new StringBuilder();
		sc = new Scanner(info.openStream());
		while (sc.hasNext())
			mcp.append(sc.nextLine());
		sc.close();

		CyanLoader.setPlatformVersion(mcp.toString());

		CyanCore.setEntryMethod("CyanForgeWrapper Version " + builder.toString().trim() + ", MCP version " + mcp);
		CyanLoader.disableVanillaMappings();
		CyanLoader.addCompatibilityMappings(CyanLoader.getForgeCompatibilityMappings(GameSide.SERVER, mcp.toString()));
		CyanLoader.initializeGame("SERVER");
		String defaultWrapper = "cpw.mods.modlauncher.Launcher";
		if (Version.fromString(CyanInfo.getMinecraftVersion()).isGreaterOrEqualTo(Version.fromString("1.17.1"))) {
			defaultWrapper = "cpw.mods.bootstraplauncher.BootstrapLauncher";
			warn("Forge 1.17 server support is VERY INCOMPLETE!");
		}
		String wrapper = System.getProperty("cyan.launcher.client.wrapper", defaultWrapper);
		CyanCore.startGame(wrapper, args);
	}
}
