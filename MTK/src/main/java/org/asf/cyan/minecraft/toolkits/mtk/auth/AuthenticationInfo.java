package org.asf.cyan.minecraft.toolkits.mtk.auth;

import java.io.IOException;
import java.util.UUID;

import org.asf.cyan.minecraft.toolkits.mtk.auth.windowed.YggdrasilAuthenticationWindow;

public class AuthenticationInfo {
	private AuthenticationInfo() {
	}

	String playerName;
	String accessToken;
	String userName;
	private UUID playerUUID;
	private MinecraftAccountType accountType;

	public String getPlayerName() {
		return playerName;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public UUID getUUID() {
		return playerUUID;
	}

	public MinecraftAccountType getAccountType() {
		return accountType;
	}

	public static AuthenticationInfo create(String playerName, String token, UUID uuid, MinecraftAccountType type) {
		AuthenticationInfo info = new AuthenticationInfo();
		info.playerName = playerName;
		info.accessToken = token;
		info.playerUUID = uuid;
		info.accountType = type;
		info.userName = info.playerName;
		return info;
	}

	public static AuthenticationInfo create(String userName, String playerName, String token, UUID uuid,
			MinecraftAccountType type) {
		AuthenticationInfo info = new AuthenticationInfo();
		info.playerName = playerName;
		info.accessToken = token;
		info.playerUUID = uuid;
		info.accountType = type;
		info.userName = userName;
		return info;
	}

	/**
	 * Authenticate with username and password (legacy and mojang only)
	 * 
	 * @param username Account username
	 * @param password Account password
	 * @param type     Account type
	 * @return New AuthenticationInfo
	 * @throws IOException If authenticating fails
	 */
	public static AuthenticationInfo authenticate(String username, String password, MinecraftAccountType type)
			throws IOException {
		if (type.equals(MinecraftAccountType.MSA))
			throw new UnsupportedOperationException("Microsoft accounts are not supported in non-interactive mode.");

		YggdrasilAuthentication.init();
		return YggdrasilAuthentication.authenticate(username, password);
	}

	/**
	 * Authenticate with username and saved credentials (legacy and mojang only)
	 * 
	 * @param username Account username
	 * @param type     Account type
	 * @return New AuthenticationInfo
	 * @throws IOException If authenticating fails
	 */
	public static AuthenticationInfo authenticate(String username, MinecraftAccountType type) throws IOException {
		if (type.equals(MinecraftAccountType.MSA))
			throw new UnsupportedOperationException("Microsoft accounts are not supported in non-interactive mode.");

		YggdrasilAuthentication.init();
		return YggdrasilAuthentication.authenticate(username);
	}

	/**
	 * Authenticate through an interactive interface
	 * 
	 * @param type Account type
	 * @return New AuthenticationInfo
	 * @throws IOException If authenticating fails
	 */
	public static AuthenticationInfo authenticate(MinecraftAccountType type) throws IOException {
		if (type.equals(MinecraftAccountType.MSA))
			return MsaAuthentication.newAuth().getAccount();

		YggdrasilAuthentication.init();
		YggdrasilAuthenticationWindow window = new YggdrasilAuthenticationWindow();
		return window.getAccount();
	}

	public String getUserName() {
		return userName;
	}
}
