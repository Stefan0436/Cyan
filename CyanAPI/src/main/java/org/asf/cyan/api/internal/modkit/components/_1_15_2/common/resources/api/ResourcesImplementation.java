package org.asf.cyan.api.internal.modkit.components._1_15_2.common.resources.api;

import java.io.IOException;
import java.io.InputStream;

import org.asf.cyan.api.internal.IModKitComponent;
import org.asf.cyan.api.internal.modkit.components._1_15_2.common.resources.CyanPackResources;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.api.resources.Resource;
import org.asf.cyan.api.resources.Resources;
import org.asf.cyan.core.CyanInfo;

import net.minecraft.server.packs.PackType;

public class ResourcesImplementation extends Resources implements IModKitComponent {

	private IModManifest ownerMod = null;
	private String owner = "";

	@Override
	protected void setDefaultOwner(IModManifest mod) {
		this.ownerMod = mod;
		this.owner = mod.id().replace(":", ".");
	}

	@Override
	protected void setDefaultOwner(String owner) {
		this.owner = owner.replace(":", ".");
	}

	@Override
	public void initializeComponent() {
		implementation = this;
	}

	@Override
	protected Resources newInstance() {
		return new ResourcesImplementation();
	}

	@Override
	public Resource getResource(String location) {
		return new ResourceImplementation(location, owner, this);
	}

	@Override
	public Resource getResource(String owner, String location) {
		return new ResourceImplementation(location, owner.replace(":", "."), this);
	}

	@Override
	public String getDefaultOwner() {
		return owner;
	}

	@Override
	public IModManifest getMod() {
		return ownerMod;
	}

	@Override
	public InputStream getResourceAsStream(Resource resource) {
		if (CyanInfo.getSide() == GameSide.CLIENT) {
			InputStream strm = CyanPackResources.getInstance().getResourceAsStream(PackType.CLIENT_RESOURCES,
					resource.toGameType());
			if (strm != null)
				return strm;
		}
		return CyanPackResources.getInstance().getResourceAsStream(PackType.SERVER_DATA, resource.toGameType());

	}

	@Override
	public String getResourceAsString(Resource resource) {
		InputStream strm = getResourceAsStream(resource);
		if (strm == null)
			return null;
		else {
			try {
				String str = new String(strm.readAllBytes());
				strm.close();
				return str;
			} catch (IOException e) {
				return null;
			}
		}
	}

}
