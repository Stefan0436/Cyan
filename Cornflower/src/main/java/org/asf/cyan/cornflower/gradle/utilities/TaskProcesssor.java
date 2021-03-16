package org.asf.cyan.cornflower.gradle.utilities;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

class TaskProcesssor {
	public static void Load(Project target, Class<? extends ExtendedPlugin> plugin) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, SecurityException
	{
		ConfigurationBuilder conf = ConfigurationBuilder.build();
		Set<URL> urls = conf.getUrls();
		urls.clear();
		conf.setUrls(urls);
		conf.addUrls(plugin.getProtectionDomain().getCodeSource().getLocation());
		conf = conf.setExpandSuperTypes(false);

		Reflections reflections = new Reflections(conf);
		Set<Class<?>> classes = reflections.getTypesAnnotatedWith(RegisterTask.class);
		for (Class<?> c : classes) {
			
			@SuppressWarnings("unchecked")
			Class<DefaultTask> c2 = (Class<DefaultTask>) c;
			for (Method a : c2.getDeclaredMethods()) {
				if (a.isAnnotationPresent(TaskAction.class)) {
					TaskProvider<DefaultTask> task = target.getTasks().register(a.getName(), c2);
					try
					{
						c2.getDeclaredMethod("registerTask", String.class, TaskProvider.class).invoke(task.get(), task.getName(), task);
					}
					catch (NoSuchMethodException ex)
					{
						
					}
				}
			}
		}
	}
}
