package org.asf.cyan.webcomponents.downloads;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.asf.cyan.backends.downloads.DownloadsBackend;
import org.asf.jazzcode.components.AbstractWebComponent;
import org.asf.jazzcode.components.FunctionInfo;
import org.asf.jazzcode.components.annotations.Function;
import org.asf.jazzcode.internal.controller.DocumentController;
import org.asf.jazzcode.util.ServiceManager;

public class GameVersionSelection extends AbstractWebComponent {

	private DownloadsBackend backend;

	@Override
	protected AbstractWebComponent newInstance() {
		return new GameVersionSelection();
	}

	@Function
	public void init(FunctionInfo function) throws UnsupportedEncodingException {
		if (!DownloadsBackend.isReady() || !function.variables.containsKey("platform")
				|| !function.variables.containsKey("repository"))
			return;

		try {
			backend = ServiceManager.getDefault().getSyntheticService(DownloadsBackend.class,
					getGenericServiceInterface());
		} catch (IOException e) {
		}

		String target = function.variables.getOrDefault("redirect", "downloads, modloaderversion: %v");
		List<String> versions = backend.getVersions(new FunctionInfo(function)
				.setParams(function.variables.get("platform"), function.variables.get("repository")));
		String url = ", page: " + target + ", version: %v, platform: " + function.variables.get("platform")
				+ ", backpage: %b";
		String button = "\t<button onclick=\"javascript:goNav('%u')\" id=\"downloads-btn\">%v</button>\n";

		StringBuilder versionString = new StringBuilder();
		for (String version : versions) {
			String backpage = "gameversion, platform: " + function.variables.get("platform") + ", redirect: " + target
					+ ", backpage: " + DownloadsBackend.jcEncodeParam(function.variables.get("backpage"));
			String btnU = url.replace("%v", version).replace("%b", DownloadsBackend.jcEncode(backpage));
			versionString.append(
					button.replace("%v", version).replace("%u", "${repository}" + DownloadsBackend.jcEncode(btnU)));
		}
		function.variables.put("versionbuttons", versionString.toString());
	}

	@Function
	public void install(FunctionInfo function) throws InvocationTargetException, IOException {
		if (function.parameters.length != 1)
			return;

		File source = new File(function.getServerContext().getSourceDirectory());
		File f = new File(function.getServerContext().getSourceDirectory(), function.namedParameters.get("execPath"))
				.getParentFile();
		if (!f.getCanonicalPath().startsWith(source.getCanonicalPath()))
			return;

		DocumentController controller = DocumentController.getNewController();
		function.variables.put("repository", function.parameters[0]);
		function.variables.putAll(function.namedParameters);
		FileInputStream strm = new FileInputStream(
				new File(f, "/webcomponents/downloads/GameVersionSelection.java.html"));
		controller.connectServer(function).addDefaultCommands().attachReader(() -> {
			try {
				return strm.read();
			} catch (IOException e) {
				return -1;
			}
		}).attachDocumentStringWriter();
		controller.getProcessor().process();
		function.write(controller.getStringWriterResult());
		strm.close();
	}

}
