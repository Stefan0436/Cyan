package org.asf.cyan.loader.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.asf.cyan.api.packet.PacketBuilder;

public class TrustPackage {
	public String name;
	public PackageTrustCollection[] entries = new PackageTrustCollection[0];

	public static TrustPackage importStore(File trustStore) throws IOException {
		TrustPackage store = new TrustPackage();

		FileInputStream strm = new FileInputStream(trustStore);
		

		return store;
	}
	
	public void export(File output) {
		PacketBuilder builder = new PacketBuilder();
		builder.add(name);
		builder.add(entries.length);
		
	}
}
