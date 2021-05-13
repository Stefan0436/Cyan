package org.asf.cyan.api.internal.modkit.components._1_16.common.resources.api;

import java.io.IOException;
import java.io.InputStream;

import org.asf.cyan.api.events.objects.resources.ResourceManagerEventObject;
import org.asf.cyan.api.events.resources.manager.ResourceManagerStartupEvent;
import org.asf.cyan.api.internal.IModKitComponent;
import org.asf.cyan.api.internal.modkit.components._1_16.common.resources.CyanPackResources;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.api.resources.Resource;
import org.asf.cyan.api.resources.Resources;
import org.asf.cyan.core.CyanInfo;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;
import org.asf.cyan.mods.internal.BaseEventController;

import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

public class ResourcesImplementation extends Resources implements IModKitComponent, IEventListenerContainer {

	private IModManifest ownerMod = null;
	private String owner = "";
	private static ResourceManager manager;

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
		BaseEventController.addEventContainer(this);
	}

	@SimpleEvent(value = ResourceManagerStartupEvent.class, synchronize = true)
	private void startResourceManager(ResourceManagerEventObject event) {
		manager = event.getResourceManager();
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
		if (CyanPackResources.getInstance() != null) {
			if (CyanInfo.getSide() == GameSide.CLIENT) {
				InputStream strm = CyanPackResources.getInstance().getResourceAsStream(PackType.CLIENT_RESOURCES,
						resource.toGameType());
				if (strm != null)
					return strm;
			}
			InputStream strm = CyanPackResources.getInstance().getResourceAsStream(PackType.SERVER_DATA,
					resource.toGameType());
			if (strm != null)
				return strm;
		}

		try {
			return manager.getResource(resource.toGameType()).getInputStream();
		} catch (IOException e) {
			return null;
		}
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
