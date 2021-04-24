package org.asf.cyan;

import org.asf.jazzcode.components.AbstractWebComponent;
import org.asf.jazzcode.components.FunctionInfo;
import org.asf.jazzcode.components.annotations.Function;

public class Home extends AbstractWebComponent {

	@Override
	protected AbstractWebComponent newInstance() {
		return new Home();
	}

	@Function
	public void init(FunctionInfo function) {
	}

}
