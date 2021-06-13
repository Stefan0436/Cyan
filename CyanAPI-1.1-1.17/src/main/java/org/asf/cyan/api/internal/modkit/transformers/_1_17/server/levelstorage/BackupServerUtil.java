package org.asf.cyan.api.internal.modkit.transformers._1_17.server.levelstorage;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JOptionPane;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.internal.modkit.components._1_17.common.LoadUtil;
import org.asf.cyan.api.internal.modkit.transformers._1_17.common.world.storage.LevelModDataReader;
import org.asf.cyan.api.internal.modkit.transformers._1_17.common.world.storage.ModloaderMeta;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

public class BackupServerUtil extends CyanComponent implements LevelModDataReader {

	private HashMap<String, ModloaderMeta> savedLoaders = new HashMap<String, ModloaderMeta>();
	private static boolean hadPrompted = false;
	private static boolean backupOverride = false;
	private static File last = null;

	public static void backupIfNeeded(File levelData) {
		if (!levelData.exists())
			return;
		try {
			if (last != null && last.getCanonicalPath().equals(levelData.getCanonicalPath())) {
				return;
			} else {
				last = levelData;
			}
			BackupServerUtil container = new BackupServerUtil();
			CompoundTag root = NbtIo.readCompressed(levelData).getCompound("Data");
			ModloaderMeta.loadAll(container.savedLoaders, root);
			LoadUtil.checkWorldJoin(container, root.getCompound("Version").getString("Name"), true, msg -> {
				if (!hadPrompted) {
					warn("");
					warn("");
					warn("Mod changes detected, please confirm load.");
					warn("The following mods have changed:");
					warn(msg);
					warn("");
					info("Adding -Dcyan.backupload=[backup/ignore/cancel] will skip this prompt.");
				}

				String defaultValue = System.getProperty("cyan.backupload");
				if (defaultValue != null && defaultValue.equals("cancel")) {
					cancelStartup();
					return;
				} else if ((defaultValue != null && defaultValue.equals("ignore"))
						|| (hadPrompted && !backupOverride)) {
					ignoreMessage();
					return;
				} else if ((defaultValue != null && defaultValue.equals("backup")) || (hadPrompted && backupOverride)) {
					try {
						createBackup(levelData.getParentFile());
					} catch (IOException e) {
						error("Backup failed", e);
					}
				} else {
					if (GraphicsEnvironment.isHeadless()) {
						warn("");
						warn("");
						System.out.print("Do you want to create a backup? [Y/n/c] ");

						@SuppressWarnings("resource")
						Scanner sc = new Scanner(System.in);
						String result = sc.nextLine().trim();
						if (result.equalsIgnoreCase("Y")) {
							try {
								createBackup(levelData.getParentFile());
							} catch (IOException e) {
								error("Backup failed", e);
							}
						} else if (result.equalsIgnoreCase("N")) {
							ignoreMessage();
						} else {
							cancelStartup();
						}
					} else {
						warn("");
						warn("");
						msg = msg.replaceAll("\\\u00A7[0-9a-fk-r]", "");
						StringBuilder message = new StringBuilder();
						message.append("Warning, the following server mods have changed:").append("\n").append(msg)
								.append("\n").append("\n");
						message.append("Do you want to create a backup before starting the server?");
						int result = JOptionPane.showConfirmDialog(null, message, "Cyan Startup Warning",
								JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
						if (result == JOptionPane.CANCEL_OPTION) {
							cancelStartup();
						} else if (result == JOptionPane.YES_OPTION) {
							try {
								createBackup(levelData.getParentFile());
							} catch (IOException e) {
								error("Backup failed", e);
							}
						} else if (result == JOptionPane.NO_OPTION) {
							ignoreMessage();
						}
					}
				}
			});
		} catch (IOException e) {
		}
	}

	private static void createBackup(File levelStorage) throws IOException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
		File output = new File("backups", "[" + dateFormat.format(new Date()) + "] " + levelStorage.getName() + ".zip");
		info("Creating backup of world folder: " + levelStorage.getName() + "... Output file: " + output.getPath());
		if (!output.getParentFile().exists())
			output.getParentFile().mkdirs();
		FileOutputStream fos = new FileOutputStream(output);
		ZipOutputStream zip = new ZipOutputStream(fos);
		zipFiles(levelStorage, "", zip);
		zip.close();
		fos.close();
		backupOverride = true;
	}

	private static void zipFiles(File dir, String pref, ZipOutputStream zip) throws IOException {
		zip.putNextEntry(new ZipEntry(pref + dir.getName() + "/"));
		zip.closeEntry();
		for (File f : dir.listFiles(t -> !t.isDirectory())) {
			zip.putNextEntry(new ZipEntry(pref + dir.getName() + "/" + f.getName()));
			FileInputStream strm = new FileInputStream(f);
			strm.transferTo(zip);
			strm.close();
		}
		for (File d : dir.listFiles(t -> t.isDirectory())) {
			zipFiles(d, pref + dir.getName() + "/", zip);
		}
	}

	private static void ignoreMessage() {
		warn("Resuming server startup, no backup will be created.");
	}

	public static void cancelStartup() {
		warn("Server startup cancelled.");
		System.exit(0);
	}

	@Override
	public String[] cyanGetAllLoaders() {
		return savedLoaders.keySet().toArray(t -> new String[t]);
	}

	@Override
	public ModloaderMeta cyanGetLoader(String loader) {
		return savedLoaders.get(loader);
	}

}
