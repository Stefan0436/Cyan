package org.asf.cyan.loader.eventbus;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.modloader.IModloaderComponent;
import org.asf.cyan.api.modloader.TargetModloader;

@TargetModloader(CyanLoader.class)
public class CyanEventBridge extends CyanComponent implements IModloaderComponent {
	
	/**
	 * @deprecated CyanLoader internal, will deny access from outside CyanLoader.
	 */
	@Deprecated
	public CyanEventBusFactory getNewFactory() {
		String clType = CallTrace.traceCall(1).getTypeName();
		if (clType.equals(CyanLoader.class.getTypeName())) {
			return new CyanEventBusFactory();
		} else 
			throw new IllegalStateException("This is a CyanLoader internal component, access denied!");
	}
	
}
