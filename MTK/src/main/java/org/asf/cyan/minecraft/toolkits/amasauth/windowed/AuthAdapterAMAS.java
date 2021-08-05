package org.asf.cyan.minecraft.toolkits.amasauth.windowed;

import java.awt.BorderLayout;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.JDialog;

import org.asf.cyan.minecraft.toolkits.mtk.auth.windowed.SwingFXWebView;

public class AuthAdapterAMAS {

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

	public void load(JDialog frame, AMASAuthWindow window, Consumer<String> callback) {
		SwingFXWebView page = new SwingFXWebView();
		page.addUrlListener(url -> {
			if (url.startsWith("about:blank?amas-cancel=true")) {
				frame.dispose();
			} else if (url.startsWith("about:blank")) {
				Map<String, String> query = parseQuery(url.substring("about:blank".length()));
				String token = query.get("amas-token");
				callback.accept(token);
				frame.dispose();
			}
		});
		try {
			page.startingPage = window.getAuthURL();
		} catch (UnsupportedEncodingException e) {
		}
		frame.getContentPane().add(page, BorderLayout.CENTER);
	}

}
