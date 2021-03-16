package org.asf.cyan;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Files;

import javax.swing.JOptionPane;

import org.asf.cyan.api.classloading.DynamicClassLoader;
import org.asf.cyan.api.config.CCFGConfigGenerator;
import org.asf.cyan.api.config.Configuration;
import org.asf.cyan.api.config.serializing.ObjectSerializer;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.core.CyanCore;
import org.asf.cyan.core.CyanInfo;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftVersionToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.auth.AuthenticationInfo;
import org.asf.cyan.minecraft.toolkits.mtk.auth.MinecraftAccountType;
import org.asf.cyan.minecraft.toolkits.mtk.auth.YggdrasilAuthentication;
import org.asf.cyan.minecraft.toolkits.mtk.auth.windowed.YggdrasilAuthenticationWindow;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionType;


// Large wrapper for the IDE, needed or CYAN won't run correctly.
// Half of it is for authenticating the game.

public class CyanIDEWrapper {

	/**
	 * Main initialization method for the IDE
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
	public static void main(String[] args) throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, IOException {

		MinecraftInstallationToolkit.setIDE();
		
		boolean deobf = Boolean.valueOf(System.getProperty("cyan.deobfuscated"));
		String side = System.getProperty("cyan.side");
		String clientMAIN = System.getProperty("cyan.launcher.client.wrapper", "net.minecraft.client.main.Main");
		String serverMAIN = System.getProperty("cyan.launcher.server.wrapper", "net.minecraft.server.Main");

		CyanCore.initLoader();
		((DynamicClassLoader) CyanCore.getCoreClassLoader()).setOptions(DynamicClassLoader.OPTION_ALLOW_DEFINE | DynamicClassLoader.OPTION_LOAD);
		((DynamicClassLoader) CyanCore.getCoreClassLoader()).addUrl(new URL(System.getProperty("mainJAR")));

		if (deobf) {
			CyanInfo.setDeobfuscated();
			CyanLoader.disableVanillaMappings();
		}

		CyanCore.setEntryMethod("IDE Launch");
		CyanLoader.initializeGame(side);

		String[] arguments = args;
		if (CyanCore.getSide() == GameSide.CLIENT) {
			File login = new File(".auth.ccfg");
			AuthStorage storage = new AuthStorage();
			if (!login.exists()) {
				int result = JOptionPane.showConfirmDialog(null,
						"Welcome to the Cyan Testing Environment,\nplease select your type of account for the game.\n\nYou can change the account by deleting the .auth.ccfg file in the game directory.\n\nDo you want to use a microsoft or mojang account?\nSelect yes for MSA\nSelect no for Mojang.",
						"Cyan IDE Debug", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if (result == JOptionPane.YES_OPTION) {
					storage.type = MinecraftAccountType.MSA;
				} else if (result == JOptionPane.NO_OPTION) {
					storage.type = MinecraftAccountType.MOJANG;
				} else {
					System.exit(1);
				}

				Files.writeString(login.toPath(), storage.toString());
			} else {
				storage.readAll(Files.readString(login.toPath()));
			}

			AuthenticationInfo account = null;
			try {
				if (storage.type != MinecraftAccountType.MSA) {
					account = AuthenticationInfo.authenticate(storage.login, storage.type);
				} else {
					// TODO: MSA
				}
			} catch (IOException ex) {
				if (storage.type != MinecraftAccountType.MSA) {
					YggdrasilAuthentication.init();
					YggdrasilAuthenticationWindow window;
					if (!storage.login.isEmpty())
						window = new YggdrasilAuthenticationWindow(storage.login, ex.getMessage());
					else
						window = new YggdrasilAuthenticationWindow();

					if (window.getAccount() == null) {
						System.exit(1);
					} else {
						storage.login = window.getAccount().getUserName();
						account = window.getAccount();
					}
				} else {
					// TODO: MSA
				}
			}
			if (account == null)
				System.exit(1);

			Files.writeString(login.toPath(), storage.toString());

			MinecraftToolkit.resetServerConnectionState();
			MinecraftToolkit.resolveVersions();

			MinecraftVersionInfo version = MinecraftVersionToolkit.createOrGetVersion(CyanInfo.getMinecraftVersion(),
					MinecraftVersionType.UNKNOWN, null, CyanInfo.getReleaseDate());

			if (!MinecraftInstallationToolkit.isVersionManifestSaved(version)
					&& !MinecraftToolkit.hasMinecraftDownloadConnection())
				throw new IOException("No network connection, manifest also not saved");

			if (!MinecraftInstallationToolkit.isVersionManifestSaved(version)) {
				MinecraftInstallationToolkit.saveVersionManifest(version);
			}

			String[] gameArgs = MinecraftInstallationToolkit.generateGameArguments(version, null, account.getPlayerName(),
					account.getUUID(), account.getAccessToken(), account.getAccountType(), new File("."),
					new File(System.getProperty("assetRoot")), System.getProperty("assetIndex"));
			arguments = new String[gameArgs.length + args.length];
			int index = 0;
			for (String arg : gameArgs) {
				arguments[index++] = arg;
			}
			for (String arg : args) {
				arguments[index++] = arg;
			}
		}

		String wrapper = (CyanCore.getSide() == GameSide.CLIENT ? clientMAIN : serverMAIN);
		CyanCore.startGame(wrapper, arguments);
	}

	static class AuthStorage extends Configuration<AuthStorage> {
		public MinecraftAccountType type;
		public String login = "";

		@Override
		public String toString() {
			return ObjectSerializer.getCCFGString(this, new CCFGConfigGenerator<AuthStorage>(this, true)); // FIXME
		}

		@Override
		public String filename() {
			return null;
		}

		@Override
		public String folder() {
			return null;
		}
	}
}
