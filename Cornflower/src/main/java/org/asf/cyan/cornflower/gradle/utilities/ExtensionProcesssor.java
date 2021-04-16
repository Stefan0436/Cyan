package org.asf.cyan.cornflower.gradle.utilities;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Stream;

import org.gradle.api.Project;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import groovy.lang.Closure;

class ExtensionProcesssor {
	public static void Load(Project target, Class<? extends ExtendedPlugin> plugin)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, SecurityException {
		ConfigurationBuilder conf = ConfigurationBuilder.build();
		Set<URL> urls = conf.getUrls();
		urls.clear();
		conf.setUrls(urls);
		conf.addUrls(plugin.getProtectionDomain().getCodeSource().getLocation());
		conf = conf.setExpandSuperTypes(false);

		Reflections reflections = new Reflections(conf);
		Set<Class<?>> classes = reflections.getTypesAnnotatedWith(RegisterExtension.class);
		for (Class<?> c : classes) {
			for (Method a : c.getMethods()) {
				if (Modifier.isStatic(a.getModifiers())) {
					target.getExtensions().getExtraProperties().set(a.getName(), new SpecialClosure(a, null, target));
				}
			}
			for (Field a : c.getFields()) {
				if (Modifier.isStatic(a.getModifiers()) && Modifier.isFinal(a.getModifiers())) {
					if (a.getType().getTypeName().equals(Class.class.getTypeName())) {
						Class<?> cls = (Class<?>) a.get(null);
						if (cls.isEnum()) {
							HashMap<String, Object> mp = new HashMap<String, Object>();

							Stream.of(cls.getFields()).filter(t -> t.getType().getTypeName().equals(cls.getTypeName()))
									.forEach(t -> {
										try {
											String nm = t.getName();
											if (nm.startsWith("__"))
												nm = nm.substring(2);
											mp.put(nm, t.get(null));
										} catch (IllegalArgumentException | IllegalAccessException e) {
										}
									});

							target.getExtensions().add(a.getName(), mp);
						} else {
							target.getExtensions().getExtraProperties().set(a.getName(), cls);
						}
					} else {
						target.getExtensions().add(a.getName(), a.get(null));
					}
				}
			}
		}
	}

	static class SpecialClosure extends Closure<Object> {
		private static final long serialVersionUID = 1331598514974935552L;
		private Method meth;
		private Project target = null;

		public SpecialClosure(Method meth, Object target, Project targetProj) {
			super(target);
			meth.setAccessible(true);
			int l = meth.getParameterCount();
			if (Stream.of(meth.getParameterTypes())
					.anyMatch(t -> t.getTypeName().equals(Project.class.getTypeName()))) {
				l--;
			}
			Class<?>[] types = new Class<?>[l];
			boolean proj = false;
			int ind = 0;
			boolean first = true;
			for (Class<?> type : meth.getParameterTypes()) {
				if (type.getTypeName().equals(Project.class.getTypeName()) && !proj && first) {
					proj = true;
					this.target = targetProj;
				} else {
					types[ind++] = type;
				}
				first = false;
			}
			this.meth = meth;
			this.parameterTypes = types;
		}

		public Object call(Object... args) {
			try {
				Object[] params = new Object[args.length];
				int ind = 0;
				if (target != null) {
					params = new Object[args.length + 1];
					params[ind++] = target;
				}
				for (Object p : args) {
					params[ind++] = p;
				}
				return meth.invoke(null, params);
			} catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
				if (e instanceof InvocationTargetException)
					throw new RuntimeException(e.getCause());
				else
					throw new RuntimeException(e);
			}
		}
	}
}
