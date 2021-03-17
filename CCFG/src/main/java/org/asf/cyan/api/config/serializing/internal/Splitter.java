package org.asf.cyan.api.config.serializing.internal;

import java.util.ArrayList;

public class Splitter {
	public static String[] split(String input, char delim) {
		if (input.isEmpty())
			return new String[0];
		int c = (int) input.chars().filter(t -> t == delim).count();
		String[] output = new String[c + 1];
		StringBuilder cbuilder = new StringBuilder();
		boolean busy = false;
		for (int i = 0; i < output.length; i++)
			output[i] = "";

		int ind = 0;
		for (int chind = 0; chind < input.length(); chind++) {
			char ch = input.charAt(chind);
			if (ch == delim) {
				if (busy) {
					output[ind] = cbuilder.toString();
					cbuilder = new StringBuilder();
				}
				ind++;
				busy = false;
			} else {
				if (!busy)
					busy = true;
				cbuilder.append(ch);
			}
		}
		if (busy) {
			output[ind] = cbuilder.toString();
			cbuilder = new StringBuilder();
		}
		return output;
	}

	public static String[] split(String input, String delim) {
		int c = count(input, delim);
		String[] output = new String[c + 1];
		StringBuilder cbuilder = new StringBuilder();
		boolean busy = false;

		for (int i = 0; i < output.length; i++)
			output[i] = "";

		int ind = 0;
		ArrayList<Character> lastCm = new ArrayList<Character>(); // last close match
		int dIndex = 0; // delimiter index
		int dLength = delim.length();

		for (int chind = 0; chind < input.length(); chind++) {
			char ch = input.charAt(chind);
			if (dIndex < dLength - 1 && ch == delim.charAt(dIndex)) {
				lastCm.add(ch);
				dIndex++;
			} else if (dIndex < dLength && ch == delim.charAt(dIndex)) {
				dIndex = 0;
				if (busy) {
					output[ind] = cbuilder.toString();
					cbuilder = new StringBuilder();
				}
				ind++;
				if (!lastCm.isEmpty()) {
					lastCm.clear();
				}
			} else {
				dIndex = 0;
				if (!lastCm.isEmpty()) {
					int l = lastCm.size();
					for (int i = 0; i < l; i++) {
						if (!busy)
							busy = true;
						cbuilder.append(lastCm.get(i));
					}
					lastCm.clear();
				}
				if (!busy)
					busy = true;

				cbuilder.append(ch);
			}
		}

		if (busy)
			output[ind] = cbuilder.toString();

		return output;
	}

	public static int count(String input, String delim) {
		int dIndex = 0; // delimiter index
		int dLength = delim.length();
		int count = 0;

		for (int chind = 0; chind < input.length(); chind++) {
			char ch = input.charAt(chind);
			if (dIndex < dLength - 1 && ch == delim.charAt(dIndex)) {
				dIndex++;
			} else if (dIndex < dLength && ch == delim.charAt(dIndex)) {
				dIndex = 0;
				count++;
			} else {
				dIndex = 0;
			}
		}

		return count;
	}
}
