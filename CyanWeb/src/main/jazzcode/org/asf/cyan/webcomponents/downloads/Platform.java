package org.asf.cyan.webcomponents.downloads;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.asf.jazzcode.components.AbstractWebComponent;
import org.asf.jazzcode.components.FunctionInfo;
import org.asf.jazzcode.components.annotations.Function;
import org.asf.jazzcode.internal.controller.DocumentController;

public class Platform extends AbstractWebComponent {

	@Override
	protected AbstractWebComponent newInstance() {
		return new Platform();
	}

	@Function
	public void init(FunctionInfo function) {
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
		function.variables.putAll(function.namedParameters);
		function.variables.put("repository", function.parameters[0]);
		FileInputStream strm = new FileInputStream(new File(f, "/webcomponents/downloads/Platform.java.html"));
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
