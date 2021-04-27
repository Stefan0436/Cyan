package org.asf.cyan.webcomponents.downloads;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;

import org.asf.cyan.backends.downloads.DownloadsBackend;
import org.asf.jazzcode.components.AbstractWebComponent;
import org.asf.jazzcode.components.FunctionInfo;
import org.asf.jazzcode.components.annotations.Function;
import org.asf.jazzcode.internal.controller.DocumentController;
import org.asf.jazzcode.util.ServiceManager;

public class Home extends AbstractWebComponent {

	private DownloadsBackend backend;

	@Override
	protected AbstractWebComponent newInstance() {
		return new Home();
	}

	@Function
	public void init(FunctionInfo function) {
	}

	@Function
	public void select(FunctionInfo function) {
		if (!DownloadsBackend.isReady() || !function.variables.containsKey("repository"))
			return;

		try {
			backend = ServiceManager.getDefault().getSyntheticService(DownloadsBackend.class,
					getGenericServiceInterface());
		} catch (IOException e) {
		}

		String repo = function.variables.get("repository");
		if (repo.equals("lts") || repo.startsWith("lts-")) {
			if (repo.equals("lts") && backend.getCyanLTSVersions(function).size() > 0) {
				String str = backend.getCyanLTSVersions(function).get(0);
				function.variables.put("repository", "lts-" + str);
				repo = function.variables.get("repository");
			}

			String button1 = "<button onclick=\"javascript:goNav('lts-%v, page: home')\" id=\"downloads-btn-noncompiled\" class=\"lts-%v\">%v</button>";
			String button2 = "<button onclick=\"javascript:goNav('lts-%v, page: home')\" id=\"downloads-btn\" class=\"lts-%v\">%v</button>";

			function.writeLine("<br/><a style=\"font-size: 28px;\">Cyan LTS Versions</a><br/>");
			for (String ltsVer : backend.getCyanLTSVersions(function)) {
				if (repo.equals("lts-" + ltsVer))
					function.writeLine(button1.replace("%v", ltsVer));
				else
					function.writeLine(button2.replace("%v", ltsVer));
			}

			function.writeLine("<br/>");
			function.writeLine("<br/>");
		}
	}

	@Function
	public void install(FunctionInfo function) throws InvocationTargetException, IOException {
		if (function.parameters.length != 1)
			return;

		String repo = function.parameters[0];
		DocumentController controller = DocumentController.getNewController();
		function.variables.put("repository", repo);
		FileInputStream strm = new FileInputStream(new File(
				new File(function.getServerContext().getSourceDirectory(),
						URLDecoder.decode(function.namedParameters.get("execPath"), "UTF-8")).getParentFile(),
				"/webcomponents/downloads/Home.java.html"));
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
