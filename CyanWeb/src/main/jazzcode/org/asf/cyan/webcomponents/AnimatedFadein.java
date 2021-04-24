package org.asf.cyan.webcomponents;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.asf.jazzcode.components.AbstractWebComponent;
import org.asf.jazzcode.components.FunctionInfo;
import org.asf.jazzcode.components.annotations.Function;
import org.asf.jazzcode.util.QueryUtil;

import org.asf.jazzcode.internal.controller.DocumentController;

public class AnimatedFadein extends AbstractWebComponent {

	@Override
	protected AbstractWebComponent newInstance() {
		return new AnimatedFadein();
	}

	@Function
	public void installFadein(FunctionInfo function) throws InvocationTargetException, IOException {
		DocumentController controller = DocumentController.getNewController();
		FileInputStream strm = new FileInputStream(new File(
				new File(function.getServerContext().getSourceDirectory(), function.getPagePath()).getParentFile(),
				"/webcomponents/AnimatedFadein.java.html"));
		controller.connectServer(function).addDefaultCommands().attachReader(() -> {
			try {
				return strm.read();
			} catch (IOException e) {
				return -1;
			}
		}).attachWriteCallback((obj) -> function.write(obj.toString())).getProcessor().process();
		strm.close();
	}

	@Function
	public boolean checkQuery(FunctionInfo func) {
		Map<String, String> query = QueryUtil.parseQuery(func.getRequest().query);
		if (func.parameters[2].equals("false"))
			return !(query.containsKey(func.parameters[0])
					&& query.get(func.parameters[0]).equals(func.parameters[1]));
		else
			return query.containsKey(func.parameters[0])
					&& query.get(func.parameters[0]).equals(func.parameters[1]);
	}

}
