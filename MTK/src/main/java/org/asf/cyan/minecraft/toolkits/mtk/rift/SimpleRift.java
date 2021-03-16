package org.asf.cyan.minecraft.toolkits.mtk.rift;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.asf.cyan.fluid.deobfuscation.DeobfuscationTargetMap;
import org.asf.cyan.fluid.remapping.Mapping;
import org.objectweb.asm.tree.ClassNode;

/**
 * 
 * SimpleRift - simplifies the rift toolchain.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class SimpleRift extends CyanComponent implements Closeable {
	SimpleRift() {
	}

	byte[] serialize(Object obj) throws IOException {
		ByteArrayOutputStream strm = new ByteArrayOutputStream();
		ObjectOutputStream serializer = new ObjectOutputStream(strm);
		serializer.writeObject(obj);
		return strm.toByteArray();
	}

	@SuppressWarnings("unchecked")
	<T> T deserialize(byte[] data) throws IOException, ClassNotFoundException {
		ByteArrayInputStream strm = new ByteArrayInputStream(data);
		ObjectInputStream deserializer = new ObjectInputStream(strm);
		Object obj = deserializer.readObject();
		strm.close();
		return (T) obj;
	}

	void assign(FluidClassPool pool1, FluidClassPool pool2, File saveFile, Mapping<?>[] mappingsCCFG) {
		libraryPool = pool1;
		sourcesPool = pool2;
		this.saveFile = saveFile;
		this.mappingsCCFG = mappingsCCFG;
	}

	private FluidClassPool libraryPool;
	private FluidClassPool sourcesPool;
	private DeobfuscationTargetMap mappings;
	private File saveFile;
	private Mapping<?>[] mappingsCCFG;

	private boolean applied = false;

	/**
	 * Get the helper classes, used for the rift process, left untouched.
	 * 
	 * @return Array of class nodes.
	 */
	public ClassNode[] getHelperClasses() {
		return libraryPool.getLoadedClasses();
	}

	/**
	 * Get the input/output classes.
	 * 
	 * @return Array of class nodes.
	 */
	public ClassNode[] getClasses() {
		return sourcesPool.getLoadedClasses();
	}

	/**
	 * Apply the rift process to the source classes.
	 * 
	 * @throws IOException            If applying fails. Also thrown if the rift
	 *                                process ahs already been applied before.
	 * @throws ClassNotFoundException If applying fails.
	 */
	public void apply() throws IOException, ClassNotFoundException {
		if (!applied) {
			if (saveFile != null && saveFile.exists()) {
				info("Loading RIFT binary mappings...");
				mappings = deserialize(Files.readAllBytes(saveFile.toPath()));
			} else {
				info("Creating RIFT target mappings...");
				mappings = Fluid.createTargetMap(getHelperClasses(), libraryPool, mappingsCCFG);

				if (saveFile != null) {
					if (!saveFile.getParentFile().exists()) {
						saveFile.getParentFile().mkdirs();
					}

					info("Writing RIFT binary mappings for future use...");
					Files.write(saveFile.toPath(), serialize(mappings));
				}
			}

			info("Remapping...");
			Fluid.remapClasses(Fluid.createMemberRemapper(mappings), Fluid.createClassRemapper(mappings), sourcesPool,
					getClasses());

			applied = true;
		} else {
			throw new IOException("Rift already applied, cannot re-apply.");
		}
	}

	@Override
	public void close() throws IOException {
		libraryPool.close();
		sourcesPool.close();
		mappings.clear();
		saveFile = null;
	}

	/**
	 * Converts a class in the class pool to bytecode.
	 */
	public byte[] toByteCode(String classPath) {
		return sourcesPool.getByteCode(classPath);
	}

	/**
	 * Retrieves a class in the class pool.
	 */
	public ClassNode getClass(String classPath) {
		try {
			return sourcesPool.getClassNode(classPath);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
}
