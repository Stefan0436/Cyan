package org.asf.cyan.api.internal.modkit.transformers._1_16.common.network;

import java.lang.reflect.Type;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.LocalVariable;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;
import org.asf.cyan.internal.modkitimpl.info.Protocols;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import net.minecraft.network.protocol.status.ServerStatus.Version;

@FluidTransformer
@TargetClass(target = "net.minecraft.network.protocol.status.ServerStatus$Version$Serializer")
public class ServerStatusModification {

	@InjectAt(location = InjectLocation.TAIL)
	@TargetType(target = "com.google.gson.JsonElement")
	public void serialize(
			@TargetType(target = "net.minecraft.network.protocol.status.ServerStatus$Version") Version var1, Type var2,
			JsonSerializationContext var3, @LocalVariable JsonObject data) {

		JsonObject modkitData = new JsonObject();
		modkitData.addProperty("protocol", Protocols.MODKIT_PROTOCOL);
		modkitData.addProperty("protocol.min", Protocols.MIN_MODKIT);
		modkitData.addProperty("protocol.max", Protocols.MAX_MODKIT);

		JsonObject modloaderData = new JsonObject();
		modloaderData.addProperty("protocol", Protocols.LOADER_PROTOCOL);
		modloaderData.addProperty("protocol.min", Protocols.MIN_LOADER);
		modloaderData.addProperty("protocol.max", Protocols.MAX_LOADER);

		JsonObject main = new JsonObject();
		main.addProperty("name", Modloader.getModloader().getName());
		main.addProperty("version", Modloader.getModloader().getVersion().toString());
		modloaderData.add("main", main);

		JsonArray loaders = new JsonArray();
		for (Modloader loader : Modloader.getAllModloaders()) {
			JsonObject modloader = new JsonObject();
			modloader.addProperty("name", loader.getName());
			modloader.addProperty("version", loader.getVersion().toString());
			modloader.addProperty("type", loader.getClass().getTypeName());

			modloader.addProperty("allmods.known.count", loader.getKnownModsCount());
			JsonArray mods = new JsonArray();
			for (IModManifest mod : loader.getLoadedMods()) {
				JsonObject modinfo = new JsonObject();
				modinfo.addProperty("id", mod.id());
				modinfo.addProperty("version", mod.version().toString());
				modinfo.addProperty("description", mod.description());
				mods.add(modinfo);
			}
			modloader.add("mods", mods);
			JsonArray coremods = new JsonArray();
			for (IModManifest mod : loader.getLoadedCoremods()) {
				JsonObject modinfo = new JsonObject();
				modinfo.addProperty("id", mod.id());
				modinfo.addProperty("version", mod.version().toString());
				modinfo.addProperty("description", mod.description());
				coremods.add(modinfo);
			}
			modloader.add("coremods", mods);

			loaders.add(modloader);
		}
		modloaderData.add("all", loaders);

		modkitData.add("modloader", modloaderData);
		data.add("modkit", modkitData);
	}

}
