package org.asf.cyan.cornflower.gradle.tasks;

import java.io.File;
import java.net.URL;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.asf.cyan.CtcUtil;
import org.asf.cyan.cornflower.gradle.Cornflower;
import org.asf.cyan.cornflower.gradle.utilities.GradleUtil;
import org.asf.cyan.cornflower.gradle.utilities.ITaskExtender;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import groovy.lang.Closure;

public class CtcTask extends DefaultTask implements ITaskExtender {

	@SuppressWarnings("unused")
	private class Credentials {
		public String group;
		public String username;
		private char[] password;

		public void username(String username) {
			this.username = username;
		}

		public void usergroup(String group) {
			this.group = group;
		}

		public void password(String password) {
			this.password = password.toCharArray();
		}

		public Credentials runClosure(Closure<?> closure) {
			closure.setDelegate(this);
			closure.call();
			return this;
		}
	}

	private ArrayList<Credentials> credentials = new ArrayList<Credentials>();
	public ArrayList<File> inputCache = new ArrayList<File>();

	public void credentials(Closure<?> closure) {
		credentials.add(new Credentials().runClosure(closure));
	}

	public String method = "";
	public ArrayList<String> inputs = new ArrayList<String>();
	public String output = "";

	public String pack = "pack";
	public String unpack = "unpack";
	public String publish = "publish";
	public String uctc = "uctc";

	public boolean createHash = false;
	
	public String version = null;
	public File outputFile = null;

	public String getVersion() {
		return version;
	}

	public File getOutput() {
		return outputFile;
	}

	public String genOutputName(File outputDir, String baseName) throws IOException {
		return outputDir.getCanonicalPath() + "/" + baseName + "-%version%.ctc";
	}

	public void createHash(boolean createHash) {
		this.createHash = createHash;
	}

	private ArrayList<String> methods = new ArrayList<String>(Arrays.asList("pack", "unpack", "publish", "uctc"));
	private int max = 0;

	public void input(Iterable<File> input) {
		for (File file : input)
			inputCache.add(file);
	}

	public void input(File[] input) {
		for (File file : input)
			inputCache.add(file);
	}

	public void addInput(Iterable<File> input) throws IOException {
		for (File file : input) {
			if (!file.exists())
				continue;
			if (file.isDirectory() && (method.equals("unpack") || method.equals("uctc") || method.equals("publish"))) {
				addInput(file.listFiles());
			} else {
				input(file.getCanonicalPath());
			}
		}
	}

	public void addInput(File[] input) throws IOException {
		for (File file : input) {
			if (!file.exists())
				continue;
			if (file.isDirectory()) {
				addInput(file.listFiles());
			} else {
				input(file.getCanonicalPath());
			}
		}
	}

	public void method(String method) {
		this.method = method;
	}

	public void input(File input) throws IOException {
		inputCache.add(input);
	}

	public void input(RegularFile input) throws IOException {
		inputCache.add(input.getAsFile());
	}

	public void input(Provider<RegularFile> input) throws IOException {
		inputCache.add(input.get().getAsFile());
	}

	public void input(String input) {
		inputs.add(input);
	}

	public void input(String[] input) {
		for (String inp : input)
			input(inp);
	}

	public void output(File output) throws IOException {
		output(output.getCanonicalPath());
	}

	public void output(String output) {
		this.output = output;
	}

	public void output(URL output) {
		output(output.toString());
	}

	@TaskAction
	public void ctc() throws IOException {
		addInput(inputCache);
		inputCache.clear();
		if (method.isEmpty()) {
			throw new IOException("No method specified, use either pack, unpack, uctc or publish.");
		} else {
			if (!methods.contains(method))
				throw new IOException("Method not recognized, use either pack, unpack, uctc or publish.");
		}
		if (method.equals("pack")) {
			File input = new File(GradleUtil.getCacheFolder(Cornflower.class, "ctc-tmp"),
					System.currentTimeMillis() + ".tmp");
			while (input.exists())
				input = new File(GradleUtil.getCacheFolder(Cornflower.class, "ctc-tmp"),
						System.currentTimeMillis() + ".tmp");
			input.mkdirs();

			final File inpFinal = input;
			inputs.forEach((inp) -> {
				File in = new File(inp);
				try {
					copyDir(in, inpFinal);
				} catch (IOException e) {
				}
			});
			max = 0;
			try {
				String version = CtcUtil.pack(input, new File(output), (num) -> max = num, (num) -> {
					Cornflower.getPluginInstance(Cornflower.class).logInfo("Added: " + num + " / " + max);
				});
				File out = new File(output.replace("%version%", version));
				Files.move(new File(output).toPath(), out.toPath());

				if (createHash) {
					Files.write(new File(out.getAbsolutePath() + ".sha256").toPath(),
							(sha256HEX(Files.readAllBytes(out.toPath())) + "  " + out.getName()).getBytes());
				}

				this.version = version;
				this.outputFile = out;
			} finally {
				Cornflower.getPluginInstance(Cornflower.class).deleteDir(input);
			}
		} else if (method.equals("unpack")) {
			if (new File(output).exists() && new File(output).isDirectory())
				Cornflower.getPluginInstance(Cornflower.class).deleteDir(new File(output));
			else
				new File(output).delete();

			File input = new File(GradleUtil.getCacheFolder(Cornflower.class, "ctc-tmp"),
					System.currentTimeMillis() + ".tmp");

			while (input.exists())
				input = new File(GradleUtil.getCacheFolder(Cornflower.class, "ctc-tmp"),
						System.currentTimeMillis() + ".tmp");
			input.mkdirs();

			final File inpFinal = input;
			inputs.forEach((inp) -> {
				try {
					max = 0;
					CtcUtil.unpack(new File(inp), inpFinal, (num) -> max = num, (num) -> {
						Cornflower.getPluginInstance(Cornflower.class).logInfo("Extracted: " + num + " / " + max);
					});

					copyDir(inpFinal, new File(output));

					Cornflower.getPluginInstance(Cornflower.class).deleteDir(inpFinal);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		} else if (method.equals("uctc")) {
			if (new File(output).exists() && new File(output).isDirectory())
				Cornflower.getPluginInstance(Cornflower.class).deleteDir(new File(output));
			else
				new File(output).delete();

			new File(output).mkdirs();
			for (String file : new ArrayList<String>(inputs)) {
				File f = new File(file);
				if (f.getName().endsWith(".jar") || f.getName().endsWith(".zip")) {
					ZipFile zip = new ZipFile(f);
					Enumeration<? extends ZipEntry> entries = zip.entries();

					while (entries.hasMoreElements()) {
						ZipEntry entry = entries.nextElement();
						String path = entry.getName().replace("\\", "/");

						while (path.startsWith("/"))
							path = path.substring(1);

						if (entry.getName().endsWith(".class")) {
							byte[] data = zip.getInputStream(entry).readAllBytes();

							ClassNode node = new ClassNode();
							ClassReader reader = new ClassReader(data);
							reader.accept(node, 0);

							String hash = sha256HEX(data);
							addHash(hash, node.name.replace("/", "."), output);
						}
					}

					zip.close();
				}
			}
			for (String file : inputs) {
				File f = new File(file);
				if (f.getName().endsWith(".class") && !f.isDirectory()) {
					ClassNode node = new ClassNode();
					FileInputStream strm = new FileInputStream(f);
					ClassReader reader = new ClassReader(strm);
					reader.accept(node, 0);
					strm.close();

					String hash = sha256HEX(Files.readAllBytes(f.toPath()));
					addHash(hash, node.name.replace("/", "."), output);
				}
			}

			Files.write(new File(output, "main.header").toPath(), ("Name: " + new File(output).getName()).getBytes());
		} else if (method.equals("publish")) {
			for (String input : inputs) {
				if (input.endsWith(".ctc")) {
					CtcUtil.publish(new File(input), new URL(output), (group) -> {
						Optional<Credentials> cred = credentials.stream().filter(t -> t.group.equals(group))
								.findFirst();
						if (!cred.isPresent())
							return null;

						return new Object[] { cred.get().username, cred.get().password };
					}, (published) -> {
						System.out.println("Published: " + published + " -> " + output + "/" + published);
					});
				}
			}
		}
	}

	private void addHash(String hash, String name, String output) throws IOException {
		String pkg = "(default)";
		if (name.contains(".")) {
			pkg = name.substring(0, name.lastIndexOf("."));
			name = name.substring(name.lastIndexOf(".") + 1);
		}

		File outp = new File(output, pkg + "/" + name + ".cls");
		if (!outp.getParentFile().exists())
			outp.getParentFile().mkdirs();

		ArrayList<String> hashes = new ArrayList<String>();
		hashes.add(hash);
		if (outp.exists()) {
			for (String existingHash : Files.readAllLines(outp.toPath())) {
				hashes.add(existingHash.trim());
			}
		}

		StringBuilder hashfile = new StringBuilder();
		hashes.forEach((t) -> hashfile.append(t).append(System.lineSeparator()));
		Files.write(outp.toPath(), hashfile.toString().getBytes());
	}

	private void copyDir(File in, File out) throws IOException {
		out.mkdirs();
		for (File dir : in.listFiles((f) -> f.isDirectory()))
			copyDir(dir, new File(out, dir.getName()));
		for (File file : in.listFiles((f) -> !f.isDirectory()))
			if (!new File(out, file.getName()).exists())
				Files.copy(file.toPath(), new File(out, file.getName()).toPath());
	}

	private String sha256HEX(byte[] array) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
		byte[] sha = digest.digest(array);
		StringBuilder result = new StringBuilder();
		for (byte aByte : sha) {
			result.append(String.format("%02x", aByte));
		}
		return result.toString();
	}

	@Override
	public void registerTask(String name, TaskProvider<DefaultTask> task) {
		if (name.equals("ctc")) {
			task.get().setDescription("Creates CTC Trust Containers");
			task.get().setGroup("Build");
		}
	}

}
