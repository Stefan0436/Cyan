package org.asf.cyan.fluid.implementation;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import org.asf.aos.util.service.extra.slib.util.ArrayUtil;
import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.Transformer;
import org.asf.cyan.fluid.api.transforming.ASM;
import org.asf.cyan.fluid.api.transforming.Constructor;
import org.asf.cyan.fluid.api.transforming.Erase;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.LocalVariable;
import org.asf.cyan.fluid.api.transforming.Modifiers;
import org.asf.cyan.fluid.api.transforming.Reflect;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetName;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;
import org.asf.cyan.fluid.api.transforming.information.metadata.TransformerMetadata;
import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.asf.cyan.fluid.bytecode.UnrecognizedEnumInfo;
import org.asf.cyan.fluid.remapping.MAPTYPE;
import org.asf.cyan.fluid.remapping.Mapping;

/**
 * 
 * Cyan implementation of the FLUID modification engine.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@CYAN_COMPONENT
public class CyanTransformer extends Transformer {

	//
	//
	// Basic getters and init handler
	//

	protected static void initComponent() {
		Transformer.setImplementation(new CyanTransformer());
	}

	protected static String getMarker() {
		return "FLUID";
	}

	@Override
	protected String getImplementationName() {
		return "Cyan";
	}

	@Override
	protected String getDeobfNameInternal(String clName) {
		for (Mapping<?> map : Fluid.getMappings()) {
			for (Mapping<?> mp : map.mappings) {
				if (mp.obfuscated.equals(clName)) {
					return mp.name;
				}
			}
		}
		return clName;
	}

	//
	//
	// FLUID implementation
	//

	protected synchronized void transformInternal(ClassNode cls, HashMap<String, String> transformerOwners,
			HashMap<String, ArrayList<ClassNode>> transformers, String clName, String loadingName, FluidClassPool pool,
			FluidClassPool transformerPool) {

		if (!transformers.containsKey(loadingName))
			return;

		ArrayList<ClassNode> arr = transformers.get(loadingName);
		int transformerIndex = 0;
		for (ClassNode transformerNode : arr) {
			String typeName = transformerNode.name.replaceAll("/", ".");
			try {
				runTransformer(cls, transformerNode, typeName, loadingName, clName, pool, transformerPool,
						transformerOwners, transformerIndex, arr, transformers);
				transformerIndex++;
			} catch (Exception ex) {
				fatal("FLUID transformation failed! Transformer: " + typeName, ex);
				File output = new File("transformer-backtrace");
				try {
					TransformerMetadata.dumpErrorBacktrace(ex.getClass().getTypeName() + ": " + ex.getMessage(),
							ex.getStackTrace(), output);
				} catch (Exception e) {
					error("Could not dump FLUID transformer metadata, an exception was thrown.", e);
				}
				throw new RuntimeException(ex);
			}
		}
	}

	private void runTransformer(ClassNode cls, ClassNode transformerNode, String typeName, String loadingName,
			String clName, FluidClassPool pool, FluidClassPool transformerPool,
			HashMap<String, String> transformerOwners, int transformerIndex, ArrayList<ClassNode> arr,
			HashMap<String, ArrayList<ClassNode>> transformers) {
		debug("Applying transformer " + transformerNode.name.replaceAll("/", ".") + " to class "
				+ Fluid.mapClass(loadingName.replaceAll("/", ".")));

		boolean asmMethods = false;
		ArrayList<String> transformedMethods = new ArrayList<String>();
		ArrayList<String> transformedFields = new ArrayList<String>();

		if (Modifier.isInterface(transformerNode.access)) {
			if (AnnotationInfo.isAnnotationPresent(Modifiers.class, transformerNode)) {
				transformerNode.access = Modifier.INTERFACE | AnnotationInfo
						.getAnnotation(Modifiers.class, transformerNode).<Integer>get("modifiers").intValue();
			}
			for (MethodNode method : transformerNode.methods) {
				boolean found = false;
				ArrayList<String> types = new ArrayList<String>();

				FluidMethodInfo info = FluidMethodInfo.create(method);

				String returnType = info.returnType;
				if (AnnotationInfo.isAnnotationPresent(TargetClass.class, method)) {
					returnType = AnnotationInfo.getAnnotation(TargetType.class, method).get("target");
					returnType = Fluid.mapClass(returnType);
				}

				int index = 0;
				for (String type : info.types) {
					String typePath = type;
					for (AnnotationInfo annotation : FluidMethodInfo.getParameterAnnotations(method, index)) {
						if (annotation.is(TargetType.class)) {
							typePath = annotation.get("target");
							typePath = Fluid.mapClass(typePath);
						}
					}
					types.add(typePath);
					index++;
				}

				String methName = Fluid.mapMethod(clName, method.name, types.toArray(new String[types.size()]));

				if (AnnotationInfo.isAnnotationPresent(Constructor.class, method)) {
					methName = AnnotationInfo.getAnnotation(Constructor.class, method).get("clinit", false) ? "<clinit>"
							: "<init>";
				}

				int newMod = method.access;
				newMod = newMod - Modifier.ABSTRACT;
				if (AnnotationInfo.isAnnotationPresent(Modifiers.class, method)) {
					newMod = AnnotationInfo.getAnnotation(Modifiers.class, method).get("modifiers");
				}
				int oldMod = newMod;

				for (MethodNode methodNode : cls.methods) {
					String desc = methodNode.desc;

					String mTypesStr = desc.substring(1, desc.lastIndexOf(")"));
					String mReturnType = Fluid.parseDescriptor(desc.substring(desc.lastIndexOf(")") + 1));

					String[] mTypes = Fluid.parseMultipleDescriptors(mTypesStr);

					if (methodNode.name.equals(methName)
							&& Arrays.equals(mTypes, types.toArray(new String[types.size()]))) {
						debug("Transforming method: " + method.name + "...");

						if (methodNode.name.equals(method.name)
								|| AnnotationInfo.isAnnotationPresent(Constructor.class, method)) {
							methodNode.access = newMod;
							oldMod = methodNode.access;
							found = true;
							break;
						} else {
							MethodNode newmethod = new MethodNode();
							InsnList instructions = new InsnList();
							for (int i = 0; i <= mTypes.length; i++) {
								instructions.add(new VarInsnNode(Opcodes.ALOAD, i));
							}
							instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, cls.name, methodNode.name,
									methodNode.desc));
							if (!mReturnType.equals("void"))
								instructions.add(new InsnNode(Opcodes.ARETURN));
							else
								instructions.add(new InsnNode(Opcodes.RETURN));
							newmethod.name = method.name;
							newmethod.maxLocals = mTypes.length + (Modifier.isStatic(newMod) ? 0 : 1);
							newmethod.instructions = instructions;
							newmethod.maxStack = methodNode.maxStack;
							newmethod.access = newMod;
							newmethod.desc = methodNode.desc;
							newmethod.exceptions = methodNode.exceptions;
							cls.methods.add(newmethod);
							found = true;
							break;
						}
					}
				}
				if (!found)
					throw new RuntimeException("Unable to apply access transformer (interface): " + typeName
							+ ", could not apply method: " + method.name + ", its counterpart could not be found.");

				transformedMethods.add(info.name + " " + info.toDescriptor() + " " + clName + " " + method.name + "&"
						+ method.desc + " " + oldMod + " " + newMod);
			}
			cls.interfaces.add(transformerNode.name);
		} else {
			for (MethodNode meth : transformerNode.methods) {
				if (AnnotationInfo.isAnnotationPresent(ASM.class, meth)) {
					asmMethods = true;
					continue;
				}
				if (Modifier.isAbstract(meth.access) || AnnotationInfo.isAnnotationPresent(Reflect.class, meth))
					continue;

				String descriptor = meth.desc;
				FluidMethodInfo info = FluidMethodInfo.create(meth);
				info.remap(clName, transformerNode, info, pool);

				String methName = info.name;
				descriptor = info.toDescriptor();

				final String descFinal = descriptor;
				final String methNameFinal = methName;
				if (AnnotationInfo.isAnnotationPresent(Erase.class, meth)) {
					if (!cls.methods.stream().anyMatch(t -> t.name.equals(methNameFinal) && t.desc.equals(descFinal))) {
						throw new RuntimeException("Unable to transform method " + methNameFinal + " of class " + clName
								+ " as it could not be found, transformer: " + typeName);
					}

					MethodNode targetNode = cls.methods.stream()
							.filter(t -> t.name.equals(methNameFinal) && t.desc.equals(descFinal)).findFirst().get();

					int methodStart = -1;
					for (AbstractInsnNode node : targetNode.instructions) {
						if (node instanceof LineNumberNode && methodStart == -1) {
							LineNumberNode lnNode = (LineNumberNode) node;
							methodStart = lnNode.line;
						}
					}

					int newMod = targetNode.access;
					if (AnnotationInfo.isAnnotationPresent(Modifiers.class, meth)) {
						newMod = AnnotationInfo.getAnnotation(Modifiers.class, meth).get("modifiers");
					}

					debug("Transforming method: " + targetNode.name + "... (using transformer method: " + meth.name
							+ ")");
					targetNode.maxStack = meth.maxStack;
					targetNode.maxLocals = meth.maxLocals
							+ (!Modifier.isStatic(targetNode.access) ? (Modifier.isStatic(meth.access) ? 0 : 1) : 0);

					targetNode.localVariables = meth.localVariables;
					targetNode.instructions = meth.instructions;
					targetNode.access = newMod;
					for (String except : meth.exceptions) {
						if (targetNode.exceptions == null)
							targetNode.exceptions = new ArrayList<String>();
						targetNode.exceptions.add(except);
					}

					for (AbstractInsnNode node : targetNode.instructions) {
						if (node instanceof LineNumberNode) {
							LineNumberNode lnNode = (LineNumberNode) node;
							lnNode.line = methodStart++;
						}
					}

					FluidMethodInfo mth = FluidMethodInfo.create(meth);
					mth.remap(clName, transformerNode, mth, pool);
					mth.transform(targetNode.instructions, transformerNode, clName, cls, pool);
					mth.apply(meth);
				} else if (AnnotationInfo.isAnnotationPresent(InjectAt.class, meth)) {
					if (!cls.methods.stream().anyMatch(t -> t.name.equals(methNameFinal) && t.desc.equals(descFinal))) {
						throw new RuntimeException("Unable to transform method " + methNameFinal + " of class " + clName
								+ " as it could not be found, transformer: " + typeName);
					}

					MethodNode targetNode = cls.methods.stream()
							.filter(t -> t.name.equals(methNameFinal) && t.desc.equals(descFinal)).findFirst().get();

					debug("Transforming method: " + targetNode.name + "... (using transformer method: " + meth.name
							+ ")");
					String targetMethName = null;
					String targetMethTypes[] = null;
					String targetMethCls = cls.name;
					if (AnnotationInfo.getAnnotation(InjectAt.class, meth).get("targetCall") != null) {
						FluidMethodInfo targetInfo = FluidMethodInfo
								.create(AnnotationInfo.getAnnotation(InjectAt.class, meth).<String>get("targetCall"));
						String targetCls = clName;

						if (AnnotationInfo.getAnnotation(InjectAt.class, meth).get("targetOwner") != null) {
							targetCls = AnnotationInfo.getAnnotation(InjectAt.class, meth).get("targetOwner");
							targetMethCls = Fluid.mapClass(targetCls).replaceAll("\\.", "/");
						}

						String superName = targetCls;
						ClassNode clsT = null;
						try {
							clsT = pool.getClassNode(Fluid.mapClass(superName));
						} catch (ClassNotFoundException e1) {
							superName = null;
						}
						boolean found = false;
						if (Fluid.getMappings().length == 0)
							found = true;
						while (!found && superName != null) {
							for (Mapping<?> map : Fluid.getMappings()) {
								for (Mapping<?> mp : map.mappings) {
									if (mp.name.equals(superName)) {
										if (Stream.of(mp.mappings)
												.anyMatch(t -> t.mappingType == MAPTYPE.METHOD
														&& t.name.equals(targetInfo.name)
														&& Arrays.equals(t.argumentTypes, targetInfo.types))) {
											found = true;
											break;
										}
									}
								}
								if (found)
									break;
							}
							if (!found) {
								superName = null;
								if (clsT.superName != null
										&& !clsT.superName.equals(Object.class.getTypeName().replaceAll("\\.", "/"))) {
									superName = getDeobfName(clsT.superName.replaceAll("/", "."));
									try {
										clsT = pool.getClassNode(clsT.superName);
									} catch (ClassNotFoundException e) {
										break;
									}
								}
							}
						}

						targetInfo.remap(superName, transformerNode, targetInfo, pool);
						targetMethName = targetInfo.name;
						targetMethTypes = targetInfo.types;
					}

					int newMod = targetNode.access;
					int oldMod = newMod;
					if (AnnotationInfo.isAnnotationPresent(Modifiers.class, meth)) {
						newMod = AnnotationInfo.getAnnotation(Modifiers.class, meth).get("modifiers");
					}

					int methodStart = -1;
					int methodEndIndex = -1;

					LabelNode methodStartLabel = null;
					LabelNode methodEndLabel = null;

					for (AbstractInsnNode node : targetNode.instructions) {
						if (node instanceof LineNumberNode && methodStart == -1) {
							LineNumberNode lnNode = (LineNumberNode) node;
							methodStart = lnNode.line;
						} else if (node instanceof LabelNode && methodStartLabel == null) {
							methodStartLabel = (LabelNode) node;
						}
					}

					AbstractInsnNode tnode = targetNode.instructions.getLast();
					boolean returned = false;
					int indexTmp = targetNode.instructions.size();
					while (tnode != null) {
						if (tnode instanceof LabelNode && returned && methodEndLabel == null) {
							methodEndLabel = (LabelNode) tnode;
							methodEndIndex = indexTmp;
						} else if (!returned) {
							switch (tnode.getOpcode()) {
							case Opcodes.ARETURN:
							case Opcodes.DRETURN:
							case Opcodes.FRETURN:
							case Opcodes.IRETURN:
							case Opcodes.LRETURN:
							case Opcodes.RETURN:
								returned = true;
								break;
							}
						}

						indexTmp--;
						tnode = tnode.getPrevious();
					}

					boolean lineless = false;
					if (methodStart == -1) {
						lineless = true;
					}

					int injectLine = -1;
					int injectNodeIndex = -1;
					AbstractInsnNode injectNode = null;
					AnnotationInfo at = AnnotationInfo.getAnnotation(InjectAt.class, meth);

					if (targetMethName != null) {
						int offset = at.get("offset", 0);
						if (at.get("location") == InjectLocation.HEAD) {
							int index = 0;
							for (AbstractInsnNode node : targetNode.instructions) {
								if (node instanceof MethodInsnNode) {
									MethodInsnNode methNode = (MethodInsnNode) node;
									if (methNode.owner.equals(targetMethCls)) {
										String methDesc = methNode.desc;
										String[] methTypes = Fluid.parseMultipleDescriptors(
												methDesc.substring(1, methDesc.lastIndexOf(")")));
										if (methNode.name.equals(targetMethName)
												&& Arrays.equals(methTypes, targetMethTypes)) {
											if (offset != 0) {
												offset--;
											} else {
												if (lineless) {
													injectNodeIndex = index;
													injectNode = methNode;
													break;
												} else {
													AbstractInsnNode pr = methNode.getPrevious();
													while (pr != null && injectNode == null) {
														if (pr instanceof LineNumberNode) {
															injectNode = pr.getPrevious();
															injectLine = ((LineNumberNode) pr).line;
															injectNodeIndex = index;
															break;
														} else if (pr instanceof LabelNode) {
															injectNode = pr;
															injectNodeIndex = index;
															break;
														}
														pr = pr.getPrevious();
													}
												}
											}
										}
									}
								}
								index++;
							}
						} else {
							AbstractInsnNode node = targetNode.instructions.getLast();
							while (node != null) {
								int index = 0;
								if (node instanceof MethodInsnNode) {
									MethodInsnNode methNode = (MethodInsnNode) node;
									if (methNode.owner.equals(targetMethCls)) {
										String methDesc = methNode.desc;
										String[] methTypes = Fluid.parseMultipleDescriptors(
												methDesc.substring(1, methDesc.lastIndexOf(")")));
										if (methNode.name.equals(targetMethName)
												&& Arrays.equals(methTypes, targetMethTypes)) {
											if (offset != 0) {
												offset--;
											} else {
												if (lineless) {
													injectNode = methNode;
													injectNodeIndex = index;
													break;
												} else {
													AbstractInsnNode pr = methNode.getPrevious();
													while (pr != null && injectNode == null) {
														if (pr instanceof LineNumberNode) {
															injectNode = pr.getPrevious();
															injectLine = ((LineNumberNode) pr).line;
															injectNodeIndex = index;
															break;
														} else if (pr instanceof LabelNode) {
															injectNode = pr;
															injectNodeIndex = index;
															break;
														}
														pr = pr.getPrevious();
													}
												}
											}
										}
									}
									index++;
								}
								node = node.getPrevious();
							}
						}
					}

					if (injectNode == null && targetMethName == null) {
						if (at.get("location") == InjectLocation.HEAD) {
							int offset = at.get("offset", 0);
							tnode = methodStartLabel;
							int index = 0;
							while (injectNode == null && tnode != null) {
								if (tnode instanceof LineNumberNode) {
									if (offset != 0) {
										offset--;
									} else {
										injectNode = tnode.getPrevious();
										injectLine = ((LineNumberNode) tnode).line;
										injectNodeIndex = index;
										break;
									}
								} else if (tnode instanceof LabelNode && tnode.getNext() != null
										&& !(tnode.getNext() instanceof LineNumberNode)) {
									if (offset != 0) {
										offset--;
									} else {
										injectNode = tnode;
										injectNodeIndex = index;
										break;
									}
								}
								index++;
								tnode = tnode.getNext();
							}
						} else {
							int offset = at.get("offset", 0);
							tnode = methodEndLabel;
							int index = methodEndIndex;
							if (tnode.getNext() != null && tnode.getNext() instanceof LineNumberNode) {
								index++;
								tnode = tnode.getNext();
							}
							while (injectNode == null && tnode != null) {
								if (tnode instanceof LineNumberNode) {
									if (offset != 0) {
										offset--;
									} else {
										injectNode = tnode.getPrevious();
										injectLine = ((LineNumberNode) tnode).line;
										injectNodeIndex = index;
										break;
									}
								} else if (tnode instanceof LabelNode && tnode.getNext() != null
										&& !(tnode.getNext() instanceof LineNumberNode)) {
									if (offset != 0) {
										offset--;
									} else {
										injectNode = tnode;
										injectNodeIndex = index;
										break;
									}
								}
								index--;
								tnode = tnode.getPrevious();
							}
						}
					}

					int injectCodeStart = -1;
					int injectCodeEnd = -1;

					int addVarStart = 0;
					if (injectNode != null) {
						int index = 0;
						for (AbstractInsnNode node : targetNode.instructions) {
							if (index == injectNodeIndex)
								break;
							if (node instanceof VarInsnNode) {
								if (((VarInsnNode) node).var > addVarStart)
									addVarStart = ((VarInsnNode) node).var;
							} else if (node instanceof IincInsnNode) {
								if (((IincInsnNode) node).var > addVarStart)
									addVarStart = ((IincInsnNode) node).var;
							}
							index++;
						}
					}

					String[] actualParams = Fluid
							.parseMultipleDescriptors(meth.desc.substring(1, meth.desc.lastIndexOf(")")));
					int appendVarLength = meth.maxLocals
							- (actualParams.length + (Modifier.isStatic(meth.access) ? 0 : 1));

					if (addVarStart == 0)
						addVarStart = info.types.length;

					targetNode.maxLocals += appendVarLength;
					InsnList newNodes = new InsnList();
					for (AbstractInsnNode node : meth.instructions) {
						if (node instanceof VarInsnNode) {
							VarInsnNode vnode = (VarInsnNode) node;
							if (vnode.var > actualParams.length) {
								int varIndex = vnode.var - actualParams.length;
								vnode.var = addVarStart + varIndex;
							}
						} else if (node instanceof IincInsnNode) {
							IincInsnNode inode = (IincInsnNode) node;
							if (inode.var > actualParams.length) {
								int varIndex = inode.var - actualParams.length;
								inode.var = addVarStart + varIndex;
							}
						}

						if (!(node instanceof FrameNode))
							newNodes.add(node);
					}
					AbstractInsnNode nd = newNodes.getLast();
					while (nd != null) {
						boolean stop = false;
						switch (nd.getOpcode()) {
						case Opcodes.ARETURN:
						case Opcodes.DRETURN:
						case Opcodes.FRETURN:
						case Opcodes.IRETURN:
						case Opcodes.LRETURN:
						case Opcodes.RETURN:
							stop = true;
							if (nd.getNext() != null && nd.getNext() instanceof LineNumberNode) {
								newNodes.remove(((LineNumberNode) nd.getNext()).start);
								newNodes.remove(nd.getNext());
							}
							newNodes.remove(nd);
							break;
						}
						if (stop)
							break;
						nd = nd.getPrevious();
					}

					if (targetNode.localVariables != null && meth.localVariables != null) {
						if (meth.localVariables.size() > actualParams.length) {
							LocalVariableNode[] mthLocalVars = new LocalVariableNode[meth.localVariables.size()
									- (actualParams.length + (Modifier.isStatic(meth.access) ? 0 : 1))];
							int index = 0;
							for (int i = actualParams.length
									+ (Modifier.isStatic(meth.access) ? 0 : 1); i < meth.localVariables.size(); i++) {
								LocalVariableNode lvn = meth.localVariables.get(i);
								if (Fluid.parseDescriptor(lvn.desc).equals(transformerNode.name.replaceAll("/", "."))) {
									lvn.desc = Fluid.getDescriptor(cls.name);
								}

								mthLocalVars[index++] = lvn;
							}
							if (mthLocalVars.length != 0) {
								if (targetNode.localVariables.size() == 0) {
									for (int i = 0; i < mthLocalVars.length; i++) {
										mthLocalVars[i].index = i;
									}
									targetNode.localVariables = Arrays.asList(mthLocalVars);
								} else {
									for (int i = 0; i < mthLocalVars.length; i++) {
										mthLocalVars[i].index = addVarStart + (Modifier.isStatic(meth.access) ? 0 : 1)
												+ i;
									}
									LocalVariableNode[] oldVars = new LocalVariableNode[targetNode.localVariables
											.size()];
									for (int i = 0; i < targetNode.localVariables.size(); i++) {
										oldVars[i] = targetNode.localVariables.get(i);
										if (i > addVarStart) {
											oldVars[i].index += mthLocalVars.length;
										}
									}
									int insertAt = 0;
									int ind = 0;
									for (LocalVariableNode var : targetNode.localVariables) {
										if (var.index == addVarStart)
											insertAt = ind;
										ind++;
									}
									LocalVariableNode[] vars = ArrayUtil.insert(oldVars,
											insertAt + (Modifier.isStatic(meth.access) ? 0 : 1), mthLocalVars);
									targetNode.localVariables = Arrays.asList(vars);
								}
							}
						}
					}

					for (AbstractInsnNode node : targetNode.instructions) {
						if (node instanceof VarInsnNode) {
							VarInsnNode vnode = (VarInsnNode) node;
							if (vnode.var > addVarStart) {
								int varIndex = vnode.var - addVarStart;
								vnode.var = addVarStart + appendVarLength + varIndex;
							}
						} else if (node instanceof IincInsnNode) {
							IincInsnNode inode = (IincInsnNode) node;
							if (inode.var > addVarStart) {
								int varIndex = inode.var - addVarStart;
								inode.var = addVarStart + appendVarLength + varIndex;
							}
						}
					}

					for (AbstractInsnNode node : newNodes) {
						if (node instanceof LineNumberNode && injectCodeStart == -1) {
							LineNumberNode lnNode = (LineNumberNode) node;
							injectCodeStart = lnNode.line;
						}
					}

					nd = newNodes.getLast();
					while (nd != null) {
						AbstractInsnNode node = nd;
						if (node instanceof LineNumberNode && injectCodeEnd == -1) {
							LineNumberNode lnNode = (LineNumberNode) node;
							injectCodeEnd = lnNode.line;
						}
						nd = nd.getPrevious();
					}

					int ind = 1;
					int codeLength = injectCodeEnd - injectCodeStart;
					if (injectCodeEnd == injectCodeStart)
						codeLength = 1;

					for (AbstractInsnNode node : targetNode.instructions) {
						if (node instanceof LineNumberNode) {
							LineNumberNode lnNode = (LineNumberNode) node;
							if (lnNode.line >= injectLine) {
								lnNode.line += codeLength + 1;
							}
						}
					}

					ind = 1;
					LabelNode mthStartNode = null;
					for (AbstractInsnNode node : newNodes) {
						if (node instanceof LineNumberNode) {
							LineNumberNode lnNode = (LineNumberNode) node;
							lnNode.line = injectLine + ind++;
						} else if (node instanceof LabelNode && mthStartNode == null) {
							mthStartNode = (LabelNode) node;
						}
					}

					info.transform(newNodes, transformerNode, clName, cls, pool);

					int offset = at.get("offset", 0);
					if (!lineless && injectNode == null && targetMethName == null) {
						throw new RuntimeException("Unable to find LabelNode offset " + offset + " for class " + clName
								+ ", transformer cannot be applied, method: " + meth.name + ", transformer: "
								+ typeName);
					} else if (!lineless && injectNode == null) {
						throw new RuntimeException("Unable to find target method '" + targetMethName + "'"
								+ (offset == 0 ? "" : " with offset " + offset) + " in class " + clName
								+ ", transformer cannot be applied, method: " + meth.name + ", transformer: "
								+ typeName);
					}

					InsnList instrs = targetNode.instructions;
					if (mthStartNode != null) {
						for (AbstractInsnNode node : instrs) {
							if (node instanceof JumpInsnNode) {
								JumpInsnNode jump = (JumpInsnNode) node;
								int jTarget = 0;
								AbstractInsnNode nodeTmp = jump.label.getPrevious();
								while (nodeTmp != null) {
									jTarget++;
									nodeTmp = nodeTmp.getPrevious();
								}
								int mTarget = 0;
								nodeTmp = injectNode.getPrevious();
								while (nodeTmp != null) {
									mTarget++;
									nodeTmp = nodeTmp.getPrevious();
								}
								if (jTarget == mTarget)
									jump.label = mthStartNode;
							} else if (node instanceof FrameNode)
								instrs.remove(node);
						}
					}
					if (injectNode != null) {
						instrs.insertBefore(injectNode, newNodes);
					} else {
						if (at.get("location") != InjectLocation.HEAD || at.get("offset", 0) != 0)
							warn("Could not apply transformer " + typeName + " to method " + meth.name
									+ " at its preferred offset, adding the instructions at the top of the method, class: "
									+ clName);
						instrs.insert(newNodes);
					}

					targetNode.instructions = instrs;
					targetNode.access = newMod;
					if (meth.maxStack > targetNode.maxStack) {
						targetNode.maxStack += meth.maxStack - targetNode.maxStack;
					}
					if (meth.tryCatchBlocks != null) {
						if (targetNode.tryCatchBlocks == null)
							targetNode.tryCatchBlocks = new ArrayList<TryCatchBlockNode>();
						targetNode.tryCatchBlocks.addAll(meth.tryCatchBlocks);
					}

					FluidMethodInfo ninfo = FluidMethodInfo.create(meth);
					ninfo.remap(clName, transformerNode, ninfo, false, pool);

					transformedMethods.add(ninfo.name + " " + ninfo.toDescriptor() + " " + clName + " " + meth.name
							+ "&" + meth.desc + " " + oldMod + " " + newMod);
				} else {
					if (meth.name.equals("<init>"))
						continue;
					if (cls.methods.stream().anyMatch(t -> t.name.equals(methNameFinal) && t.desc.equals(descFinal))) {

						if (!meth.name.equals("<clinit>"))
							throw new RuntimeException("Unable to add method " + methNameFinal + " to class " + clName
									+ " as a method with the same signature already exists, transformer: " + typeName);
						else
							continue;
					}

					for (int i = 0; i < meth.localVariables.size(); i++) {
						LocalVariableNode lvn = meth.localVariables.get(i);
						if (Fluid.parseDescriptor(lvn.desc).equals(transformerNode.name.replaceAll("/", "."))) {
							lvn.desc = Fluid.getDescriptor(cls.name);
						}

						meth.localVariables.set(i, lvn);
					}

					int newMod = meth.access;
					if (AnnotationInfo.isAnnotationPresent(Modifiers.class, meth)) {
						newMod = AnnotationInfo.getAnnotation(Modifiers.class, meth).get("modifiers");
					}
					int oldMod = meth.access;

					FluidMethodInfo mth = FluidMethodInfo.create(meth);
					mth.name = methNameFinal;
					mth.remap(clName, transformerNode, mth, pool);
					mth.transform(meth.instructions, transformerNode, clName, cls, pool);
					mth.apply(meth);
					meth.access = newMod;
					cls.methods.add(meth);

					FluidMethodInfo ninfo = FluidMethodInfo.create(meth);
					ninfo.remap(clName, transformerNode, ninfo, false, pool);

					debug("Created method " + ninfo.name);
					transformedMethods.add(ninfo.name + " " + ninfo.toDescriptor() + " " + clName + " " + meth.name
							+ "&" + meth.desc + " " + oldMod + " " + newMod + " true");
				}
			}

			for (FieldNode field : transformerNode.fields) {
				String superName = clName;
				ClassNode clsT = null;
				try {
					clsT = pool.getClassNode(Fluid.mapClass(superName));
				} catch (ClassNotFoundException e1) {
					superName = null;
				}
				boolean found = false;
				final String fName = field.name;
				if (Fluid.getMappings().length == 0)
					found = true;
				while (!found && superName != null) {
					for (Mapping<?> map : Fluid.getMappings()) {
						for (Mapping<?> mp : map.mappings) {
							if (mp.name.equals(superName)) {
								if (Stream.of(mp.mappings)
										.anyMatch(t -> t.mappingType == MAPTYPE.PROPERTY && t.name.equals(fName))) {
									found = true;
									break;
								}
							}
						}
						if (found)
							break;
					}
					if (!found) {
						superName = null;
						if (clsT.superName != null
								&& !clsT.superName.equals(Object.class.getTypeName().replaceAll("\\.", "/"))) {
							superName = getDeobfName(clsT.superName.replaceAll("/", "."));
							try {
								clsT = pool.getClassNode(clsT.superName);
							} catch (ClassNotFoundException e) {
								break;
							}
						}
					}
				}

				if (superName == null)
					superName = clName;

				String fname = Fluid.mapProperty(superName, field.name);
				String ftype = Fluid.parseDescriptor(field.desc);

				if (AnnotationInfo.isAnnotationPresent(TargetType.class, field)) {
					ftype = AnnotationInfo.getAnnotation(TargetType.class, field).get("target");
				}

				int newMod = -1;
				if (AnnotationInfo.isAnnotationPresent(Modifiers.class, field)) {
					newMod = AnnotationInfo.getAnnotation(Modifiers.class, field).get("modifiers");
				}
				boolean isNew = false;

				if (!checkField(fname, pool, cls)) {
					field.desc = Fluid.getDescriptor(Fluid.mapClass(ftype));
					cls.fields.add(field);
					isNew = true;
				} else {
					field = getField(fname, pool, cls);
				}

				int oldMod = field.access;
				if (newMod != -1) {
					field.access = newMod;
				}

				transformedFields.add(fName + " " + Fluid.getDescriptor(ftype) + " " + superName + " " + fName + " "
						+ oldMod + " " + field.access + " " + isNew);
			}

			for (String _interface : transformerNode.interfaces) {
				if (!cls.interfaces.contains(_interface))
					cls.interfaces.add(_interface);
			}

			if (asmMethods) {
				debug("Loading transformer " + typeName + " as a class, it contains @ASM methods...");
				try {
					Class<?> transformer = ClassLoader.getSystemClassLoader().loadClass(typeName);
					for (Method meth : transformer.getMethods()) {
						if (Modifier.isStatic(meth.getModifiers()) && meth.isAnnotationPresent(ASM.class)) {
							debug("Transforming " + clName + " with ASM-based transformer " + meth.getName()
									+ "... Transformer class: " + typeName);
							ArrayList<Object> params = new ArrayList<Object>();
							boolean clsPoolAdded = false;
							boolean clsNameAdded = false;
							boolean error = false;
							int nodes = 0;
							for (Parameter param : meth.getParameters()) {
								if (param.getType().getTypeName().equals(FluidClassPool.class.getTypeName())
										&& !clsPoolAdded) {
									params.add(pool);
									clsPoolAdded = true;
								} else if (param.getType().getTypeName().equals(ClassNode.class.getTypeName())
										&& nodes < 2) {
									if (nodes == 0) {
										params.add(cls);
										nodes++;
									} else if (nodes == 1) {
										params.add(transformerNode);
										nodes++;
									}
								} else if (param.getType().isAssignableFrom(String.class) && !clsNameAdded) {
									params.add(loadingName);
									clsNameAdded = true;
								} else {
									error("Unable to run @ASM transformer method: " + meth.getName()
											+ ", could not recognize parameter: " + param.getName() + " of type "
											+ param.getType().getTypeName());
									error("Known parameters:");
									error(" - second ClassNode   - transformer class");
									error(" - first ClassNode    - target class");
									error(" - FluidClassPool     - class pool used to load the transformers and classes");
									error(" - String             - deobfuscated class name");

									error = true;
									break;
								}
							}
							if (!error) {
								try {
									meth.invoke(null, params.toArray(t -> new Object[t]));
								} catch (IllegalAccessException | IllegalArgumentException
										| InvocationTargetException e) {
									error("Failed to transform with ASM transformer " + meth.getName()
											+ ", transformer: " + typeName, e);
								}
							}
						}
					}
				} catch (ClassNotFoundException e) {
					error("Failed to load transformer " + typeName + " as a class, needed for @ASM methods.", e);
				}

			}
		}
		TransformerMetadata.createMetadata(transformerNode, transformerOwners.get(transformerNode.name),
				transformedFields, transformedMethods, pool);
		try {
			transformerPool.detachClass(transformerNode.name);
			transformerNode = transformerPool.getClassNode(transformerNode.name);
		} catch (ClassNotFoundException e) {
		}
		arr.set(transformerIndex, transformerNode);
		transformers.put(loadingName, arr);
	}

	@Override
	protected boolean checkField(String fname, FluidClassPool pool, ClassNode cls) {
		if (cls.fields.stream().anyMatch(t -> t.name.equals(fname)))
			return true;

		if (cls.superName != null && !cls.superName.equals(Object.class.getTypeName().replaceAll("\\.", "/"))) {
			try {
				if (checkField(fname, pool, pool.getClassNode(cls.superName)))
					return true;
			} catch (ClassNotFoundException e) {
			}
		}

		return false;
	}

	//
	//
	// Field getters
	//

	@Override
	protected FieldNode getField(String fname, FluidClassPool pool, ClassNode cls) {
		if (cls.fields.stream().anyMatch(t -> t.name.equals(fname)))
			return cls.fields.stream().filter(t -> t.name.equals(fname)).findFirst().get();

		if (cls.superName != null && !cls.superName.equals(Object.class.getTypeName().replaceAll("\\.", "/"))) {
			try {
				if (checkField(fname, pool, pool.getClassNode(cls.superName)))
					return getField(fname, pool, pool.getClassNode(cls.superName));
			} catch (ClassNotFoundException e) {
			}
		}

		return null;
	}

	//
	//
	// Implementaion of the AnnotationInfo system.
	//

	@Override
	protected List<AnnotationInfo> getParameterAnnotations(MethodNode method, int param) {
		ArrayList<AnnotationInfo> annotations = new ArrayList<AnnotationInfo>();

		if (method.visibleParameterAnnotations != null) {
			if (method.visibleParameterAnnotations.length > param) {
				if (method.visibleParameterAnnotations[param] != null) {
					for (AnnotationNode anno : method.visibleParameterAnnotations[param]) {
						annotations.add(AnnotationInfo.create(anno));
					}
				}
			}
		}

		if (method.invisibleParameterAnnotations != null) {
			if (method.invisibleParameterAnnotations.length > param) {
				if (method.invisibleParameterAnnotations[param] != null) {
					for (AnnotationNode anno : method.invisibleParameterAnnotations[param]) {
						annotations.add(AnnotationInfo.create(anno));
					}
				}
			}
		}

		return annotations;
	}

	@Override
	protected AnnotationInfo[] createAnnoInfo(AbstractInsnNode node) {
		ArrayList<AnnotationInfo> infos = new ArrayList<AnnotationInfo>();

		if (node.visibleTypeAnnotations != null) {
			for (AnnotationNode anno : node.visibleTypeAnnotations) {
				infos.add(createAnnoInfo(anno));
			}
		}

		if (node.invisibleTypeAnnotations != null) {
			for (AnnotationNode anno : node.invisibleTypeAnnotations) {
				infos.add(createAnnoInfo(anno));
			}
		}

		return infos.toArray(t -> new AnnotationInfo[t]);
	}

	@Override
	protected AnnotationInfo[] createAnnoInfo(FieldNode node) {
		ArrayList<AnnotationInfo> infos = new ArrayList<AnnotationInfo>();

		if (node.visibleAnnotations != null) {
			for (AnnotationNode anno : node.visibleAnnotations) {
				infos.add(createAnnoInfo(anno));
			}
		}

		if (node.invisibleAnnotations != null) {
			for (AnnotationNode anno : node.invisibleAnnotations) {
				infos.add(createAnnoInfo(anno));
			}
		}

		return infos.toArray(t -> new AnnotationInfo[t]);
	}

	@Override
	protected AnnotationInfo[] createAnnoInfo(MethodNode node) {
		ArrayList<AnnotationInfo> infos = new ArrayList<AnnotationInfo>();

		if (node.visibleAnnotations != null) {
			for (AnnotationNode anno : node.visibleAnnotations) {
				infos.add(createAnnoInfo(anno));
			}
		}

		if (node.invisibleAnnotations != null) {
			for (AnnotationNode anno : node.invisibleAnnotations) {
				infos.add(createAnnoInfo(anno));
			}
		}

		return infos.toArray(t -> new AnnotationInfo[t]);
	}

	@Override
	protected AnnotationInfo[] createAnnoInfo(ClassNode node) {
		ArrayList<AnnotationInfo> infos = new ArrayList<AnnotationInfo>();

		if (node.visibleAnnotations != null) {
			for (AnnotationNode anno : node.visibleAnnotations) {
				infos.add(createAnnoInfo(anno));
			}
		}

		if (node.invisibleAnnotations != null) {
			for (AnnotationNode anno : node.invisibleAnnotations) {
				infos.add(createAnnoInfo(anno));
			}
		}

		return infos.toArray(t -> new AnnotationInfo[t]);
	}

	@Override
	protected AnnotationInfo createAnnoInfo(AnnotationNode node) {
		AnnotationInfo info = new AnnotationInfo();
		info.name = Fluid.parseDescriptor(node.desc);
		boolean key = true;
		String keyValue = "";
		if (node.values != null) {
			for (Object obj : node.values) {
				if (key) {
					keyValue = obj.toString();
				} else {
					obj = parseObj(obj);
					info.values.put(keyValue, obj);
					keyValue = "";
				}
				key = !key;
			}
		}
		return info;
	}

	private Object parseObj(Object obj) {
		if (obj instanceof String[]) {
			String[] data = (String[]) obj;
			String type = Fluid.parseDescriptor(data[0]);
			Class<?> enumCls = null;
			try {
				enumCls = Class.forName(type);
			} catch (ClassNotFoundException e) {
				UnrecognizedEnumInfo uenum = new UnrecognizedEnumInfo(type, data[1]);
				return uenum;
			}
			try {
				obj = enumCls.getField(data[1]).get(null);
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				e.printStackTrace();
			}
		} else if (obj instanceof List) {
			@SuppressWarnings("unchecked")
			List<Object> lst = (List<Object>) obj;
			for (int i = 0; i < lst.size(); i++) {
				lst.set(i, parseObj(lst.get(i)));
			}
		}
		return obj;
	}

	//
	//
	// Implementation of the FluidMethodInfo system
	//

	@Override
	protected FluidMethodInfo createFMI(MethodNode method) {
		FluidMethodInfo info = new FluidMethodInfo();

		String desc = method.desc;
		info.name = method.name;

		String returnT = Fluid.parseDescriptor(desc.substring(desc.lastIndexOf(")") + 1));
		String typeStr = desc.substring(1, desc.lastIndexOf(")"));
		ArrayList<String> mTypes = new ArrayList<String>(Arrays.asList(Fluid.parseMultipleDescriptors(typeStr)));

		int paramIndex = 0;
		int size = mTypes.size();
		for (int i = 0; i < size; i++) {
			boolean remove = false;
			for (AnnotationInfo anno : FluidMethodInfo.getParameterAnnotations(method, i)) {
				if (anno.is(LocalVariable.class)) {
					mTypes.remove(paramIndex);
					remove = true;
				}
			}
			if (!remove)
				paramIndex++;
		}

		info.types = mTypes.toArray(t -> new String[t]);
		info.returnType = returnT;

		return info;
	}

	@Override
	protected FluidMethodInfo createFMI(MethodInsnNode method) {
		FluidMethodInfo info = new FluidMethodInfo();

		String desc = method.desc;
		info.name = method.name;

		String returnT = Fluid.parseDescriptor(desc.substring(desc.lastIndexOf(")") + 1));
		String typeStr = desc.substring(1, desc.lastIndexOf(")"));
		String[] mTypes = Fluid.parseMultipleDescriptors(typeStr);

		info.types = mTypes;
		info.returnType = returnT;

		return info;
	}

	@Override
	protected FluidMethodInfo createFMI(String methodName, String methodDesc) {
		FluidMethodInfo info = new FluidMethodInfo();

		info.name = methodName;

		String returnT = Fluid.parseDescriptor(methodDesc.substring(methodDesc.lastIndexOf(")") + 1));
		String typeStr = methodDesc.substring(1, methodDesc.lastIndexOf(")"));
		String[] mTypes = Fluid.parseMultipleDescriptors(typeStr);

		info.types = mTypes;
		info.returnType = returnT;

		return info;
	}

	@Override
	protected FluidMethodInfo createFMI(String name, String[] types, String returnType, String owner) {
		FluidMethodInfo info = new FluidMethodInfo();

		int i = 0;
		for (String type : types) {
			types[i++] = Fluid.mapClass(type);
		}
		returnType = Fluid.mapClass(returnType);
		name = Fluid.mapMethod(owner.replaceAll("/", "."), info.name, types);

		info.name = name;
		info.types = types;
		info.returnType = returnType;

		return null;
	}

	@Override
	protected FluidMethodInfo createFMI(String methodIdentifier) {
		String name = methodIdentifier;
		String desc = "";

		if (name.contains("(")) {
			desc = name.substring(name.indexOf("("));
			name = name.substring(0, name.indexOf("("));
			if (desc.endsWith(")")) {
				String typesStr = desc.substring(1, desc.lastIndexOf(")"));
				desc = "(";
				if (typesStr.contains(" "))
					typesStr = typesStr.replaceAll(" ", "");
				if (!typesStr.isEmpty()) {
					for (String type : typesStr.split(",")) {
						desc += Fluid.getDescriptor(type);
					}
				}
				desc += ")V";
			}
		}

		return createFMI(name, desc);
	}

	@Override
	protected MethodNode remapFMI(FluidMethodInfo self, String clName, ClassNode transformerNode,
			FluidMethodInfo method, boolean fullRemap, FluidClassPool pool) {
		Optional<MethodNode> nodeOpt = transformerNode.methods.stream()
				.filter(t -> t.name.equals(method.name) && FluidMethodInfo.create(t).equals(method)).findFirst();

		if (!nodeOpt.isEmpty()) {
			MethodNode call = nodeOpt.get();

			if (AnnotationInfo.isAnnotationPresent(TargetName.class, call)) {
				self.name = AnnotationInfo.getAnnotation(TargetName.class, call).get("target");
			}

			if (AnnotationInfo.isAnnotationPresent(Constructor.class, call)) {
				self.name = AnnotationInfo.getAnnotation(Constructor.class, call).get("clinit", false) ? "<clinit>"
						: "<init>";
			}

			if (AnnotationInfo.isAnnotationPresent(TargetType.class, call))
				self.returnType = AnnotationInfo.getAnnotation(TargetType.class, call).get("target");

			int index = 0;

			for (String type : method.types) {
				String typePath = type;
				for (AnnotationInfo anno : FluidMethodInfo.getParameterAnnotations(call, index)) {
					if (anno.is(TargetType.class)) {
						typePath = anno.get("target");
					}
				}
				self.types[index++] = typePath;
			}
		}
		if (fullRemap)
			self.returnType = Fluid.mapClass(self.returnType);

		if (fullRemap) {
			self.name = mapMethodName(clName, self.name, self.types, true, null, pool);
			int index = 0;

			for (int i = 0; i < method.types.length; i++) {
				self.types[index] = Fluid.mapClass(self.types[index++]);
			}
		}

		return (nodeOpt.isEmpty() ? null : nodeOpt.get());
	}

	private String mapMethodName(String clsName, String methName, String[] types, boolean toplevel, ClassNode clsT,
			FluidClassPool pool) {

		boolean found = false;
		String mName = methName;

		if (methName.equals("<init>") || methName.equals("<clinit>") || clsName == null) {
			return methName;
		}

		for (Mapping<?> root : Fluid.getMappings()) {
			Mapping<?> map = root.mapClassToMapping(clsName,
					t -> Stream.of(t.mappings)
							.anyMatch(t2 -> t2.mappingType == MAPTYPE.METHOD
									&& (t2.name.equals(mName) || t2.obfuscated.equals(mName))
									&& Arrays.equals(t2.argumentTypes, types)),
					false);

			if (map != null) {
				methName = Stream.of(map.mappings)
						.filter(t2 -> t2.mappingType == MAPTYPE.METHOD
								&& (t2.name.equals(mName) || t2.obfuscated.equals(mName))
								&& Arrays.equals(t2.argumentTypes, types))
						.findFirst().get().obfuscated;

				found = true;
				break;
			} else {
				if (clsT == null) {
					try {
						clsT = pool.getClassNode(Fluid.mapClass(clsName));
					} catch (ClassNotFoundException ex) {
						break;
					}
				}

				String outP = "";
				for (String inter : clsT.interfaces) {
					ClassNode iClsT;
					clsName = getDeobfName(inter.replaceAll("/", "."));
					try {
						iClsT = pool.getClassNode(inter);
					} catch (ClassNotFoundException e) {
						break;
					}
					outP = mapMethodName(clsName, methName, types, false, iClsT, pool);
					if (outP != null) {
						found = true;
						methName = outP;
						break;
					} else if (!found && iClsT.superName != null
							&& !iClsT.superName.equals(Object.class.getTypeName().replaceAll("\\.", "/"))) {
						clsName = getDeobfName(iClsT.superName.replaceAll("/", "."));
						try {
							iClsT = pool.getClassNode(iClsT.superName);
						} catch (ClassNotFoundException e) {
							break;
						}

						outP = mapMethodName(clsName, methName, types, false, iClsT, pool);
						if (outP != null) {
							found = true;
							methName = outP;
						}
					}
				}
				if (!found && clsT.superName != null
						&& !clsT.superName.equals(Object.class.getTypeName().replaceAll("\\.", "/"))) {
					if (!found) {
						clsName = getDeobfName(clsT.superName.replaceAll("/", "."));
						try {
							clsT = pool.getClassNode(clsT.superName);
						} catch (ClassNotFoundException e) {
							break;
						}

						outP = mapMethodName(clsName, methName, types, false, clsT, pool);
						if (outP != null) {
							found = true;
							methName = outP;
						}
					}
				} else {
					break;
				}
			}
		}

		if (!found && !toplevel) {
			return null;
		}

		return methName;
	}

	@Override
	protected FluidMethodInfo createFMI(String name, String[] types, String returnType) {
		FluidMethodInfo info = new FluidMethodInfo();

		info.name = name;
		info.types = types;
		info.returnType = returnType;

		return info;
	}

	@Override
	protected FluidMethodInfo applyFMI(FluidMethodInfo self, MethodNode method) {
		method.name = self.name;
		method.desc = "(" + Fluid.getDescriptors(self.types) + ")" + Fluid.getDescriptor(self.returnType);
		return self;
	}

	@Override
	protected FluidMethodInfo applyFMI(FluidMethodInfo self, String owner, MethodInsnNode method) {
		method.name = self.name;
		method.desc = "(" + Fluid.getDescriptors(self.types) + ")" + Fluid.getDescriptor(self.returnType);
		method.owner = owner.replaceAll("\\.", "/");
		return self;
	}

	@Override
	protected FluidMethodInfo transformFMI(FluidMethodInfo self, InsnList instructions, ClassNode transformerNode,
			String clName, ClassNode cls, FluidClassPool pool) {
		for (AbstractInsnNode node : instructions) {
			if (node instanceof MethodInsnNode) {
				MethodInsnNode mnode = (MethodInsnNode) node;
				if (mnode.owner.equals(transformerNode.name)) {
					processMethod(mnode, clName, pool, transformerNode);
				} else {
					ClassNode clsT = null;
					try {
						clsT = pool.getClassNode(mnode.owner);
					} catch (ClassNotFoundException e1) {
					}
					if (clsT != null) {
						if (AnnotationInfo.isAnnotationPresent(TargetClass.class, clsT)) {
							String newClName = AnnotationInfo.getAnnotation(TargetClass.class, clsT).get("target");
							processMethod(mnode, newClName, pool, clsT);
						}
					}
				}
			} else if (node instanceof FieldInsnNode) {
				FieldInsnNode fnode = (FieldInsnNode) node;
				if (fnode.owner.equals(transformerNode.name)) {
					processField(fnode, clName, pool, transformerNode);
				} else {
					ClassNode clsT = null;
					try {
						clsT = pool.getClassNode(fnode.owner);
					} catch (ClassNotFoundException e1) {
					}
					if (clsT != null) {
						if (AnnotationInfo.isAnnotationPresent(TargetClass.class, clsT)) {
							String newClName = AnnotationInfo.getAnnotation(TargetClass.class, clsT).get("target");
							processField(fnode, newClName, pool, clsT);
						}
					}
				}
			} else if (node instanceof InvokeDynamicInsnNode) {
				InvokeDynamicInsnNode dnode = (InvokeDynamicInsnNode) node;
				String typesStr = dnode.desc;
				String type = typesStr.substring(typesStr.lastIndexOf(")") + 1);
				typesStr = typesStr.substring(1, typesStr.lastIndexOf(")"));

				String[] types = Fluid.parseMultipleDescriptors(typesStr);
				type = Fluid.parseDescriptor(type);

				int ind = 0;
				for (String param : types) {
					if (param.equals(transformerNode.name.replaceAll("/", "."))) {
						types[ind] = cls.name.replaceAll("/", ".");
					}
					ind++;
				}
				if (type.equals(transformerNode.name.replaceAll("/", "."))) {
					type = cls.name.replaceAll("/", ".");
				}

				dnode.desc = "(" + Fluid.getDescriptors(types) + ")" + Fluid.getDescriptor(type);

				ind = 0;
				for (Object arg : dnode.bsmArgs) {
					if (arg instanceof Handle) {
						Handle h = (Handle) arg;
						if (h.getOwner().equals(transformerNode.name)) {
							dnode.bsmArgs[ind] = new Handle(h.getTag(), cls.name, h.getName(), h.getDesc(),
									h.isInterface());
						}
					}
					ind++;
				}
			} else if (node instanceof FrameNode) {
				if (((FrameNode) node).stack != null) {
					int index = 0;
					for (Object stackEntry : ((FrameNode) node).stack) {
						if (stackEntry.equals(transformerNode.name)) {
							((FrameNode) node).stack.set(index, cls.name);
						}
						index++;
					}
				}
				if (((FrameNode) node).local != null) {
					int index = 0;
					for (Object localEntry : ((FrameNode) node).local) {
						if (localEntry.equals(transformerNode.name)) {
							((FrameNode) node).local.set(index, cls.name);
						}
						index++;
					}
				}
			}
		}
		return self;
	}

	private void processMethod(MethodInsnNode mnode, String clName, FluidClassPool pool, ClassNode targetClass) {
		FluidMethodInfo info = FluidMethodInfo.create(mnode);
		info.remap(clName, targetClass, info, false, pool);
		String superName = clName;
		ClassNode clsT = null;
		try {
			clsT = pool.getClassNode(Fluid.mapClass(superName));
		} catch (ClassNotFoundException e1) {
			superName = null;
		}
		boolean found = false;
		if (Fluid.getMappings().length == 0)
			found = true;
		while (!found && superName != null) {
			for (Mapping<?> map : Fluid.getMappings()) {
				for (Mapping<?> mp : map.mappings) {
					if (mp.name.equals(superName)) {
						if (Stream.of(mp.mappings).anyMatch(t -> t.mappingType == MAPTYPE.METHOD
								&& t.name.equals(info.name) && Arrays.equals(t.argumentTypes, info.types))) {
							found = true;
							break;
						}
					}
				}
				if (found)
					break;
			}
			if (!found) {
				superName = null;
				if (clsT.superName != null
						&& !clsT.superName.equals(Object.class.getTypeName().replaceAll("\\.", "/"))) {
					superName = getDeobfName(clsT.superName.replaceAll("/", "."));
					try {
						clsT = pool.getClassNode(clsT.superName);
					} catch (ClassNotFoundException e) {
						break;
					}
				}
			}
		}

		if (superName == null)
			superName = clName;

		info.name = Fluid.mapMethod(superName, mnode.name, info.types);
		int index = 0;
		for (int i = 0; i < info.types.length; i++) {
			info.types[index] = Fluid.mapClass(info.types[index++]);
		}
		info.returnType = Fluid.mapClass(info.returnType);
		info.apply(Fluid.mapClass(superName), mnode);
	}

	private static void processField(FieldInsnNode fnode, String clName, FluidClassPool pool, ClassNode targetClass) {
		String superName = clName;
		ClassNode clsT = null;
		try {
			clsT = pool.getClassNode(Fluid.mapClass(superName));
		} catch (ClassNotFoundException e1) {
			superName = null;
		}
		boolean found = false;
		if (Fluid.getMappings().length == 0)
			found = true;
		while (!found && superName != null) {
			for (Mapping<?> map : Fluid.getMappings()) {
				for (Mapping<?> mp : map.mappings) {
					if (mp.name.equals(superName)) {
						if (Stream.of(mp.mappings)
								.anyMatch(t -> t.mappingType == MAPTYPE.PROPERTY && t.name.equals(fnode.name))) {
							found = true;
							break;
						}
					}
				}
				if (found)
					break;
			}
			if (!found) {
				superName = null;
				if (clsT.superName != null
						&& !clsT.superName.equals(Object.class.getTypeName().replaceAll("\\.", "/"))) {
					superName = getDeobfName(clsT.superName.replaceAll("/", "."));
					try {
						clsT = pool.getClassNode(clsT.superName);
					} catch (ClassNotFoundException e) {
						break;
					}
				}
			}
		}

		if (superName == null)
			superName = clName;
		FieldNode targetNode = targetClass.fields.stream().filter(t -> t.name.equals(fnode.name)).findFirst().get();

		if (AnnotationInfo.isAnnotationPresent(TargetType.class, targetNode)) {
			fnode.desc = Fluid.getDescriptor(
					Fluid.mapClass(AnnotationInfo.getAnnotation(TargetType.class, targetNode).get("target")));
		}

		fnode.name = Fluid.mapProperty(superName, fnode.name);
		fnode.owner = Fluid.mapClass(superName).replaceAll("\\.", "/");
	}
}
