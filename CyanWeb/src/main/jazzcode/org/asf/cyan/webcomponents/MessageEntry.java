package org.asf.cyan.webcomponents;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.asf.jazzcode.components.AbstractWebComponent;
import org.asf.jazzcode.components.FunctionInfo;
import org.asf.jazzcode.components.annotations.Function;

import org.asf.jazzcode.internal.controller.DocumentController;

public class MessageEntry extends AbstractWebComponent {

	@Override
	protected AbstractWebComponent newInstance() {
		return new MessageEntry();
	}

	@Function
	public void installSingleMessage(FunctionInfo function) throws InvocationTargetException, IOException {
		DocumentController controller = DocumentController.getNewController();
		FileInputStream strm = new FileInputStream(
				new File(new File(function.getServerContext().getSourceDirectory(), function.variables.get("path"))
						.getParentFile(), "/webcomponents/MessageEntry.java.html"));
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
