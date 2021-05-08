package org.asf.cyan.fluid.implementation;

import java.util.ArrayList;

import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.api.transforming.enums.MemberType;
import org.asf.cyan.fluid.api.transforming.information.metadata.MemberMetadata;
import org.asf.cyan.fluid.api.transforming.information.metadata.TransformerMetadata;
import org.asf.cyan.fluid.bytecode.FluidClassPool;

/**
 * 
 * Cyan implementation of the Transformer Metadata System.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@CYAN_COMPONENT
public class CyanTransformerMetadata extends TransformerMetadata {

	private static ArrayList<TransformerMetadata> transformers = new ArrayList<TransformerMetadata>();

	private ArrayList<MemberMetadata> fields = new ArrayList<MemberMetadata>();
	private ArrayList<MemberMetadata> methods = new ArrayList<MemberMetadata>();

	private static FluidClassPool cp;
	private String trCls = "";
	private String trOwner = "";
	private String targetMapped = "";
	private String target = "";

	public static void initComponent() {
		setImplementation(new CyanTransformerMetadata());
	}

	@Override
	public String getTransfomerClass() {
		return trCls;
	}

	@Override
	public String getTargetClass() {
		return target;
	}

	@Override
	public String getMappedTargetClass() {
		return targetMapped;
	}

	@Override
	public MemberMetadata[] getTransformedFields() {
		return fields.toArray(t -> new MemberMetadata[t]);
	}

	@Override
	public MemberMetadata[] getTransformedMethods() {
		return methods.toArray(t -> new MemberMetadata[t]);
	}

	@Override
	protected String getImplementationName() {
		return "Cyan";
	}

	@Override
	protected TransformerMetadata getNewInstance() {
		return new CyanTransformerMetadata();
	}

	@Override
	protected void storeInstance(TransformerMetadata metadata) {
		transformers.add(metadata);
	}

	@Override
	protected boolean validateNewInstane(TransformerMetadata metadata) {
		return true;
	}

	@Override
	protected TransformerMetadata[] internalGetLoadedTransformers() {
		return transformers.toArray(t -> new TransformerMetadata[t]);
	}

	@Override
	protected TransformerMetadata getMetadataByTransformer(String transformerName) {
		for (TransformerMetadata md : transformers) {
			if (md.getTransfomerClass().equals(transformerName))
				return md;
		}
		return null;
	}

	@Override
	protected TransformerMetadata[] getMetadataByObfusTarget(String targetObfus) {
		ArrayList<TransformerMetadata> items = new ArrayList<TransformerMetadata>();
		for (TransformerMetadata md : transformers) {
			if (md.getMappedTargetClass().equals(targetObfus))
				items.add(md);
		}
		return items.toArray(t -> new TransformerMetadata[t]);
	}

	@Override
	protected TransformerMetadata[] getMetadataByDeobfTarget(String targetDeobf) {
		ArrayList<TransformerMetadata> items = new ArrayList<TransformerMetadata>();
		for (TransformerMetadata md : transformers) {
			if (md.getTargetClass().equals(targetDeobf))
				items.add(md);
		}
		return items.toArray(t -> new TransformerMetadata[t]);
	}

	@Override
	protected String toDesc(String type, String[] types) {
		if (types != null) {
			return "(" + Fluid.getDescriptors(types) + ")" + Fluid.getDescriptor(type);
		} else {
			return Fluid.getDescriptor(type);
		}
	}

	@Override
	protected MemberMetadata parseMethod(String owner, String transformerMemberName, String name, String desc, String[] types,
			String returnType, int oldMod, int newMod, boolean isNew) {
		MemberMetadata md = this.constructMetadata();
		this.assignMetadataValues(md, MemberType.METHOD, owner, transformerMemberName, name, desc, returnType, types, oldMod,
				newMod, isNew);
		return md;
	}

	@Override
	protected MemberMetadata parseField(String owner, String transformerMemberName, String name, String type,
			int oldMod, int newMod, boolean isNew) {
		MemberMetadata md = this.constructMetadata();
		this.assignMetadataValues(md, MemberType.FIELD, owner, transformerMemberName, name, null, type, null, oldMod, newMod,
				isNew);
		return md;
	}

	@Override
	protected void storeMember(MemberMetadata metadata) {
		if (metadata.getMemberType() == MemberType.FIELD)
			fields.add(metadata);
		else
			methods.add(metadata);
	}

	@Override
	protected String[] parseParams(String descriptor) {
		String typesStr = "";
		if (descriptor.contains(")"))
			typesStr = descriptor.substring(1, descriptor.lastIndexOf(")"));
		return Fluid.parseMultipleDescriptors(typesStr);
	}

	@Override
	protected String parseType(String descriptor) {
		String type = descriptor;
		if (descriptor.contains(")"))
			type = type.substring(type.lastIndexOf(")") + 1);
		return Fluid.parseDescriptor(type);
	}

	@Override
	protected String mapClass(String className) {
		return Fluid.mapClass(className);
	}

	@Override
	protected String mapMethod(String className, String methodName, String[] types) {
		return Fluid.mapMethod(className, methodName, types);
	}

	@Override
	protected String mapField(String className, String fieldName, String type) {
		return Fluid.mapProperty(className, fieldName);
	}

	@Override
	protected void assignTransformer(TransformerMetadata data, String transformerName, String transformerOwner,
			String target, String mappedTarget, FluidClassPool pool) {
		CyanTransformerMetadata md = (CyanTransformerMetadata) data;
		md.target = target;
		md.targetMapped = mappedTarget;
		md.trOwner = transformerOwner;
		md.trCls = transformerName;
		if (cp == null)
			cp = pool;
	}

	@Override
	protected String[] parseIdentifier(String identifier, String owner) {
		int oldMod = -1;
		String name = identifier;
		String desc = "";
		String isNew = "false";
		if (name.contains(" ")) {
			desc = name.substring(name.indexOf(" ") + 1);
			name = name.substring(0, name.indexOf(" "));
		}
		if (desc.contains(" ")) {
			owner = desc.substring(desc.indexOf(" ") + 1);
			desc = desc.substring(0, desc.indexOf(" "));
		}
		String mname = name;
		if (owner.contains(" ")) {
			mname = owner.substring(owner.indexOf(" ") + 1);
			owner = owner.substring(0, owner.indexOf(" "));
		}
		int newMod = oldMod;
		if (mname.contains(" ")) {
			String oldModStr = mname.substring(mname.indexOf(" ") + 1);
			String newModStr = oldModStr;
			mname = mname.substring(0, mname.indexOf(" "));
			if (oldModStr.contains(" ")) {
				newModStr = oldModStr.substring(oldModStr.indexOf(" ") + 1);
				oldModStr = oldModStr.substring(0, oldModStr.indexOf(" "));
				if (newModStr.contains(" ")) {
					isNew = newModStr.substring(newModStr.indexOf(" ") + 1);
					newModStr = newModStr.substring(0, newModStr.indexOf(" "));
				}
			}
			oldMod = Integer.parseInt(oldModStr);
			newMod = Integer.parseInt(newModStr);
		}

		return new String[] { name, desc, owner, mname, "" + oldMod, "" + newMod, "" + isNew };
	}

	@Override
	public String getTransfomerOwner() {
		return trOwner;
	}

	@Override
	protected FluidClassPool getClassPool() {
		return cp;
	}

}
