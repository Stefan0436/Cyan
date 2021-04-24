package org.asf.cyan;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.asf.cyan.backends.NewsBackend;
import org.asf.cyan.webcomponents.Menubar;
import org.asf.jazzcode.components.AbstractWebComponent;
import org.asf.jazzcode.components.FunctionInfo;
import org.asf.jazzcode.components.annotations.Function;
import org.asf.jazzcode.components.annotations.Referenceable;
import org.asf.jazzcode.util.QueryUtil;
import org.asf.jazzcode.util.ServiceManager;

public class News extends AbstractWebComponent {

	private NewsBackend backend;

	@Override
	protected AbstractWebComponent newInstance() {
		return new News();
	}

	@Function
	@Referenceable
	public void checkBackend(FunctionInfo function) {
		if (!NewsBackend.isReady())
			function.getResponse().status = 500;
	}

	@Function
	public void init(FunctionInfo function) throws InvocationTargetException, IOException {
		boolean ready = true;
		if (!NewsBackend.isReady()) {
			ready = false;
		}
		try {
			backend = ServiceManager.getDefault().getSyntheticService(NewsBackend.class, getGenericServiceInterface());
		} catch (IOException e) {
		}
		Map<String, String> query = QueryUtil.parseQuery(function.getRequest().query);
		if (!query.containsKey("viewsingle")) {
			function.getComponent(Menubar.class).installMenubar(function);
		}
		if (ready) {
			backend.getNews(function);
		} else {
			function.writeLine("<script>");
			function.writeLine("\tfunction checkBackend() {");
			function.writeLine("\t\t$.ajax({");
			function.writeLine("\t\t\ttype: \"GET\",");
			function.writeLine("\t\t\turl: \"jc:checkBackend()\",");
			function.writeLine("\t\t\tsuccess: function(result) {");
			function.writeLine("\t\t\t\tlocation.reload();");
			function.writeLine("\t\t\t}");
			function.writeLine("\t\t});");
			function.writeLine("\t}");
			function.writeLine("\t");
			function.writeLine("\t$(document).ready(function() {");
			function.writeLine("\t\tsetInterval(checkBackend, 2000);");
			function.writeLine("\t});");
			function.writeLine("</script>");
			function.writeLine("");
			function.writeLine("<br /><center>Please wait, our news backend is still starting up...</center>");
		}
	}

}
