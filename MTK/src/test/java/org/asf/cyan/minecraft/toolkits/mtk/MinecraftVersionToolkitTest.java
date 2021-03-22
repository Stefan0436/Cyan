package org.asf.cyan.minecraft.toolkits.mtk;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;

import org.asf.cyan.core.CyanCore;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;

public class MinecraftVersionToolkitTest {
	
	public MinecraftVersionToolkitTest() {		
		if (!CyanCore.isInitialized()) {
			CyanCore.simpleInit();
			MinecraftToolkit.initializeMTK();
			CyanCore.initializeComponents();
		}
	}

	@Test
	public void getLatestReleaseVersion() throws IOException {
		// Download all version information objects
		MinecraftToolkit.resolveVersions();
		
		// Parse it manually
		MinecraftVersionInfo info = MinecraftVersionToolkit.getLatestReleaseVersion();
		URL u = new URL(MinecraftToolkit.version_manifest_url);
		InputStreamReader reader = new InputStreamReader(u.openStream());
		JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
		reader.close();
		Gson gson = new Gson();
		@SuppressWarnings("unchecked")
		Map<String, String> latest = gson.fromJson(json.get("latest"), Map.class);
		String v = latest.get("release");
		
		// Test if the version information matches
		assertTrue(info.getVersion().equals(v));
	}


	@Test
	public void getLatestSnapshotVersion() throws IOException {
		// Download all version information objects
		MinecraftToolkit.resolveVersions();
		
		// Parse it manually
		MinecraftVersionInfo info = MinecraftVersionToolkit.getLatestSnapshotVersion();
		URL u = new URL(MinecraftToolkit.version_manifest_url);
		InputStreamReader reader = new InputStreamReader(u.openStream());
		JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
		reader.close();
		Gson gson = new Gson();
		@SuppressWarnings("unchecked")
		Map<String, String> latest = gson.fromJson(json.get("latest"), Map.class);
		String v = latest.get("snapshot");
		
		// Test if the version information matches
		assertTrue(info.getVersion().equals(v));
	}
}
