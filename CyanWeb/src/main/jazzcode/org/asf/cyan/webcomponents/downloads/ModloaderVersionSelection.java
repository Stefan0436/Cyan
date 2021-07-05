package org.asf.cyan.webcomponents.downloads;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.asf.cyan.backends.downloads.DownloadsBackend;
import org.asf.jazzcode.components.AbstractWebComponent;
import org.asf.jazzcode.components.FunctionInfo;
import org.asf.jazzcode.components.annotations.Function;
import org.asf.jazzcode.internal.controller.DocumentController;
import org.asf.jazzcode.util.ServiceManager;

public class ModloaderVersionSelection extends AbstractWebComponent {

	private DownloadsBackend backend;

	@Override
	protected AbstractWebComponent newInstance() {
		return new ModloaderVersionSelection();
	}

	@Function
	public void init(FunctionInfo function) throws UnsupportedEncodingException {
		if (!DownloadsBackend.isReady() || !function.variables.containsKey("platform")
				|| !function.variables.containsKey("repository") || !function.variables.containsKey("version"))
			return;

		try {
			backend = ServiceManager.getDefault().getSyntheticService(DownloadsBackend.class,
					getGenericServiceInterface());
		} catch (IOException e) {
		}

		String target = function.variables.getOrDefault("redirect", "downloads, modloaderversion: %v");
		List<String> versions = backend
				.getModloaderVersions(new FunctionInfo(function).setParams(function.variables.get("version"),
						function.variables.get("repository"), function.variables.get("platform")));

		String backpage = "modloaderversions, platform: " + function.variables.get("platform") + ", version: "
				+ function.variables.get("version") + ", backpage: "
				+ DownloadsBackend.jcEncodeParam(function.variables.get("backpage"));

		String dest = "${repository}, page: " + target
				+ DownloadsBackend.jcEncode(", version: " + function.variables.get("version") + ", platform: "
						+ function.variables.get("platform") + ", backpage: " + DownloadsBackend.jcEncode(backpage));

		String buttonSupported = "\t<button onclick=\"javascript:goNav('" + dest
				+ "')\" id=\"downloads-btn-supported\">#%v</button>\n";
		String buttonDated = "\t<button onclick=\"javascript:goNav('" + dest
				+ "')\" id=\"downloads-btn-dated\">#%v</button>\n";

		StringBuilder versionString = new StringBuilder();
		for (String version : new ArrayList<String>(versions)) {
			FunctionInfo inter = new FunctionInfo(function).setParams(function.variables.get("platform"),
					function.variables.get("repository"), function.variables.get("version"), version);

			if (backend.hasHighSupport(inter)) {
				versionString.append(buttonSupported.replace("%v", version));
				versions.remove(version);
			}
		}
		for (String version : new ArrayList<String>(versions)) {
			versionString.append(buttonDated.replace("%v", version));
			versions.remove(version);
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
				new File(f, "/webcomponents/downloads/ModloaderVersionSelection.java.html"));
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
