package org.asf.cyan.modifications._1_15_2.server.paper;

import java.util.function.Supplier;

public class CyanErrorSupplier implements Supplier<IllegalStateException> {

	private String name;

	public CyanErrorSupplier(String name) {
		this.name = name;
	}

	@Override
	public IllegalStateException get() {
		return new IllegalStateException("Unknown Entity Type: " + name);
	}

}
