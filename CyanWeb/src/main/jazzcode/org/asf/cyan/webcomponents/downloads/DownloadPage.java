package org.asf.cyan.webcomponents.downloads;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.asf.cyan.backends.downloads.DownloadsBackend;
import org.asf.cyan.backends.downloads.DownloadsBackend.ZipInfo.ZipStatus;
import org.asf.jazzcode.components.AbstractWebComponent;
import org.asf.jazzcode.components.FunctionInfo;
import org.asf.jazzcode.components.annotations.Function;
import org.asf.jazzcode.components.annotations.Referenceable;
import org.asf.jazzcode.internal.controller.DocumentController;
import org.asf.jazzcode.util.ServiceManager;

public class DownloadPage extends AbstractWebComponent {

	private DownloadsBackend backend;

	@Override
	protected AbstractWebComponent newInstance() {
		return new DownloadPage();
	}

	@Function
	@Referenceable
	public void getZipInfo(FunctionInfo function) throws IOException {
		if (!DownloadsBackend.isReady() || !function.namedParameters.containsKey("platform")
				|| !function.namedParameters.containsKey("repository")
				|| !function.namedParameters.containsKey("version"))
			return;

		try {
			backend = ServiceManager.getDefault().getSyntheticService(DownloadsBackend.class,
					getGenericServiceInterface());
		} catch (IOException e) {
		}

		String platform = function.namedParameters.get("platform");
		String repository = function.namedParameters.get("repository");
		String gameVersion = function.namedParameters.get("version");
		String modloaderVersion = function.namedParameters.get("modloader");

		FunctionInfo inter = new FunctionInfo(function).setParams(platform, repository, gameVersion, modloaderVersion);
		String cyanVersion = backend.getCyanVersion(inter);

		String json = "{\"status\": \"%s\", \"coremodkit\": \"%cmk\", \"modkit\": \"%ck\"}";
		File base = backend.getVersionDir(inter);
		json = json.replace("%cmk", new File(base, "modkits/cyan-coremodkit-" + gameVersion + "-" + platform + "-"
				+ modloaderVersion + "-" + cyanVersion + ".zip").exists() + "");
		json = json.replace("%ck", new File(base, "modkits/cyan-modkit-" + gameVersion + "-" + platform + "-"
				+ modloaderVersion + "-" + cyanVersion + ".zip").exists() + "");
		if (backend.getZipStatus(inter) != null)
			json = json.replace("%s", backend.getZipStatus(inter).toString().toLowerCase());
		else
			json = json.replace("%s", "never_started");

		function.getResponse().message = "OK";
		function.getResponse().status = 200;
		function.getResponse().setContent("application/json", json);
	}

	@Function
	@Referenceable
	public void downloadAutoUpdateKit(FunctionInfo function) throws IOException {
		if (!DownloadsBackend.isReady() || !function.namedParameters.containsKey("platform")
				|| !function.namedParameters.containsKey("repository")
				|| !function.namedParameters.containsKey("version"))
			return;

		try {
			backend = ServiceManager.getDefault().getSyntheticService(DownloadsBackend.class,
					getGenericServiceInterface());
		} catch (IOException e) {
		}

		String platform = function.namedParameters.get("platform");
		String repository = function.namedParameters.get("repository");
		String gameVersion = function.namedParameters.get("version");
		String modloaderVersion = function.namedParameters.get("modloader");

		FunctionInfo inter = new FunctionInfo(function).setParams(platform, repository, gameVersion, modloaderVersion);
		String cyanVersion = backend.getCyanVersion(inter);

		File baseDir = backend.getVersionDir(inter);
		if (!baseDir.exists())
			baseDir.mkdirs();
		File startZip = new File(baseDir,
				"cyan-updatekit-" + gameVersion + "-" + platform + "-" + modloaderVersion + "-" + cyanVersion + ".zip");
		File startZipLock = new File(baseDir, "cyan-updatekit-" + gameVersion + "-" + platform + "-" + modloaderVersion
				+ "-" + cyanVersion + ".zip.lck");
		if (!startZip.exists() || startZipLock.exists()) {
			File startZipTmp = new File(baseDir, "cyan-updatekit-" + gameVersion + "-" + platform + "-"
					+ modloaderVersion + "-" + cyanVersion + ".zip.tmpdir");

			if (!startZipLock.exists())
				startZipLock.createNewFile();
			if (startZipTmp.exists())
				backend.deleteDir(startZipTmp);
			startZipTmp.mkdirs();

			File source = new File(function.getServerContext().getSourceDirectory());
			File f = new File(function.getServerContext().getSourceDirectory(),
					function.namedParameters.get("execPath")).getParentFile();
			if (!f.getCanonicalPath().startsWith(source.getCanonicalPath()))
				return;
			f = new File(f, "webcomponents/downloads/util");
			Files.copy(new File(f, "start.config").toPath(), new File(startZipTmp, "start.config").toPath());
			Files.copy(new File(f, "start.sh").toPath(), new File(startZipTmp, "start.sh").toPath());
			Files.copy(new File(f, "updatecheck.sh").toPath(), new File(startZipTmp, "updatecheck.sh").toPath());
			FileInputStream strm = new FileInputStream(new File(f, "versions.info"));
			String str = new String(strm.readAllBytes());
			str = str.replace("%gameversion%", gameVersion);
			str = str.replace("%repository%", repository);
			str = str.replace("%modloader%", platform);
			Files.write(new File(startZipTmp, "versions.info").toPath(), str.getBytes());

			String cyanCore = backend.getManifest(inter).libraryVersions.get("CyanCore");
			URL u = new URL(DownloadsBackend.getMavenRepo() + "/org/asf/cyan/CyanCore/" + cyanCore + "/CyanCore-"
					+ cyanCore + ".jar");

			InputStream inStream = u.openStream();
			FileOutputStream outStream = new FileOutputStream(new File(startZipTmp, "CyanCore.jar"));
			inStream.transferTo(outStream);
			outStream.close();
			inStream.close();

			FileOutputStream zipStream = new FileOutputStream(startZip);
			ZipOutputStream zip = new ZipOutputStream(zipStream);
			zip(startZipTmp, "", zip);
			zip.close();
			zipStream.close();
			startZipLock.delete();
			backend.deleteDir(startZipTmp);
		}

		function.getResponse().setContent("application/zip", new FileInputStream(startZip));
		function.getResponse().status = 200;
		function.getResponse().message = "OK";
		function.getResponse().setHeader("Content-Disposition", "attachment; filename=\"" + startZip.getName() + "\"");
	}

	private void zip(File input, String prefix, ZipOutputStream output) throws IOException {
		if (input.isDirectory()) {
			for (File f : input.listFiles(t -> t.isDirectory())) {
				ZipEntry entry = new ZipEntry(prefix + f.getName() + "/");
				output.putNextEntry(entry);
				output.closeEntry();
			}
			for (File f : input.listFiles(t -> !t.isDirectory()))
				zip(f, prefix, output);
			return;
		}
		ZipEntry entry = new ZipEntry(prefix + input.getName() + (input.isDirectory() ? "/" : ""));
		output.putNextEntry(entry);
		FileInputStream strm = new FileInputStream(input);
		strm.transferTo(output);
		strm.close();
		output.closeEntry();
	}

	@Function
	public void init(FunctionInfo function) {
		if (!DownloadsBackend.isReady() || !function.variables.containsKey("platform")
				|| !function.variables.containsKey("repository") || !function.variables.containsKey("version"))
			return;

		try {
			backend = ServiceManager.getDefault().getSyntheticService(DownloadsBackend.class,
					getGenericServiceInterface());
		} catch (IOException e) {
		}

		String platform = function.variables.get("platform");
		String repository = function.variables.get("repository");
		String gameversion = function.variables.get("version");
		String modloaderversion = function.variables.get("modloaderversion");
		FunctionInfo inter = new FunctionInfo(function).setParams(platform, repository, gameversion, modloaderversion);
		if (!backend.hasHighSupport(inter)) {
			function.writeLine("<style>");
			function.writeLine(".updatekitbtn {");
			function.writeLine("\tdisplay: none;");
			function.writeLine("}");
			function.writeLine("</style>");
		}

		try {
			function.variables.put("execPathEncoded",
					DownloadsBackend.jcEncode(function.variables.getOrDefault("execPath", "")));
		} catch (UnsupportedEncodingException e) {
		}
		function.variables.put("filedir", backend.getVersionURI(inter));
		function.variables.put("installerlink", backend.createInstallerDownloadURL(inter));
		function.variables.put("elements", "");
		function.variables.put("cyanversion", backend.getCyanVersion(inter));

		if (platform.equals("vanilla")) {
			modloaderversion = gameversion;
			function.variables.put("modloaderversion", gameversion);
			function.variables.put("targetpage", "gameversion");
		} else
			function.variables.put("targetpage", "modloaderversions");

		function.variables.put("style.download", "display: block;");
		String cyanVersion = backend.getCyanVersion(inter);

		File baseDir = backend.getVersionDir(inter);
		File coremodkit = new File(baseDir, "modkits/cyan-coremodkit-" + gameversion + "-" + platform + "-"
				+ modloaderversion + "-" + cyanVersion + ".zip");
		File modkit = new File(baseDir, "modkits/cyan-modkit-" + gameversion + "-" + platform + "-" + modloaderversion
				+ "-" + cyanVersion + ".zip");
		File modkitDir = new File(baseDir, "modkits");
		if (!modkitDir.exists()) {
			try {
				new URL(DownloadsBackend.getMavenRepo()).openStream().close();
				modkitDir.mkdirs();
				try {
					String url = backend.createModKitDownloadURL(new FunctionInfo(function).setParams(platform,
							repository, gameversion, modloaderversion, "modkit"));
					InputStream strm = new URL(url).openStream();
					FileOutputStream outp = new FileOutputStream(modkit);
					strm.transferTo(outp);
					outp.close();
					strm.close();
				} catch (IOException e) {
				}
				try {
					String url = backend.createModKitDownloadURL(new FunctionInfo(function).setParams(platform,
							repository, gameversion, modloaderversion, "coremodkit"));
					InputStream strm = new URL(url).openStream();
					FileOutputStream outp = new FileOutputStream(coremodkit);
					strm.transferTo(outp);
					outp.close();
					strm.close();
				} catch (IOException e) {
				}
			} catch (IOException e) {
			}
		}

		function.variables.put("style.modkits", "display: block;");
		if (!coremodkit.exists() && !modkit.exists()) {
			function.variables.put("style.modkits", "display: none;");
		} else {
			if (!modkit.exists()) {
				function.writeLine("<style>");
				function.writeLine(".modkitbtn {");
				function.writeLine("\tdisplay: none;");
				function.writeLine("}");
				function.writeLine("</style>");
			}
			if (!coremodkit.exists()) {
				function.writeLine("<style>");
				function.writeLine(".coremodkitbtn {");
				function.writeLine("\tdisplay: none;");
				function.writeLine("}");
				function.writeLine("</style>");
			}
		}

		if (backend.getZipStatus(inter) == ZipStatus.NEVER_STARTED) {
			backend.queueZip(inter);
		}
	}

	@Function
	public void install(FunctionInfo function) throws InvocationTargetException, IOException {
		if (function.parameters.length != 1)
			return;

		File source = new File(function.getServerContext().getSourceDirectory());
		File f = new File(function.getServerContext().getSourceDirectory(), function.namedParameters.get("execPath"))
				.getParentFile();
		if (!f.getCanonicalPath().startsWith(source.getCanonicalPath()))
			return;

		DocumentController controller = DocumentController.getNewController();
		function.variables.put("repository", function.parameters[0]);
		function.variables.putAll(function.namedParameters);
		FileInputStream strm = new FileInputStream(new File(f, "/webcomponents/downloads/DownloadPage.java.html"));
		controller.connectServer(function).addDefaultCommands().attachReader(() -> {
			try {
				return strm.read();
			} catch (IOException e) {
				return -1;
			}
		}).attachDocumentStringWriter();
		controller.getProcessor().process();
		function.write(controller.getStringWriterResult());
		strm.close();
	}

}
