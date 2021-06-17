package org.asf.cyan.fluid.implementation;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.asf.aos.util.service.extra.slib.util.ArrayUtil;
import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.Transformer;
import org.asf.cyan.fluid.api.transforming.Constructor;
import org.asf.cyan.fluid.api.transforming.LocalVariable;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetName;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;
import org.asf.cyan.fluid.api.transforming.util.CodeControl;
import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.asf.cyan.fluid.bytecode.UnrecognizedEnumInfo;
import org.asf.cyan.fluid.bytecode.enums.OpcodeUseCase;
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

	public static void initComponent() {
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

	@Override
	protected void applyClassModifiers(TransformContext context, int oldModifiers, int newModifiers) {
		context.targetClass.access = newModifiers;
	}

	@Override
	protected boolean applyMethodInterfaceTransformer(TransformContext context, MethodNode target,
			MethodNode transformer, int oldMod, int newMod, FluidMethodInfo methodInfo) {
		if (target.name.equals(transformer.name)
				|| AnnotationInfo.isAnnotationPresent(Constructor.class, transformer)) {
			target.access = newMod;
			oldMod = target.access;
			return true;
		} else {
			MethodNode newmethod = new MethodNode();
			InsnList instructions = new InsnList();

			int i = 0;
			if (!Modifier.isStatic(newMod))
				instructions.add(new VarInsnNode(Opcodes.ALOAD, i++));
			for (int i2 = 0; i2 <= methodInfo.types.length; i2++) {
				if (methodInfo.types[i2].equals("int"))
					instructions.add(new VarInsnNode(Opcodes.ILOAD, i++));
				else if (methodInfo.types[i2].equals("float"))
					instructions.add(new VarInsnNode(Opcodes.FLOAD, i++));
				else if (methodInfo.types[i2].equals("double"))
					instructions.add(new VarInsnNode(Opcodes.DLOAD, i++));
				else if (methodInfo.types[i2].equals("long"))
					instructions.add(new VarInsnNode(Opcodes.LLOAD, i++));
				else if (methodInfo.types[i2].equals("int[]"))
					instructions.add(new VarInsnNode(Opcodes.IALOAD, i++));
				else if (methodInfo.types[i2].equals("float[]"))
					instructions.add(new VarInsnNode(Opcodes.FALOAD, i++));
				else if (methodInfo.types[i2].equals("double[]"))
					instructions.add(new VarInsnNode(Opcodes.DALOAD, i++));
				else if (methodInfo.types[i2].equals("long[]"))
					instructions.add(new VarInsnNode(Opcodes.LALOAD, i++));
				else if (methodInfo.types[i2].endsWith("[]"))
					instructions.add(new VarInsnNode(Opcodes.AALOAD, i++));
				else
					instructions.add(new VarInsnNode(Opcodes.ALOAD, i++));
			}

			instructions
					.add(new MethodInsnNode((Modifier.isStatic(newMod) ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL),
							context.targetClass.name, target.name, target.desc));

			if (!methodInfo.returnType.equals("void")) {
				if (methodInfo.returnType.equals("int") || methodInfo.returnType.equals("boolean")
						|| methodInfo.returnType.equals("short"))
					instructions.add(new InsnNode(Opcodes.IRETURN));
				else if (methodInfo.returnType.equals("double"))
					instructions.add(new InsnNode(Opcodes.DRETURN));
				else if (methodInfo.returnType.equals("float"))
					instructions.add(new InsnNode(Opcodes.FRETURN));
				else if (methodInfo.returnType.equals("long"))
					instructions.add(new InsnNode(Opcodes.LRETURN));
				else
					instructions.add(new InsnNode(Opcodes.ARETURN));
			} else
				instructions.add(new InsnNode(Opcodes.RETURN));

			newmethod.name = transformer.name;
			newmethod.maxLocals = methodInfo.getVarOffset() + (Modifier.isStatic(newMod) ? 0 : 1);
			newmethod.instructions = instructions;
			newmethod.maxStack = target.maxStack;
			newmethod.access = newMod;
			newmethod.desc = target.desc;
			newmethod.exceptions = target.exceptions;
			context.targetClass.methods.add(newmethod);
			return true;
		}
	}

	@Override
	public void applyMethodRewriteTransformer(TransformContext context, MethodNode target, MethodNode transformer,
			int newModifiers, FluidMethodInfo targetInfo) {
		int methodStart = -1;
		for (AbstractInsnNode node : target.instructions) {
			if (node instanceof LineNumberNode && methodStart == -1) {
				LineNumberNode lnNode = (LineNumberNode) node;
				methodStart = lnNode.line;
			}
		}

		target.maxStack = transformer.maxStack;
		target.maxLocals = transformer.maxLocals
				+ (!Modifier.isStatic(target.access) ? (Modifier.isStatic(transformer.access) ? 0 : 1) : 0);

		target.localVariables = transformer.localVariables;
		target.instructions = transformer.instructions;
		target.access = newModifiers;
		for (String except : transformer.exceptions) {
			if (target.exceptions == null)
				target.exceptions = new ArrayList<String>();
			target.exceptions.add(except);
		}

		for (AbstractInsnNode node : target.instructions) {
			if (node instanceof LineNumberNode) {
				LineNumberNode lnNode = (LineNumberNode) node;
				lnNode.line = methodStart++;
			}
		}

		FluidMethodInfo mth = FluidMethodInfo.create(transformer);
		mth.remap(context.mappedName, context.transformer, mth, context.programPool);
		mth.transform(target.instructions, context.transformer, context.mappedName, context.targetClass,
				context.programPool);
		mth.apply(transformer);
	}

	@Override
	protected void applyInjectAt(TransformContext context, TargetInfo targetInfo, MethodNode target,
			MethodNode transformer, int oldModifiers, int newModifiers, FluidMethodInfo methodInfo) {
		int methodStart = -1;
		int methodEndIndex = -1;

		LabelNode methodStartLabel = null;
		LabelNode methodEndLabel = null;

		for (AbstractInsnNode node : target.instructions) {
			if (node instanceof LineNumberNode && methodStart == -1) {
				LineNumberNode lnNode = (LineNumberNode) node;
				methodStart = lnNode.line;
			} else if (node instanceof LabelNode && methodStartLabel == null) {
				methodStartLabel = (LabelNode) node;
			}
		}

		AbstractInsnNode tnode = target.instructions.getLast();
		boolean returned = false;
		int indexTmp = target.instructions.size();
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
		if (methodEndLabel == null) {
			tnode = target.instructions.getLast();
			indexTmp = target.instructions.size();
			while (tnode != null) {
				if (tnode instanceof LabelNode && methodEndLabel == null) {
					methodEndLabel = (LabelNode) tnode;
					methodEndIndex = indexTmp;
					break;
				}

				indexTmp--;
				tnode = tnode.getPrevious();
			}
		}

		boolean lineless = false;
		if (methodStart == -1) {
			lineless = true;
		}

		int injectLine = -1;
		int injectNodeIndex = -1;

		AbstractInsnNode injectNode = null;
		if (targetInfo.targetMethodName != null) {
			int offset = targetInfo.offset;
			if (targetInfo.location == InjectLocation.HEAD) {
				int index = 0;
				for (AbstractInsnNode node : target.instructions) {
					if (node instanceof MethodInsnNode) {
						MethodInsnNode methNode = (MethodInsnNode) node;
						if (methNode.owner.equals(targetInfo.targetMethodClass)) {
							String methDesc = methNode.desc;
							String[] methTypes = Fluid
									.parseMultipleDescriptors(methDesc.substring(1, methDesc.lastIndexOf(")")));
							if (methNode.name.equals(targetInfo.targetMethodName)
									&& Arrays.equals(methTypes, targetInfo.targetMethodTypes)) {
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
				AbstractInsnNode node = target.instructions.getLast();
				while (node != null) {
					int index = 0;
					if (node instanceof MethodInsnNode) {
						MethodInsnNode methNode = (MethodInsnNode) node;
						if (methNode.owner.equals(targetInfo.targetMethodClass)) {
							String methDesc = methNode.desc;
							String[] methTypes = Fluid
									.parseMultipleDescriptors(methDesc.substring(1, methDesc.lastIndexOf(")")));
							if (methNode.name.equals(targetInfo.targetMethodName)
									&& Arrays.equals(methTypes, targetInfo.targetMethodTypes)) {
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

		if (injectNode == null && targetInfo.targetMethodName == null) {
			if (targetInfo.location == InjectLocation.HEAD) {
				int offset = targetInfo.offset;
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
				int offset = targetInfo.offset;
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

		String[] actualParams = Fluid
				.parseMultipleDescriptors(transformer.desc.substring(1, transformer.desc.lastIndexOf(")")));
		int min = 0;
		for (String type : actualParams) {
			if (type.equals("double[]"))
				min += 2;
			else
				min++;
		}
		if (!Modifier.isStatic(transformer.access) && Modifier.isStatic(target.access))
			min--;
		else if (Modifier.isStatic(transformer.access) && !Modifier.isStatic(target.access))
			min++;

		int appendVarLength = transformer.maxLocals
				- (actualParams.length + (Modifier.isStatic(transformer.access) ? 0 : 1));

		int injectCodeStart = -1;
		int injectCodeEnd = -1;

		int addVarStart = 0;
		if (injectNode != null) {
			int index = 0;
			for (AbstractInsnNode node : target.instructions) {
				if (index == injectNodeIndex)
					break;
				if (node instanceof VarInsnNode) {
					addVarStart = ((VarInsnNode) node).var;
				} else if (node instanceof IincInsnNode) {
					addVarStart = ((IincInsnNode) node).var;
				}
				index++;
			}
		}

		if (addVarStart == 0)
			addVarStart = methodInfo.getVarOffset() + (Modifier.isStatic(target.access) ? 0 : 1);

		target.maxLocals += appendVarLength;
		InsnList newNodes = new InsnList();

		for (AbstractInsnNode node : transformer.instructions) {
			if (node instanceof VarInsnNode) {
				VarInsnNode vnode = (VarInsnNode) node;
				if (vnode.var > min) {
					int varIndex = vnode.var - min;
					vnode.var = addVarStart + varIndex;
				}
			} else if (node instanceof IincInsnNode) {
				IincInsnNode inode = (IincInsnNode) node;
				if (inode.var > min) {
					int varIndex = inode.var - min;
					inode.var = addVarStart + varIndex;
				}
			}

			if (!(node instanceof FrameNode))
				newNodes.add(node);
		}
		AbstractInsnNode nd = newNodes.getLast();

		AbstractInsnNode endLabel = injectNode;
		while (endLabel != null && !(endLabel instanceof LabelNode)) {
			endLabel = endLabel.getPrevious();
		}

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
				if (nd.getNext() != null && nd.getNext() instanceof LabelNode) {
					LabelNode label = (LabelNode) nd.getNext();
					if (injectNode != null && transformer.localVariables != null && endLabel != null) {
						for (LocalVariableNode var : transformer.localVariables) {
							if (transformer.instructions.indexOf(label) == transformer.instructions.indexOf(var.end)) {
								var.end = (LabelNode) endLabel;
							}
						}
						for (AbstractInsnNode node : newNodes) {
							if (node instanceof JumpInsnNode) {
								JumpInsnNode jump = (JumpInsnNode) node;
								if (transformer.instructions.indexOf(label) == transformer.instructions
										.indexOf(jump.label)) {
									jump.label = (LabelNode) endLabel;
								}
							}
						}
					}
					newNodes.remove(label);
				}
				if (nd.getPrevious() != null && nd.getPrevious() instanceof LineNumberNode) {
					LineNumberNode line = (LineNumberNode) nd.getPrevious();
					injectCodeEnd = line.line;
					LabelNode label = line.start;
					if (injectNode != null && transformer.localVariables != null && endLabel != null) {
						for (LocalVariableNode var : transformer.localVariables) {
							if (transformer.instructions.indexOf(label) == transformer.instructions.indexOf(var.end)) {
								var.end = (LabelNode) endLabel;
							}
						}
						for (AbstractInsnNode node : newNodes) {
							if (node instanceof JumpInsnNode) {
								JumpInsnNode jump = (JumpInsnNode) node;
								if (transformer.instructions.indexOf(label) == transformer.instructions
										.indexOf(jump.label)) {
									jump.label = (LabelNode) endLabel;
								}
							}
						}
					}
					newNodes.remove(label);
					newNodes.remove(line);
				}
				newNodes.remove(nd);
				break;
			}
			if (stop)
				break;
			nd = nd.getPrevious();
		}

		if (target.localVariables != null && transformer.localVariables != null) {
			if (transformer.localVariables.size() > actualParams.length + (Modifier.isStatic(target.access) ? 0 : 1)) {
				LocalVariableNode[] mthLocalVars = new LocalVariableNode[transformer.localVariables.size()
						- (actualParams.length + (Modifier.isStatic(transformer.access) ? 0 : 1))];
				int index = 0;
				for (int i = actualParams.length
						+ (Modifier.isStatic(transformer.access) ? 0 : 1); i < transformer.localVariables.size(); i++) {
					LocalVariableNode lvn = transformer.localVariables.get(i);
					if (Fluid.parseDescriptor(lvn.desc).equals(context.transformer.name.replaceAll("/", "."))) {
						lvn.desc = Fluid.getDescriptor(context.targetClass.name);
					}

					mthLocalVars[index++] = lvn;
				}
				if (mthLocalVars.length != 0) {
					if (target.localVariables.size() == 0) {
						for (int i = 0; i < mthLocalVars.length; i++) {
							mthLocalVars[i].index = i;
							AbstractInsnNode start = mthLocalVars[i].start;
							while (start != null) {
								start = start.getPrevious();
								if (start instanceof LabelNode) {
									mthLocalVars[i].start = (LabelNode) start;
									break;
								}
							}
						}
						target.localVariables = Arrays.asList(mthLocalVars);
					} else {
						index = 0;
						LocalVariableNode[] oldVars = new LocalVariableNode[target.localVariables.size()];
						for (LocalVariableNode var : target.localVariables) {
							if (var.index > addVarStart)
								var.index += appendVarLength;
							oldVars[index++] = var;
						}

						for (int i = 0; i < mthLocalVars.length; i++) {
							mthLocalVars[i].index = addVarStart + (mthLocalVars[i].index - actualParams.length);
							AbstractInsnNode start = mthLocalVars[i].start;
							while (start != null) {
								start = start.getPrevious();
								if (start instanceof LabelNode) {
									mthLocalVars[i].start = (LabelNode) start;
									break;
								}
							}
						}
						LocalVariableNode[] vars = ArrayUtil.append(oldVars, mthLocalVars);
						target.localVariables = Arrays.asList(vars);
					}
				}
			}
		}

		for (AbstractInsnNode node : newNodes) {
			if (node instanceof LineNumberNode && injectCodeStart == -1) {
				LineNumberNode lnNode = (LineNumberNode) node;
				injectCodeStart = lnNode.line;
			}
		}

		int ind = 1;
		int codeLength = injectCodeEnd - injectCodeStart;
		if (injectCodeEnd == injectCodeStart)
			codeLength = 1;

		for (AbstractInsnNode node : target.instructions) {
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

		methodInfo.transform(newNodes, context.transformer, context.mappedName, context.targetClass,
				context.programPool);

		int offset = targetInfo.offset;
		if (!lineless && injectNode == null && targetInfo.targetMethodName == null) {
			throw new RuntimeException("Unable to find LabelNode offset " + offset + " for class " + context.mappedName
					+ ", transformer cannot be applied, method: " + transformer.name + ", transformer: "
					+ context.transformerType);
		} else if (!lineless && injectNode == null) {
			throw new RuntimeException("Unable to find target method '" + targetInfo.targetMethodName + "'"
					+ (offset == 0 ? "" : " with offset " + offset) + " in class " + context.mappedName
					+ ", transformer cannot be applied, method: " + transformer.name + ", transformer: "
					+ context.transformerType);
		}

		InsnList instrs = target.instructions;
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
		for (AbstractInsnNode node : instrs) {
			if (node instanceof VarInsnNode) {
				VarInsnNode vnode = (VarInsnNode) node;
				if (vnode.var > addVarStart) {
					vnode.var += appendVarLength;
				}
			} else if (node instanceof IincInsnNode) {
				IincInsnNode vnode = (IincInsnNode) node;
				if (vnode.var > addVarStart) {
					vnode.var += appendVarLength;
				}
			}
		}
		if (injectNode != null) {
			instrs.insertBefore(injectNode, newNodes);
		} else {
			if (targetInfo.location != InjectLocation.HEAD || targetInfo.offset != 0)
				warn("Could not apply transformer " + context.transformerType + " to method " + transformer.name
						+ " at its preferred offset, adding the instructions at the top of the method, class: "
						+ context.mappedName);
			instrs.insert(newNodes);
		}

		target.instructions = instrs;
		target.access = newModifiers;
		if (transformer.maxStack > target.maxStack) {
			target.maxStack += transformer.maxStack - target.maxStack;
		}
		if (transformer.tryCatchBlocks != null) {
			if (target.tryCatchBlocks == null)
				target.tryCatchBlocks = new ArrayList<TryCatchBlockNode>();
			target.tryCatchBlocks.addAll(transformer.tryCatchBlocks);
		}
	}

	@Override
	protected FluidMethodInfo createMethod(TransformContext context, MethodNode transformer, String methodName,
			int oldModifiers, int newModifiers) {
		FluidMethodInfo mth = FluidMethodInfo.create(transformer);
		mth.name = methodName;
		mth.remap(context.mappedName, context.transformer, mth, context.programPool);
		mth.transform(transformer.instructions, context.transformer, context.mappedName, context.targetClass,
				context.programPool);
		mth.apply(transformer);
		transformer.access = newModifiers;

		FluidMethodInfo ninfo = FluidMethodInfo.create(transformer);
		ninfo.remap(context.mappedName, context.transformer, ninfo, false, context.programPool);

		InsnList instrs = new InsnList();
		for (AbstractInsnNode nd : transformer.instructions) {
			if (!(nd instanceof FrameNode))
				instrs.add(nd);
		}
		transformer.instructions = instrs;
		context.targetClass.methods.add(transformer);
		return ninfo;
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

	private static HashMap<OpcodeUseCase, HashMap<String, Integer>> opcodes = new HashMap<OpcodeUseCase, HashMap<String, Integer>>();

	static {
		for (Field field : Opcodes.class.getFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				field.setAccessible(true);
				try {
					Object value = field.get(null);
					OpcodeUseCase useCase = OpcodeUseCase.valueOf(field.getName().toUpperCase(),
							field.getType().getTypeName());
					HashMap<String, Integer> mp = opcodes.getOrDefault(useCase, new HashMap<String, Integer>());
					mp.put(field.getName().toUpperCase(), (Integer) value);
					opcodes.put(useCase, mp);
				} catch (IllegalArgumentException | IllegalAccessException e) {
				}
			}
		}
	}

	private static int getOpcode(String name) {
		return opcodes.get(OpcodeUseCase.JVM_OPCODE).get(name);
	}

	@Override
	protected FluidMethodInfo transformFMI(FluidMethodInfo self, InsnList instructions, ClassNode transformerNode,
			String clName, ClassNode cls, FluidClassPool pool) {
		InsnList lst = new InsnList();
		for (AbstractInsnNode nd : instructions) {
			lst.add(nd);
		}
		int nodeInd = 0;
		int skip = 0;
		int skipForced = 0;
		for (@SuppressWarnings("unused")
		AbstractInsnNode nd : lst) {
			if (skipForced != 0) {
				skipForced--;
				continue;
			}
			if (skip != 0) {
				instructions.remove(instructions.get(nodeInd));
				skip--;
				continue;
			}
			AbstractInsnNode node = instructions.get(nodeInd);
			if (node instanceof MethodInsnNode) {
				MethodInsnNode mnode = (MethodInsnNode) node;
				if (mnode.owner.equals(CodeControl.class.getTypeName().replace(".", "/"))) {
					if (mnode.name.contains("STORE")) {
						if (mnode.name.equals("ASTORE") && mnode.getNext() instanceof TypeInsnNode) {
							skip++;
						}
						instructions.remove(mnode);
					} else if (mnode.name.contains("LOAD")) {
						int var;
						if (mnode.getPrevious() instanceof InsnNode) {
							var = mnode.getPrevious().getOpcode() - 3;
						} else {
							IntInsnNode value = (IntInsnNode) mnode.getPrevious();
							var = value.operand;
						}
						instructions.remove(instructions.get(nodeInd - 1));
						instructions.set(mnode, new VarInsnNode(getOpcode(mnode.name), var));
					}
					continue;
				}
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
						if (stackEntry != null && stackEntry.equals(transformerNode.name)) {
							((FrameNode) node).stack.set(index, cls.name);
						}
						index++;
					}
				}
				if (((FrameNode) node).local != null) {
					int index = 0;
					for (Object localEntry : ((FrameNode) node).local) {
						if (localEntry != null && localEntry.equals(transformerNode.name)) {
							((FrameNode) node).local.set(index, cls.name);
						}
						index++;
					}
				}
			} else if (node instanceof TypeInsnNode) {
				TypeInsnNode tnode = (TypeInsnNode) node;
				ClassNode clsT = null;
				try {
					clsT = pool.getClassNode(tnode.desc);
				} catch (ClassNotFoundException e1) {
				}
				if (clsT != null) {
					if (AnnotationInfo.isAnnotationPresent(TargetClass.class, clsT)) {
						String newClName = AnnotationInfo.getAnnotation(TargetClass.class, clsT).get("target");
						tnode.desc = Fluid.mapClass(newClName).replace(".", "/");
					}
				}
			} else if (node instanceof LdcInsnNode) {
				LdcInsnNode tnode = (LdcInsnNode) node;
				if (tnode.cst instanceof Type) {
					Type type = (Type) tnode.cst;
					ClassNode clsT = null;
					try {
						clsT = pool.getClassNode(Fluid.parseDescriptor(type.getDescriptor()));
					} catch (ClassNotFoundException e1) {
					}
					if (clsT != null) {
						if (AnnotationInfo.isAnnotationPresent(TargetClass.class, clsT)) {
							String newClName = AnnotationInfo.getAnnotation(TargetClass.class, clsT).get("target");
							tnode.cst = Type.getType("L" + Fluid.mapClass(newClName).replace(".", "/") + ";");
						}
					}
				}
			}

			nodeInd++;
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
