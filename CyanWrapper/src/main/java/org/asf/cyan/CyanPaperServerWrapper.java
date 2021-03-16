package org.asf.cyan;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Scanner;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.events.core.ISynchronizedEventListener;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.core.CyanCore;
import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CyanPaperServerWrapper extends CyanComponent {

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
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException, NoSuchMethodException,
			SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		URL info = CyanClientWrapper.class.getResource("/wrapper.info");
		StringBuilder builder = new StringBuilder();
		Scanner sc = new Scanner(info.openStream());
		while (sc.hasNext())
			builder.append(sc.nextLine());
		sc.close();

		Fluid.addAgent("io.papermc.paperclip.Agent", "premain");
		CyanCore.setEntryMethod("CyanPaperWrapper Version " + builder.toString().trim());

		CyanLoader.setupModloader("SERVER");
		CyanLoader.getModloader().attachEventListener("mtk.mappings.downloaded", new ISynchronizedEventListener() {

			@Override
			public String getListenerName() {
				return CyanPaperServerWrapper.class.getTypeName() + ":mtk.mappings.downloaded";
			}

			@Override
			public void received(Object... params) {
				if (params.length >= 4) {
					if (params[0] instanceof String && params[1] instanceof MinecraftVersionInfo
							&& params[2] instanceof GameSide && params[3] instanceof Mapping<?>) {
						String type = params[0].toString();
						MinecraftVersionInfo version = (MinecraftVersionInfo) params[1];
						Mapping<?> mappings = (Mapping<?>) params[3];

						if (type.equals("spigot")) {
							String paperURL = "https://papermc.io/api/v2/projects/paper/versions/" + version;
							String paperVersion = "";
							try {
								URL u = new URL(paperURL);
								InputStream strm = u.openStream();
								JsonObject jsonInfo = JsonParser.parseReader(new InputStreamReader(strm))
										.getAsJsonObject();
								JsonArray versions = jsonInfo.get("builds").getAsJsonArray();
								paperVersion = jsonInfo.get("builds").getAsJsonArray().get(versions.size() - 1)
										.getAsString();
								strm.close();
							} catch (Exception ex) {
								fatal("Paper version download failed, cannot save the mappings, will not continue as it is too risky!");
								fatal("");
								fatal("Please try again at a later date,");
								fatal("CYAN will NOT launch because it can risk world damage if the mappings are inconsistent.");
								fatal("Throwing exception now.");
								fatal("");
								throw new RuntimeException(ex);
							}
							mappings.mappingsVersion = paperVersion;
						}
					}
				}
			}

		});

		CyanLoader.getModloader().attachEventListener("mtk.mappings.loaded", new ISynchronizedEventListener() {

			@Override
			public String getListenerName() {
				return CyanPaperServerWrapper.class.getTypeName() + ":mtk.mappings.loaded";
			}

			@Override
			public void received(Object... params) {
				if (params.length >= 6) {
					if (params[0] instanceof String && params[1] instanceof String
							&& params[2] instanceof MinecraftVersionInfo && params[3] instanceof GameSide
							&& params[4] instanceof Mapping<?> && params[5] instanceof File) {

						String identifier = params[0].toString();
						Mapping<?> mappings = (Mapping<?>) params[4];
						File mappingsFile = (File) params[5];

						if (identifier.equals("spigot")) {
							String currentVersion = Modloader.getModloader("Paper").getVersion();
							if (mappings.mappingsVersion == null || currentVersion == null
									|| !mappings.mappingsVersion.equals(currentVersion)) {
								fatal("");
								fatal("");
								fatal("");
								fatal("--------------- REFUSING TO LAUNCH WITH OUTDATED PAPER SERVER! ---------------");
								fatal("");
								fatal("Cyan CANNOT launch as the bundled PAPER server is out of date,");
								fatal("Launching with out-of-date servers is REALLY unsafe, please update the CYAN");
								fatal("installation by re-downloading or re-compiling for the newest version.");
								fatal("");
								fatal("Mappings version: "
										+ (mappings.mappingsVersion == null ? "UNKNOWN" : mappings.mappingsVersion));
								fatal("Bundled paper version: "
										+ (currentVersion == null ? "UNKNOWN" : currentVersion));
								fatal("");
								fatal("If you are CERTAIN that you have the right mappings cached, you can adjust");
								fatal("the mappingsVersion property in the mappings file. File name:");
								fatal("Path: .cyan-data/caches/mappings/" + mappingsFile.getName());
								fatal("");
								fatal("If you do change it, the ASF will not be held responsible for damaged worlds.");
								fatal("");
								fatal("Exiting now...");
								fatal("");
								fatal("------------------------------------------------------------------------------");
								fatal("");
								System.exit(-1);
							}
						}
					}
				}
			}

		});

		CyanLoader.disableVanillaMappings();
		CyanLoader.addCompatibilityMappings(CyanLoader.getPaperCompatibilityMappings());
		CyanLoader.initializeGame("SERVER");
		String wrapper = System.getProperty("cyan.launcher.server.wrapper", "io.papermc.paperclip.Paperclip");
		CyanCore.startGame(wrapper, args);
	}

}
