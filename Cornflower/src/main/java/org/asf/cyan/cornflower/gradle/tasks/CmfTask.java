package org.asf.cyan.cornflower.gradle.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.asf.cyan.api.config.Configuration;
import org.asf.cyan.cornflower.gradle.utilities.ITaskExtender;
import org.asf.cyan.cornflower.gradle.utilities.modding.manifests.IModManifest;

import org.gradle.api.DefaultTask;
import org.gradle.api.internal.file.copy.CopySpecInternal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Zip;

import groovy.lang.Closure;

public class CmfTask extends Zip implements ITaskExtender {

	private File manifestTmp;
	private Closure<Configuration<?>> manifest;

	private Configuration<?> manifestConfig;

	public void manifest(Closure<Configuration<?>> manifest) {
		this.manifest = manifest;
	}

	public void manifest(Configuration<?> manifest) {
		this.manifest = new Closure<Configuration<?>>(manifest) {

			private static final long serialVersionUID = 1L;

			@Override
			public Configuration<?> call() {
				return manifest;
			}

		};
	}

	@TaskAction
	public void cmf() {
	}

	public CmfTask() {
		getArchiveExtension().set("cmf");
		getDestinationDirectory().set(new File(getProject().getBuildDir(), "cmf"));

		File manifestTmpDir = new File(getProject().getBuildDir(), "tmp/cmf");
		manifestTmpDir.mkdirs();

		manifestTmp = new File(manifestTmpDir, "mod.manifest.ccfg");
		((CopySpecInternal) getRootSpec().addFirst().into("/")).addChild().from(manifestTmp);
	}

	@Override
	public void registerTask(String name, TaskProvider<DefaultTask> task) {
		if (name.equals("cmf")) {
			task.get().setDescription("Creates CMF archives");
			task.get().setGroup("build");

			task.get().getProject().afterEvaluate(new Closure<Object>(task) {

				private static final long serialVersionUID = 1L;

				@Override
				public Object call(Object... args) {
					if (CmfTask.this.manifest != null) {
						Configuration<?> manifest = CmfTask.this.manifest.call();
						if (manifest instanceof IModManifest) {
							((IModManifest) manifest).getJars().forEach((source, dest) -> {
								if (dest.endsWith(source.getName()))
									dest = dest.substring(0, dest.length() - source.getName().length());

								CopySpecInternal spec = (CopySpecInternal) getRootSpec().addFirst().into(dest);
								spec.addChild().from(source);
							});
						}

						manifestConfig = manifest;
					}
					return task;
				}

			});

			task.get().doFirst(new Closure<Object>(task) {

				private static final long serialVersionUID = 1L;

				@Override
				public Object call(Object... args) {
					if (CmfTask.this.manifest != null) {
						try {
							Files.write(manifestTmp.toPath(), manifestConfig.toString().getBytes());
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
					return task;
				}

			});
		}
	}
}
