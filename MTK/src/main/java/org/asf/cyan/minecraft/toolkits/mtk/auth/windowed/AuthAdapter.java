package org.asf.cyan.minecraft.toolkits.mtk.auth.windowed;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import org.asf.cyan.minecraft.toolkits.mtk.auth.MsaAuthentication;
import org.asf.cyan.minecraft.toolkits.mtk.auth.windowed.MsaAuthWindow.UserTokens;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AuthAdapter {

	/**
	 * Parses a given query into a map
	 * 
	 * @param query Query string
	 * @return HashMap instance with the query values
	 */
	public static Map<String, String> parseQuery(String query) {

		HashMap<String, String> map = new HashMap<String, String>();

		String key = "";
		String value = "";
		boolean isKey = true;

		for (int i = 0; i < query.length(); i++) {
			char ch = query.charAt(i);
			if (ch == '&' || ch == '?') {
				if (isKey && !key.isEmpty()) {
					map.put(key, "");
					key = "";
				} else if (!isKey && !key.isEmpty()) {
					try {
						map.put(key, URLDecoder.decode(value, "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						map.put(key, value);
					}
					isKey = true;
					key = "";
					value = "";
				}
			} else if (ch == '=') {
				isKey = !isKey;
			} else {
				if (isKey) {
					key += ch;
				} else {
					value += ch;
				}
			}
		}
		if (!key.isEmpty() || !value.isEmpty()) {
			try {
				map.put(key, URLDecoder.decode(value, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				map.put(key, value);
			}
		}

		return map;
	}

	public void load(MsaAuthentication auth, JDialog frame, MsaAuthWindow window,
			BiConsumer<UserTokens, BiConsumer<String, String>> callback) {
		String baseHTML = "%message2%";
		try {
			InputStream strm = getResource("postlogin.html");
			baseHTML = new String(strm.readAllBytes());
			strm.close();
		} catch (IOException e) {
		}

		final String htmlF = baseHTML;

		SwingFXWebView page = new SwingFXWebView();
		page.addPostLoadListener(doc -> {

			// Remove the broken footer links
			// Sorry Microsoft, but popups crash the program
			page.getWebEngine().executeScript(
					  "    var script = document.createElement(\"SCRIPT\");\n"
					+ "    script.src = 'https://code.jquery.com/jquery-3.6.0.min.js';\n"
					+ "    script.type = 'text/javascript';\n"
					+ "    script.onload = function() {\n"
					+ "       	var $ = window.jQuery;\n"
					+ "        	$(document).bind('DOMNodeInserted', function() {\n"
					+ "				if (document.getElementById('ftrTerms') != null)"
					+ "					document.getElementById('ftrTerms').remove();"
					+ "				if (document.getElementById('ftrPrivacy') != null)"
					+ "					document.getElementById('ftrPrivacy').remove();"
					+ "				if (document.getElementById('msccBanner') != null)"
					+ "					document.getElementById('msccBanner').remove();"
					+ "			});"
					+ "			if (document.getElementById('ftrTerms') != null)"
					+ "				document.getElementById('ftrTerms').remove();"
					+ "			if (document.getElementById('ftrPrivacy') != null)"
					+ "				document.getElementById('ftrPrivacy').remove();"
					+ "			if (document.getElementById('msccBanner') != null)"
					+ "				document.getElementById('msccBanner').remove();"
					+ "     }\n"
					+ "    document.getElementsByTagName(\"head\")[0].appendChild(script);\n");
			
		});
		page.addUrlListener(url -> {
			if (url.startsWith(MsaAuthentication.authResponseBase)) {
				try {
					URL u = new URL(url);
					String state = auth.getState();
					Map<String, String> query = parseQuery(u.toURI().getQuery());
					if (query.getOrDefault("state", "").equals(state)) {
						String code = query.get("code");

						String message = htmlF.replace("%title%", "Minecraft Login");
						message = message.replace("%message1%", "Please wait, logging you in...");
						message = message.replace("%message2%", "Retrieving token...");

						page.loadContent(message);
						new Thread(() -> {
							try {
								String json = auth.getTokenJson(code);
								JsonObject info = JsonParser.parseString(json).getAsJsonObject();

								String token = info.get("access_token").getAsString();
								String refreshToken = info.get("refresh_token").getAsString();

								UserTokens tokens = new UserTokens();
								tokens.accessToken = token;
								tokens.refreshToken = refreshToken;
								callback.accept(tokens, (title, msg) -> {
									if (title == null) {
										SwingUtilities.invokeLater(() -> frame.dispose());
										return;
									}
									String message2 = htmlF.replace("%title%", "Minecraft Login");
									message2 = message2.replace("%message1%", title);
									message2 = message2.replace("%message2%", msg);

									page.loadContent(message2);
								});
							} catch (IOException e) {
								page.setPage(auth.getAuthURL());
							}
						}, "Token connection").start();
					}
				} catch (IOException | URISyntaxException e) {
				}
			} else if (url.startsWith(MsaAuthentication.authBaseURL)) {
				try {
					URL u = new URL(url);
					Map<String, String> query = parseQuery(u.toURI().getQuery());
					if (query.getOrDefault("res", "").equals("cancel")) {
						frame.dispose();
					}
				} catch (IOException | URISyntaxException e) {
				}
			}
		});
		page.startingPage = auth.getAuthURL();
		frame.getContentPane().add(page, BorderLayout.CENTER);
	}

	public InputStream getResource(String resource) throws IOException {
		InputStream strm = getResourceStream(resource);
		if (strm == null)
			strm = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
		if (strm == null)
			strm = getClass().getResourceAsStream(resource);
		return strm;
	}

	private InputStream getResourceStream(String resource) {
		Object[] o = getResourceData(resource);
		if (o == null)
			return null;
		else
			return (InputStream) o[0];
	}

	private Object[] getResourceData(String resource) {
		String base = getClass().getProtectionDomain().getCodeSource().getLocation().toString();
		if (base.toString().startsWith("jar:"))
			base = base.substring(0, base.lastIndexOf("!")) + "!";
		else if (base.endsWith("/" + getClass().getTypeName().replace(".", "/") + ".class")) {
			base = base.substring(0,
					base.length() - ("/" + getClass().getTypeName().replace(".", "/") + ".class").length());
		}
		if (base.endsWith(".jar") || base.endsWith(".zip"))
			base = "jar:" + base + "!";
		try {
			URL u = new URL(base + "/" + resource);
			return new Object[] { u.openStream(), u };
		} catch (IOException e) {
			return null;
		}
	}
}
