package org.asf.cyan.backends.news;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.asf.cyan.webcomponents.MessageEntry;
import org.asf.jazzcode.components.FunctionInfo;
import org.asf.jazzcode.components.JWebService;
import org.asf.jazzcode.components.annotations.Function;
import org.asf.rats.Memory;
import org.asf.rats.ModuleBasedConfiguration;

public class NewsBackend extends JWebService {

	private String newsGroup = "";
	private ArrayList<NewsMessage> newsMessages = new ArrayList<NewsMessage>();
	private HashMap<String, String> newsConfiguration;

	private static boolean refreshing = false;
	private static boolean refreshNow = false;
	private static boolean ready = false;
	private boolean refreshed = false;

	public static void refreshNow() {
		refreshNow = true;
	}

	public static boolean isReady() {
		return ready;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void startService() {
		HashMap<String, String> configuration = new HashMap<String, String>();
		configuration.put("news-group", "cyannews");
		newsConfiguration = (HashMap<String, String>) Memory.getInstance().get("memory.modules.shared.config")
				.getValue(ModuleBasedConfiguration.class).modules.getOrDefault("CyanWebNews",
						new HashMap<String, String>());
		configuration.forEach((k, v) -> newsConfiguration.putIfAbsent(k, v));
		Memory.getInstance().get("memory.modules.shared.config").getValue(ModuleBasedConfiguration.class).modules
				.put("newsConfiguration", newsConfiguration);
		try {
			Memory.getInstance().get("memory.modules.shared.config").getValue(ModuleBasedConfiguration.class)
					.writeAll();
		} catch (IOException e) {
		}

		newsGroup = newsConfiguration.get("news-group");
		ready = true;
	}

	@Function
	private void refresh(FunctionInfo function) throws InterruptedException, IOException {
		refreshing = true;
		File newsContainers = new File(function.getServerContext().getSourceDirectory(), "news-cyan");
		if (!newsContainers.exists())
			newsContainers.mkdirs();

		newsMessages.clear();
		for (File container : newsContainers
				.listFiles((file) -> !file.isDirectory() && file.getName().endsWith("cn"))) {
			newsMessages.add(NewsMessage.load(container));
		}

		refreshing = false;
		int i = 0;
		while (!refreshNow) {
			i++;
			Thread.sleep(1);
			if (i == 1000 * 60 * 10)
				break;
		}

		refreshNow = false;
		runFunction("refresh", function.getRequest(), function.getResponse(), function.getPagePath(),
				(str) -> function.write(str), function.variables, (obj) -> {
				}, function.getServer(), function.getContextRoot(), function.getServerContext(), function.getClient(),
				null, null, null);
	}

	@Function
	public void getNews(FunctionInfo function) {
		if (!refreshed) {
			refreshing = true;
			refreshed = true;
			this.runFunction("refresh", function.getRequest(), function.getResponse(), function.getPagePath(),
					(str) -> function.write(str), function.variables, (obj) -> {
					}, function.getServer(), function.getContextRoot(), function.getServerContext(),
					function.getClient(), null, null, null);
		}
		while (refreshing) {
		}

		ArrayList<NewsMessage> messages = new ArrayList<NewsMessage>(newsMessages);
		messages.sort((a, b) -> -a.time.compareTo(b.time));
		int start = Integer.valueOf(function.namedParameters.getOrDefault("viewstart", "0"));
		int end = Integer.valueOf(function.namedParameters.getOrDefault("viewend", "-1"));
		if (end != -1)
			end += start;

		for (int i = start; i < (end == -1 ? messages.size() : end) && i < newsMessages.size(); i++) {
			NewsMessage msg = messages.get(i);
			FunctionInfo func = new FunctionInfo(function);
			func.variables = new HashMap<String, String>();
			func.variables.put("path", function.namedParameters.get("execPath"));
			func.variables.put("title", msg.title);
			func.variables.put("author", msg.author);
			func.variables.put("authorimg", msg.authorImagePath);
			func.variables.put("message", msg.message);
			func.variables.put("datecp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(msg.time));
			func.variables.put("datepretty", new SimpleDateFormat("MM/dd/yyyy HH:mm").format(msg.time));
			try {
				new MessageEntry().installSingleMessage(func);
			} catch (IOException | InvocationTargetException e) {
			}
		}
	}

	@Function
	public String getUploadGroup(FunctionInfo function) {
		if (!refreshed) {
			refreshing = true;
			refreshed = true;
			this.runFunction("refresh", function.getRequest(), function.getResponse(), function.getPagePath(),
					(str) -> function.write(str), function.variables, (obj) -> {
					}, function.getServer(), function.getContextRoot(), function.getServerContext(),
					function.getClient(), null, null, null);
		}
		while (newsGroup.isEmpty()) {
		}
		return newsGroup;
	}

}
