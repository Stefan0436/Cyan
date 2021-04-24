package org.asf.cyan.backends;

import java.util.Map;

import org.asf.jazzcode.components.FunctionInfo;
import org.asf.jazzcode.components.JWebService;
import org.asf.jazzcode.components.annotations.Function;
import org.asf.jazzcode.util.QueryUtil;

public class NewsBackend extends JWebService {
	
	private static boolean ready = false;
	
	public static boolean isReady() {
		return ready;
	}
	
	@Override
	protected void startService() {
		ready = true;
	}
	
	@Function
	public void getNews(FunctionInfo function) {
		Map<String, String> query = QueryUtil.parseQuery(function.getRequest().query);
	}
	
}
