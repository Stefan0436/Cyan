package modkit.util.remotedata;

import java.util.LinkedHashMap;
import java.util.Map;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.versioning.Version;

import modkit.protocol.ModKitModloader.ModKitProtocolRules;

/**
 * 
 * Remote modloader -- provides information about remote modloaders
 * 
 * @since Modkit 1.3
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public class RemoteModloader {

	private String name;
	private String simpleName;
	private String typeName;
	private Version version;

	private ModKitProtocolRules protocolRules;

	private Version gameVersion;

	private RemoteMod[] coremods;
	private RemoteMod[] mods;

	private Map<String, Version> ruleEntries;

	public RemoteModloader(String name, String simpleName, String typeName, Version version,
			ModKitProtocolRules protocols, Version gameVersion, Map<String[], String> coremods,
			Map<String[], String> mods, Map<String, Version> ruleEntries) {
		this.name = name;
		this.typeName = typeName;
		this.version = version;
		this.simpleName = simpleName;
		this.protocolRules = protocols;
		this.gameVersion = gameVersion;
		this.ruleEntries = ruleEntries;

		this.mods = mods.keySet().stream().map(t -> {
			return new RemoteMod(t[0], t[1], Version.fromString(mods.get(t)), RemoteModType.MOD, findInstance());
		}).toArray(t -> new RemoteMod[t]);

		this.coremods = coremods.keySet().stream().map(t -> {
			return new RemoteMod(t[0], t[1], Version.fromString(coremods.get(t)), RemoteModType.COREMOD,
					findInstance());
		}).toArray(t -> new RemoteMod[t]);
	}

	/**
	 * Tries to retrieve the Modloader instance, returns null if not installed on
	 * this client.
	 * 
	 * @return Modloader instance or null
	 */
	public Modloader findInstance() {
		Modloader inst = Modloader.getModloaderByTypeName(typeName);
		if (inst != null)
			return inst;
		return Modloader.getModloader(name);
	}

	/**
	 * Retrieves the modloader version
	 * 
	 * @return Modloader version
	 */
	public Version getVersion() {
		return version;
	}

	/**
	 * Retrieves the modloader simple name
	 * 
	 * @return Modloader simple name
	 */
	public String getSimpleName() {
		return simpleName;
	}

	/**
	 * Retrieves the modloader name
	 * 
	 * @return Modloader name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves the modloader type name
	 * 
	 * @return Modloader type name
	 */
	public String getTypeName() {
		return typeName;
	}

	/**
	 * Retrieves an array of coremods loaded by the remote modloader
	 * 
	 * @return Array of {@link RemoteMod} instances.
	 */
	public RemoteMod[] getCoreMods() {
		return coremods.clone();
	}

	/**
	 * Retrieves an array of mods loaded by the remote modloader
	 * 
	 * @return Array of {@link RemoteMod} instances.
	 */
	public RemoteMod[] getMods() {
		return mods.clone();
	}

	/**
	 * Retrieves a remote (core)mod instance
	 * 
	 * @param id Mod ID
	 * @return RemoteMod instance or null
	 */
	public RemoteMod getMod(String id) {
		for (RemoteMod md : mods)
			if (md.getModID().equalsIgnoreCase(id))
				return md;
		for (RemoteMod md : coremods)
			if (md.getModID().equalsIgnoreCase(id))
				return md;
		return null;
	}

	/**
	 * Retrieves a remote (core)mod instance
	 * 
	 * @param group Mod group
	 * @param id    Mod ID
	 * @return RemoteMod instance or null
	 */
	public RemoteMod getMod(String group, String id) {
		return getMod(group + ":" + id);
	}

	/**
	 * Checks if a the given (core)mod ID is present on the remote end
	 * 
	 * @param group Mod group
	 * @param id    Mod ID
	 * @return True if loaded on the remote end, false otherwise
	 */
	public boolean isModLoaded(String group, String id) {
		return isModLoaded(group + ":" + id);
	}

	/**
	 * Checks if a the given (core)mod ID is present on the remote end
	 * 
	 * @param id Mod ID
	 * @return True if loaded on the remote end, false otherwise
	 */
	public boolean isModLoaded(String id) {
		for (RemoteMod md : mods)
			if (md.getModID().equalsIgnoreCase(id))
				return true;
		for (RemoteMod md : coremods)
			if (md.getModID().equalsIgnoreCase(id))
				return true;
		return false;
	}

	/**
	 * Retrieves a remote mod instance
	 * 
	 * @param id Mod ID
	 * @return RemoteMod instance or null
	 */
	public RemoteMod getRegularMod(String id) {
		for (RemoteMod md : mods)
			if (md.getModID().equalsIgnoreCase(id))
				return md;
		return null;
	}

	/**
	 * Retrieves a remote mod instance
	 * 
	 * @param group Mod group
	 * @param id    Mod ID
	 * @return RemoteMod instance or null
	 */
	public RemoteMod getRegularMod(String group, String id) {
		return getRegularMod(group + ":" + id);
	}

	/**
	 * Checks if a the given mod ID is present on the remote end
	 * 
	 * @param group Mod group
	 * @param id    Mod ID
	 * @return True if loaded on the remote end, false otherwise
	 */
	public boolean isRegularModLoaded(String group, String id) {
		return isRegularModLoaded(group + ":" + id);
	}

	/**
	 * Checks if a the given mod ID is present on the remote end
	 * 
	 * @param id Mod ID
	 * @return True if loaded on the remote end, false otherwise
	 */
	public boolean isRegularModLoaded(String id) {
		for (RemoteMod md : mods)
			if (md.getModID().equalsIgnoreCase(id))
				return true;
		return false;
	}

	/**
	 * Retrieves a remote coremod instance
	 * 
	 * @param id Mod ID
	 * @return RemoteMod instance or null
	 */
	public RemoteMod getCoreMod(String id) {
		for (RemoteMod md : coremods)
			if (md.getModID().equalsIgnoreCase(id))
				return md;
		return null;
	}

	/**
	 * Retrieves a remote coremod instance
	 * 
	 * @param group Mod group
	 * @param id    Mod ID
	 * @return RemoteMod instance or null
	 */
	public RemoteMod getCoreMod(String group, String id) {
		return getCoreMod(group + ":" + id);
	}

	/**
	 * Checks if a the given coremod ID is present on the remote end
	 * 
	 * @param group Mod group
	 * @param id    Mod ID
	 * @return True if loaded on the remote end, false otherwise
	 */
	public boolean isCorerModLoaded(String group, String id) {
		return isCorerModLoaded(group + ":" + id);
	}

	/**
	 * Checks if a the given coremod ID is present on the remote end
	 * 
	 * @param id Mod ID
	 * @return True if loaded on the remote end, false otherwise
	 */
	public boolean isCorerModLoaded(String id) {
		for (RemoteMod md : coremods)
			if (md.getModID().equalsIgnoreCase(id))
				return true;
		return false;
	}

	/**
	 * Retrieves the modloader protocol version (returns -1 if not present)
	 * 
	 * @return Modloader protocol version or -1 if not present
	 */
	public double getModloaderProtocolVersion() {
		return protocolRules == null ? -1 : protocolRules.modloaderProtocol();
	}

	/**
	 * Retrieves the modloader ModKit protocol version (returns -1 if not present)
	 * 
	 * @return Modloader ModKit protocol version or -1 if not present
	 */
	public double getModKitProtocolVersion() {
		return protocolRules == null ? -1 : protocolRules.modkitProtocolVersion();
	}

	/**
	 * Retrieves the modloader minimal protocol version (returns -1 if not present)
	 * 
	 * @return Modloader minimal protocol version or -1 if not present
	 */
	public double getModloaderMinProtocolVersion() {
		return protocolRules == null ? -1 : protocolRules.modloaderMinProtocol();
	}

	/**
	 * Retrieves the modloader maximal protocol version (returns -1 if not present)
	 * 
	 * @return Modloader maximal protocol version or -1 if not present
	 */
	public double getModloaderMaxProtocolVersion() {
		return protocolRules == null ? -1 : protocolRules.modloaderMaxProtocol();
	}

	/**
	 * Retrieves the modloader game version (returns null if not present)
	 * 
	 * @return Modloader game version or null if not present
	 */
	public Version getGameVersion() {
		return gameVersion;
	}

	/**
	 * Retrieves the handshake rule entry map
	 * 
	 * @return Map of handshake rule entries
	 */
	public Map<String, Version> getRuleEntries() {
		return new LinkedHashMap<String, Version>(ruleEntries);
	}

}
