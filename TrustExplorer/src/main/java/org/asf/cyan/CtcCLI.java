package org.asf.cyan;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

public class CtcCLI {
	static int max = 0;
	private static Object[] credentials = null;
	private static Scanner in = new Scanner(System.in);
	private static URL dest;
	private static String lastGroup = "";

	public static void main(String[] args) throws IOException {
		if (args.length < 3 || (!args[0].equals("pack") && !args[0].equals("unpack") && !args[0].equals("publish"))) {
			System.err.println("Usage: ctc pack/unpack/publish <input> <output>");
			System.exit(-1);
			return;
		}
		if (args[0].equals("unpack")) {
			CtcUtil.unpack(new File(args[1]), new File(args[2]), (num) -> max = num, (num) -> {
				System.out.println("Extracted: " + num + " / " + max);
			});
		} else if (args[0].equals("pack")) {
			CtcUtil.pack(new File(args[1]), new File(args[2]), (num) -> max = num, (num) -> {
				System.out.println("Added: " + num + " / " + max);
			});
		} else if (args[0].equals("publish")) {
			dest = new URL(args[2]);
			CtcUtil.publish(new File(args[1]), dest, (group) -> {
				return getCredentials(group);
			}, (published) -> {
				if (published.startsWith("/"))
					published = published.substring(1);
				if (!args[2].endsWith("/"))
					args[2] = args[2] + "/";
				System.out.println("Published: " + published + " -> " + args[2] + published);
			});
			in.close();
		}
	}

	private static Object[] getCredentials(String group) {
		if (credentials != null && lastGroup.equals(group)) {
			return credentials;
		}

		lastGroup = group;
		System.out.println("Please log in to your " + group + " account at " + dest.getHost() + "...");
		System.out.print("Username: ");
		String username = in.nextLine();
		System.out.print("Password for " + username + "@" + dest.getHost() + ": ");
		char[] password = System.console().readPassword();
		credentials = new Object[] { username, password };
		return credentials;
	}

}
