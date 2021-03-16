package org.asf.cyan.core;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.providers.IModloaderInfoProvider;

public class SimpleModloader extends Modloader {
	
	public SimpleModloader(String name, String simpleName, IModloaderInfoProvider... providers) {
		this.name = name;
		this.simpleName = simpleName;
		for (IModloaderInfoProvider provider : providers) {
			this.addInformationProvider(provider);
		}
	}
	
	private String name = "";
	private String simpleName = "";

	@Override
	protected String getImplementationName() {
		return name;
	}

	@Override
	public String getSimpleName() {
		return simpleName;
	}

	@Override
	public String getName() {
		return name;
	}

}
