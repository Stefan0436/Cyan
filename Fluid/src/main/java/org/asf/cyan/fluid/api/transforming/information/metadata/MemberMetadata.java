package org.asf.cyan.fluid.api.transforming.information.metadata;

import org.asf.cyan.fluid.api.transforming.enums.MemberType;

public class MemberMetadata {
	MemberMetadata() {
	}

	private MemberType memType;
	private String transformerMemberName;

	private String name;
	private String type;
	private String[] types;
	
	private String desc;

	private String obfusName;
	private String obfusType;
	private String[] obfusTypes;
	
	private boolean appended = false;
	
	private int oldMod = -1;
	private int newMod = -1;

	void assign(MemberType meberType, String transformerMemberName, String ownerDeobf, String name, String desc, String type, String[] types, int oldMod, int newMod, boolean appended) {
		this.name = name;
		this.type = type;
		this.types = types;
		this.memType = meberType;
		this.transformerMemberName = transformerMemberName;
		this.oldMod = oldMod;
		this.newMod = newMod;
		this.appended = appended;
		this.desc = desc;
		
		if (memType == MemberType.METHOD) {
			obfusName = TransformerMetadata.getImplmentationInstance().mapMethod(ownerDeobf, name, types);
			obfusTypes = new String[types.length];
			int index = 0;
			for (String mType : types) {
				obfusTypes[index++] = TransformerMetadata.getImplmentationInstance().mapClass(mType);
			}
		} else {
			obfusName = TransformerMetadata.getImplmentationInstance().mapField(ownerDeobf, name, type);
		}
		obfusType = TransformerMetadata.getImplmentationInstance().mapClass(type);
	}

	public MemberType getMemberType() {
		return memType;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public String[] getTypes() {
		return types;
	}
	
	public String getTransformerMemberName() {
		return transformerMemberName;
	}
	
	public int getOldModifier() {
		return oldMod;
	}

	public int getNewModifier() {
		return newMod;
	}

	public String toDescriptor() {
		if (memType == MemberType.METHOD) {
			return TransformerMetadata.toDescriptor(type, types);
		} else {
			return TransformerMetadata.toDescriptor(type, null);
		}
	}

	public String getMappedName() {
		return obfusName;
	}

	public String getMappedType() {
		return obfusType;
	}

	public String[] getMappedTypes() {
		return obfusTypes;
	}
	
	public boolean isNew() {
		return appended;
	}

	public String toMappedDescriptor() {
		if (memType == MemberType.METHOD) {
			return TransformerMetadata.toDescriptor(obfusType, obfusTypes);
		} else {
			return TransformerMetadata.toDescriptor(obfusType, null);
		}
	}

	public String toTransformerDescriptor() {
		return desc;
	}
}
