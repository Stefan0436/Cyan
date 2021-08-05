package org.asf.cyan;

import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import org.asf.cyan.security.ClassTrustEntry;
import org.asf.cyan.security.PackageTrustEntry;
import org.asf.cyan.security.TrustContainer;
import org.asf.cyan.security.TrustContainerBuilder;

public class CtcUtil {

	public static abstract class HttpCredential {

		public String provideHeaderEarly() {
			return null;
		}

		public abstract boolean supportsAuthMethod(String type);

		public abstract boolean supportsGroup(String group);

		public abstract String buildHeader(String group, String type);

	}

	public static String pack(File input, File output, Consumer<Integer> setMax, Consumer<Integer> setValue)
			throws IOException {

		int value = 1;

		if (output.getPath().startsWith(".") || output.getPath().startsWith("/"))
			output.getParentFile().mkdirs();

		if (!new File(input, "main.header").exists())
			throw new IOException("Invalid UCTC directory!");

		int amount = 0;
		for (File f : input.listFiles()) {
			if (f.isDirectory()) {
				for (File f2 : f.listFiles()) {
					if (!f2.getName().endsWith(".cls")) {
						throw new IOException("Invalid UCTC directory!");
					}
					amount++;
				}
				amount++;
			} else {
				if (f.getName().equals("main.header"))
					amount++;
				else {
					throw new IOException("Invalid UCTC directory!");
				}
			}
		}

		setMax.accept(amount);
		String name = "";
		for (String line : Files.readAllLines(new File(input, "main.header").toPath())) {
			if (line.startsWith("Name: ")) {
				name = line.substring("Name: ".length());
			}
		}

		if (name.isEmpty())
			throw new IOException("Invalid UCTC directory! Invalid header file!");

		TrustContainerBuilder builder = new TrustContainerBuilder(name);
		setValue.accept(value++);

		for (File pkg : input.listFiles()) {
			if (pkg.isDirectory()) {
				for (File cls : pkg.listFiles()) {
					if (cls.getName().endsWith(".cls")) {
						String clname = cls.getName().substring(0, cls.getName().lastIndexOf(".cls"));
						if (Files.readAllLines(cls.toPath()).size() < 1) {
							throw new IOException("Invalid UCTC directory! Invalid class file detected!");
						}
						ArrayList<String> hashes = new ArrayList<String>();
						for (String line : Files.readAllLines(cls.toPath())) {
							line = line.trim().replace("\t", "    ");
							if (line.contains(" "))
								line = line.substring(0, line.indexOf(" "));

							if (!line.isEmpty()) {
								hashes.add(line);
							}
						}
						if (pkg.getName().equals("(default)")) {
							builder.addClass("", clname, hashes.toArray(t -> new String[t]));
						} else
							builder.addClass(pkg.getName(), clname, hashes.toArray(t -> new String[t]));
						setValue.accept(value++);
					}
				}
				setValue.accept(value++);
			}
		}

		TrustContainer container = builder.build();
		container.exportContainer(output);
		return container.getVersion();
	}

	public static void unpack(File input, File output, Consumer<Integer> setMax, Consumer<Integer> setValue)
			throws IOException {
		output.mkdirs();
		int value = 1;
		TrustContainer container = TrustContainer.importContainer(input);
		int amount = container.getEntries().length;
		for (PackageTrustEntry en : container.getEntries()) {
			amount += en.getEntries().length;
		}
		setMax.accept(amount + 1);

		StringBuilder mainHeader = new StringBuilder();
		mainHeader.append("Name: " + container.getContainerName()).append(System.lineSeparator());
		mainHeader.append("Timestamp: " + container.getVersion()).append(System.lineSeparator());
		Files.writeString(new File(output, "main.header").toPath(), mainHeader.toString());
		setValue.accept(value++);

		for (PackageTrustEntry en : container.getEntries()) {
			String pkg = en.getName();
			if (pkg.isEmpty())
				pkg = "(default)";
			File packageDir = new File(output, pkg);
			if (!packageDir.exists())
				packageDir.mkdirs();

			for (ClassTrustEntry cls : en.getEntries()) {
				StringBuilder hashFile = new StringBuilder();
				for (String hash : cls.getHashes()) {
					hashFile.append(hash).append(System.lineSeparator());
				}

				Files.writeString(new File(packageDir, cls.getName() + ".cls").toPath(), hashFile.toString());
				setValue.accept(value++);
			}
			setValue.accept(value++);
		}
	}

	public static void publish(File input, URL output, List<HttpCredential> credential, Consumer<String> outputLogger)
			throws IOException {
		TrustContainer container = TrustContainer.importContainer(input);
		publishInternal(input,
				new URL(output.toString() + "/" + input.getName() + (output.toString().contains("?") ? "&" : "?")
						+ "version=" + container.getVersion() + "&name=" + container.getContainerName()),
				null, credential, new HashMap<String, String>(), null);
		outputLogger.accept(input.getName());

		if (new File(input.getCanonicalPath() + ".sha256").exists()) {
			publishInternal(new File(input.getCanonicalPath() + ".sha256"),
					new URL(output.toString() + "/" + new File(input.getCanonicalPath() + ".sha256").getName()
							+ (output.toString().contains("?") ? "&" : "?") + "version=" + container.getVersion()
							+ "&name=" + container.getContainerName()),
					null, credential, new HashMap<String, String>(), null);
			outputLogger.accept(new File(input.getCanonicalPath() + ".sha256").getName());
		}
	}

	private static void publishInternal(File input, URL dest, HttpURLConnection connection,
			List<HttpCredential> credential, HashMap<String, String> cookies, String authorization) throws IOException {
		if (connection == null) {
			connection = (HttpURLConnection) dest.openConnection();
			connection.setRequestProperty("X-Use-HTTP-Authentication", "True");
		}

		connection.setInstanceFollowRedirects(false);
		connection.setRequestMethod("PUT");
		connection.setRequestProperty("Content-Type", "application/octet-stream");

		String cookieStr = "";
		for (String cookie : cookies.keySet()) {
			String value = cookies.get(cookie);
			if (!cookieStr.isEmpty()) {
				cookieStr += "; ";
			}

			cookieStr += URLEncoder.encode(cookie, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8");
		}

		if (!cookieStr.isEmpty()) {
			connection.setRequestProperty("Cookie", cookieStr);
		}

		if (authorization != null) {
			connection.setRequestProperty("Authorization", authorization);
		} else {
			String h = null;
			for (HttpCredential cred : credential) {
				h = cred.provideHeaderEarly();
				if (h != null)
					break;
			}
			if (h != null)
				connection.setRequestProperty("Authorization", h);
		}

		HashMap<String, List<String>> headers = new HashMap<String, List<String>>(connection.getRequestProperties());
		connection.setDoOutput(true);
		connection.setFixedLengthStreamingMode(input.length());

		FileInputStream strm = new FileInputStream(input);
		try {
			strm.transferTo(connection.getOutputStream());
		} catch (Exception ex) {
		}
		strm.close();

		connection.connect();
		int status = connection.getResponseCode();

		if (status == 201 || status == 204 || status == 200) {
			return;
		} else {
			String header = connection.getHeaderField("WWW-Authenticate");
			List<String> cookieHeader = connection.getHeaderFields().get("Set-Cookie");
			if (cookieHeader != null) {
				cookieHeader.forEach((entry) -> {
					String key = entry.substring(0, entry.indexOf("="));
					String value = entry.substring(entry.indexOf("=") + 1);
					if (value.contains("; ")) {
						value = value.substring(0, value.indexOf("; "));
					}
					try {
						cookies.put(URLDecoder.decode(key, "UTF-8"), URLDecoder.decode(value, "UTF-8"));
					} catch (UnsupportedEncodingException e) {
					}
				});
			}
			if (status > 300 && status < 400) {
				String auth = null;
				if (authorization != null) {
					auth = authorization;
				} else {
					String h = null;
					for (HttpCredential cred : credential) {
						h = cred.provideHeaderEarly();
						if (h != null)
							break;
					}
					if (h != null)
						auth = h;
				}
				String location = connection.getHeaderField("Location");
				if (location.startsWith("/")) {
					location = dest.getProtocol() + "://" + dest.getHost()
							+ (dest.getPort() != -1 ? ":" + dest.getPort() : "") + location;
				} else if (!location.matches("^.*://")) {
					String file = dest.getFile();
					if (file.contains("?"))
						file = file.substring(file.indexOf("?"));
					if (file.contains("/"))
						file = file.substring(0, file.lastIndexOf("/"));
					location = dest.getProtocol() + "://" + dest.getHost()
							+ (dest.getPort() != -1 ? ":" + dest.getPort() : "") + file + "/" + location;
				} else {
					if (headers.containsKey("Authorization")) {
						headers.remove("Authorization");
					}
					auth = null;
				}
				dest = new URL(location);
				connection = (HttpURLConnection) dest.openConnection();
				for (String k : headers.keySet()) {
					List<String> v = headers.get(k);
					if (k != null) {
						for (String e : v) {
							connection.setRequestProperty(k, e);
						}
					}
				}

				publishInternal(input, dest, connection, credential, cookies, auth);
			} else if (status == 401 && header != null) {
				connection = (HttpURLConnection) dest.openConnection();
				for (String k : headers.keySet()) {
					List<String> v = headers.get(k);
					if (k != null) {
						for (String e : v) {
							connection.setRequestProperty(k, e);
						}
					}
				}

				String realm = "";
				String type = header;
				if (header.contains(" "))
					type = type.substring(0, type.indexOf(" "));
				if (header.contains(" realm=")) {
					realm = header.substring(header.indexOf(" realm=") + " realm=".length());
				}

				String cred = null;
				boolean found = false;
				for (HttpCredential c : credential) {
					if (c.supportsAuthMethod(type) && (!realm.isEmpty() && c.supportsGroup(realm))) {
						cred = c.buildHeader(realm, type);
						found = true;
					}
				}
				if (!found && cred == null)
					throw new IOException("Server requested incompatible authentication method.");
				if (cred == null) {
					throw new IOException(
							"No credentials provided, cannot upload to server, server requires authentication.");
				}

				connection.setRequestProperty("Authorization", cred);
				connection.setUseCaches(false);
				connection.setDoInput(true);
				connection.setDoOutput(true);

				publishInternal(input, dest, connection, credential, cookies, cred);
			} else {
				if (status == 401 || status == 403) {
					throw new IOException("Credentials were not accepted by the server");
				}
				throw new IOException("Unexpected response: " + status + " " + connection.getResponseMessage());
			}
		}
	}
}
