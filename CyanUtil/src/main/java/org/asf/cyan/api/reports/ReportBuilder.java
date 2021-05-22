package org.asf.cyan.api.reports;

import java.util.ArrayList;
import java.util.function.Supplier;

import org.asf.cyan.api.common.CyanComponent;

/**
 * 
 * ReportBuilder, system to create extensive report files.<br/>
 * <b>Warning:</b> this class needs to have an implementation in order to work.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class ReportBuilder extends CyanComponent {
	private static ReportBuilder selectedImplementation;

	public static ReportBuilder getImplementationInstance() {
		return selectedImplementation;
	}

	protected ReportBuilder() {
	}

	protected static void setImplementation(ReportBuilder implementation) {
		debug("Assigning CyanUtil ReportBuilder Implementation... Using the " + implementation.getImplementationName()
				+ " Implementation...");
		selectedImplementation = implementation;
	}

	public static ReportBuilder create(String head) {
		return selectedImplementation.getNewInstance(head);
	}

	protected abstract String getImplementationName();

	protected abstract ReportBuilder getNewInstance(String head);

	protected abstract void buildHeadString(StringBuilder builder);

	protected abstract void buildNodeHead(StringBuilder builder, ReportNode node);

	protected abstract void buildCategoryHead(StringBuilder builder, String name, int longestNameLength);

	protected abstract void buildEntry(StringBuilder builder, ReportEntry<?> name, int longestNameLength);

	public abstract String getHead();

	public abstract void setHead(String head);

	public abstract ReportCategory[] getCategories();

	public abstract void append(ReportCategory category);

	public abstract void remove(ReportCategory category);

	public abstract ReportCategory newCategory(String name);

	public abstract ReportCategory newCategory(String name, ArrayList<ReportNode> nodes);

	public abstract ReportNode newNode(ReportCategory category, String name);

	public abstract ReportNode newNode(ReportCategory category, String name, ReportEntryList entries);

	public abstract ReportNode appendNode(ReportCategory category, ReportNode node);

	public abstract ReportEntry<Integer> newEntry(String name, int value);

	public abstract ReportEntry<Boolean> newEntry(String name, boolean value);

	public abstract ReportEntry<Float> newEntry(String name, float value);

	public abstract ReportEntry<Double> newEntry(String name, double value);

	public abstract ReportEntry<Byte> newEntry(String name, byte value);

	public abstract ReportEntry<Character> newEntry(String name, char value);

	public abstract ReportEntry<String> newEntry(String name, String value);

	public abstract ReportEntry<Object> newEntry(String name, Object value);

	public abstract ReportEntry<?> newEntry(String name, Supplier<?> value);

	public ReportNode[] appendNode(ReportCategory category, ReportNode... nodes) {
		for (ReportNode n : nodes) {
			appendNode(category, n);
		}
		return nodes;
	}

	@SuppressWarnings("unchecked")
	public ReportEntry<Integer>[] newEntry(ReportNode node, int... values) {
		ArrayList<ReportEntry<Integer>> nodes = new ArrayList<ReportEntry<Integer>>();

		for (int value : values) {
			nodes.add(newEntry(node, null, value));
		}

		return (ReportEntry<Integer>[]) nodes.toArray(t -> new ReportEntry<?>[t]);
	}

	@SuppressWarnings("unchecked")
	public ReportEntry<Boolean>[] newEntry(ReportNode node, boolean... values) {
		ArrayList<ReportEntry<Boolean>> nodes = new ArrayList<ReportEntry<Boolean>>();

		for (Boolean value : values) {
			nodes.add(newEntry(node, null, value.booleanValue()));
		}

		return (ReportEntry<Boolean>[]) nodes.toArray(t -> new ReportEntry<?>[t]);
	}

	@SuppressWarnings("unchecked")
	public ReportEntry<Float>[] newEntry(ReportNode node, float... values) {
		ArrayList<ReportEntry<Float>> nodes = new ArrayList<ReportEntry<Float>>();

		for (Float value : values) {
			nodes.add(newEntry(node, null, value.floatValue()));
		}

		return (ReportEntry<Float>[]) nodes.toArray(t -> new ReportEntry<?>[t]);
	}

	@SuppressWarnings("unchecked")
	public ReportEntry<Double>[] newEntry(ReportNode node, double... values) {
		ArrayList<ReportEntry<Double>> nodes = new ArrayList<ReportEntry<Double>>();

		for (Double value : values) {
			nodes.add(newEntry(node, null, value.doubleValue()));
		}

		return (ReportEntry<Double>[]) nodes.toArray(t -> new ReportEntry<?>[t]);
	}

	@SuppressWarnings("unchecked")
	public ReportEntry<Byte>[] newEntry(ReportNode node, byte... values) {
		ArrayList<ReportEntry<Byte>> nodes = new ArrayList<ReportEntry<Byte>>();

		for (Byte value : values) {
			nodes.add(newEntry(node, null, value.byteValue()));
		}

		return (ReportEntry<Byte>[]) nodes.toArray(t -> new ReportEntry<?>[t]);
	}

	@SuppressWarnings("unchecked")
	public ReportEntry<Character>[] newEntry(ReportNode node, char... values) {
		ArrayList<ReportEntry<Character>> nodes = new ArrayList<ReportEntry<Character>>();

		for (Character value : values) {
			nodes.add(newEntry(node, null, value.charValue()));
		}

		return (ReportEntry<Character>[]) nodes.toArray(t -> new ReportEntry<?>[t]);
	}

	@SuppressWarnings("unchecked")
	public ReportEntry<String>[] newEntry(ReportNode node, String... values) {
		ArrayList<ReportEntry<String>> nodes = new ArrayList<ReportEntry<String>>();

		for (String value : values) {
			nodes.add(newEntry(node, null, value));
		}

		return (ReportEntry<String>[]) nodes.toArray(t -> new ReportEntry<?>[t]);
	}

	public ReportEntry<Integer> newEntry(ReportNode node, String name, int value) {
		ReportEntry<Integer> rEntry = newEntry(name, value);
		node.add(rEntry);
		return rEntry;
	}

	public ReportEntry<Boolean> newEntry(ReportNode node, String name, boolean value) {
		ReportEntry<Boolean> rEntry = newEntry(name, value);
		node.add(rEntry);
		return rEntry;
	}

	public ReportEntry<Float> newEntry(ReportNode node, String name, float value) {
		ReportEntry<Float> rEntry = newEntry(name, value);
		node.add(rEntry);
		return rEntry;
	}

	public ReportEntry<Double> newEntry(ReportNode node, String name, double value) {
		ReportEntry<Double> rEntry = newEntry(name, value);
		node.add(rEntry);
		return rEntry;
	}

	public ReportEntry<Byte> newEntry(ReportNode node, String name, byte value) {
		ReportEntry<Byte> rEntry = newEntry(name, value);
		node.add(rEntry);
		return rEntry;
	}

	public ReportEntry<Character> newEntry(ReportNode node, String name, char value) {
		ReportEntry<Character> rEntry = newEntry(name, value);
		node.add(rEntry);
		return rEntry;
	}

	public ReportEntry<String> newEntry(ReportNode node, String name, String value) {
		ReportEntry<String> rEntry = newEntry(name, value);
		node.add(rEntry);
		return rEntry;
	}

	public ReportEntry<Object> newEntry(ReportNode node, String name, Object value) {
		ReportEntry<Object> rEntry = newEntry(name, value);
		node.add(rEntry);
		return rEntry;
	}

	public void build(StringBuilder builder) {
		buildHeadString(builder);
		builder.append("\n");

		builder.append("\n");
		int longest = 0;
		for (ReportCategory category : getCategories()) {
			String name = category.name;
			if (name.length() > longest) {
				longest = name.length();
			}
		}

		boolean first = true;
		for (ReportCategory category : getCategories()) {
			String name = category.name;
			if (!first) {
				builder.append("\n");
			} else
				first = false;

			if (name != null) {
				buildCategoryHead(builder, name, longest);
				builder.append("\n");
			}

			int longestName = 0;

			for (ReportNode node : category.nodes) {
				if (node.name != null) {
					for (ReportEntry<?> ent : node.entries) {
						if (ent.key != null && ent.key.length() > longestName)
							longestName = ent.key.length();
					}
				}
			}

			for (ReportNode node : category.nodes) {
				buildNodeHead(builder, node);
				builder.append("\n");
				for (ReportEntry<?> ent : node.entries) {
					buildEntry(builder, ent, longestName);
				}
				builder.append("\n");
			}
		}
	}
}
