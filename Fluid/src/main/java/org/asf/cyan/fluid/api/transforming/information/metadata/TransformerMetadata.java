package org.asf.cyan.fluid.api.transforming.information.metadata;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.reports.ReportBuilder;
import org.asf.cyan.api.reports.ReportCategory;
import org.asf.cyan.api.reports.ReportNode;
import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.Transformer;
import org.asf.cyan.fluid.Transformer.AnnotationInfo;
import org.asf.cyan.fluid.Transformer.FluidMethodInfo;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.Reflect;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.MemberType;
import org.asf.cyan.fluid.bytecode.BytecodeExporter;
import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.asf.cyan.fluid.remapping.MAPTYPE;
import org.asf.cyan.fluid.remapping.Mapping;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * 
 * Transformer metadata, contains information about loaded transformers.<br/>
 * <b>Warning:</b> this class needs to have an implementation in order to work.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class TransformerMetadata extends CyanComponent {
	private static TransformerMetadata selectedImplementation;

	public static TransformerMetadata getImplementationInstance() {
		return selectedImplementation;
	}

	protected TransformerMetadata() {
	}

	public abstract String getTransfomerOwner();

	public abstract String getTransfomerClass();

	public abstract String getTargetClass();

	public abstract String getMappedTargetClass();

	public abstract MemberMetadata[] getTransformedFields();

	public abstract MemberMetadata[] getTransformedMethods();

	protected abstract String getImplementationName();

	protected abstract TransformerMetadata getNewInstance();

	protected abstract void storeInstance(TransformerMetadata metadata);

	protected abstract boolean validateNewInstane(TransformerMetadata metadata);

	protected abstract TransformerMetadata[] internalGetLoadedTransformers();

	protected abstract TransformerMetadata getMetadataByTransformer(String transformerName);

	protected abstract TransformerMetadata[] getMetadataByObfusTarget(String targetObfus);

	protected abstract TransformerMetadata[] getMetadataByDeobfTarget(String targetDeobf);

	protected abstract String toDesc(String type, String[] types);

	protected abstract MemberMetadata parseMethod(String owner, String transformerMemberName, String name, String desc,
			String[] types, String returnType, int oldModifier, int newModifier, boolean isNew);

	protected abstract MemberMetadata parseField(String owner, String transformerMemberName, String name, String type,
			int oldModifier, int newModifier, boolean isNew);

	protected abstract void storeMember(MemberMetadata metadata);

	protected abstract String[] parseParams(String descriptor);

	protected abstract String parseType(String descriptor);

	protected abstract String mapClass(String className);

	protected abstract String mapMethod(String className, String methodName, String[] types);

	protected abstract String mapField(String className, String fieldName, String type);

	protected abstract FluidClassPool getClassPool();

	protected abstract void assignTransformer(TransformerMetadata data, String transformerName, String owner,
			String target, String mappedTarget, FluidClassPool pool);

	/**
	 * Should return the following: 0 = name, 1 = descriptor, 2 = owner, 3 =
	 * internal member name and descriptor, 4 = old modifier, 5 = new modifier, 6 =
	 * is new (boolean)
	 */
	protected abstract String[] parseIdentifier(String identifier, String owner);

	protected String getTransformerTarget(ClassNode transformer) {
		Transformer.AnnotationInfo anno = Transformer.AnnotationInfo.getAnnotation(TargetClass.class, transformer);
		if (anno != null)
			return anno.get("target");

		return null;
	}

	public static TransformerMetadata[] getMetadataByTarget(String targetClass) {
		ArrayList<TransformerMetadata> items = new ArrayList<TransformerMetadata>();
		for (TransformerMetadata md : selectedImplementation
				.getMetadataByObfusTarget(targetClass.replaceAll("/", "."))) {
			if (!items.stream().anyMatch(t -> t.getTransfomerClass().equals(md.getTransfomerClass()))) {
				items.add(md);
			}
		}
		for (TransformerMetadata md : selectedImplementation
				.getMetadataByDeobfTarget(targetClass.replaceAll("/", "."))) {
			if (!items.stream().anyMatch(t -> t.getTransfomerClass().equals(md.getTransfomerClass()))) {
				items.add(md);
			}
		}
		return items.toArray(t -> new TransformerMetadata[t]);
	}

	public static TransformerMetadata getMetadataByTransformerClass(String transformer) {
		return selectedImplementation.getMetadataByTransformer(transformer.replaceAll("/", "."));
	}

	public static TransformerMetadata createMetadata(ClassNode transformerNode, String transformerOwner,
			ArrayList<String> transformedFields, ArrayList<String> transformedMethods, FluidClassPool pool) {
		TransformerMetadata data = selectedImplementation.getNewInstance();

		String target = selectedImplementation.getTransformerTarget(transformerNode);
		String mappedTarget = selectedImplementation.mapClass(target);

		for (String identifier : transformedMethods) {
			String[] info = selectedImplementation.parseIdentifier(identifier, target);
			String name = info[0];
			String desc = info[1];
			String owner = info[2];
			String memNameInternal = info[3];
			String memDesc = "";
			if (memNameInternal.contains("&")) {
				memDesc = memNameInternal.substring(memNameInternal.indexOf("&") + 1);
				memNameInternal = memNameInternal.substring(0, memNameInternal.indexOf("&"));
			}

			int oldModifier = Integer.parseInt(info[4]);
			int newModifier = Integer.parseInt(info[5]);
			boolean isNew = Boolean.parseBoolean(info[6]);

			String[] types = selectedImplementation.parseParams(desc);
			String type = selectedImplementation.parseType(desc);

			MemberMetadata member = data.parseMethod(owner, memNameInternal, name, memDesc, types, type, oldModifier,
					newModifier, isNew);
			data.storeMember(member);
		}

		for (String identifier : transformedFields) {
			String[] info = selectedImplementation.parseIdentifier(identifier, target);
			String name = info[0];
			String desc = info[1];
			String owner = info[2];
			String memNameInternal = info[3];
			int oldModifier = Integer.parseInt(info[4]);
			int newModifier = Integer.parseInt(info[5]);
			boolean isNew = Boolean.parseBoolean(info[6]);

			String type = selectedImplementation.parseType(desc);

			MemberMetadata member = data.parseField(owner, memNameInternal, name, type, oldModifier, newModifier,
					isNew);
			data.storeMember(member);
		}

		selectedImplementation.assignTransformer(data, transformerNode.name.replaceAll("/", "."), transformerOwner,
				target, mappedTarget, pool);

		if (!selectedImplementation.validateNewInstane(data))
			return null;

		selectedImplementation.storeInstance(data);
		return data;
	}

	public static String toDescriptor(String type, String[] types) {
		return selectedImplementation.toDesc(type, types);
	}

	public static TransformerMetadata[] getLoadedTransformers() {
		return selectedImplementation.internalGetLoadedTransformers();
	}

	public static void createStackTrace(StackTraceElement[] elements, Consumer<String> append,
			Consumer<String> appendIndented) {
		selectedImplementation.createStackTraceInternal(elements, append, appendIndented);
	}

	protected void createStackTraceInternal(StackTraceElement[] elements, Consumer<String> append,
			Consumer<String> appendIndented) {
		ArrayList<String> entries = new ArrayList<String>();
		ArrayList<String> entriesSecondary = new ArrayList<String>();

		for (StackTraceElement element : elements) {
			String name = element.getClassName();
			for (TransformerMetadata data : TransformerMetadata.getMetadataByTarget(name)) {
				String targetName = data.getMappedTargetClass();
				String tname2 = data.getTargetClass();
				if (tname2.contains("."))
					tname2 = tname2.substring(tname2.lastIndexOf(".") + 1);
				if (targetName.contains("."))
					targetName = targetName.substring(targetName.lastIndexOf(".") + 1);

				if (!data.getTargetClass().equals(data.getMappedTargetClass())) {
					targetName += " (aka " + tname2 + ")";
				}

				boolean match = false;
				for (MemberMetadata method : data.getTransformedMethods()) {
					if (method.getMappedName().equals(element.getMethodName())
							|| method.getName().equals(element.getMethodName())) {
						match = true;
						break;
					}
				}
				if (match)
					entries.add(
							"[" + data.getTransfomerOwner() + "] " + data.getTransfomerClass() + " -> " + targetName);
			}
		}

		for (StackTraceElement element : elements) {
			String name = element.getClassName();
			for (TransformerMetadata data : TransformerMetadata.getMetadataByTarget(name)) {
				String targetName = data.getMappedTargetClass();
				String tname2 = data.getTargetClass();
				if (tname2.contains("."))
					tname2 = tname2.substring(tname2.lastIndexOf(".") + 1);
				if (targetName.contains("."))
					targetName = targetName.substring(targetName.lastIndexOf(".") + 1);

				if (!data.getTargetClass().equals(data.getMappedTargetClass())) {
					targetName += " (aka " + tname2 + ")";
				}

				String entryStr = "[" + data.getTransfomerOwner() + "] " + data.getTransfomerClass() + " -> "
						+ targetName;
				if (!entries.contains(entryStr) && !entriesSecondary.contains(entryStr))
					entriesSecondary.add(entryStr);
			}
		}

		if (entries.size() != 0 || entriesSecondary.size() != 0) {
			int length = 0;
			for (String entry : entries) {
				String trInfo = entry.substring(0, entry.lastIndexOf(" -> "));
				if (trInfo.length() > length)
					length = trInfo.length();
			}

			if (entries.size() != 0)
				append.accept("The following transformers have most likely been called:");
			for (String entry : entries) {
				String trInfo = entry.substring(0, entry.lastIndexOf(" -> "));
				String trTarget = entry.substring(entry.lastIndexOf(" -> ") + 4);
				for (int i = trInfo.length(); i < length; i++)
					trInfo += " ";
				appendIndented.accept(trInfo + " -> " + trTarget);
			}

			if (entriesSecondary.size() != 0) {
				length = 0;
				for (String entry : entriesSecondary) {
					String trInfo = entry.substring(0, entry.lastIndexOf(" -> "));
					if (trInfo.length() > length)
						length = trInfo.length();
				}
				if (entries.size() != 0)
					append.accept(
							"The following transformers were also present, but might not have been called at all:");
				else
					append.accept("The following transformers were present, but might not have been called at all:");
				for (String entry : entriesSecondary) {
					String trInfo = entry.substring(0, entry.lastIndexOf(" -> "));
					String trTarget = entry.substring(entry.lastIndexOf(" -> ") + 4);
					for (int i = trInfo.length(); i < length; i++)
						trInfo += " ";

					appendIndented.accept(trInfo + " -> " + trTarget);
				}
			}
		}
	}

	public static void dumpErrorBacktrace(String message, StackTraceElement[] elements, File output)
			throws IOException {
		selectedImplementation.dumpErrorBacktraceInternal(message, elements, output,
				selectedImplementation.getClassPool());
	}

	public static void dumpBacktraceOnly(File output, Consumer<String> logDebug, Consumer<String> logInfo,
			Consumer<String> logWarn, BiConsumer<String, Throwable> logError) throws IOException {
		selectedImplementation.dumpBacktraceNoStacktrace(output, selectedImplementation.getClassPool(), false, logDebug,
				logInfo, logWarn, logError);
	}

	protected synchronized void dumpErrorBacktraceInternal(String message, StackTraceElement[] elements, File output,
			FluidClassPool pool) throws IOException {
		if (!output.exists()) {
			warn("Stack trace received... Dumping transformer backtrace... Message: " + message);
			output.mkdirs();
		}

		File classesDump = new File(output, "classes");
		boolean classesNew = !classesDump.exists();
		if (!classesDump.exists())
			classesDump.mkdirs();

		File metadataDump = new File(output, "metadata");
		if (!metadataDump.exists())
			metadataDump.mkdirs();

		StringBuilder errorReport = new StringBuilder();
		errorReport.append("Message:\n");
		errorReport.append("\t" + message + "\n");
		errorReport.append("Stacktrace:\n");
		for (StackTraceElement element : elements) {
			errorReport.append("\t").append("at ").append(element).append("\n");
		}

		errorReport.append("Transformers:").append("\n");
		createStackTrace(elements, t -> {
			errorReport.append("\t").append(t).append("\n");
		}, t -> {
			errorReport.append("\t- ").append(t).append("\n");
		});

		File errorReportFile = new File(output, "stacktrace.log");
		if (!errorReportFile.exists())
			Files.writeString(errorReportFile.toPath(), errorReport.toString());

		dumpBacktraceNoStacktrace(output, pool, classesNew, str -> debug(str), str -> warn(str), str -> warn(str),
				(str, e) -> error(str, e));
	}

	public void dumpBacktraceNoStacktrace(File output, FluidClassPool pool, boolean classesNew,
			Consumer<String> logDebug, Consumer<String> logInfo, Consumer<String> logWarn,
			BiConsumer<String, Throwable> logError) throws IOException {
		if (!output.exists()) {
			classesNew = true;
			output.mkdirs();
		}

		File classesDump = new File(output, "classes");
		File metadataDump = new File(output, "metadata");
		File loadedTransformers = new File(output, "loaded-transformers.txt");
		if (!loadedTransformers.exists()) {
			logInfo.accept("Dumping transformer list...");
			StringBuilder transformerData = new StringBuilder();
			transformerData.append("All loaded transformers:").append("\n");
			ArrayList<String> entries = new ArrayList<String>();
			for (TransformerMetadata data : getLoadedTransformers()) {
				String targetName = data.getMappedTargetClass();
				String tname2 = data.getTargetClass();
				if (tname2.contains("."))
					tname2 = tname2.substring(tname2.lastIndexOf(".") + 1);
				if (targetName.contains("."))
					targetName = targetName.substring(targetName.lastIndexOf(".") + 1);

				if (!data.getTargetClass().equals(data.getMappedTargetClass())) {
					targetName += " (aka " + tname2 + ")";
				}
				entries.add("[" + data.getTransfomerOwner() + "] " + data.getTransfomerClass() + " -> " + targetName);
			}
			int length = 0;
			for (String entry : entries) {
				String trInfo = entry.substring(0, entry.lastIndexOf(" -> "));
				if (trInfo.length() > length)
					length = trInfo.length();
			}
			for (String entry : entries) {
				String trInfo = entry.substring(0, entry.lastIndexOf(" -> "));
				String trTarget = entry.substring(entry.lastIndexOf(" -> ") + 4);
				for (int i = trInfo.length(); i < length; i++)
					trInfo += " ";
				transformerData.append("- " + trInfo + " -> " + trTarget).append("\n");
			}
			Files.writeString(loadedTransformers.toPath(), transformerData.toString());
		}

		File classesDumpTransformersPC = new File(classesDump, "transformers/pseudocode");
		if (!classesDumpTransformersPC.exists())
			classesDumpTransformersPC.mkdirs();

		File classesDumpProgramPC = new File(classesDump, "program/pseudocode");
		if (!classesDumpProgramPC.exists())
			classesDumpProgramPC.mkdirs();

		File classesDumpTransformersBIN = new File(classesDump, "transformers/bytecode");
		if (!classesDumpTransformersBIN.exists())
			classesDumpTransformersBIN.mkdirs();

		File classesDumpProgramBIN = new File(classesDump, "program/bytecode");
		if (!classesDumpProgramBIN.exists())
			classesDumpProgramBIN.mkdirs();

		for (TransformerMetadata md : getLoadedTransformers()) {
			try {
				dumpMetadata(md, metadataDump, logDebug);
			} catch (ClassNotFoundException | IOException e) {
				logError.accept("Transformer metadata dump failed, transformer: " + md.getTransfomerClass(), e);
			}
		}

		dumpMappings(Fluid.getMappings(), output, logInfo);
		File readme = new File(classesDump, "README, VERY IMPORTANT.txt");

		StringBuilder readmeTxt = new StringBuilder();
		readmeTxt.append("COPYTIGHT(c) AUTHORS OF PROGRAM AND AUTHORS OF TRANSFORMERS, DO NOT DISTRIBUTE!")
				.append(System.lineSeparator());
		readmeTxt.append("-------------------------------------------------------------------------------")
				.append(System.lineSeparator());
		readmeTxt.append("").append(System.lineSeparator());
		readmeTxt.append("The PSEUDOCODE and BYTECODE files in this directory and sub-directories are not")
				.append(System.lineSeparator());
		readmeTxt.append("to be distributed, they have been exported directy from the FLUID class pools")
				.append(System.lineSeparator());
		readmeTxt.append("and may ONLY be used for debugging purposes.").append(System.lineSeparator());
		readmeTxt.append("").append(System.lineSeparator());
		readmeTxt.append("You have the permission to transfer a copy of the files to the author of the")
				.append(System.lineSeparator());
		readmeTxt.append("transformers so they can fix the crash, the files may not be used for anything")
				.append(System.lineSeparator());
		readmeTxt.append("other than debugging purposes and must be destroyed afterwards.")
				.append(System.lineSeparator());

		if (!readme.exists()) {
			logDebug.accept("Creating readme file for backtrace...");
		}
		Files.writeString(readme.toPath(), readmeTxt.toString());
		if (classesNew) {
			logInfo.accept("Dumping transformer classes...");
			for (TransformerMetadata md : getLoadedTransformers()) {
				try {
					File trPseudoCode = new File(classesDumpTransformersPC,
							md.getTransfomerOwner().replace(":", ".") + "/" + md.getTransfomerClass() + ".pc.java");
					File trClassFile = new File(classesDumpTransformersBIN, md.getTransfomerOwner().replace(":", ".")
							+ "/" + md.getTransfomerClass().replace(".", "/") + ".class");

					if (!trPseudoCode.exists()) {
						trPseudoCode.getParentFile().mkdirs();
						trClassFile.getParentFile().mkdirs();
						logDebug.accept("Dumping transformer " + md.getTransfomerClass() + " PseudoCode...");
						Files.write(trPseudoCode.toPath(),
								BytecodeExporter.classToString(pool.getClassNode(md.getTransfomerClass())).getBytes());

						logDebug.accept("Dumping transformer " + md.getTransfomerClass() + " bytecode class file...");
						Files.write(trClassFile.toPath(), pool.getByteCode(md.getTransfomerClass()));
					}
				} catch (ClassNotFoundException | IOException e) {
					error("Transformer class dump failed, transformer: " + md.getTransfomerClass(), e);
				}
			}

			logInfo.accept("Dumping program class files...");
			for (TransformerMetadata md : getLoadedTransformers()) {
				try {
					File clPseudoCode = new File(classesDumpProgramPC,
							md.getTargetClass().replace(".", "/") + ".pc.java");
					File clClassFile = new File(classesDumpProgramBIN,
							md.getTargetClass().replace(".", "/") + ".class");

					if (!clPseudoCode.exists()) {
						clPseudoCode.getParentFile().mkdirs();
						clClassFile.getParentFile().mkdirs();
						logDebug.accept("Dumping class " + md.getTargetClass() + " PseudoCode...");
						ClassNode targetClass = null;
						try {
							targetClass = pool.getClassNode(md.getMappedTargetClass());
						} catch (ClassNotFoundException e) {
							targetClass = pool.getClassNode(md.getTargetClass());
						}
						Files.write(clPseudoCode.toPath(), BytecodeExporter.classToString(targetClass).getBytes());

						logDebug.accept("Dumping class " + md.getTargetClass() + " bytecode class file...");
						Files.write(clClassFile.toPath(), pool.getByteCode(targetClass.name));
					}
				} catch (ClassNotFoundException | IOException e) {
					logError.accept("Transformer class dump failed, transformer: " + md.getTargetClass(), e);
				}
			}
		}
	}

	protected synchronized void dumpMappings(Mapping<?>[] mappings, File outputDir, Consumer<String> logInfo)
			throws IOException {
		int index = 0;
		for (Mapping<?> root : mappings) {
			File output = new File(outputDir,
					"mappings.output" + (mappings.length == 1 ? "" : "." + (index + 1)) + ".txt");
			if (!output.exists()) {
				logInfo.accept("Dumping mappings class '" + root.getClass().getTypeName()
						+ "' in transformer backtrace... This can take a while...");
				StringBuilder strBuilder = new StringBuilder();
				StringBuilder head = new StringBuilder();
				head.append(
						"NOTICE: You may not distribute the following mappings, they have been created for debugging purposes ONLY.\nThe following statement(s) have been taken directly from FLUID's mappings cache, it does not change the above.");
				if (!root.getConfigHeader().isEmpty()) {
					head.append("\n");
					head.append("\n");
					head.append(root.getConfigHeader());
				}
				ReportBuilder builder = ReportBuilder.create(head.toString());
				ReportCategory headCategory = builder.newCategory("Summary");
				builder.newNode(headCategory, "HEAD").addAll("Mappings class", root.getClass().getTypeName(),
						"Mappings simple name", root.getClass().getSimpleName(), "Mappings count (all in root)",
						root.mappings.length);

				for (Mapping<?> clsMapping : root.mappings) {
					if (clsMapping.getMappingType() == MAPTYPE.CLASS) {
						String obfusNode = clsMapping.obfuscated;
						String pkg = "(default)";
						if (obfusNode.contains(".")) {
							pkg = obfusNode.substring(0, obfusNode.lastIndexOf("."));
							obfusNode = obfusNode.substring(obfusNode.lastIndexOf(".") + 1);
						}

						ReportCategory clsCat = builder.newCategory("Class: " + obfusNode);
						ReportNode cls = builder.newNode(clsCat, "Details");
						cls.add("Name", obfusNode);
						cls.add("Package", pkg);
						cls.add("Type name", clsMapping.obfuscated);
						cls.add("Target", clsMapping.name);

						ReportNode methods = builder.newNode(clsCat, "Methods");
						ReportNode fields = builder.newNode(clsCat, "Fields");

						int longest1 = 0;

						for (Mapping<?> memberMapping : clsMapping.mappings) {
							if (memberMapping.mappingType == MAPTYPE.METHOD) {
								String type = memberMapping.type;
								if (type.contains("."))
									type = type.substring(type.lastIndexOf(".") + 1);
								type = type.substring(0, 1).toUpperCase() + type.substring(1);
								if (type.length() > longest1)
									longest1 = type.length();
							} else if (memberMapping.mappingType == MAPTYPE.PROPERTY) {
								String type = memberMapping.type;
								if (type.contains("."))
									type = type.substring(type.lastIndexOf(".") + 1);
								type = type.substring(0, 1).toUpperCase() + type.substring(1);
								if (type.length() > longest1)
									longest1 = type.length();
							}
						}

						for (Mapping<?> memberMapping : clsMapping.mappings) {
							if (memberMapping.mappingType == MAPTYPE.METHOD) {
								String type = memberMapping.type;
								if (type.contains("."))
									type = type.substring(type.lastIndexOf(".") + 1);
								type = type.substring(0, 1).toUpperCase() + type.substring(1);

								for (int i = type.length(); i < longest1; i++)
									type += " ";

								boolean first = true;
								StringBuilder typesBuilder = new StringBuilder();
								for (String typeStr : memberMapping.argumentTypes) {
									if (!first)
										typesBuilder.append(", ");

									typesBuilder.append(typeStr);
									first = false;
								}

								methods.add(type + " " + memberMapping.obfuscated,
										memberMapping.name + "(" + typesBuilder.toString() + ")");
							} else if (memberMapping.mappingType == MAPTYPE.PROPERTY) {
								String type = memberMapping.type;
								if (type.contains("."))
									type = type.substring(type.lastIndexOf(".") + 1);
								type = type.substring(0, 1).toUpperCase() + type.substring(1);

								for (int i = type.length(); i < longest1; i++)
									type += " ";

								fields.add(type + " " + memberMapping.obfuscated, memberMapping.name);
							}
						}

						cls.add("Field count", fields.entries.size());
						cls.add("Method count", methods.entries.size());
					}
				}

				builder.build(strBuilder);
				if (!root.getConfigFooter().isEmpty()) {
					strBuilder.append("\n");
					strBuilder.append("\n");
					strBuilder.append(root.getConfigFooter());
				}
				Files.writeString(output.toPath(), strBuilder.toString());
				logInfo.accept(
						"Dumped mappings class '" + root.getClass().getTypeName() + "' in transformer backtrace.");
			}
			index++;
		}
	}

	protected synchronized void dumpMetadata(TransformerMetadata metadata, File output, Consumer<String> logDebug)
			throws IOException, ClassNotFoundException {
		File transformerDir = new File(output,
				metadata.getTransfomerOwner().replace(":", ".") + "/" + metadata.getTransfomerClass());
		if (!transformerDir.exists())
			transformerDir.mkdirs();

		File summaryFile = new File(output, metadata.getTransfomerClass() + ".tmd.txt");
		if (!summaryFile.exists()) {
			logDebug.accept(
					"Dumping transformer summary metadata file... Transformer: " + metadata.getTransfomerClass());
			Files.writeString(summaryFile.toPath(), selectedImplementation.buildSummary(metadata));
			logDebug.accept("Dumped transformer summary.");
		}

		ClassNode transformer = metadata.getClassPool().getClassNode(metadata.getTransfomerClass());
		ClassNode targetClass = null;
		try {
			targetClass = metadata.getClassPool().getClassNode(metadata.getMappedTargetClass());
		} catch (ClassNotFoundException e) {
			targetClass = metadata.getClassPool().getClassNode(metadata.getTargetClass());
		}

		dumpTransformerFieldInfo(metadata, new File(transformerDir, "fields"), transformer, targetClass);
		dumpTransformerMethodInfo(metadata, new File(transformerDir, "methods"), transformer, targetClass);

		File summaryFile2 = new File(transformerDir, metadata.getTransfomerClass() + ".tmd.txt");
		if (!summaryFile2.exists()) {
			logDebug.accept(
					"Dumping transformer summary metadata file... Transformer: " + metadata.getTransfomerClass());
			Files.writeString(summaryFile2.toPath(), selectedImplementation.buildSummary(metadata));
			logDebug.accept("Dumped transformer summary.");
		}
	}

	protected void dumpTransformerFieldInfo(TransformerMetadata metadata, File output, ClassNode transformer,
			ClassNode targetClass) throws IOException {
		if (output.exists())
			return;

		debug("Dumping transformer field metadata... Transformer: " + metadata.getTransfomerClass());
		output.mkdirs();
		for (MemberMetadata fieldData : metadata.getTransformedFields()) {
			File fieldOut = new File(output, fieldData.getTransformerMemberName() + ".tmd.txt");
			ReportBuilder builder = ReportBuilder.create("Tansformer Metadata Format 1.0,\n"
					+ "the following details are for a field named " + fieldData.getTransformerMemberName() + ".");

			String fieldModOld = Modifier.toString(fieldData.getOldModifier()).replaceAll(" ", ", ");
			String fieldModNew = Modifier.toString(fieldData.getNewModifier()).replaceAll(" ", ", ");

			FieldNode trFieldNode = transformer.fields.stream()
					.filter(t -> t.name.equals(fieldData.getTransformerMemberName())).findFirst().get();

			String transformMethod = "reflecting field (in-transformer usage)";
			if (!fieldModOld.equals(fieldModNew)) {
				transformMethod = "access transformer (changes field access)";
			}

			if (fieldData.isNew()) {
				transformMethod = "appending field (new field)";
			}

			ReportCategory basicInfo = builder.newCategory("Basic information");
			builder.newNode(basicInfo, "Details").addAll("Field name", fieldData.getTransformerMemberName(),
					"In-transformer modifiers", Modifier.toString(trFieldNode.access).replaceAll(" ", ", "),
					"Transform method", transformMethod);

			ReportNode nd = builder.newNode(basicInfo, "Target information").addAll("Target name", fieldData.getName(),
					"Target type", fieldData.getType(), "Target name (mapped)", fieldData.getMappedName(),
					"Target type (mapped)", fieldData.getMappedType(), "Target modifiers", fieldModOld);
			if (!fieldModOld.equals(fieldModNew)) {
				nd.add("New target modifiers", fieldModNew);
			}

			StringBuilder strBuilder = new StringBuilder();
			builder.build(strBuilder);
			Files.writeString(fieldOut.toPath(), strBuilder.toString());
		}

		debug("Dumped transformer metadata.");
	}

	protected void dumpTransformerMethodInfo(TransformerMetadata metadata, File output, ClassNode transformer,
			ClassNode targetClass) throws IOException {
		if (output.exists())
			return;

		debug("Dumping transformer method metadata... Transformer: " + metadata.getTransfomerClass());
		output.mkdirs();

		for (MemberMetadata methodData : metadata.getTransformedMethods()) {
			File methodOutput = new File(output, methodData.getTransformerMemberName() + ".tmd.txt");
			ReportBuilder builder = ReportBuilder.create("Tansformer Metadata Format 1.0,\n"
					+ "the following details are for a method named " + methodData.getTransformerMemberName() + ".");

			String mthModOld = Modifier.toString(methodData.getOldModifier()).replaceAll(" ", ", ");
			String mthModNew = Modifier.toString(methodData.getNewModifier()).replaceAll(" ", ", ");

			Optional<MethodNode> nodeOpt = transformer.methods.stream()
					.filter(t -> t.name.equals(methodData.getTransformerMemberName())
							&& t.desc.equals(methodData.toTransformerDescriptor()))
					.findFirst();
			MethodNode trMthNode = nodeOpt.get();

			String transformMethod = "unrecognized";
			if (methodData.isNew()) {
				transformMethod = "appending method (new method)";
			} else if (AnnotationInfo.isAnnotationPresent(InjectAt.class, trMthNode)) {
				transformMethod = "injecting method (modifies methods)";
			} else if (AnnotationInfo.isAnnotationPresent(Reflect.class, trMthNode)
					|| (Modifier.isAbstract(trMthNode.access) && !Modifier.isInterface(transformer.access))) {
				transformMethod = "reflecting method (in-transformer usage)";
			} else if (mthModOld.equals(mthModNew)) {
				transformMethod = "access transformer (changes method access)";
			}

			ReportCategory basicInfo = builder.newCategory("Basic information");
			ReportNode details = builder.newNode(basicInfo, "Details").addAll("Method name",
					methodData.getTransformerMemberName(), "In-transformer modifiers",
					Modifier.toString(trMthNode.access).replaceAll(" ", ", "), "Transform method", transformMethod,
					"Transformer return type", methodData.getType());

			FluidMethodInfo mInfo = FluidMethodInfo.create(trMthNode);

			if (AnnotationInfo.isAnnotationPresent(InjectAt.class, trMthNode)) {
				ReportNode target = builder.newNode(basicInfo, "Inject target metadata");

				AnnotationInfo info = AnnotationInfo.getAnnotation(InjectAt.class, trMthNode);
				target.add("Inject location", info.values.get("location"));

				if (info.values.containsKey("targetCall")) {
					target.add("Target call", info.values.get("targetCall"));
				}
				if (info.values.containsKey("targetOwner")) {
					target.add("Target owner", info.values.get("targetOwner"));
				}

				if (info.values.containsKey("offset")) {
					target.add("Inject offset", info.values.get("offset"));
				}
			}

			details.add("Parameter count", mInfo.types.length);

			if (mInfo.types.length != 0) {
				ReportNode params = builder.newNode(basicInfo, "Transformer parameters");

				int offset = (Modifier.isStatic(trMthNode.access) ? 0 : 1);
				for (int i = 0; i < mInfo.types.length; i++) {
					String type = mInfo.types[i];
					String name = "var" + i;
					if (trMthNode.localVariables != null && trMthNode.localVariables.size() > (offset + i)) {
						name = trMthNode.localVariables.get(offset + i).name;
					}

					for (AnnotationInfo anno : FluidMethodInfo.getParameterAnnotations(trMthNode, i)) {
						if (anno.name.equals("org.asf.cyan.fluid.api.transforming.LocalVariable")) {
							name += " (local variable reference)";
						}
					}

					params.add(name, type);
				}
			}
			if (trMthNode.localVariables != null && trMthNode.localVariables.size() != 0) {
				details.add("Local variable count", trMthNode.localVariables.size());

				ReportNode vars = builder.newNode(basicInfo, "Transformer local variables");
				int index = 0;
				int offset = (Modifier.isStatic(trMthNode.access) ? 0 : 1);
				for (LocalVariableNode var : trMthNode.localVariables) {
					if ((offset + mInfo.types.length) <= index) {
						String type = Fluid.parseDescriptor(var.desc);
						vars.add(var.name, "type: " + type + ", index: " + var.index);
					}
					index++;
				}
			}

			if (trMthNode.instructions != null && !Modifier.isAbstract(trMthNode.access)) {
				details.add("Instruction count", trMthNode.instructions.size());
			}

			ReportNode nd = builder.newNode(basicInfo, "Target information").addAll("Target name", methodData.getName(),
					"Target type", methodData.getType(), "Target name (mapped)", methodData.getMappedName(),
					"Target type (mapped)", methodData.getMappedType(), "Target descriptor", methodData.toDescriptor(),
					"Target descriptor (mapped)", methodData.toMappedDescriptor(), "Target modifiers", mthModOld);

			if (!mthModOld.equals(mthModNew)) {
				nd.add("New target modifiers", mthModNew);
			}

			StringBuilder strBuilder = new StringBuilder();
			builder.build(strBuilder);
			Files.writeString(methodOutput.toPath(), strBuilder.toString());
		}

		debug("Dumped transformer metadata.");
	}

	protected String buildSummary(TransformerMetadata metadata) {
		StringBuilder summary = new StringBuilder();

		String simpleName = metadata.getTransfomerClass();
		if (simpleName.contains("."))
			simpleName = simpleName.substring(simpleName.lastIndexOf(".") + 1);

		ReportBuilder builder = ReportBuilder.create("Tansformer Metadata Format 1.0,\n"
				+ "the following details are for the " + simpleName + " transformer.");

		ReportCategory basicInfo = builder.newCategory("Basic information");
		builder.newNode(basicInfo, "Details").addAll("Simple name", simpleName, "Fully qualified name",
				metadata.getTransfomerClass(), "Target class", metadata.getTargetClass(), "Mapped target class",
				metadata.getMappedTargetClass(), "Field count", metadata.getTransformedFields().length, "Method count",
				metadata.getTransformedMethods().length);

		ReportCategory trInfo = builder.newCategory("Transformer information");

		ArrayList<String> methods = new ArrayList<String>();
		int longestMethType = 0;

		for (MemberMetadata method : metadata.getTransformedMethods()) {
			if (method.getType().length() > longestMethType)
				longestMethType = method.getType().length();
		}

		for (MemberMetadata method : metadata.getTransformedMethods()) {
			StringBuilder methodBuilder = new StringBuilder();
			methodBuilder.append(method.getType().substring(0, 1).toUpperCase());
			methodBuilder.append(method.getType().substring(1));
			for (int i = method.getType().length(); i < longestMethType; i++) {
				methodBuilder.append(" ");
			}
			methodBuilder.append("  ");
			methodBuilder.append(method.getTransformerMemberName());
			methodBuilder.append("(");
			boolean first = true;
			for (String type : method.getTypes()) {
				if (!first)
					methodBuilder.append(", ");

				methodBuilder.append(type);

				first = false;
			}
			methodBuilder.append(")");

			methods.add(methodBuilder.toString());

			methodBuilder = new StringBuilder();
			methodBuilder.append(method.getMappedName());
			if (!method.getName().equals(method.getMappedName())) {
				methodBuilder.append(" (aka ");
				methodBuilder.append(method.getName());
				methodBuilder.append(")");
			}

			methods.add(methodBuilder.toString());
		}
		builder.newNode(trInfo, "Method transformers").addAll(methods.toArray());

		ArrayList<String> fields = new ArrayList<String>();
		int longestFieldType = 0;

		for (MemberMetadata field : metadata.getTransformedFields()) {
			if (field.getType().length() > longestFieldType)
				longestFieldType = field.getType().length();
		}
		for (MemberMetadata field : metadata.getTransformedFields()) {
			StringBuilder fieldBuilder = new StringBuilder();
			fieldBuilder.append(field.getType().substring(0, 1).toUpperCase());
			fieldBuilder.append(field.getType().substring(1));
			for (int i = field.getType().length(); i < longestFieldType; i++) {
				fieldBuilder.append(" ");
			}
			fieldBuilder.append("  ");
			fieldBuilder.append(field.getTransformerMemberName());

			fields.add(fieldBuilder.toString());
			fieldBuilder = new StringBuilder();
			fieldBuilder.append(field.getMappedName());
			if (!field.getName().equals(field.getMappedName())) {
				fieldBuilder.append(" (aka ");
				fieldBuilder.append(field.getName());
				fieldBuilder.append(")");
			}

			fields.add(fieldBuilder.toString());
		}
		builder.newNode(trInfo, "Field transformers").addAll(fields.toArray());

		builder.build(summary);

		return summary.toString();
	}

	protected static void setImplementation(TransformerMetadata implementation) {
		debug("Assigning FLUID Transformer Metadata Implementation... Using the "
				+ implementation.getImplementationName() + " Implementation...");
		selectedImplementation = implementation;
	}

	protected MemberMetadata constructMetadata() {
		return new MemberMetadata();
	}

	protected MemberMetadata assignMetadataValues(MemberMetadata data, MemberType memberType, String owner,
			String transformerMemberName, String name, String desc, String type, String[] params, int oldModifier,
			int newModifier, boolean isNew) {
		data.assign(memberType, transformerMemberName, owner, name, desc, type, params, oldModifier, newModifier,
				isNew);
		return data;
	}
}
