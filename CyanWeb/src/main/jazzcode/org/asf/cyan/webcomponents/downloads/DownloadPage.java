package org.asf.cyan.webcomponents.downloads;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;

import org.asf.cyan.backends.downloads.DownloadsBackend;
import org.asf.cyan.backends.downloads.DownloadsBackend.CompileInfo;
import org.asf.jazzcode.components.AbstractWebComponent;
import org.asf.jazzcode.components.FunctionInfo;
import org.asf.jazzcode.components.annotations.Function;
import org.asf.jazzcode.components.annotations.Referenceable;
import org.asf.jazzcode.internal.controller.DocumentController;
import org.asf.jazzcode.util.ServiceManager;

public class DownloadPage extends AbstractWebComponent {

	private DownloadsBackend backend;

	@Override
	protected AbstractWebComponent newInstance() {
		return new DownloadPage();
	}

	@Function
	@Referenceable
	public void getCompileStats(FunctionInfo function) {
		function.getResponse().message = "OK";
		function.getResponse().status = 200;

		function.getResponse().setContent("application/json", "{\"status\":\"Done\", \"log\":\"\"}");

		if (function.parameters.length == 4) {
			String platform = function.parameters[0];
			String repository = function.parameters[1];
			String gameversion = function.parameters[2];
			String modloaderversion = function.parameters[3];
			
			if (!DownloadsBackend.isReady())
				return;

			try {
				backend = ServiceManager.getDefault().getSyntheticService(DownloadsBackend.class,
						getGenericServiceInterface());
			} catch (IOException e) {
			}

			FunctionInfo inter = new FunctionInfo(function).setParams(platform, repository, gameversion, modloaderversion);
			CompileInfo info = backend.getCompilingVersion(inter);
			if (info == null) {
				return;
			}
			
			String json = "{\"status\": \"%s\", \"log\": \"%l\"}";
			json = json.replace("%s", info.status.replace("\\", "\\\\").replace("\r", "").replace("\t", "    ").replace("\n", "\\n").replace("\"", "\\\""));
			json = json.replace("%l", info.outputLog.replace(System.getProperty("user.name"), "****").replace("\t", "    ").replaceAll("\u001B\\[[\\d;]*[^\\d;]","").replace("\\", "\\\\").replace("\r", "").replace("\n", "\\n").replace("\"", "\\\""));
			function.getResponse().setContent("application/json", json);
		}
	}

	@Function
	@Referenceable
	public void startCompiler(FunctionInfo function) {
		if (!DownloadsBackend.isReady() || !function.namedParameters.containsKey("platform")
				|| !function.namedParameters.containsKey("repository")
				|| !function.namedParameters.containsKey("version")
				|| !function.namedParameters.containsKey("modloaderversion"))
			return;

		try {
			backend = ServiceManager.getDefault().getSyntheticService(DownloadsBackend.class,
					getGenericServiceInterface());
		} catch (IOException e) {
		}

		String platform = function.namedParameters.get("platform");
		String repository = function.namedParameters.get("repository");
		String gameversion = function.namedParameters.get("version");
		String modloaderversion = function.namedParameters.get("modloaderversion");

		FunctionInfo inter = new FunctionInfo(function).setParams(platform, repository, gameversion, modloaderversion);
		backend.queueCompilation(inter);
	}

	@Function
	public void init(FunctionInfo function) {
		if (!DownloadsBackend.isReady() || !function.variables.containsKey("platform")
				|| !function.variables.containsKey("repository") || !function.variables.containsKey("version")
				|| !function.variables.containsKey("modloaderversion"))
			return;

		try {
			backend = ServiceManager.getDefault().getSyntheticService(DownloadsBackend.class,
					getGenericServiceInterface());
		} catch (IOException e) {
		}

		String platform = function.variables.get("platform");
		String repository = function.variables.get("repository");
		String gameversion = function.variables.get("version");
		String modloaderversion = function.variables.get("modloaderversion");
		FunctionInfo inter = new FunctionInfo(function).setParams(platform, repository, gameversion, modloaderversion);
		function.variables.put("downloadlink", backend.getCompiledVersionURI(inter));
		function.variables.put("elements", "");

		if (backend.getCompiledVersion(inter) == null) {
			CompileInfo info = backend.getCompilingVersion(inter);
			if (info == null) {
				function.variables.put("compile.status", "Not running");
				function.writeLine(
						"<button onclick=\"window.location.href='/org.asf.cyan.webcomponents.downloads.DownloadPage/jz:startCompiler(platform: "
								+ platform + ", repository: " + repository + ", version: " + gameversion
								+ ", modloaderversion: " + modloaderversion + ")'; goNav('" + repository
								+ ", page: downloads, version: " + gameversion + ", modloaderversion: "
								+ modloaderversion + ", platform: " + platform
								+ "');\" style=\"width: 400px;\" id=\"downloads-btn\">Run compiler</button>");
			} else {
				function.variables.put("compile.status", info.status);
				function.variables.put("elements", "<div id=\"loader\"></div>");
			}
			function.variables.put("style.compiler", "display: block;");
			function.variables.put("style.download", "display: none;");
		} else {
			function.variables.put("compile.status", "Done");
			function.variables.put("style.compiler", "display: none;");
			function.variables.put("style.download", "display: block;");
			if (platform.equals("paper")) {
				function.writeLine("<style>");
				function.writeLine(".clientbtn {");
				function.writeLine("\tdisplay: none;");
				function.writeLine("}");
				function.writeLine("</style>");
			}
		}
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
				"/webcomponents/downloads/DownloadPage.java.html"));
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