package org.asf.cyan.cornflower.gradle.utilities;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskProvider;

/**
 * Task extension for automatic task registration
 *
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@RegisterTask
public interface ITaskExtender {
	public void registerTask(String name, TaskProvider<DefaultTask> task);
}
