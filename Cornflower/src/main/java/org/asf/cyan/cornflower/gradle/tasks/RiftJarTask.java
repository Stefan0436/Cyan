package org.asf.cyan.cornflower.gradle.tasks;

import java.io.File;

import org.asf.cyan.cornflower.gradle.utilities.ITaskExtender;
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

public class RiftJarTask extends AbstractArchiveTask implements ITaskExtender {

	public IRiftToolchainProvider provider = null;

	public RiftJarTask() {
		getArchiveClassifier().set("rift");
		getArchiveExtension().set("jar");
		getDestinationDirectory().set(new File(getProject().getBuildDir(), "rift"));
	}

	public IRiftToolchainProvider provider(IRiftToolchainProvider provider) {
		this.provider = provider;
		return provider;
	}

	@TaskAction
	public void rift() {
		File output = this.getArchiveFile().get().getAsFile();
		output = output;
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
		return new RiftCopyAction(getInputs(), getArchiveFile().get().getAsFile(), this);
	}

	private class RiftCopyAction implements CopyAction {

		private File output;
		private TaskInputs inputs;
		private RiftJarTask task;

		public RiftCopyAction(TaskInputs inputs, File output, RiftJarTask task) {
			this.output = output;
			this.inputs = inputs;
			this.task = task;
		}

		@Override
		public WorkResult execute(CopyActionProcessingStream stream) {
			for (File input : inputs.getFiles()) {
				input = input;
			}
			if (provider == null)
				return WorkResults.didWork(false);
			else 
				return null;
		}

	}

}
