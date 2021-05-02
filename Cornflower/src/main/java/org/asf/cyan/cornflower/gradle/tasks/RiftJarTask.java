package org.asf.cyan.cornflower.gradle.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.cornflower.gradle.utilities.modding.IPlatformConfiguration;
import org.asf.cyan.cornflower.gradle.utilities.ITaskExtender;
import org.asf.cyan.cornflower.gradle.utilities.Log4jToGradleAppender;
import org.asf.cyan.fluid.bytecode.sources.FileClassSourceProvider;
import org.asf.cyan.minecraft.toolkits.mtk.rift.SimpleRift;
import org.asf.cyan.minecraft.toolkits.mtk.rift.SimpleRiftBuilder;
import org.asf.cyan.minecraft.toolkits.mtk.rift.providers.IRiftToolchainProvider;
import org.gradle.api.DefaultTask;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.internal.file.copy.CopyActionProcessingStream;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskInputs;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.WorkResult;
import org.gradle.api.tasks.WorkResults;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public class RiftJarTask extends AbstractArchiveTask implements ITaskExtender {

	public ArrayList<IRiftToolchainProvider> providers = new ArrayList<IRiftToolchainProvider>();
	public String mappings_identifier = null;
	public File mappings_savedir = null;

	public IPlatformConfiguration platform;
	public GameSide side;

	public void platform(IPlatformConfiguration platform) {
		this.platform = platform;
	}

	public void side(GameSide side) {
		this.side = side;
	}

	public void mappings_savedir(File dir) {
		mappings_savedir = dir;
	}

	public void mappings_identifier(String identifier) {
		mappings_identifier = identifier;
	}

	public RiftJarTask() {
		getArchiveClassifier().set("rift");
		getArchiveExtension().set("jar");
		getDestinationDirectory().set(new File(getProject().getBuildDir(), "rift"));
	}

	public IRiftToolchainProvider provider(IRiftToolchainProvider provider) {
		if (provider != null)
			providers.add(provider);
		return provider;
	}

	@TaskAction
	public void rift() {
	}

	@Override
	public void registerTask(String name, TaskProvider<DefaultTask> task) {
		if (name.equals("rift")) {
			task.get().setDescription("Creates RIFT jars");
			task.get().setGroup("Build");
		}
	}

	@Override
	protected CopyAction createCopyAction() {
		return new RiftCopyAction(getInputs(), getArchiveFile().get().getAsFile());
	}

	private class RiftCopyAction implements CopyAction {

		private File output;
		private TaskInputs inputs;

		public RiftCopyAction(TaskInputs inputs, File output) {
			this.output = output;
			this.inputs = inputs;
		}

		@Override
		public WorkResult execute(CopyActionProcessingStream stream) {
			if (providers.size() == 0)
				return WorkResults.didWork(false);
			else {
				Log4jToGradleAppender.logInfo();

				ArrayList<String> classes = new ArrayList<String>();
				SimpleRiftBuilder rift = new SimpleRiftBuilder();

				for (File input : inputs.getFiles()) {
					rift.appendSources(new FileClassSourceProvider(input));

					if (input.isDirectory()) {
						try {
							scan(input, classes);
						} catch (IOException e) {
							try {
								rift.close();
							} catch (IOException e2) {
							}
							Log4jToGradleAppender.noLogInfo();
							return WorkResults.didWork(false);
						}
					} else {
						try {
							FileInputStream inputStream = new FileInputStream(input);
							try {
								ZipInputStream strm = new ZipInputStream(inputStream);
								ZipEntry entry = strm.getNextEntry();
								while (entry != null) {
									if (new File(entry.getName()).getName().endsWith(".class")) {
										ClassReader reader = new ClassReader(strm);
										ClassNode node = new ClassNode();
										reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

										classes.add(node.name.replace("/", "."));
									}
									entry = strm.getNextEntry();
								}
								strm.close();
							} catch (IOException e) {
								try {
									rift.close();
								} catch (IOException e2) {
								}
								Log4jToGradleAppender.noLogInfo();
								return WorkResults.didWork(false);
							}
							inputStream.close();
						} catch (IOException e) {

						}
					}
				}

				for (IRiftToolchainProvider provider : providers) {
					rift.appendRiftProvider(provider);
				}
				for (String cls : classes) {
					rift.addClass(cls);
				}

				if (mappings_identifier != null)
					rift.setIdentifier(mappings_identifier);

				if (mappings_savedir != null)
					rift.setMappingsSaveDir(mappings_savedir);

				try {
					SimpleRift riftInst = rift.build();
					for (File input : inputs.getFiles()) {
						if (input.isDirectory()) {
							try {
								addFiles(input, "/", riftInst);
							} catch (IOException e) {
								Log4jToGradleAppender.noLogInfo();
								return WorkResults.didWork(false);
							}
						} else {
							try {
								ZipFile zip = new ZipFile(input);
								Enumeration<? extends ZipEntry> entries = zip.entries();
								while (entries.hasMoreElements()) {
									ZipEntry ent = entries.nextElement();
									if (!new File(ent.getName()).getName().endsWith(".class")) {
										riftInst.addFile(ent.getName(), zip.getInputStream(ent));
									}
								}
								zip.close();
							} catch (IOException e) {
								Log4jToGradleAppender.noLogInfo();
								return WorkResults.didWork(false);
							}
						}
					}

					riftInst.apply();
					if (output.exists())
						output.delete();
					riftInst.export(output);
					riftInst.close();
				} catch (ClassNotFoundException | IOException e) {
					try {
						rift.close();
					} catch (IOException e2) {
					}
					Log4jToGradleAppender.noLogInfo();
					throw new RuntimeException(e);
				}

				try {
					rift.close();
				} catch (IOException e) {
				}
				Log4jToGradleAppender.noLogInfo();
				return WorkResults.didWork(true);
			}
		}

		private void addFiles(File input, String path, SimpleRift rift) throws IOException {
			for (File dir : input.listFiles((f) -> f.isDirectory()))
				addFiles(dir, path + dir.getName() + "/", rift);
			for (File file : input.listFiles((f) -> !f.isDirectory() && !f.getName().endsWith(".class"))) {
				FileInputStream strm = new FileInputStream(file);
				rift.addFile(path, strm);
				strm.close();
			}
		}

		private void scan(File input, ArrayList<String> classes) throws IOException {
			for (File dir : input.listFiles((f) -> f.isDirectory()))
				scan(dir, classes);
			for (File classFile : input.listFiles((f) -> !f.isDirectory() && !f.getName().endsWith(".class"))) {
				FileInputStream strm = new FileInputStream(classFile);
				ClassReader reader = new ClassReader(strm);
				ClassNode node = new ClassNode();
				reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
				strm.close();

				classes.add(node.name.replace("/", "."));
			}
		}

	}

}
