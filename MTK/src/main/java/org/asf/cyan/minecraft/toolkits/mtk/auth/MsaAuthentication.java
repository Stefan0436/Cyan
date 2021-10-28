package org.asf.cyan.minecraft.toolkits.mtk.auth;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;
import java.util.function.BiConsumer;

import javax.swing.JOptionPane;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.config.Configuration;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.auth.windowed.MsaAuthWindow;
import org.asf.cyan.minecraft.toolkits.mtk.auth.windowed.MsaAuthWindow.UserTokens;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 
 * Microsoft Account Authentication Library
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public class MsaAuthentication extends CyanComponent {

	//
	// Azure client id
	//
	// NOTE:
	// The Azure application requires 'https://login.live.com/oauth20_desktop.srf'
	// (LiveSDK) as redirect URL.
	//
	private static final String clientID = "20ea11e3-8526-48cc-b187-4c2960502df6";

	// Authentication template URL
	private static final String authTemplateURL = "https://login.live.com/oauth20_authorize.srf"
			+ "?client_id=%clientid%" + "&response_type=code"
			+ "&redirect_url=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf"
			+ "&scope=XboxLive.signin%20offline_access" + "&state=%state%";

	private static final String tokenURL = "https://login.live.com/oauth20_token.srf";
	private static final String tokenBodyTemplate = "client_id=%clientid%" + "&code=%code%"
			+ "&grant_type=authorization_code" + "&redirect_url=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf";
	private static final String refreshBodyTemplate = "client_id=%clientid%" + "&refresh_token=%token%"
			+ "&grant_type=refresh_token" + "&redirect_url=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf";

	private static final String mcProfileURL = "https://api.minecraftservices.com/minecraft/profile";
	private static final String mcStoreURL = "https://api.minecraftservices.com/entitlements/mcstore";
	private static final String loginURL = "https://api.minecraftservices.com/authentication/login_with_xbox";
	private static final String xblURL = "https://user.auth.xboxlive.com/user/authenticate";
	private static final String xstsURL = "https://xsts.auth.xboxlive.com/xsts/authorize";

	public static final String baseURL = "https://login.live.com/oauth20_desktop.srf";
	public static final String authBaseURL = "https://login.live.com/oauth20_authorize.srf";
	public static final String authResponseBase = baseURL + "?code=";

	/**
	 * Retrieves the token JSON document
	 * 
	 * @param code Authorization code from GUI
	 * @return JSON string
	 * @throws IOException If the request fails
	 */
	public String getTokenJson(String code) throws IOException {
		String u = tokenURL;
		HttpURLConnection conn = (HttpURLConnection) new URL(u).openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		String body = tokenBodyTemplate.replace("%clientid%", clientID).replace("%code%", code);
		conn.setRequestProperty("Content-Length", body.getBytes().length + "");
		conn.getOutputStream().write(body.getBytes());
		InputStream strm = conn.getInputStream();
		String response = new String(strm.readAllBytes());
		strm.close();
		return response;
	}

	/**
	 * Refreshes the account using the given refresh token
	 * 
	 * @param token Refresh token
	 * @return JSON string
	 * @throws IOException If the request fails
	 */
	public String refreshToken(String token) throws IOException {
		String u = tokenURL;
		HttpURLConnection conn = (HttpURLConnection) new URL(u).openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		String body = refreshBodyTemplate.replace("%clientid%", clientID).replace("%token%", token);
		conn.setRequestProperty("Content-Length", body.getBytes().length + "");
		conn.getOutputStream().write(body.getBytes());
		InputStream strm = conn.getInputStream();
		String response = new String(strm.readAllBytes());
		strm.close();
		return response;
	}

	/**
	 * Creates a new authentication layer
	 * 
	 * @return MsaAuthentication instance
	 */
	public static MsaAuthentication newAuth() {
		return new MsaAuthentication();
	}

	private UUID state = UUID.randomUUID();

	/**
	 * Generates the authentication URL
	 * 
	 * @return URL string
	 */
	public String getAuthURL() {
		return authTemplateURL.replace("%clientid%", clientID).replace("%state%", state.toString());
	}

	private AuthenticationInfo info = null;
	private boolean auth = false;

	/**
	 * Retrieves the output user, returns null if cancelled
	 * 
	 * @return AuthenticationInfo or null
	 */
	public AuthenticationInfo getAccount() {
		if (!auth) {
			begin();
		}
		return info;
	}

	private File authDir;

	private void begin() {
		info("Authenticating the game with Microsoft...");
		authDir = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), ".msa-auth");
		if (!authDir.exists())
			authDir.mkdir();

		File tokenFile = new File(authDir, "tokens.ccfg");
		if (tokenFile.exists()) {
			TokenFile file;
			try {
				file = new TokenFile(authDir).readAll();
			} catch (IOException e1) {
				throw new RuntimeException(e1);
			}
			UserTokens tokens = new UserTokens();
			tokens.refreshToken = file.refreshToken;
			if (tokens.refreshToken != null) {
				try {
					JsonObject obj = JsonParser.parseString(refreshToken(tokens.refreshToken)).getAsJsonObject();
					tokens.accessToken = obj.get("access_token").getAsString();
					tokens.refreshToken = obj.get("refresh_token").getAsString();
					authenticate(tokens, (t1, t2) -> info(t2), false);
					return;
				} catch (IOException e) {
					if (!MinecraftToolkit.hasMinecraftDownloadConnection() && file.uuid != null
							&& file.playerName != null && file.accessToken != null) {
						info = AuthenticationInfo.create(file.playerName, file.accessToken,
								UUID.fromString(file.uuid.replaceFirst(
										"(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
										"$1-$2-$3-$4-$5")),
								MinecraftAccountType.MSA);
						return;
					}
				}
			}
		}

		auth = true;
		new MsaAuthWindow(this, (t, writeback) -> {
			try {
				authenticate(t, writeback, true);
			} catch (IOException e) {
			}
		});
	}

	private static String mcPubKey = "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAtz7jy4jRH3psj5AbVS6WNHjniqlr/f5JDly2M8OKGK81nPEq765tJuSILOWrC3KQRvHJIhf84+ekMGH7iGlO4DPGDVb6hBGoMMBhCq2jkBjuJ7fVi3oOxy5EsA/IQqa69e55ugM+GJKUndLyHeNnX6RzRzDT4tX/i68WJikwL8rR8Jq49aVJlIEFT6F+1rDQdU2qcpfT04CBYLM5gMxEfWRl6u1PNQixz8vSOv8pA6hB2DU8Y08VvbK7X2ls+BiS3wqqj3nyVWqoxrwVKiXRkIqIyIAedYDFSaIq5vbmnVtIonWQPeug4/0spLQoWnTUpXRZe2/+uAKN1RY9mmaBpRFV/Osz3PDOoICGb5AZ0asLFf/qEvGJ+di6Ltt8/aaoBuVw+7fnTw2BhkhSq1S/va6LxHZGXE9wsLj4CN8mZXHfwVD9QG0VNQTUgEGZ4ngf7+0u30p7mPt5sYy3H+FmsWXqFZn55pecmrgNLqtETPWMNpWc2fJu/qqnxE9o2tBGy/MqJiw3iLYxf7U+4le4jM49AUKrO16bD1rdFwyVuNaTefObKjEMTX9gyVUF6o7oDEItp5NHxFm3CqnQRmchHsMs+NxEnN4E9a8PDB23b4yjKOQ9VHDxBxuaZJU60GBCIOF9tslb7OAkheSJx5XyEYblHbogFGPRFU++NrSQRX0CAwEAAQ==";

	private void authenticate(UserTokens tokens, BiConsumer<String, String> logger, boolean newLogin)
			throws IOException {
		TokenFile file = new TokenFile(authDir);
		file.refreshToken = tokens.refreshToken;
		try {
			file.writeAll();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		if (logger != null)
			logger.accept("Please wait, logging you in...", "Authenticating with XBL...");
		try {
			String json = getXBLJson(tokens);
			JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
			String token = obj.get("Token").getAsString();
			String uhs = obj.get("DisplayClaims").getAsJsonObject().get("xui").getAsJsonArray().get(0).getAsJsonObject()
					.get("uhs").getAsString();

			if (logger != null)
				logger.accept("Please wait, logging you in...", "Authenticating with XSTS...");
			json = getXSTSJson(token, uhs);
			obj = JsonParser.parseString(json).getAsJsonObject();
			if (obj.has("XErr")) {
				if (obj.get("XErr").getAsLong() == 2148916233l)
					if (logger != null && newLogin)
						logger.accept("Login Failed!",
								"<br/>Cannot continue with the login process.<br/><br/>Please create an Xbox Live account before using it with the MTK.");
					else {
						throw new IOException("Authentication Error");
					}
				else if (obj.get("XErr").getAsLong() == 2148916233l)
					if (logger != null && newLogin)
						logger.accept("Login Failed!",
								"<br/>Cannot continue with the login process.<br/><br/>This account is a child account, someone will need to add it to a Microsoft Family before it can be used with the MTK.");
					else {
						throw new IOException("Authentication Error");
					}
				return;
			}
			token = obj.get("Token").getAsString();

			if (logger != null)
				logger.accept("Please wait, logging you in...", "Logging into Minecraft...");
			json = getLoginJson(token, uhs);
			obj = JsonParser.parseString(json).getAsJsonObject();
			token = obj.get("access_token").getAsString();

			if (logger != null)
				logger.accept("Please wait, logging you in...", "Checking game ownership...");
			json = getMcJson(token);
			obj = JsonParser.parseString(json).getAsJsonObject();

			JsonArray items = obj.get("items").getAsJsonArray();

			try {
				verifySignature(obj.get("signature").getAsString());
			} catch (SignatureException e) {
				if (logger != null && newLogin) {
					logger.accept("Login Failed!",
							"<br/>Response signature check failed!<br/><br/>This is either an MTK error or the minecraft server URL has been highjacked!");
				} else {
					throw new IOException("Authentication Error");
				}
				return;
			}

			boolean found = false;
			boolean foundProduct = false;
			for (JsonElement ele : items) {
				JsonObject item = ele.getAsJsonObject();
				String name = item.get("name").getAsString();
				if (name.equals("product_minecraft")) {
					foundProduct = true;
				} else if (name.equals("game_minecraft")) {
					found = true;
				}

				try {
					verifySignature(item.get("signature").getAsString());
				} catch (SignatureException e) {
					if (logger != null && newLogin) {
						logger.accept("Login Failed!",
								"<br/>Response signature check failed!<br/><br/>This is either an MTK error or the minecraft server URL has been highjacked!");
					} else {
						throw new IOException("Authentication Error");
					}
					return;
				}
			}

			if (!found || !foundProduct) {
				if (logger != null && newLogin) {
					logger.accept("Login Failed!", "<br/>This user does not own a minecraft profile.");
				} else {
					throw new IOException("Authentication Error");
				}
				return;
			}

			json = getProfileJson(token);
			obj = JsonParser.parseString(json).getAsJsonObject();
			info = AuthenticationInfo.create(obj.get("name").getAsString(), token,
					UUID.fromString(obj.get("id").getAsString().replaceFirst(
							"(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
							"$1-$2-$3-$4-$5")),
					MinecraftAccountType.MSA);
			file.playerName = obj.get("name").getAsString();
			file.accessToken = token;
			file.uuid = obj.get("id").getAsString();
			try {
				file.writeAll();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			if (logger != null && newLogin)
				logger.accept(null, "");
		} catch (IOException e) {
			if (logger != null && newLogin) {
				JOptionPane.showMessageDialog(null, "Failed to authenticate, closing login window.",
						"Authentication Error", JOptionPane.ERROR_MESSAGE);
				logger.accept(null, "");
			} else
				throw new IOException("Authentication Error");
		}
	}

	private void verifySignature(String token) throws SignatureException {
		try {
			String[] segments = token.split("\\.");
			byte[] signature = Base64.getUrlDecoder().decode(segments[2]);
			String content = segments[0] + "." + segments[1];

			X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.getDecoder().decode(mcPubKey.getBytes()));
			KeyFactory keys = KeyFactory.getInstance("RSA");
			PublicKey key = keys.generatePublic(spec);
			Signature sig = Signature.getInstance("Sha256WithRSA");
			sig.initVerify(key);
			sig.update(content.getBytes());
			if (!sig.verify(signature))
				throw new SignatureException("Signature check failed");
		} catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | SignatureException e) {
			throw new SignatureException("Signature check failed");
		}
	}

	private String getMcJson(String token) throws MalformedURLException, IOException {
		String u = mcStoreURL;

		HttpURLConnection conn = (HttpURLConnection) new URL(u).openConnection();
		conn.setDoOutput(true);
		conn.setRequestProperty("Authorization", "Bearer " + token);

		InputStream strm = conn.getInputStream();
		String response = new String(strm.readAllBytes());
		strm.close();
		return response;
	}

	private String getProfileJson(String token) throws MalformedURLException, IOException {
		String u = mcProfileURL;

		HttpURLConnection conn = (HttpURLConnection) new URL(u).openConnection();
		conn.setDoOutput(true);
		conn.setRequestProperty("Authorization", "Bearer " + token);

		InputStream strm = conn.getInputStream();
		String response = new String(strm.readAllBytes());
		strm.close();
		return response;
	}

	private String getLoginJson(String token, String uhs) throws MalformedURLException, IOException {
		String u = loginURL;

		JsonObject json = new JsonObject();
		json.addProperty("identityToken", "XBL3.0 x=" + uhs + ";" + token);

		String body = json.toString();

		HttpURLConnection conn = (HttpURLConnection) new URL(u).openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setRequestProperty("Accept", "application/json");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Content-Length", body.getBytes().length + "");
		conn.getOutputStream().write(body.getBytes());

		if (conn.getResponseCode() == 401) {
			InputStream error = conn.getErrorStream();
			String errorMsg = new String(error.readAllBytes());
			error.close();
			return errorMsg;
		}

		InputStream strm = conn.getInputStream();
		String response = new String(strm.readAllBytes());
		strm.close();
		return response;
	}

	private String getXSTSJson(String token, String uhs) throws MalformedURLException, IOException {
		String u = xstsURL;

		JsonObject json = new JsonObject();
		JsonObject json2 = new JsonObject();
		json2.addProperty("SandboxId", "RETAIL");
		JsonArray arr = new JsonArray();
		arr.add(token);
		json2.add("UserTokens", arr);
		json.add("Properties", json2);
		json.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
		json.addProperty("TokenType", "JWT");

		String body = json.toString();

		HttpURLConnection conn = (HttpURLConnection) new URL(u).openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setRequestProperty("Accept", "application/json");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Content-Length", body.getBytes().length + "");
		conn.getOutputStream().write(body.getBytes());

		if (conn.getResponseCode() == 401) {
			InputStream error = conn.getErrorStream();
			String errorMsg = new String(error.readAllBytes());
			error.close();
			return errorMsg;
		}

		InputStream strm = conn.getInputStream();
		String response = new String(strm.readAllBytes());
		strm.close();
		return response;
	}

	private String getXBLJson(UserTokens tokens) throws MalformedURLException, IOException {
		String u = xblURL;

		JsonObject json = new JsonObject();
		JsonObject json2 = new JsonObject();
		json2.addProperty("AuthMethod", "RPS");
		json2.addProperty("SiteName", "user.auth.xboxlive.com");
		json2.addProperty("RpsTicket", "d=" + tokens.accessToken);
		json.add("Properties", json2);
		json.addProperty("RelyingParty", "http://auth.xboxlive.com");
		json.addProperty("TokenType", "JWT");

		String body = json.toString();

		HttpURLConnection conn = (HttpURLConnection) new URL(u).openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setRequestProperty("Accept", "application/json");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Content-Length", body.getBytes().length + "");
		conn.getOutputStream().write(body.getBytes());

		InputStream strm = conn.getInputStream();
		String response = new String(strm.readAllBytes());
		strm.close();
		return response;
	}

	public class TokenFile extends Configuration<TokenFile> {

		public String refreshToken;
		public String accessToken;
		public String uuid;
		public String playerName;

		public TokenFile(File dir) {
			super(dir.getAbsolutePath());
		}

		@Override
		public String filename() {
			return "tokens.ccfg";
		}

		@Override
		public String folder() {
			return "";
		}

	}

	/**
	 * Retrieves the 'state' UUID
	 * 
	 * @return UUID string
	 */
	public String getState() {
		return state.toString();
	}

}
