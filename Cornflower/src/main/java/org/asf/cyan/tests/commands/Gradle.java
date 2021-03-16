package org.asf.cyan.tests.commands;

import java.lang.reflect.InvocationTargetException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.asf.cyan.tests.InteractiveTestCommand;
import org.gradle.wrapper.GradleWrapperMain;

public class Gradle extends InteractiveTestCommand {
	private static void forbidSystemExitCall() {
		final SecurityManager securityManager = new SecurityManager() {
			public void checkPermission(Permission permission) {
				if (permission.getName().startsWith("exitVM.")) {
					throw (new SecurityException("Exit denied"));
				}
			}
		};
		System.setSecurityManager(securityManager);
	}

	private static void enableSystemExitCall() {
		System.setSecurityManager(null);
	}

	@Override
	public String getId() {
		return "gradle";
	}

	@Override
	public List<String> getAliases() {
		return Arrays.asList("gradlew", "./gradlew");
	}

	@Override
	public String helpSyntax() {
		return "[gradle arguments]";
	}

	@Override
	public String helpDescription() {
		return "run gradle";
	}

	@Override
	protected Boolean execute(String[] arguments) throws Exception {
		forbidSystemExitCall();
		ArrayList<String> strs = new ArrayList<String>(Arrays.asList(arguments));
		strs.add("--no-daemon");
		if (!strs.contains("--debug"))strs.add("--info");
		try {
			// Gradle wrapper exit is overridden by the agent, so it won't send the annoying
			// stack trace to the console
			GradleWrapperMain.main(strs.toArray(new String[0]));
		} catch (Exception ex) {
			if (!CheckException(ex)) {
				enableSystemExitCall();
				throw ex;
			}
		}
		enableSystemExitCall();
		return true;
	}

	boolean CheckException(Throwable ex) {
		if (ex instanceof SecurityException && ex.getMessage() == "Exit denied") {
			return true;
		} else if (ex instanceof InvocationTargetException) {
			InvocationTargetException ex2 = (InvocationTargetException) ex;
			return CheckException(ex2.getTargetException());
		} else
			return false;
	}
}
