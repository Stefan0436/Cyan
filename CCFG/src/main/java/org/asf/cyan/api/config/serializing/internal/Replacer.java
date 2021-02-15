package org.asf.cyan.api.config.serializing.internal;

public class Replacer {
	public static String replaceAllChars(String input, char matcher, char replacement) {
		char[] output = new char[input.length()];
		int length = input.length();
		int ind = 0;
		for (int i = 0; i < length; i++) {
			char ch = input.charAt(i);
			if (ch == matcher)
				output[ind++] = replacement;
			else 
				output[ind++] = ch;
		}
		return String.copyValueOf(output);
	}
	public static String removeChar(String input, char matcher) {
		char[] output = new char[input.length() - (int) input.chars().filter(t -> t == matcher).count()];
		int length = input.length();
		int ind = 0;
		for (int i = 0; i < length; i++) {
			char ch = input.charAt(i);
			if (ch != matcher)
				output[ind++] = ch;
		}
		return String.copyValueOf(output);
	}
}
