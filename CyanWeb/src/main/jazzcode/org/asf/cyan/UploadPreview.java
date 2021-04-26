package org.asf.cyan;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.asf.connective.usermanager.UserManagerModule;
import org.asf.connective.usermanager.api.AuthResult;
import org.asf.connective.usermanager.api.IAuthFrontend;
import org.asf.cyan.backends.news.NewsBackend;
import org.asf.cyan.backends.news.NewsMessage;
import org.asf.jazzcode.components.AbstractWebComponent;
import org.asf.jazzcode.components.FunctionInfo;
import org.asf.jazzcode.components.annotations.Function;
import org.asf.jazzcode.components.annotations.Referenceable;
import org.asf.jazzcode.util.QueryUtil;
import org.asf.jazzcode.util.ServiceManager;
import org.asf.rats.Memory;

public class UploadPreview extends AbstractWebComponent {

	private NewsBackend backend;

	@Override
	protected AbstractWebComponent newInstance() {
		return new UploadPreview();
	}

	@Function
	@Referenceable
	public void back(FunctionInfo function) throws IOException {
		boolean ready = true;
		if (!NewsBackend.isReady()) {
			ready = false;
		}
		try {
			backend = ServiceManager.getDefault().getSyntheticService(NewsBackend.class, getGenericServiceInterface());
		} catch (IOException e) {
		}
		if (ready && function.parameters.length == 1) {
			String group = backend.getUploadGroup(function);
			IAuthFrontend frontend = Memory.getInstance().get("usermanager.auth.frontend")
					.getValue(IAuthFrontend.class);

			if (frontend.check(group, getRequest(), getResponse())
					&& function.namedParameters.containsKey("publishpage")) {
				getResponse().status = 302;
				getResponse().message = "File found";
				getResponse().headers.put("Location",
						URLDecoder.decode(function.namedParameters.get("publishpage"), "UTF-8"));
			}
		}
	}

	@Function
	@Referenceable
	public void confirm(FunctionInfo function) throws IOException {
		boolean ready = true;
		if (!NewsBackend.isReady()) {
			ready = false;
		}
		try {
			backend = ServiceManager.getDefault().getSyntheticService(NewsBackend.class, getGenericServiceInterface());
		} catch (IOException e) {
		}
		if (ready && function.parameters.length == 1) {
			String group = backend.getUploadGroup(function);
			IAuthFrontend frontend = Memory.getInstance().get("usermanager.auth.frontend")
					.getValue(IAuthFrontend.class);

			if (frontend.check(group, getRequest(), getResponse())
					&& function.namedParameters.containsKey("publishpage")
					&& function.namedParameters.containsKey("file")) {
				AuthResult user = frontend.authenticate(backend.getUploadGroup(function), function.getRequest(),
						function.getResponse());

				if (user.success()) {
					File file = new File(URLDecoder.decode(function.namedParameters.get("file"), "UTF-8"));
					if (!file.getParentFile().getCanonicalPath()
							.equals(new File(System.getProperty("java.io.tmpdir")).getCanonicalPath())
							|| !file.exists()) {
						getResponse().setContent("text/plain", "");
						getResponse().status = 302;
						getResponse().message = "File found";
						getResponse().headers.put("Location", function.namedParameters.get("publishpage"));
						return;
					}
					File output = new File(function.getServerContext().getSourceDirectory(),
							"news-cyan/" + System.currentTimeMillis() + ".cn");
					if (!output.getParentFile().exists())
						output.getParentFile().mkdirs();
					Files.move(file.toPath(), output.toPath());
					NewsBackend.refreshNow();

					getResponse().status = 302;
					getResponse().message = "File found";
					getResponse().headers.put("Location",
							URLDecoder.decode(function.namedParameters.get("publishpage"), "UTF-8"));
				} else {
					function.writeLine("<br /><center>Please authenticate</center>");
				}
			}
		}
	}

	@Function
	public void init(FunctionInfo function) throws IOException, InvocationTargetException {
		function.variables.put("http.location",
				URLEncoder.encode(URLEncoder.encode(function.getRequest().path, "UTF-8"), "UTF-8"));
		boolean ready = true;
		if (!NewsBackend.isReady()) {
			ready = false;
		}
		try {
			backend = ServiceManager.getDefault().getSyntheticService(NewsBackend.class, getGenericServiceInterface());
		} catch (IOException e) {
		}
		if (ready) {
			IAuthFrontend frontend = Memory.getInstance().get("usermanager.auth.frontend")
					.getValue(IAuthFrontend.class);
			String group = backend.getUploadGroup(function);

			String url = "/" + function.getContextRoot() + "/" + UserManagerModule.getBase() + "/"
					+ UserManagerModule.getAuthCommand();
			while (url.contains("//"))
				url = url.replace("//", "/");

			if (!frontend.check(group, getRequest(), getResponse())) {
				function.writeLine("<iframe frameBorder=\"0\" style=\"margin: 0px; width: 100%; height: 94%;\" src=\""
						+ url + "?group=" + group + "&target="
						+ URLEncoder.encode(function.getRequest().path + "?reload=true", "UTF-8") + "\" />");
			} else {
				AuthResult user = frontend.authenticate(backend.getUploadGroup(function), function.getRequest(),
						function.getResponse());
				Map<String, String> query = QueryUtil.parseQuery(getRequest().query);
				if (user.success() && query.containsKey("publishpage") && query.containsKey("file")) {
					File file = new File(query.get("file"));
					if (!file.getParentFile().getCanonicalPath()
							.equals(new File(System.getProperty("java.io.tmpdir")).getCanonicalPath())
							|| !file.exists()) {
						getResponse().setContent("text/plain", "");
						getResponse().status = 302;
						getResponse().message = "File found";
						getResponse().headers.put("Location", query.get("publishpage"));
						return;
					}

					NewsMessage msg = NewsMessage.load(file);
					function.variables.put("username", user.getUsername());
					function.variables.put("file",
							URLEncoder.encode(URLEncoder.encode(file.getCanonicalPath(), "UTF-8"), "UTF-8"));
					function.variables.put("publishpage",
							URLEncoder.encode(URLEncoder.encode(query.get("publishpage"), "UTF-8"), "UTF-8"));
					function.variables.put("title", msg.title);
					function.variables.put("author", msg.author);
					function.variables.put("message", msg.message);
					function.variables.put("datecp",
							new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(msg.time));
					function.variables.put("datepretty", new SimpleDateFormat("MM/dd/yyyy HH:mm").format(msg.time));
				} else {
					function.writeLine("<br /><center>Please authenticate</center>");
				}
			}
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
