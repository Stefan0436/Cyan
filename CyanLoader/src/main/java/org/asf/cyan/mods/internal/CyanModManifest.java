package org.asf.cyan.mods.internal;

import java.util.stream.Stream;

import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.mods.AbstractMod;

public class CyanModManifest implements IModManifest {

	private AbstractMod owner;

	public CyanModManifest(AbstractMod owner) {
		this.owner = owner;
	}

	public AbstractMod getInstance() {
		return owner;
	}

	@Override
	public String id() {
		return owner.getId();
	}

	@Override
	public String displayName() {
		return owner.getDisplayName();
	}

	@Override
	public String[] dependencies() {
		return Stream.of(owner.getDependencies()).map(t -> t.getId()).toArray(t -> new String[t]);
	}

	@Override
	public String description() {
		return owner.getDescription();
	}

	@Override
	public String[] optionalDependencies() {
		return owner.getOptionalDependencies();
	}

	@Override
	public Version version() {
		return owner.getVersion();
	}

}
