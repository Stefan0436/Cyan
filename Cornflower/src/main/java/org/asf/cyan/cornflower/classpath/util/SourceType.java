package org.asf.cyan.cornflower.classpath.util;

public enum SourceType {
	__default(null, "default", "org.eclipse.debug.core.containerType.default"),
	javaProject(EntryType.PROJECT, "javaProject", "org.eclipse.jdt.launching.sourceContainer.javaProject"),
	archive(EntryType.ARCHIVE, "archive", "org.eclipse.debug.core.containerType.archive"),
	externalArchive(EntryType.ARCHIVE, "archive", "org.eclipse.debug.core.containerType.externalArchive"),
	directory(EntryType.ARCHIVE, "directory", "org.eclipse.debug.core.containerType.directory"),
	folder(EntryType.ARCHIVE, "folder", "org.eclipse.debug.core.containerTyper.folder"),
	classpathVariable(EntryType.VARIABLE, "classpathVariable", "org.eclipse.jdt.launching.sourceContainer.classpathVariable"),
	classpathContainer(EntryType.CONTAINER, "classpathContainer", "org.eclipse.jdt.launching.sourceContainer.classpathContainer"),
	
	/**
	 * Not intended for source jars, classpath only
	 */
	variableString(null, null, "org.eclipse.jdt.launching.classpathentry.variableClasspathEntry");
	
	public String valueStr;
	public EntryType value;
	public String cls; 
	SourceType(EntryType value, String valueStr, String cls) {
		this.value = value;
		this.cls = cls;
		this.valueStr = valueStr;
	}
	
	@Override
	public String toString() {
		return valueStr;
	}
}
