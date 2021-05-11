package org.asf.cyan;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.util.Map;

import org.asf.cyan.backends.downloads.DownloadsBackend;
import org.asf.jazzcode.components.AbstractWebComponent;
import org.asf.jazzcode.components.FunctionInfo;
import org.asf.jazzcode.components.annotations.Function;
import org.asf.jazzcode.components.annotations.Referenceable;
import org.asf.jazzcode.util.QueryUtil;
import org.asf.jazzcode.util.ServiceManager;

public class Downloads extends AbstractWebComponent {

	private DownloadsBackend backend;

	@Override
	protected AbstractWebComponent newInstance() {
		return new Downloads();
	}

	@Function
	@Referenceable
	public void pullDownloads(FunctionInfo function) throws InvocationTargetException, IOException {
		if (!DownloadsBackend.isReady())
			return;

		try {
			backend = ServiceManager.getDefault().getSyntheticService(DownloadsBackend.class,
					getGenericServiceInterface());
		} catch (IOException e) {
		}

		function.getResponse().status = 200;
		function.getResponse().message = "OK";
		backend.setupWizard(function);
	}

	@Function
	public boolean setToSpecificPage(FunctionInfo function) {
		boolean ready = true;
		if (!DownloadsBackend.isReady()) {
			ready = false;
		}
		try {
			backend = ServiceManager.getDefault().getSyntheticService(DownloadsBackend.class,
					getGenericServiceInterface());
		} catch (IOException e) {
		}
		if (ready) {
			Map<String, String> query = QueryUtil.parseQuery(function.getRequest().query);
			if (query.containsKey("modloader")) {
				String loader = query.get("modloader");
				String repo = backend.getFirstType(function);
				if (query.containsKey("repository") && (query.get("repository").equals("testing")
						|| query.get("repository").equals("latest") || query.get("repository").equals("stable")
						|| query.get("repository").equals("lts") || query.get("repository").startsWith("lts-"))) {
					repo = query.get("repository");
				}
				if (loader.equals("vanilla") || loader.equals("fabric") || loader.equals("forge")
						|| loader.equals("paper")) {
					String gameVersion = null;
					if (query.containsKey("gameversion")) {
						gameVersion = query.get("gameversion");
					}

					if (gameVersion == null) {
						if (loader.equals("vanilla")) {
							function.variables.put("manuallyAssignedPage",
									repo + ", page: gameversion, platform: vanilla, backpage: home");
						} else {
							function.variables.put("manuallyAssignedPage",
									repo + ", page: gameversion, redirect: modloaderversions, platform: " + loader
											+ ", backpage: home");
						}
					} else {
						FunctionInfo inter = new FunctionInfo(function).setParams(loader, repo);

						final String gameFinal = gameVersion;
						if (!backend.getVersions(inter).stream().anyMatch(t -> t.equals(gameFinal)))
							return false;

						String loaderVersion = null;
						if (query.containsKey("loaderversion")) {
							loaderVersion = query.get("loaderversion");
						}

						final String loaderFinal = loaderVersion;
						inter = new FunctionInfo(function).setParams(gameVersion, repo, loader);
						if (loaderVersion != null
								&& !backend.getModloaderVersions(inter).stream().anyMatch(t -> t.equals(loaderFinal))) {
							return false;
						}

						if (loader.equals("vanilla")) {
							function.variables.put("manuallyAssignedPage",
									repo + ", page: downloads, version: " + gameVersion + ", modloaderversion: "
											+ gameVersion + ", platform: vanilla, backpage: home");
						} else {
							if (loaderVersion == null) {
								function.variables.put("manuallyAssignedPage",
										repo + ", page: modloaderversions, version: " + gameVersion + ", platform: "
												+ loader + ", backpage: home");
							} else {
								function.variables.put("manuallyAssignedPage",
										repo + ", page: downloads, version: " + gameVersion + ", modloaderversion: "
												+ loaderVersion + ", platform: " + loader + ", backpage: home");
							}
						}
					}

					return true;
				}
			}
		}

		return false;
	}

	@Function
	public void init(FunctionInfo function) throws UnsupportedEncodingException {
		Map<String, String> query = QueryUtil.parseQuery(function.getRequest().query);
		if (query.containsKey("repository") && (query.get("repository").equals("testing")
				|| query.get("repository").equals("latest") || query.get("repository").equals("stable")
				|| query.get("repository").equals("lts") || query.get("repository").startsWith("lts-"))) {
			function.variables.put("menuentry", query.get("repository"));
		}
		boolean ready = true;
		if (!DownloadsBackend.isReady()) {
			ready = false;
		}
		try {
			backend = ServiceManager.getDefault().getSyntheticService(DownloadsBackend.class,
					getGenericServiceInterface());
		} catch (IOException e) {
		}
		if (ready) {
			if (backend.isDown()) {
				function.writeLine(
						"<br /><center>We're sorry, but our download backend has been stopped for maintenance.<br />Please try again later.</center>");
			} else {
				function.variables.put("http.path",
						URLEncoder.encode(URLEncoder.encode(getRequest().path, "UTF-8"), "UTF-8"));
				function.variables.putIfAbsent("menuentry", backend.getFirstType(function));
			}
		} else {
			function.writeLine("<script>");
			function.writeLine("\tfunction checkBackend() {");
			function.writeLine("\t\t$.ajax({");
			function.writeLine("\t\t\ttype: \"GET\",");
			function.writeLine("\t\t\turl: \"jc:checkBackend()\",");
			function.writeLine("\t\t\tsuccess: function(result) {");
			function.writeLine("\t\t\t\tnav('Downloads');");
			function.writeLine("\t\t\t}");
			function.writeLine("\t\t});");
			function.writeLine("\t}");
			function.writeLine("\t");
			function.writeLine("\t$(document).ready(function() {");
			function.writeLine("\t\tsetInterval(checkBackend, 2000);");
			function.writeLine("\t});");
			function.writeLine("</script>");
			function.writeLine("");
			function.writeLine("<br /><center>Please wait, our downloads backend is still starting up...</center>");
		}
	}

}
