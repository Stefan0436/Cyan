package org.asf.cyan.cornflower.gradle.tasks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.asf.cyan.cornflower.classpath.util.PathPriority;
import org.asf.cyan.cornflower.classpath.util.SourceType;
import org.asf.cyan.cornflower.gradle.utilities.ITaskExtender;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;

import groovy.lang.Closure;

public class EclipseLaunchGenerator extends DefaultTask implements ITaskExtender {
	public static final EclipseLaunchType local = EclipseLaunchType.LOCAL;
	public static final EclipseLaunchType remote = EclipseLaunchType.REMOTE;

	public boolean disable = false;
	public boolean sourceMementoSpecified = false;
	public boolean classpathMementoSpecified = false;

	public String JRE = null; // can be used, just not recommended
	public ArrayList<SourceMemento> sourceLookup = new ArrayList<SourceMemento>();
	public ArrayList<ClasspathMemento> classPath = new ArrayList<ClasspathMemento>();
	public HashMap<String, String> environmentVariables = new HashMap<String, String>();
	public ArrayList<String> jvmArguments = new ArrayList<String>();
	public ArrayList<String> progArguments = new ArrayList<String>();

	public String name = "";
	public String mainClass = "";
	public File workingDir = null;

	public ConfigurationType launchType = new ConfigurationType();

	public EclipseLaunchGenerator() {
		sourceLookup.add(new SourceMemento());
		classPath.add(new ClasspathMemento(getProject()));

		this.workingDir = new File("run");
		this.name = "Launch " + getProject().getName();
		if (getProject().getGroup() != null && !getProject().getGroup().equals(""))
			mainClass = getProject().getGroup().toString() + ".main.Main";
		else
			mainClass = getProject().getName().toLowerCase() + ".main.Main";
	}

	@TaskAction
	public void createEclipseLaunches() {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document dom = builder.newDocument();

			Element launchConfiguration = dom.createElement("launchConfiguration");
			launchConfiguration.setAttribute("type", launchType.type.cls);

			Element MAPPED_RESOURCE_PATHS = dom.createElement("listAttribute");
			MAPPED_RESOURCE_PATHS.setAttribute("key", "org.eclipse.debug.core.MAPPED_RESOURCE_PATHS");

			Element projName = dom.createElement("listEntry");
			projName.setAttribute("value", "/" + getProject().getName());
			MAPPED_RESOURCE_PATHS.appendChild(projName);

			launchConfiguration.appendChild(MAPPED_RESOURCE_PATHS);

			Element MAPPED_RESOURCE_TYPES = dom.createElement("listAttribute");
			MAPPED_RESOURCE_TYPES.setAttribute("key", "org.eclipse.debug.core.MAPPED_RESOURCE_TYPES");

			Element resourcetype = dom.createElement("listEntry");
			resourcetype.setAttribute("value", "4");
			MAPPED_RESOURCE_TYPES.appendChild(resourcetype);

			launchConfiguration.appendChild(MAPPED_RESOURCE_TYPES);

			if (launchType.type == local) {
				if (environmentVariables.size() != 0) {
					Element ENVIRONMAP = dom.createElement("mapAttribute");
					ENVIRONMAP.setAttribute("key", "org.eclipse.debug.core.environmentVariables");

					environmentVariables.forEach((k, v) -> {
						Element entry = dom.createElement("mapEntry");
						entry.setAttribute("key", k);
						entry.setAttribute("value", v);
						ENVIRONMAP.appendChild(entry);
					});

					launchConfiguration.appendChild(ENVIRONMAP);
				}
			}

			if (sourceLookup.size() != 1 || !sourceLookup.get(0).type.equals(SourceType.__default)) {
				Element source_locator_id = dom.createElement("stringAttribute");
				source_locator_id.setAttribute("key", "org.eclipse.debug.core.source_locator_id");
				source_locator_id.setAttribute("value",
						"org.eclipse.jdt.launching.sourceLocator.JavaSourceLookupDirector");
				launchConfiguration.appendChild(source_locator_id);

				Element source_locator_memento = dom.createElement("stringAttribute");
				String memento = SourceMemento.generateMemento(builder,
						sourceLookup.toArray(new SourceMemento[sourceLookup.size()]));
				source_locator_memento.setAttribute("key", "org.eclipse.debug.core.source_locator_memento");
				source_locator_memento.setAttribute("value", memento);
				launchConfiguration.appendChild(source_locator_memento);
			}

			Element classPathLst = dom.createElement("listAttribute");
			classPathLst.setAttribute("key", "org.eclipse.jdt.launching.CLASSPATH");

			for (ClasspathMemento cp : classPath) {
				classPathLst.appendChild(cp.createMemento(getProject(), dom, builder));
			}

			launchConfiguration.appendChild(classPathLst);

			if (launchType.type == remote) {
				Element ALLOW_TERMINATE = dom.createElement("booleanAttribute");
				ALLOW_TERMINATE.setAttribute("key", "org.eclipse.jdt.launching.ALLOW_TERMINATE");
				ALLOW_TERMINATE.setAttribute("value", (launchType.allowTerminate ? "true" : "false"));
				launchConfiguration.appendChild(ALLOW_TERMINATE);

				Element CONNECT_MAP = dom.createElement("mapAttribute");
				CONNECT_MAP.setAttribute("key", "org.eclipse.jdt.launching.CONNECT_MAP");

				Element hostname = dom.createElement("mapEntry");
				hostname.setAttribute("key", "hostname");
				hostname.setAttribute("value", launchType.hostname);
				CONNECT_MAP.appendChild(hostname);

				Element port = dom.createElement("mapEntry");
				port.setAttribute("key", "port");
				port.setAttribute("value", launchType.port + "");
				CONNECT_MAP.appendChild(port);

				launchConfiguration.appendChild(CONNECT_MAP);

				Element VM_CONNECTOR_ID = dom.createElement("stringAttribute");
				VM_CONNECTOR_ID.setAttribute("key", "org.eclipse.jdt.launching.VM_CONNECTOR_ID");
				VM_CONNECTOR_ID.setAttribute("value", "org.eclipse.jdt.launching.socketAttachConnector");
				launchConfiguration.appendChild(VM_CONNECTOR_ID);
			} else {
				Element dfcp = dom.createElement("booleanAttribute");
				dfcp.setAttribute("key", "org.eclipse.jdt.launching.DEFAULT_CLASSPATH");
				dfcp.setAttribute("value", "false");
				launchConfiguration.appendChild(dfcp);

				if (JRE != null) {
					Element JRE_CONTAINER = dom.createElement("stringAttribute");
					JRE_CONTAINER.setAttribute("key", "org.eclipse.jdt.launching.JRE_CONTAINER");
					JRE_CONTAINER.setAttribute("value", JRE);
					launchConfiguration.appendChild(JRE_CONTAINER);
				}

				Element mainType = dom.createElement("stringAttribute");
				mainType.setAttribute("key", "org.eclipse.jdt.launching.MAIN_TYPE");
				mainType.setAttribute("value", mainClass);
				launchConfiguration.appendChild(mainType);

				Element MODULEPATH = dom.createElement("listAttribute");
				MODULEPATH.setAttribute("key", "org.eclipse.jdt.launching.MODULEPATH");
				launchConfiguration.appendChild(MODULEPATH);

				Element MODULE_NAME = dom.createElement("stringAttribute");
				MODULE_NAME.setAttribute("key", "org.eclipse.jdt.launching.MODULE_NAME");
				MODULE_NAME.setAttribute("value", getProject().getName());
				launchConfiguration.appendChild(MODULE_NAME);
			}

			if (progArguments.size() != 0 && launchType.type == local) {
				String args = "";
				for (String arg : progArguments) {
					if (arg.contains(" "))
						arg = "\"" + arg + "\"";
					if (args.isEmpty())
						args = arg;
					else
						args += " " + arg;
				}
				Element PROGRAM_ARGUMENTS = dom.createElement("stringAttribute");
				PROGRAM_ARGUMENTS.setAttribute("key", "org.eclipse.jdt.launching.PROGRAM_ARGUMENTS");
				PROGRAM_ARGUMENTS.setAttribute("value", args);
				launchConfiguration.appendChild(PROGRAM_ARGUMENTS);
			}

			Element PROJECT_ATTR = dom.createElement("stringAttribute");
			PROJECT_ATTR.setAttribute("key", "org.eclipse.jdt.launching.PROJECT_ATTR");
			PROJECT_ATTR.setAttribute("value", getProject().getName());
			launchConfiguration.appendChild(PROJECT_ATTR);

			if (jvmArguments.size() != 0 && launchType.type == local) {
				String args = "";
				for (String arg : jvmArguments) {
					if (arg.contains(" "))
						arg = "\"" + arg + "\"";
					if (args.isEmpty())
						args = arg;
					else
						args += " " + arg;
				}
				Element VM_ARGUMENTS = dom.createElement("stringAttribute");
				VM_ARGUMENTS.setAttribute("key", "org.eclipse.jdt.launching.VM_ARGUMENTS");
				VM_ARGUMENTS.setAttribute("value", args);
				launchConfiguration.appendChild(VM_ARGUMENTS);
			}

			String dir = workingDir.getAbsolutePath();
			try {
				dir = workingDir.getCanonicalPath();
			} catch (IOException e) {
			}

			if ((workingDir.isAbsolute()
					&& getProject().getProjectDir().toPath().relativize(workingDir.toPath()).startsWith(".."))
					|| workingDir.getPath().startsWith("..")) {
				try {
					dir = workingDir.getCanonicalPath();
				} catch (IOException e) {
					dir = workingDir.getAbsolutePath();
				}
			} else {
				if (!workingDir.isAbsolute()) {
					dir = "${workspace_loc:" + getProject().getName() + "/" + workingDir.getPath() + "}";
				} else
					dir = "${workspace_loc:" + getProject().getName() + "/"
							+ getProject().getProjectDir().toPath().relativize(workingDir.toPath()).toString() + "}";
			}

			if (!workingDir.exists())
				workingDir.mkdirs();

			Element WORKING_DIRECTORY = dom.createElement("stringAttribute");
			WORKING_DIRECTORY.setAttribute("key", "org.eclipse.jdt.launching.WORKING_DIRECTORY");
			WORKING_DIRECTORY.setAttribute("value", dir);
			launchConfiguration.appendChild(WORKING_DIRECTORY);

			dom.appendChild(launchConfiguration);

			Transformer tr = TransformerFactory.newInstance().newTransformer();
			tr.setOutputProperty(OutputKeys.INDENT, "yes");
			tr.setOutputProperty(OutputKeys.VERSION, "1.0");
			tr.setOutputProperty(OutputKeys.METHOD, "xml");
			tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			tr.setOutputProperty(OutputKeys.STANDALONE, "no");

			FileWriter writer = new FileWriter(new File(getProject().getProjectDir(), "/" + name + ".launch"));

			tr.transform(new DOMSource(dom), new StreamResult(writer));
			writer.close();
		} catch (ParserConfigurationException | TransformerException | TransformerFactoryConfigurationError
				| IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void environment(String key, String value) {
		environmentVariables.put(key, value);
	}

	public void launchType(Closure<ConfigurationType> closure) {
		ConfigurationType output = new ConfigurationType();
		closure.setDelegate(output);
		closure.call();
		launchType = output;
	}

	public void launchType(ConfigurationType type) {
		launchType = type;
	}

	public void launchType(EclipseLaunchType type) {
		launchType = new ConfigurationType();
		launchType.type = type;
	}

	public void launchType(EclipseLaunchType type, String hostname) {
		launchType = new ConfigurationType();
		launchType.type = type;
		launchType.hostname = hostname;
	}

	public void launchType(EclipseLaunchType type, int port) {
		launchType = new ConfigurationType();
		launchType.type = type;
		launchType.port = port;
	}

	public void launchType(EclipseLaunchType type, String hostname, int port) {
		launchType = new ConfigurationType();
		launchType.type = type;
		launchType.hostname = hostname;
		launchType.port = port;
	}

	public void clearJvm() {
		jvmArguments.clear();
	}

	public void jvm(String... arguments) {
		for (String argument : arguments) {
			jvmArguments.add(argument);
		}
	}

	public void jvm(Iterable<String> arguments) {
		for (String argument : arguments) {
			jvmArguments.add(argument);
		}
	}

	public void jvm(String argument) {
		jvmArguments.add(argument);
	}

	public void clearArguments() {
		progArguments.clear();
	}

	public void argument(String... arguments) {
		for (String argument : arguments) {
			progArguments.add(argument);
		}
	}

	public void argument(Iterable<String> arguments) {
		for (String argument : arguments) {
			progArguments.add(argument);
		}
	}

	public void argument(String argument) {
		progArguments.add(argument);
	}

	public void name(String name) {
		this.name = name;
	}

	public void mainClass(String main) {
		this.mainClass = main;
	}

	public void main(String main) {
		this.mainClass = main;
	}

	public void workingDir(File path) {
		this.workingDir = path;
	}

	public void workingDir(String path) {
		this.workingDir = new File(path);
	}

	public void sourceLookup(File archive) {
		SourceMementoClosureOwner output = new SourceMementoClosureOwner();
		if (!sourceMementoSpecified)
			sourceLookup.clear();

		sourceMementoSpecified = true;
		output.add(archive);
	}

	public void sourceLookup(File[] archives) {
		SourceMementoClosureOwner output = new SourceMementoClosureOwner();
		if (!sourceMementoSpecified)
			sourceLookup.clear();

		sourceMementoSpecified = true;
		output.add(archives);
	}

	public void sourceLookup(Iterable<File> archives) {
		SourceMementoClosureOwner output = new SourceMementoClosureOwner();
		if (!sourceMementoSpecified)
			sourceLookup.clear();

		sourceMementoSpecified = true;
		output.archive(archives);
	}

	public void sourceLookup(Project[] projects) {
		SourceMementoClosureOwner output = new SourceMementoClosureOwner();
		if (!sourceMementoSpecified)
			sourceLookup.clear();

		sourceMementoSpecified = true;
		output.add(projects);
	}

	public void sourceLookup(Project project) {
		SourceMementoClosureOwner output = new SourceMementoClosureOwner();
		if (!sourceMementoSpecified)
			sourceLookup.clear();

		sourceMementoSpecified = true;
		output.add(project);
	}

	public void sourceLookup(String info) {
		SourceMementoClosureOwner output = new SourceMementoClosureOwner();
		if (!sourceMementoSpecified)
			sourceLookup.clear();

		sourceMementoSpecified = true;
		output.add(info);
	}

	public void sourceLookup(SourceType type, String info) {
		SourceMementoClosureOwner output = new SourceMementoClosureOwner();
		if (!sourceMementoSpecified)
			sourceLookup.clear();

		sourceMementoSpecified = true;
		output.add(type, info);
	}

	public void sourceLookup(SourceType type, String... info) {
		SourceMementoClosureOwner output = new SourceMementoClosureOwner();
		if (!sourceMementoSpecified)
			sourceLookup.clear();

		sourceMementoSpecified = true;
		output.add(type, info);
	}

	public void sourceLookup(Closure<SourceMementoClosureOwner> closure) {
		SourceMementoClosureOwner output = new SourceMementoClosureOwner();
		closure.setDelegate(output);
		if (!sourceMementoSpecified)
			sourceLookup.clear();

		sourceMementoSpecified = true;
		closure.call();
	}

	public void classpath(File archive) {
		ClasspathMementoClosureOwner output = new ClasspathMementoClosureOwner();
		if (!classpathMementoSpecified)
			classPath.clear();

		classpathMementoSpecified = true;
		output.add(archive);
	}

	public void classpath(File[] archives) {
		ClasspathMementoClosureOwner output = new ClasspathMementoClosureOwner();
		if (!classpathMementoSpecified)
			classPath.clear();

		classpathMementoSpecified = true;
		output.add(archives);
	}

	public void classpath(File archive, File sourceJar) {
		ClasspathMementoClosureOwner output = new ClasspathMementoClosureOwner();
		if (!classpathMementoSpecified)
			classPath.clear();

		classpathMementoSpecified = true;
		output.archive(archive, sourceJar);
	}

	public void classpath(Iterable<File> archives) {
		ClasspathMementoClosureOwner output = new ClasspathMementoClosureOwner();
		if (!classpathMementoSpecified)
			classPath.clear();

		classpathMementoSpecified = true;
		output.archive(archives);
	}

	public void classpath(Project[] projects) {
		ClasspathMementoClosureOwner output = new ClasspathMementoClosureOwner();
		if (!classpathMementoSpecified)
			classPath.clear();

		classpathMementoSpecified = true;

		if (!sourceMementoSpecified)
			sourceLookup.clear();

		sourceMementoSpecified = true;

		output.add(projects);
	}

	public void classpath(Project project) {
		ClasspathMementoClosureOwner output = new ClasspathMementoClosureOwner();
		if (!classpathMementoSpecified)
			classPath.clear();

		classpathMementoSpecified = true;

		if (!sourceMementoSpecified)
			sourceLookup.clear();

		sourceMementoSpecified = true;

		output.add(project);
	}

	public void classpath(String info) {
		ClasspathMementoClosureOwner output = new ClasspathMementoClosureOwner();
		if (!classpathMementoSpecified)
			classPath.clear();

		classpathMementoSpecified = true;
		output.add(info);
	}

	public void classpath(SourceType type, String info) {
		ClasspathMementoClosureOwner output = new ClasspathMementoClosureOwner();
		if (!classpathMementoSpecified)
			classPath.clear();

		classpathMementoSpecified = true;
		output.add(type, info);
	}

	public void classpath(SourceType type, String... info) {
		ClasspathMementoClosureOwner output = new ClasspathMementoClosureOwner();
		if (!classpathMementoSpecified)
			classPath.clear();

		classpathMementoSpecified = true;
		output.add(type, info);
	}

	public void classpath(Closure<ClasspathMementoClosureOwner> closure) {
		ClasspathMementoClosureOwner output = new ClasspathMementoClosureOwner();
		closure.setDelegate(output);
		if (!classpathMementoSpecified)
			classPath.clear();

		classpathMementoSpecified = true;
		closure.call();
	}

	boolean Check() {
		return !disable;
	}

	@Override
	public void registerTask(String name, TaskProvider<DefaultTask> task) {
		if (name.equals("createEclipseLaunches")) {
			task.get().setDescription("Generate the eclipse launch files");
			task.get().setGroup("IDE");
			task.get().onlyIf((task2) -> Check());
		}
	}

	public static enum EclipseLaunchType {
		LOCAL("org.eclipse.jdt.launching.localJavaApplication"),
		REMOTE("org.eclipse.jdt.launching.remoteJavaApplication");

		public String cls;

		EclipseLaunchType(String value) {
			cls = value;
		}
	}

	public class ClasspathMementoClosureOwner {
		public SourceType type = SourceType.archive;

		public final SourceType archive = SourceType.archive;
		public final SourceType internalArchive = SourceType.archive;
		public final SourceType externalArchive = SourceType.externalArchive;
		public final SourceType classpathContainer = SourceType.classpathContainer;
		public final SourceType classpathVariable = SourceType.classpathVariable;
		public final SourceType directory = SourceType.directory;
		public final SourceType externalDirectory = SourceType.directory;
		public final SourceType folder = SourceType.folder;
		public final SourceType internalDirectory = SourceType.folder;
		public final SourceType sourceFolder = SourceType.folder;
		public final SourceType javaProject = SourceType.javaProject;
		public final SourceType project = SourceType.javaProject;
		public final SourceType string = SourceType.variableString;
		public final SourceType variableString = SourceType.variableString;

		public void clear() {
			classPath.clear();
		}

		public void type(SourceType type) {
			this.type = type;
		}

		public void add(String[] values) {
			for (String value : values)
				add(value);
		}

		public void add(SourceType type, String value) {
			classPath.add(new ClasspathMemento(getProject(), type, value));
		}

		public void add(SourceType type, String... values) {
			for (String value : values)
				add(type, value);
		}

		public void add(String value) {
			classPath.add(new ClasspathMemento(getProject(), type, value));
		}

		public void dir(String file) {
			classPath.add(new ClasspathMemento(getProject(), SourceType.folder, file));
		}

		public void dir(String... files) {
			for (String file : files) {
				archive(file);
			}
		}

		public void archive(String file) {
			classPath.add(new ClasspathMemento(getProject(), SourceType.archive, file));
		}

		public void archive(File file) {
			classPath.add(new ClasspathMemento(getProject(), file));
		}

		public void archive(String... files) {
			for (String file : files) {
				archive(file);
			}
		}

		public void archive(File... files) {
			for (File file : files) {
				archive(file);
			}
		}

		public void archive(Iterable<File> files) {
			for (File file : files) {
				archive(file);
			}
		}

		public void archive(File file, File source) {
			classPath.add(new ClasspathMemento(getProject(), file, source));
		}

		public void archive(String file, String source) {
			classPath.add(new ClasspathMemento(getProject(), file, source));
		}

		public void variable(String variable) {
			classPath.add(new ClasspathMemento(getProject(), SourceType.classpathVariable, variable));
		}

		public void variable(String... variables) {
			for (String variable : variables) {
				variable(variable);
			}
		}

		public void string(String variable) {
			classPath.add(new ClasspathMemento(getProject(), SourceType.variableString, variable));
		}

		public void string(String... variables) {
			for (String variable : variables) {
				variable(variable);
			}
		}

		public void add(File[] files) {
			for (File file : files) {
				classPath.add(new ClasspathMemento(getProject(), file));
			}
		}

		public void add(File file) {
			classPath.add(new ClasspathMemento(getProject(), file));
		}

		public void add(Iterable<Project> projects) {
			for (Project project : projects) {
				classPath.add(new ClasspathMemento(project));
				sourceLookup.add(new SourceMemento(project));
			}
		}

		public void add(Map<String, Project> projects) {
			for (Project project : projects.values()) {
				classPath.add(new ClasspathMemento(project));
				sourceLookup.add(new SourceMemento(project));
			}
		}

		public void add(Project[] projects) {
			for (Project project : projects) {
				classPath.add(new ClasspathMemento(project));
				sourceLookup.add(new SourceMemento(project));
			}
		}

		public void add(Project project) {
			classPath.add(new ClasspathMemento(project));
			sourceLookup.add(new SourceMemento(project));
		}

		public void proj(Iterable<Project> projects) {
			for (Project project : projects) {
				classPath.add(new ClasspathMemento(project));
				sourceLookup.add(new SourceMemento(project));
			}
		}

		public void proj(Map<String, Project> projects) {
			for (Project project : projects.values()) {
				classPath.add(new ClasspathMemento(project));
				sourceLookup.add(new SourceMemento(project));
			}
		}

		public void proj(Project... projects) {
			for (Project project : projects) {
				classPath.add(new ClasspathMemento(project));
				sourceLookup.add(new SourceMemento(project));
			}
		}

		public void proj(Project project) {
			classPath.add(new ClasspathMemento(project));
			sourceLookup.add(new SourceMemento(project));
		}
	}

	public class ClasspathMemento {
		public SourceType type = SourceType.archive;
		public PathPriority priority = PathPriority.CLASSPATH;

		public String path = "";
		public String source = "";

		public ClasspathMemento() {
			type = SourceType.__default;
		}

		public ClasspathMemento(Project target, File archive) {
			loadArchive(target, archive, null);
		}

		public ClasspathMemento(Project target, File archive, File source) {
			loadArchive(target, archive, source);
		}

		public ClasspathMemento(Project target, String archive, String source) {
			type = SourceType.archive;
			loadArchive(target, new File(archive), new File(source));
		}

		private void loadArchive(Project target, File archive, File source) {
			if ((archive.isAbsolute() && target.getProjectDir().toPath().relativize(archive.toPath()).startsWith(".."))
					|| archive.getPath().startsWith("..")) {
				try {
					path = archive.getCanonicalPath();
				} catch (IOException e) {
					path = archive.getAbsolutePath();
				}
				if (archive.isDirectory())
					type = SourceType.directory;
				else
					type = SourceType.externalArchive;
			} else {
				if (archive.isAbsolute())
					path = "/" + target.getName() + "/"
							+ target.getProjectDir().toPath().relativize(archive.toPath()).toString();
				else
					path = "/" + target.getName() + "/" + archive.getPath();
				if (archive.isDirectory())
					type = SourceType.folder;
				else
					type = SourceType.archive;
			}
			if (source != null) {
				if ((source.isAbsolute()
						&& target.getProjectDir().toPath().relativize(source.toPath()).startsWith(".."))
						|| source.getPath().startsWith("..")) {
					try {
						this.source = source.getCanonicalPath();
					} catch (IOException e) {
						this.source = source.getAbsolutePath();
					}
				} else {
					if (source.isAbsolute())
						this.source = "/" + source.getName() + "/"
								+ target.getProjectDir().toPath().relativize(source.toPath()).toString();
					else
						this.source = "/" + source.getName() + "/" + source.getPath();
				}
			}
		}

		public ClasspathMemento(Project target, SourceType type, String path) {
			this.type = type;
			this.path = path;
			if (type == SourceType.__default) {
				throw new IllegalArgumentException("Default source type does not exist for the classpath");
			}
			if (type != SourceType.classpathContainer && type != SourceType.classpathVariable) {
				File archive = new File(path);
				loadArchive(target, archive, null);
			}
		}

		public ClasspathMemento(Project target) {
			type = SourceType.javaProject;
			path = target.getName();
		}

		public Element createMemento(Project target, Document root, DocumentBuilder builder) {
			try {
				Document dom = builder.newDocument();

				String nodeName = null;
				String mementoName = null;
				boolean overrideToCls = false;

				switch (type) {
				case folder:
				case archive:
					nodeName = "internalArchive";
					break;
				case directory:
				case externalArchive:
					nodeName = "externalArchive";
					break;
				case javaProject:
					nodeName = "projectName";
					break;
				case classpathVariable:
				case classpathContainer:
					nodeName = "containerPath";
					break;
				case variableString:
					nodeName = "id";
					overrideToCls = true;
					mementoName = "variableString";
					break;
				default:
					break;
				}

				if ((source == null || source.isEmpty())
						&& (nodeName.equals("internalArchive") || nodeName.equals("externalArchive"))) {
					File file = new File(path);
					String name = file.getName();
					if (name.endsWith(".jar"))
						name = name.substring(0, name.lastIndexOf(".jar"));
					else if (name.endsWith(".zip"))
						name = name.substring(0, name.lastIndexOf(".zip"));

					File alternate = new File(file.getParentFile(), name + "-sources.jar");
					if (!alternate.exists()) {
						alternate = new File(file.getParentFile(), name + "-src.jar");
						if (!alternate.exists()) {
							alternate = new File(file.getParentFile(), name + "-Sources.jar");
							if (!alternate.exists()) {
								alternate = new File(file.getParentFile(), name + "-Src.jar");
								if (!alternate.exists()) {
									if (name.contains("-"))
										name = name.substring(name.lastIndexOf("-"));

									alternate = new File(file.getParentFile(), name + "-sources.jar");
									if (!alternate.exists()) {
										alternate = new File(file.getParentFile(), name + "-src.jar");
										if (!alternate.exists()) {
											alternate = new File(file.getParentFile(), name + "-Sources.jar");
											if (!alternate.exists()) {
												alternate = new File(file.getParentFile(), name + "-Src.jar");
											}
										}
									}
								}
							}
						}
					}
					if (alternate.exists()) {
						try {
							source = alternate.getCanonicalPath();
						} catch (IOException e) {
							source = alternate.getAbsolutePath();
						}
						alternate = new File(source);
						if (!target.getProjectDir().toPath().relativize(alternate.toPath()).startsWith("..")) {
							source = target.getProjectDir().toPath().relativize(alternate.toPath()).toString();
						}
					}
				}

				Element runtimeClasspathEntry = dom.createElement("runtimeClasspathEntry");
				runtimeClasspathEntry.setAttribute(nodeName, (!overrideToCls ? path : type.cls));
				if (overrideToCls) {
					Element memento = dom.createElement("memento");
					memento.setAttribute("path", Integer.toString(priority.value));
					memento.setAttribute(mementoName, nodeName);
					if (!source.isEmpty()) {
						memento.setAttribute("sourceAttachmentPath", source);
						memento.setAttribute("sourceRootPath", "");
					}
					runtimeClasspathEntry.appendChild(memento);
				} else {
					runtimeClasspathEntry.setAttribute("type", Integer.toString(type.value.value));
					runtimeClasspathEntry.setAttribute("path", Integer.toString(priority.value));
					if (!source.isEmpty()) {
						runtimeClasspathEntry.setAttribute("sourceAttachmentPath", source);
						runtimeClasspathEntry.setAttribute("sourceRootPath", "");
					}
				}

				dom.appendChild(runtimeClasspathEntry);
				Transformer tr = TransformerFactory.newInstance().newTransformer();
				tr.setOutputProperty(OutputKeys.INDENT, "yes");
				tr.setOutputProperty(OutputKeys.VERSION, "1.0");
				tr.setOutputProperty(OutputKeys.METHOD, "xml");
				tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
				tr.setOutputProperty(OutputKeys.STANDALONE, "no");

				StringWriter strOut = new StringWriter();
				tr.transform(new DOMSource(dom), new StreamResult(strOut));
				strOut.close();

				Element memento = root.createElement("listEntry");
				memento.setAttribute("value", strOut.toString());
				return memento;
			} catch (TransformerException | IOException e) {
			}
			return null;
		}
	}

	public class SourceMementoClosureOwner {
		public SourceType type = SourceType.archive;

		public final SourceType archive = SourceType.archive;
		public final SourceType internalArchive = SourceType.archive;
		public final SourceType externalArchive = SourceType.externalArchive;
		public final SourceType classpathContainer = SourceType.classpathContainer;
		public final SourceType classpathVariable = SourceType.classpathVariable;
		public final SourceType directory = SourceType.directory;
		public final SourceType externalDirectory = SourceType.directory;
		public final SourceType folder = SourceType.folder;
		public final SourceType internalDirectory = SourceType.folder;
		public final SourceType sourceFolder = SourceType.folder;
		public final SourceType javaProject = SourceType.javaProject;
		public final SourceType project = SourceType.javaProject;
		public final SourceType defaultSource = SourceType.__default;

		public void clear() {
			sourceLookup.clear();
		}

		public void defaultJRE() {
			add(classpathContainer, "org.eclipse.jdt.launching.JRE_CONTAINER");
		}

		public void type(SourceType type) {
			this.type = type;
		}

		public void add(String[] values) {
			for (String value : values)
				add(value);
		}

		public void add(SourceType type, String value) {
			sourceLookup.add(new SourceMemento(getProject(), type, value));
		}

		public void add(SourceType type, String... values) {
			for (String value : values)
				add(type, value);
		}

		public void add(String value) {
			sourceLookup.add(new SourceMemento(getProject(), type, value));
		}

		public void dir(String file) {
			sourceLookup.add(new SourceMemento(getProject(), SourceType.folder, file));
		}

		public void dir(String... files) {
			for (String file : files) {
				archive(file);
			}
		}

		public void archive(String file) {
			sourceLookup.add(new SourceMemento(getProject(), SourceType.archive, file));
		}

		public void archive(String... files) {
			for (String file : files) {
				archive(file);
			}
		}

		public void archive(File file) {
			sourceLookup.add(new SourceMemento(getProject(), file));
		}

		public void archive(File... files) {
			for (File file : files) {
				archive(file);
			}
		}

		public void archive(Iterable<File> files) {
			for (File file : files) {
				archive(file);
			}
		}

		public void variable(String variable) {
			sourceLookup.add(new SourceMemento(getProject(), SourceType.classpathVariable, variable));
		}

		public void variable(String... variables) {
			for (String variable : variables) {
				variable(variable);
			}
		}

		public void addDefault() {
			sourceLookup.add(new SourceMemento());
		}

		public void add(File[] files) {
			for (File file : files) {
				sourceLookup.add(new SourceMemento(getProject(), file));
			}
		}

		public void add(File file) {
			sourceLookup.add(new SourceMemento(getProject(), file));
		}

		public void add(Iterable<Project> projects) {
			for (Project project : projects) {
				sourceLookup.add(new SourceMemento(project));
			}
		}

		public void add(Map<String, Project> projects) {
			for (Project project : projects.values()) {
				sourceLookup.add(new SourceMemento(project));
			}
		}

		public void add(Project... projects) {
			for (Project project : projects) {
				sourceLookup.add(new SourceMemento(project));
			}
		}

		public void add(Project project) {
			sourceLookup.add(new SourceMemento(project));
		}

		public void proj(Iterable<Project> projects) {
			for (Project project : projects) {
				sourceLookup.add(new SourceMemento(project));
			}
		}

		public void proj(Map<String, Project> projects) {
			for (Project project : projects.values()) {
				sourceLookup.add(new SourceMemento(project));
			}
		}

		public void proj(Project... projects) {
			for (Project project : projects) {
				sourceLookup.add(new SourceMemento(project));
			}
		}

		public void proj(Project project) {
			sourceLookup.add(new SourceMemento(project));
		}
	}

	public static class SourceMemento {
		public static String generateMemento(DocumentBuilder builder, SourceMemento... sources) {
			try {
				Document dom = builder.newDocument();

				Element sourceLookupDirector = dom.createElement("sourceLookupDirector");
				Element sourceContainers = dom.createElement("sourceContainers");
				sourceContainers.setAttribute("duplicates", "false");

				for (SourceMemento memento : sources) {
					sourceContainers.appendChild(memento.createMemento(dom, builder));
				}

				sourceLookupDirector.appendChild(sourceContainers);
				dom.appendChild(sourceLookupDirector);

				Transformer tr = TransformerFactory.newInstance().newTransformer();
				tr.setOutputProperty(OutputKeys.INDENT, "yes");
				tr.setOutputProperty(OutputKeys.VERSION, "1.0");
				tr.setOutputProperty(OutputKeys.METHOD, "xml");
				tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
				tr.setOutputProperty(OutputKeys.STANDALONE, "no");

				StringWriter strOut = new StringWriter();
				tr.transform(new DOMSource(dom), new StreamResult(strOut));
				strOut.close();
				String str = strOut.toString();
				return str;
			} catch (IOException | TransformerException e) {
			}
			return null;
		}

		public SourceType type = SourceType.archive;
		public String path = "";

		public SourceMemento() {
			type = SourceType.__default;
		}

		public SourceMemento(Project target, File archive) {
			loadArchive(target, archive);
		}

		private void loadArchive(Project target, File archive) {
			if ((archive.isAbsolute() && target.getProjectDir().toPath().relativize(archive.toPath()).startsWith(".."))
					|| archive.getPath().startsWith("..")) {
				try {
					path = archive.getCanonicalPath();
				} catch (IOException e) {
					path = archive.getAbsolutePath();
				}
				if (archive.isDirectory())
					type = SourceType.directory;
				else
					type = SourceType.externalArchive;
			} else {
				if (archive.isAbsolute()) {
					path = "/" + target.getName() + "/"
							+ target.getProjectDir().toPath().relativize(archive.toPath()).toString();
				} else {
					path = "/" + target.getName() + "/" + archive.getPath();
				}
				if (archive.isDirectory())
					type = SourceType.folder;
				else
					type = SourceType.archive;
			}
		}

		public SourceMemento(Project target, SourceType type, String path) {
			this.type = type;
			this.path = path;
			if (type == SourceType.variableString) {
				throw new IllegalArgumentException(
						"Source type 'variableString' does not exist for the source lookup configuration");
			}
			if (type != SourceType.classpathContainer && type != SourceType.classpathVariable) {
				File archive = new File(path);
				loadArchive(target, archive);
			}
		}

		public SourceMemento(Project target) {
			type = SourceType.javaProject;
			path = target.getName();
		}

		public Element createMemento(Document root, DocumentBuilder builder) {
			try {
				Document dom = builder.newDocument();
				Element element = dom.createElement(type.valueStr);

				boolean detectRoot = false;
				String propName;
				switch (type) {
				case __default:
					propName = null;
					detectRoot = false;
					break;
				case javaProject:
					propName = "name";
					detectRoot = false;
					break;
				case archive:
				case externalArchive:
					propName = "path";
					detectRoot = true;
					break;
				default:
					detectRoot = false;
					propName = "path";
					break;
				}

				if (detectRoot) {
					element.setAttribute("detectRoot", "true");
				}
				if (propName != null) {
					element.setAttribute(propName, path);
				}

				dom.appendChild(element);
				Transformer tr = TransformerFactory.newInstance().newTransformer();
				tr.setOutputProperty(OutputKeys.INDENT, "yes");
				tr.setOutputProperty(OutputKeys.VERSION, "1.0");
				tr.setOutputProperty(OutputKeys.METHOD, "xml");
				tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
				tr.setOutputProperty(OutputKeys.STANDALONE, "no");

				StringWriter strOut = new StringWriter();
				tr.transform(new DOMSource(dom), new StreamResult(strOut));
				strOut.close();

				Element memento = root.createElement("container");
				memento.setAttribute("memento", strOut.toString());
				memento.setAttribute("typeId", type.cls);
				return memento;
			} catch (TransformerException | IOException e) {
			}
			return null;
		}
	}

	public static class ConfigurationType {
		public static final EclipseLaunchType local = EclipseLaunchType.LOCAL;
		public static final EclipseLaunchType remote = EclipseLaunchType.REMOTE;

		public EclipseLaunchType type = EclipseLaunchType.LOCAL;

		public String hostname = "localhost";
		public int port = 5005;
		public boolean allowTerminate = false;

		public void type(EclipseLaunchType type) {
			this.type = type;
		}

		public void allowTerminate() {
			allowTerminate = true;
		}

		public void allowTerminate(boolean allowTerminate) {
			this.allowTerminate = allowTerminate;
		}

		public void hostname(String hostname) {
			this.hostname = hostname;
		}

		public void port(int port) {
			this.port = port;
		}
	}

}
