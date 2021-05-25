package org.asf.cyan;

import org.asf.jazzcode.components.AbstractWebComponent;
import org.asf.jazzcode.components.FunctionInfo;
import org.asf.jazzcode.components.annotations.Function;

public class Forums extends AbstractWebComponent {

	@Override
	protected AbstractWebComponent newInstance() {
		return new Forums();
	}

	@Function
	public void init(FunctionInfo function) {
		
	}

}
