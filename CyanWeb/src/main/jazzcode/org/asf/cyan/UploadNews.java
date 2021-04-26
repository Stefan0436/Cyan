package org.asf.cyan;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.Date;
import java.util.Map;

import org.asf.connective.usermanager.UserManagerModule;
import org.asf.connective.usermanager.api.AuthResult;
import org.asf.connective.usermanager.api.IAuthFrontend;
import org.asf.cyan.api.packet.PacketBuilder;
import org.asf.cyan.backends.MultipartFormdata;
import org.asf.cyan.backends.news.NewsBackend;
import org.asf.cyan.backends.news.NewsMessage;
import org.asf.cyan.webcomponents.Menubar;
import org.asf.jazzcode.components.AbstractWebComponent;
import org.asf.jazzcode.components.FunctionInfo;
import org.asf.jazzcode.components.annotations.Function;
import org.asf.jazzcode.components.annotations.Referenceable;
import org.asf.jazzcode.cookies.Cookie;
import org.asf.jazzcode.cookies.Cookie.CookieOption;
import org.asf.jazzcode.util.QueryUtil;
import org.asf.jazzcode.util.ServiceManager;
import org.asf.rats.Memory;

public class UploadNews extends AbstractWebComponent {

	private NewsBackend backend;

	@Override
	protected AbstractWebComponent newInstance() {
		return new UploadNews();
	}

	@Function
	@Referenceable
	public void upload(FunctionInfo function) throws IOException {
		boolean ready = true;
		if (!NewsBackend.isReady()) {
			ready = false;
		}
		try {
			backend = ServiceManager.getDefault().getSyntheticService(NewsBackend.class, getGenericServiceInterface());
		} catch (IOException e) {
		}

		if (ready) {
			String group = backend.getUploadGroup(function);
			IAuthFrontend frontend = Memory.getInstance().get("usermanager.auth.frontend")
					.getValue(IAuthFrontend.class);
			if (frontend.check(group, getRequest(), getResponse())) {
				AuthResult user = frontend.authenticate(group, getRequest(), getResponse());
				MultipartFormdata form = MultipartFormdata.getFirst(getRequest());

				NewsMessage message = new NewsMessage();
				message.author = user.getUsername();

				File image = null;
				while (form != null) {
					String name = form.getProp("name");
					if (name.equals("title")) {
						message.title = form.getContentString();
						if (message.title.isEmpty())
							message.title = null;
					} else if (name.equals("message")) {
						message.message = form.getContentString().replace("\r", "");
						if (message.message.isEmpty())
							message.message = null;
					} else if (name.equals("author")) {
						message.author = form.getContentString().replace("\r", "");
						if (message.author.isEmpty())
							message.author = user.getUsername();
					} else if (name.equals("image")) {
						image = File.createTempFile("img.author.cyan-", ".jpg");
						image.deleteOnExit();

						FileOutputStream strm = new FileOutputStream(image);
						form.transfer(strm);
						strm.close();
						if (image.length() == 0) {
							image.delete();
							image = null;
						}
					}
					form = form.getNext();
				}
				if (message.title != null && message.message != null) {
					if (image != null) {
						File img = new File(function.getServerContext().getSourceDirectory(),
								"news-cyan/imgs/author." + user.getUsername() + ".jpg");
						if (!img.getParentFile().exists())
							img.getParentFile().mkdirs();
						if (img.exists())
							img.delete();
						Files.move(image.toPath(), img.toPath());
					}

					File tmpOut = File.createTempFile("newsmsg-", ".cn");
					tmpOut.deleteOnExit();

					PacketBuilder builder = new PacketBuilder();
					builder.add(message.author);
					builder.add(message.title);
					builder.add(user.getUsername());
					builder.add(message.message);
					builder.add(getResponse().getHttpDate(new Date()));

					FileOutputStream strm = new FileOutputStream(tmpOut);
					builder.build(strm);
					strm.close();

					function.getResponse().status = 302;
					function.getResponse().message = "File found";

					String path = URLDecoder.decode(function.parameters[0], "UTF-8");
					function.getResponse().headers.put("Location",
							path + "?preview=true&file=" + URLEncoder.encode(tmpOut.getCanonicalPath(), "UTF-8"));
				} else {
					function.writeLine("<script>alert('Missing required fields.'); window.history.back();</script>");
					function.getResponse().status = 400;
					function.getResponse().message = "Bad request";
				}
				if (image != null && image.exists()) {
					image.delete();
				}

				return;
			}
		}

		MultipartFormdata form = MultipartFormdata.getFirst(getRequest());
		while (form != null) {
			form = form.getNext();
		}
	}

	@Function
	@Referenceable
	public void logout(FunctionInfo function) throws IOException {
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
			if (frontend.check(group, getRequest(), getResponse())) {
				getResponse().status = 302;
				getResponse().message = "File found";

				String path = URLDecoder.decode(function.parameters[0], "UTF-8");
				getCookies().set("session",
						Cookie.create("session").setValue("logout").setOption(CookieOption.PATH, "/"));
				getResponse().headers.put("Location", path);
			}
		}
	}

	@Function
	public void initPre(FunctionInfo function) throws InvocationTargetException, IOException {
		if (function.getRequest().query.equals("reload=true")) {
			function.writeLine("<script>parent.location.reload();</script>");
			return;
		}
		function.getComponent(Menubar.class).installMenubar(function);
	}

	@Function
	public void init(FunctionInfo function) throws IOException, InvocationTargetException {
		if (function.getRequest().query.equals("reload=true")) {
			return;
		}

		function.variables.put("http.location",
				URLEncoder.encode(URLEncoder.encode(function.getRequest().path, "UTF-8"), "UTF-8"));
		Map<String, String> query = QueryUtil.parseQuery(getRequest().query);
		if (query.containsKey("preview") && query.get("preview").equals("true")) {
			if (query.containsKey("file")) {
				function.getResponse().status = 302;
				function.getResponse().message = "File found";
				function.getResponse().headers.put("Location",
						"UploadPreview.java.html&file=" + URLEncoder.encode(query.get("file"), "UTF-8")
								+ "&publishpage=" + URLEncoder.encode(function.getRequest().path, "UTF-8"));
				return;
			}
		}
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
				if (user.success()) {
					function.variables.put("username", user.getUsername());
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
