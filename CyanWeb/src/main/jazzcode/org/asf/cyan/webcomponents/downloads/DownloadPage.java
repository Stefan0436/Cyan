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

public class DownloadPage extends AbstractWebComponent {

	private DownloadsBackend backend;

	@Override
	protected AbstractWebComponent newInstance() {
		return new DownloadPage();
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
