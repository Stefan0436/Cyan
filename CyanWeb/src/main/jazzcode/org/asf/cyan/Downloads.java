package org.asf.cyan;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;

import org.asf.cyan.backends.downloads.DownloadsBackend;
import org.asf.jazzcode.components.AbstractWebComponent;
import org.asf.jazzcode.components.FunctionInfo;
import org.asf.jazzcode.components.annotations.Function;
import org.asf.jazzcode.components.annotations.Referenceable;
import org.asf.jazzcode.util.ServiceManager;

public class Downloads extends AbstractWebComponent {

	private DownloadsBackend backend;

	@Override
	protected AbstractWebComponent newInstance() {
		return new Downloads();
	}

	@Function
	@Referenceable
	public void pullDownloads(FunctionInfo function) throws InvocationTargetException, IOException {
		if (!DownloadsBackend.isReady())
			return;

		try {
			backend = ServiceManager.getDefault().getSyntheticService(DownloadsBackend.class,
					getGenericServiceInterface());
		} catch (IOException e) {
		}

		function.getResponse().status = 200;
		function.getResponse().message = "OK";
		backend.setupWizard(function);
	}

	@Function
	public void init(FunctionInfo function) throws UnsupportedEncodingException {
		boolean ready = true;
		if (!DownloadsBackend.isReady()) {
			ready = false;
		}
		try {
			backend = ServiceManager.getDefault().getSyntheticService(DownloadsBackend.class,
					getGenericServiceInterface());
		} catch (IOException e) {
		}
		if (ready) {
			function.variables.put("http.path",
					URLEncoder.encode(URLEncoder.encode(getRequest().path, "UTF-8"), "UTF-8"));
			function.variables.put("menuentry", backend.getFirstType(function));
		} else {
			function.writeLine("<script>");
			function.writeLine("\tfunction checkBackend() {");
			function.writeLine("\t\t$.ajax({");
			function.writeLine("\t\t\ttype: \"GET\",");
			function.writeLine("\t\t\turl: \"jc:checkBackend()\",");
			function.writeLine("\t\t\tsuccess: function(result) {");
			function.writeLine("\t\t\t\tnav('Downloads');");
			function.writeLine("\t\t\t}");
			function.writeLine("\t\t});");
			function.writeLine("\t}");
			function.writeLine("\t");
			function.writeLine("\t$(document).ready(function() {");
			function.writeLine("\t\tsetInterval(checkBackend, 2000);");
			function.writeLine("\t});");
			function.writeLine("</script>");
			function.writeLine("");
			function.writeLine("<br /><center>Please wait, our downloads backend is still starting up...</center>");
		}
	}

}
