package org.asf.cyan.tests;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;

public class Agent {
	public static void agentmain(final String args, final Instrumentation inst) {
		inst.addTransformer(new ClassFileTransformer() {
			@Override
			public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,	ProtectionDomain protectionDomain, byte[] classfileBuffer) {
				if (className.equals("org/gradle/launcher/bootstrap/ProcessBootstrap")) {
					try {
						ClassPool cp = ClassPool.getDefault();
						CtClass cc = cp.get("org.gradle.launcher.bootstrap.ProcessBootstrap");
						 
						CtMethod m4 = CtNewMethod.make(""
								+ "public void LoadLibs() {"
								+ "		for (int i = 0; i < org.gradle.internal.installation.CurrentGradleInstallation.get().getLibDirs().size(); i++) {\n"
								+ "				Object e = org.gradle.internal.installation.CurrentGradleInstallation.get().getLibDirs().get(i);\n"
								+ "				java.net.URL u = ((java.io.File)e).toURI().toURL();\n"
								+ "				org.gradle.internal.classloader.VisitableURLClassLoader urlClassLoader = (org.gradle.internal.classloader.VisitableURLClassLoader)Thread.currentThread().getContextClassLoader();\n"
								+ "				urlClassLoader.addURL(u);\n"
								+ "		}\n"
								+ "}", cc);
						cc.addMethod(m4);
						CtMethod m = cc.getDeclaredMethod("run");
						CtMethod m2 = cc.getDeclaredMethod("runNoExit");

						m2.insertAt(55, "$0.LoadLibs();");
						
						m.insertBefore("runNoExit(mainClassName, args); return;");
						byte[] byteCode = cc.toBytecode();
						cc.detach();
						return byteCode;
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
				else if (className.equalsIgnoreCase("org/gradle/launcher/bootstrap/EntryPoint")) {
					try {
						ClassPool cp = ClassPool.getDefault();
						CtClass cc = cp.get("org.gradle.launcher.bootstrap.EntryPoint");
				        cc.getDeclaredMethod("run").addLocalVariable("c", cp.get("java.lang.Class"));
						cc.getDeclaredMethod("run").insertBefore("if (args[0].equals(\"--asfcyantstload\")) {\n"
								+ "		c = Thread.currentThread().getContextClassLoader().loadClass(\"org.gradle.util.GradleVersion\");\n"
								+ "		System.setProperty(\"cyan.gradlever\", c.getMethod(\"getVersion\", new Class[0]).invoke(c.getMethod(\"current\", new Class[0]).invoke(null, null), null).toString());\n"
								+ "		return;\n"
								+ "}");
						byte[] byteCode = cc.toBytecode();
						cc.detach();
						return byteCode;
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
				else if (className.equalsIgnoreCase("org/gradle/internal/classloader/ClassLoaderUtils")) {
					try {
						ClassPool cp = ClassPool.getDefault();
						CtClass cc = cp.get("org.gradle.internal.classloader.ClassLoaderUtils");
						cc.getDeclaredMethod("tryClose").setBody("{ }");
						byte[] byteCode = cc.toBytecode();
						cc.detach();
						return byteCode;
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}

				return null;
			}
		});
	}
}
