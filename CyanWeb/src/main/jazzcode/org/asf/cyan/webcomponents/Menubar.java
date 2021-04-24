package org.asf.cyan.webcomponents;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.asf.jazzcode.components.AbstractWebComponent;
import org.asf.jazzcode.components.FunctionInfo;
import org.asf.jazzcode.components.annotations.Function;

import org.asf.jazzcode.internal.controller.DocumentController;

public class Menubar extends AbstractWebComponent {

	@Override
	protected AbstractWebComponent newInstance() {
		return new Menubar();
	}

	@Function
	public void installMenubar(FunctionInfo function) throws InvocationTargetException, IOException {
		DocumentController controller = DocumentController.getNewController();
		FileInputStream strm = new FileInputStream(new File(
				new File(function.getServerContext().getSourceDirectory(), function.getPagePath()).getParentFile(),
				"/webcomponents/Menubar.java.html"));
		controller.connectServer(function).addDefaultCommands().attachReader(() -> {
			try {
				return strm.read();
			} catch (IOException e) {
				return -1;
			}
		}).attachWriteCallback((obj) -> function.write(obj.toString())).getProcessor().process();
		strm.close();
	}

}
