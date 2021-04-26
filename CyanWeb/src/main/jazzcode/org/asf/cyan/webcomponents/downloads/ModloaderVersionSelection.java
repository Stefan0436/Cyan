package org.asf.cyan.webcomponents.downloads;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
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
	public void init(FunctionInfo function) {
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

		String button = "\t<button onclick=\"javascript:goNav('${repository}, page: " + target + ", version: "
				+ function.variables.get("version") + ", platform: " + function.variables.get("platform")
				+ "')\" id=\"downloads-btn\">#%v</button>\n";

		String buttonCompiling = "\t<button onclick=\"javascript:goNav('${repository}, page: " + target + ", version: "
				+ function.variables.get("version") + ", platform: " + function.variables.get("platform")
				+ "')\" id=\"downloads-btn-compiling\">#%v</button>\n";

		String buttonNonCompiled = "\t<button onclick=\"javascript:goNav('${repository}, page: " + target
				+ ", version: " + function.variables.get("version") + ", platform: "
				+ function.variables.get("platform") + "')\" id=\"downloads-btn-noncompiled\">#%v</button>\n";

		StringBuilder versionString = new StringBuilder();
		for (String version : new ArrayList<String>(versions)) {
			FunctionInfo inter = new FunctionInfo(function).setParams(function.variables.get("platform"),
					function.variables.get("repository"), function.variables.get("version"), version);

			if (backend.getCompilingVersion(inter) == null && backend.getCompiledVersion(inter) != null) {
				versionString.append(button.replace("%v", version));
				versions.remove(version);
			}
		}
		for (String version : new ArrayList<String>(versions)) {
			FunctionInfo inter = new FunctionInfo(function).setParams(function.variables.get("platform"),
					function.variables.get("repository"), function.variables.get("version"), version);

			if (backend.getCompilingVersion(inter) != null && backend.getCompiledVersion(inter) == null) {
				versionString.append(buttonCompiling.replace("%v", version));
				versions.remove(version);
			}
		}
		for (String version : new ArrayList<String>(versions)) {
			FunctionInfo inter = new FunctionInfo(function).setParams(function.variables.get("platform"),
					function.variables.get("repository"), function.variables.get("version"), version);

			if (backend.getCompilingVersion(inter) == null && backend.getCompiledVersion(inter) == null) {
				versionString.append(buttonNonCompiled.replace("%v", version));
				versions.remove(version);
			}
		}
		function.variables.put("versionbuttons", versionString.toString());
	}

	@Function
	public void install(FunctionInfo function) throws InvocationTargetException, IOException {
		if (function.parameters.length != 1)
			return;

		DocumentController controller = DocumentController.getNewController();
		function.variables.put("repository", function.parameters[0]);
		function.variables.putAll(function.namedParameters);
		FileInputStream strm = new FileInputStream(new File(
				new File(function.getServerContext().getSourceDirectory(),
						URLDecoder.decode(function.namedParameters.get("execPath"), "UTF-8")).getParentFile(),
				"/webcomponents/downloads/ModloaderVersionSelection.java.html"));
		controller.connectServer(function).addDefaultCommands().attachReader(() -> {
			try {
				return strm.read();
			} catch (IOException e) {
				return -1;
			}
		}).attachStringWriter();
		controller.attachResultEvent((output) -> {
			for (String key : function.variables.keySet()) {
				output = output.replace("${" + key + "}", function.variables.get(key));
			}
			return output;
		});
		controller.getProcessor().process();
		function.write(controller.getStringWriterResult());
		strm.close();
	}

}
