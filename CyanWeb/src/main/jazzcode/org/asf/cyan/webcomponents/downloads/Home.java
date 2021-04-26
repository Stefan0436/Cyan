package org.asf.cyan.webcomponents.downloads;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;

import org.asf.jazzcode.components.AbstractWebComponent;
import org.asf.jazzcode.components.FunctionInfo;
import org.asf.jazzcode.components.annotations.Function;
import org.asf.jazzcode.internal.controller.DocumentController;

public class Home extends AbstractWebComponent {

	@Override
	protected AbstractWebComponent newInstance() {
		return new Home();
	}

	@Function
	public void init(FunctionInfo function) {
	}

	@Function
	public void install(FunctionInfo function) throws InvocationTargetException, IOException {
		if (function.parameters.length != 1)
			return;

		DocumentController controller = DocumentController.getNewController();
		function.variables.put("repository", function.parameters[0]);
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
