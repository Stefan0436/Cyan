package org.asf.cyan.minecraft.toolkits.mtk.auth;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

import org.asf.cyan.api.config.CCFGConfigGenerator;
import org.asf.cyan.api.config.Configuration;
import org.asf.cyan.api.config.serializing.ObjectSerializer;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftToolkit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Minecraft authentication library, WARNING: Minecraft is migrating to
 * Microsoft accounts, this class will break down in the near future!
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class YggdrasilAuthentication {
	static Gson gson = new Gson();
	static File mcdir = null;
	static File mtkloginsavedir = null;
	static UUID token = null;
	static String authServer = "https://authserver.mojang.com/";

	static class AuthHolder extends Configuration<AuthHolder> {

		public String playerName;
		public String accessToken;
		public UUID playerUUID;
		public MinecraftAccountType accountType;
		public String userName;

		@Override
		public String filename() {
			return null;
		}

		@Override
		public String folder() {
			return null;
		}

	}

	/**
	 * Refreshing interval in minutes (default is 60)
	 */
	public static int refreshInterval = 60;

	/**
	 * Check if a user is saved (by login username)
	 * 
	 * @param username Login username
	 * @return True if saved, false otherwise
	 */
	public static boolean hasBeenSaved(String username) {
		File userfile = new File(mtkloginsavedir, "profile." + username.toLowerCase() + ".ccfg");
		return userfile.exists();
	}

	/**
	 * Initialize a new MinecraftAuthentication object by logging in with username
	 * and password
	 * 
	 * @param username Minecraft login username
	 * @param password Minecraft login password
	 * @throws IOException If file saving fails
	 */
	public static AuthenticationInfo authenticate(String username, String password) throws IOException {
		init();

		try {
			URL u = new URL(authServer + "/authenticate");
			HttpURLConnection c = (HttpURLConnection) u.openConnection();
			try {
				JsonObject root = new JsonObject();
				JsonObject agent = new JsonObject();
				agent.addProperty("name", "Minecraft");
				agent.addProperty("version", "1");

				root.add("agent", agent);
				root.addProperty("username", username);
				root.addProperty("password", password);
				root.addProperty("requestUser", false);
				root.addProperty("clientToken", token.toString());

				byte[] cont = gson.toJson(root).getBytes(StandardCharsets.UTF_8);
				c.setRequestMethod("POST");

				c.setRequestProperty("User-Agent",
						"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36");
				c.setRequestProperty("Content-Type", "application/json");
				c.setDoOutput(true);
				c.setDoInput(true);
				c.connect();
				c.getOutputStream().write(cont);

				if (c.getResponseCode() == 200) {
					String json = new String(c.getInputStream().readAllBytes());
					root = JsonParser.parseString(json).getAsJsonObject();

					JsonObject selectedProfile = root.get("selectedProfile").getAsJsonObject();
					String accessToken = root.get("accessToken").getAsString();

					boolean legacyProfile = (selectedProfile.has("legacyProfile")
							? selectedProfile.get("legacyProfile").getAsBoolean()
							: false);
					UUID uuid = java.util.UUID.fromString(selectedProfile.get("id").getAsString().replaceFirst(
							"(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
							"$1-$2-$3-$4-$5"));
					String playerName = selectedProfile.get("name").getAsString();

					AuthenticationInfo account = AuthenticationInfo.create(username, playerName, accessToken, uuid,
							(legacyProfile ? MinecraftAccountType.LEGACY : MinecraftAccountType.MOJANG));
					c.disconnect();

					saveUser(account);
					return account;
				} else {
					String json = new String(c.getErrorStream().readAllBytes());
					root = JsonParser.parseString(json).getAsJsonObject();
					String msg = root.get("errorMessage").getAsString();
					String err = root.get("error").getAsString();
					if (msg.equals("Forbidden") && err.equals("ForbiddenOperationException"))
						msg = "Invalid credentials.";
					c.disconnect();
					throw new IOException(msg);
				}
			} catch (IOException e) {
				c.disconnect();
				throw e;
			}
		} catch (IOException e1) {
			throw e1;
		}
	}

	/**
	 * Initialize a new MinecraftAuthentication object by logging in with username
	 * and saved credential information
	 * 
	 * @param username Minecraft login username
	 * @throws IOException If file loading fails
	 */
	public static AuthenticationInfo authenticate(String username) throws IOException {
		return authenticate(username, !MinecraftToolkit.hasMinecraftDownloadConnection());
	}

	/**
	 * Initialize a new MinecraftAuthentication object by logging in with username
	 * and saved credential information
	 * 
	 * @param username Minecraft login username
	 * @param offline  True to use local information only
	 * @throws IOException If file loading fails
	 */
	public static AuthenticationInfo authenticate(String username, boolean offline) throws IOException {
		init();
		File userfile = new File(mtkloginsavedir, "profile." + username.toLowerCase() + ".ccfg");
		if (!userfile.exists())
			throw new IOException("User file not found");

		AuthHolder holder = new AuthHolder().readAll(Files.readString(userfile.toPath()));
		AuthenticationInfo account = AuthenticationInfo.create(holder.userName, holder.playerName, holder.accessToken, holder.playerUUID,
				holder.accountType);

		if (!offline) {
			if (!isValid(account)) {
				try {
					refresh(account);
				} catch (IOException e) {
					userfile.delete();
					throw new IOException("Login no longer valid.");
				}
			}
		}
		
		saveUser(account);
		return account;
	}

	/**
	 * Save the user login information (needed for the username-only constructor)
	 * 
	 * @throws IOException If saving fails
	 */
	public static void saveUser(AuthenticationInfo account) throws IOException {
		if (getLoginSaveDir() == null)
			throw new IOException("Login save directory not loaded");
		File userfile = new File(mtkloginsavedir, "profile." + account.getUserName().toLowerCase() + ".ccfg");
		if (userfile.exists())
			userfile.delete();
		FileWriter writer = new FileWriter(userfile);
		AuthHolder holder = new AuthHolder();
		holder.userName = account.getUserName();
		holder.playerName = account.getPlayerName();
		holder.accessToken = account.getAccessToken();
		holder.playerUUID = account.getUUID();
		holder.accountType = account.getAccountType();
		writer.write(holder.toString());
		writer.close();
	}

	/**
	 * Check if the current information is still valid
	 * 
	 * @return True if the login information is valid, false otherwise
	 */
	public static boolean isValid(AuthenticationInfo account) {
		try {
			URL u = new URL(authServer + "/validate");
			HttpURLConnection c = (HttpURLConnection) u.openConnection();
			try {
				JsonObject root = new JsonObject();
				root.addProperty("accessToken", account.getAccessToken());
				root.addProperty("clientToken", getClientToken().toString());
				byte[] cont = gson.toJson(root).getBytes(StandardCharsets.UTF_8);
				c.setRequestMethod("POST");
				c.setRequestProperty("Content-Type", "application/json");
				c.setDoOutput(true);
				c.setDoInput(true);
				c.connect();
				c.getOutputStream().write(cont);

				if (c.getResponseCode() == 204) {
					c.disconnect();
					return true;
				} else {
					c.disconnect();
					return false;
				}
			} catch (IOException e) {
				c.disconnect();
				return false;
			}
		} catch (IOException e1) {
			return false;
		}
	}

	/**
	 * Refresh the access token
	 * 
	 * @throws IOException If saving or refreshing fails
	 */
	public static void refresh(AuthenticationInfo account) throws IOException {
		saveUser(account);

		try {
			URL u = new URL(authServer + "/refresh");
			HttpURLConnection c = (HttpURLConnection) u.openConnection();
			try {
				JsonObject root = new JsonObject();
				root.addProperty("accessToken", account.getAccessToken());
				root.addProperty("clientToken", getClientToken().toString());

				byte[] cont = gson.toJson(root).getBytes(StandardCharsets.UTF_8);
				c.setRequestMethod("POST");
				c.setRequestProperty("Content-Type", "application/json");
				c.setDoOutput(true);
				c.setDoInput(true);
				c.connect();
				c.getOutputStream().write(cont);

				if (c.getResponseCode() == 200) {
					String json = new String(c.getInputStream().readAllBytes());
					root = JsonParser.parseString(json).getAsJsonObject();
					JsonObject selectedProfile = root.get("selectedProfile").getAsJsonObject();
					
					account.playerName = selectedProfile.get("name").getAsString();
					account.accessToken = root.get("accessToken").getAsString();

					c.disconnect();
				} else {
					String json = new String(c.getErrorStream().readAllBytes());
					root = JsonParser.parseString(json).getAsJsonObject();
					String msg = root.get("errorMessage").getAsString();
					c.disconnect();
					throw new IOException(msg);
				}
			} catch (IOException e) {
				c.disconnect();
				throw e;
			}
		} catch (IOException e1) {
			throw e1;
		}

		saveUser(account);
	}

	/**
	 * Load the save directory and client token
	 */
	public static void init() {
		if (getLoginSaveDir() == null)
			loadLoginSaveDir();
		if (getClientToken() == null) {
			if (!loadClientToken()) {
				generateClientToken();
				saveClientToken();
			}
		}
	}

	/**
	 * Get the client token of the Cornflower Launcher API
	 * 
	 * @return Client token UUID
	 */
	public static UUID getClientToken() {
		return token;
	}

	/**
	 * Load the login save directory, automatically called on MTK initialization
	 */
	public static void loadLoginSaveDir() {
		mcdir = MinecraftInstallationToolkit.getMinecraftDirectory();
		mtkloginsavedir = new File(mcdir, ".yggdrasil-auth");
		if (!mtkloginsavedir.exists())
			mtkloginsavedir.mkdir();
	}

	/**
	 * Get the login save directory, null if not loaded
	 * 
	 * @return The login save directory
	 */
	public static File getLoginSaveDir() {
		return mtkloginsavedir;
	}

	/**
	 * Load the login client token of the Cornflower Launcher API, automatically
	 * called on MTK initialization
	 * 
	 * @return True if success, false otherwise (False if it cannot be loaded due to
	 *         nonexistent files or directories)
	 */
	public static boolean loadClientToken() {
		if (getLoginSaveDir() == null)
			return false;
		File logindat = new File(getLoginSaveDir(), "client.token");
		if (!logindat.exists())
			return false;
		else {
			try {
				token = UUID.fromString(Files.readString(logindat.toPath()));
				return true;
			} catch (IOException e) {
				return false;
			}
		}
	}

	/**
	 * Save the login client token of the Cornflower Launcher API, automatically
	 * called if the token is newly generated by the constructor
	 * 
	 * @return True if success, false otherwise (False if it cannot be saved due to
	 *         nonexistent directories)
	 */
	public static boolean saveClientToken() {
		if (getLoginSaveDir() == null)
			return false;
		File logindat = new File(getLoginSaveDir(), "client.token");
		if (logindat.exists())
			logindat.delete();
		try {
			FileOutputStream strm = new FileOutputStream(logindat);
			strm.write(token.toString().getBytes());
			strm.close();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Generate a new UUID Client Token (UUID.randomUUID)
	 * 
	 * @return New UUID
	 */
	public static UUID generateClientToken() {
		token = UUID.randomUUID();
		return token;
	}
}
