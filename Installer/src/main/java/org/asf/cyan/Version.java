package org.asf.cyan;

import java.math.BigInteger;
import java.util.ArrayList;

/**
 * 
 * Cyan Version API - parses versions and has various features;
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class Version {

	private ArrayList<VersionSegment> segments = new ArrayList<VersionSegment>();

	protected static class VersionSegment {
		public String data = null;
		public BigInteger value = BigInteger.valueOf(-1);

		public int separator = -1;
		public boolean hasSeparator = false;

		@Override
		public String toString() {
			return data + (separator != -1 ? (char) separator : "");
		}
	}

	protected Version() {
	}

	/**
	 * Parses the given string into a version wrapper instance.
	 * 
	 * @param version Version String
	 * @return Version instance.
	 */
	public static Version fromString(String version) {
		Version ver = new Version();
		return ver.parse(version);
	}

	private Version parse(String version) {
		segments.clear();

		boolean lastWasAlpha = false;
		boolean first = true;

		VersionSegment last = new VersionSegment();
		segments.add(last);

		for (char ch : version.toCharArray()) {
			if (!first) {
				if (last.data != null) {
					if ((Character.isAlphabetic(ch) && !lastWasAlpha) || (Character.isDigit(ch) && lastWasAlpha)) {
						if (last.data.matches("^[0-9]+$"))
							last.value = new BigInteger(last.data);

						last.hasSeparator = true;
						if (last.value.equals(BigInteger.valueOf(-1)) && last.data != null && last.data.length() > 0
								&& Character.isAlphabetic(last.data.charAt(0)))
							last.value = BigInteger.valueOf(last.data.charAt(0));

						last = new VersionSegment();
						segments.add(last);
					}
				}
			}

			if (!Character.isDigit(ch) && !Character.isAlphabetic(ch)) {
				if (first) {
					continue;
				}

				if (last.data != null) {
					if (last.data.matches("^[0-9]+$"))
						last.value = new BigInteger(last.data);

					last.separator = ch;
					last.hasSeparator = true;
					last = new VersionSegment();
					segments.add(last);
				}
				continue;
			}

			if (last.data == null) {
				last.data = "";
			}

			if (Character.isAlphabetic(ch) && lastWasAlpha && last.data.length() > 0) {
				if (last.value.equals(BigInteger.valueOf(-1)))
					last.value = BigInteger.valueOf(last.data.charAt(0));
				last.data += ch;
				continue;
			}

			last.data += ch;

			lastWasAlpha = Character.isAlphabetic(ch);
			first = false;
		}

		if (last.data != null && last.data.matches("^[0-9]+$"))
			last.value = new BigInteger(last.data);
		if (last.value.equals(BigInteger.valueOf(-1)) && last.data != null && last.data.length() > 0
				&& Character.isAlphabetic(last.data.charAt(0)))
			last.value = BigInteger.valueOf(last.data.charAt(0));

		return this;
	}

	/**
	 * Compares this version to another
	 * 
	 * @param other Version to compare to
	 * @return 1 if greater, 0 if equal and -1 if less.
	 */
	public int compareTo(Version other) {
		if (isEqualTo(other))
			return 0;
		if (isGreaterThan(other))
			return 1;
		if (isLessThan(other))
			return -1;

		return 0;
	}

	public boolean isEqualTo(Version other) {
		if (other.segments.size() != segments.size())
			return false;

		int i = 0;
		for (VersionSegment segment : segments) {
			VersionSegment otherSegment = other.segments.get(i);
			if (segment.value != otherSegment.value)
				return false;
			i++;
		}

		return true;
	}

	public boolean isGreaterThan(Version other) {
		if (isEqualTo(other))
			return false;
		int i = 0;
		boolean lastWasGreater = false;
		for (VersionSegment segment : segments) {
			if (i >= other.segments.size())
				return true;

			VersionSegment otherSegment = other.segments.get(i);
			if (isSnapshot(otherSegment) && !isSnapshot(segment))
				return true;
			else if (!isSnapshot(segment) && isSnapshot(otherSegment))
				return false;

			if (segment.value.compareTo(otherSegment.value) == -1)
				return lastWasGreater;
			else if (!segment.value.equals(otherSegment.value))
				lastWasGreater = true;
			i++;
		}
		if (i < other.segments.size()) {
			if (lastWasGreater)
				return true;
			if (isSnapshot(other.segments.get(i)))
				return true;
			return false;
		}

		return true;
	}

	private boolean isSnapshot(VersionSegment t) {
		return t.toString().toLowerCase().contains("snapshot") || t.toString().toLowerCase().contains("beta")
				|| t.toString().toLowerCase().contains("alpha") || t.toString().toLowerCase().contains("pre");
	}

	public boolean isLessThan(Version other) {
		if (isEqualTo(other))
			return false;

		boolean lastWasLess = false;
		int i = 0;
		for (VersionSegment segment : segments) {
			if (i >= other.segments.size()) {
				if (isSnapshot(segment) && !other.segments.stream().anyMatch(t -> isSnapshot(t))) {
					break;
				}
				return lastWasLess;
			}

			VersionSegment otherSegment = other.segments.get(i);
			if (isSnapshot(otherSegment) && !isSnapshot(segment))
				return false;
			else if (!isSnapshot(otherSegment) && isSnapshot(segment))
				return true;

			if (segment.value.compareTo(otherSegment.value) == 1)
				return lastWasLess;
			lastWasLess = segment.value.compareTo(otherSegment.value) == -1;

			i++;
		}

		return true;
	}

	public boolean isGreaterOrEqualTo(Version other) {
		int comp = compareTo(other);
		return comp == 1 || comp == 0;
	}

	public boolean isLessOrEqualTo(Version other) {
		int comp = compareTo(other);
		return comp == -1 || comp == 0;
	}

	@Override
	public String toString() {
		String str = "";
		for (VersionSegment segment : segments) {
			str += segment.toString();
		}
		return str;
	}

}
