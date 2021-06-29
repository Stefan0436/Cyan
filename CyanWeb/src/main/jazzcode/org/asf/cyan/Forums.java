package org.asf.cyan;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;

import org.asf.connective.usermanager.UserManagerModule;
import org.asf.connective.usermanager.api.AuthResult;
import org.asf.connective.usermanager.api.AuthSecureStorage;
import org.asf.connective.usermanager.api.IAuthFrontend;
import org.asf.jazzcode.components.AbstractWebComponent;
import org.asf.jazzcode.components.FunctionInfo;
import org.asf.jazzcode.components.JWebComponent;
import org.asf.jazzcode.components.annotations.Function;
import org.asf.jazzcode.components.annotations.Referenceable;
import org.asf.jazzcode.cookies.Cookie;
import org.asf.jazzcode.cookies.Cookie.CookieOption;
import org.asf.jazzcode.util.MultipartFormdata;
import org.asf.jazzcode.util.QueryUtil;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;
import org.asf.rats.Memory;
import org.asf.rats.http.ProviderContext;

public class Forums extends AbstractWebComponent {

	private AuthResult user = null;
	private IAuthFrontend frontend = Memory.getInstance().get("usermanager.auth.frontend")
			.getValue(IAuthFrontend.class);

	private AuthSecureStorage userData = null;

	public void setup(ConnectiveHTTPServer server, ProviderContext context, String contextRoot, String path,
			HttpRequest request, HttpResponse response, Socket client, ArrayList<JWebComponent> components) {
		super.setup(server, context, contextRoot, path, request, response, client, components);
		try {
			if (frontend.check("cyanforums", getRequest(), getResponse())) {
				user = frontend.authenticate("cyanforums", getRequest(), getResponse());
				userData = user.getUserStorage();
			}
		} catch (IOException e) {
		}
	}

	@Override
	protected AbstractWebComponent newInstance() {
		return new Forums();
	}

	@Function
	public void accountCheckJavaScript(FunctionInfo function) throws IOException {
		Map<String, String> query = QueryUtil.parseQuery(function.getRequest().query);
		if (query.containsKey("logout") && query.get("logout").equals("true"))
			return;

		if (user != null) {
			function.writeLine("account();");
		} else {
			getCookies().set("cyanforums.session",
					Cookie.create("cyanforums.session").setValue("logout").setOption(CookieOption.PATH, "/"));
			function.writeLine("document.getElementById(\"loginFrame\").classList.toggle(\"loginFrameActive\");");
			function.writeLine("document.getElementById(\"loginFrame\").classList.toggle(\"loginFrameInactive\");");
			function.writeLine("document.getElementById(\"cancelLogin\").classList.toggle(\"cancelLoginActive\");");
			function.writeLine("document.getElementById(\"cancelLogin\").classList.toggle(\"cancelLoginInactive\");");

			String url = "/" + function.getContextRoot() + "/" + UserManagerModule.getBase() + "/"
					+ UserManagerModule.getAuthCommand();
			while (url.contains("//"))
				url = url.replace("//", "/");

			function.writeLine("document.getElementById(\"loginFrame\").src = \"" + url + "?group=cyanforums&target="
					+ URLEncoder.encode(function.getRequest().path + "&login=true", "UTF-8") + "\";");
		}
	}

	@Function
	@Referenceable
	public void changePassword(FunctionInfo function) throws IOException {
		MultipartFormdata data = MultipartFormdata.getFirst(getRequest());
		if (data == null)
			return;
		function.getResponse().status = 204;
		if (user != null) {
		}
		while (data != null)
			data = data.getNext();
	}

	@Function
	@Referenceable
	public void changeProfileIcon(FunctionInfo function) throws IOException {
		MultipartFormdata data = MultipartFormdata.getFirst(getRequest());
		if (data == null)
			return;
		if (user != null) {
			ByteArrayOutputStream strm = new ByteArrayOutputStream();
			data.transfer(strm);
			String base64 = new String(Base64.getEncoder().encode(strm.toByteArray()));
			userData.set("avatar", base64);
			userData.write();
		}
		while (data != null)
			data = data.getNext();
		function.getResponse().status = 204;
	}

	@Function
	@Referenceable
	public void pullUserInfo(FunctionInfo function) {
		if (user != null) {
			String nickname = "";
			if (userData.has("forums.nickname", String.class)) {
				nickname = userData.get("forums.nickname");
			} else {
				nickname = user.getUsername();
			}
			String json = "{";
			json += "\"username\":\"" + escapeJson(user.getUsername()) + "\",";
			json += "\"nickname\":\"" + escapeJson(nickname) + "\"";
			json += "}";

			function.getResponse().setContent("text/json", json);
			function.getResponse().status = 200;
			function.getResponse().message = "OK";
		}
	}

	@Function
	public void accountSetupJavaScript(FunctionInfo function) {
		Map<String, String> query = QueryUtil.parseQuery(function.getRequest().query);
		if (query.containsKey("logout") && query.get("logout").equals("true")) {
			String path = query.get("returnurl");
			getResponse().status = 302;
			getResponse().message = "File found";
			getResponse().headers.put("Location", path);
			return;
		}

		if (user != null) {
			String nickname = "";
			if (userData.has("forums.nickname", String.class)) {
				nickname = userData.get("forums.nickname");
			} else {
				nickname = user.getUsername();
			}
			function.writeLine("document.getElementById('nickname').innerHTML = \"" + escape(nickname) + "\";");
			function.writeLine("document.getElementById('nicknameBox').value = \"" + escape(nickname) + "\";");
			function.writeLine(
					"document.getElementById('usernameBox').value = \"" + escape(user.getUsername()) + "\";");

			if (userData.has("avatar", String.class)) {
				function.writeLine("$(\"#account-image\").css(\"background-image\", \"url('data:image/png;base64, "
						+ userData.get("avatar", String.class) + "')\")");
			}
		} else {
			getCookies().set("cyanforums.session",
					Cookie.create("cyanforums.session").setValue("logout").setOption(CookieOption.PATH, "/"));
		}
	}

	private String escape(String input) {
		input = input.replace("\\b", "\\\\b");
		input = input.replace("\"", "\\\"");
		input = input.replace("\\f", "\\\\f");
		input = input.replace("\\n", "\\\\n");
		input = input.replace("\\r", "\\\\r");
		input = input.replace("\\t", "\\\\t");
		input = input.replace("\\v", "\\\\v");
		input = input.replace("\\0", "\\\\0");
		input = input.replace("\\", "\\\\");
		return input;
	}

	private String escapeJson(String input) {
		input = input.replace("\\b", "\\\\b");
		input = input.replace("\"", "\\\"");
		input = input.replace("\\f", "\\\\f");
		input = input.replace("\\n", "\\\\n");
		input = input.replace("\\r", "\\\\r");
		input = input.replace("\\t", "\\\\t");
		input = input.replace("\\", "\\\\");
		return input;
	}

	@Function
	@Referenceable
	public void updateUserInfo(FunctionInfo function) throws IOException {
		if (user != null) {
			if (function.content != null) {
				String newName = function.content.get("username");
				String newNickName = function.content.get("nickname");
				if (!newName.equals(user.getUsername())) {
					user.setNewUsername(newName);
					userData = user.getUserStorage();
				}
				userData.set("forums.nickname", newNickName);
				userData.write();

				String json = "{";
				json += "\"username\":\"" + escapeJson(user.getUsername()) + "\",";
				json += "\"nickname\":\"" + escapeJson(newNickName) + "\",";
				if (userData.has("avatar", String.class))
					json += "\"accountimage\":\"" + escapeJson(userData.get("avatar", String.class)) + "\"";
				json += "}";

				function.getResponse().setContent("text/json", json);
				function.getResponse().status = 200;
				function.getResponse().message = "OK";
			}
		}
	}

	@Function
	public void accountImage(FunctionInfo function) throws IOException {
		Map<String, String> query = QueryUtil.parseQuery(function.getRequest().query);
		if (query.containsKey("logout") && query.get("logout").equals("true"))
			return;

		if (!frontend.check("cyanforums", getRequest(), getResponse())) {
			getCookies().set("cyanforums.session",
					Cookie.create("cyanforums.session").setValue("logout").setOption(CookieOption.PATH, "/"));
			function.write("&#10068;");
		}
	}

	@Function
	public void init(FunctionInfo function) throws IOException {
		Map<String, String> query = QueryUtil.parseQuery(function.getRequest().query);
		if (query.containsKey("login") && query.get("login").equals("true")) {
			function.getResponse().setContent("text/html",
					"<script>parent.navDirect(parent.window.location);</script>");
			return;
		}
		if (query.containsKey("logout") && query.get("logout").equals("true")) {
			getCookies().set("cyanforums.session",
					Cookie.create("cyanforums.session").setValue("logout").setOption(CookieOption.PATH, "/"));
		}
	}

}
