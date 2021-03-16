package org.asf.cyan.api.reports;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 
 * Report category.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ReportCategory {

	public String name;
	public ArrayList<ReportNode> nodes;

	public ReportCategory(String name) {
		this.name = name;
		this.nodes = new ArrayList<ReportNode>();
	}

	public ReportCategory(String name, ArrayList<ReportNode> nodes) {
		this.name = name;
		this.nodes = nodes;
	}

	public ReportCategory addAll(Collection<ReportNode> c) {
		nodes.addAll(c);
		return this;
	}

	public ReportCategory addAll(int index, Collection<ReportNode> c) {
		nodes.addAll(index, c);
		return this;
	}

	public ReportCategory add(int index, ReportNode entry) {
		nodes.add(index, entry);
		return this;
	}

	public ReportCategory add(ReportNode entry) {
		nodes.add(entry);
		return this;
	}

}
