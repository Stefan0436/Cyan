package org.asf.cyan.fluid;

import static org.junit.Assert.assertTrue;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Random;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.asf.cyan.fluid.deobfuscation.DeobfuscationTargetMap;
import org.asf.cyan.fluid.remapping.FluidClassRemapper;
import org.asf.cyan.fluid.remapping.FluidMemberRemapper;
import org.asf.cyan.fluid.remapping.Mapping;
import org.junit.Test;
import org.objectweb.asm.tree.ClassNode;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;


public class FluidTest extends CyanComponent {
	Random rnd = new Random();
	static ArrayList<Mapping<?>> loadedMappings = new ArrayList<Mapping<?>>();

	@Test
	public void deobfuscateTest()
			throws IOException, ClassNotFoundException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException, URISyntaxException {
		TestMappings mappings = new TestMappings().create();
		FluidClassPool pool = FluidClassPool.create();

		ClassNode[] classes = new ClassNode[] { 
			pool.getClassNode("aaa"), 
			pool.getClassNode("SomeTest"),
			pool.getClassNode("aab"), 
			pool.getClassNode("aac"), 
			pool.getClassNode("aac$a1")
		};

		DeobfuscationTargetMap mp = Fluid.createTargetMap(classes, pool, mappings);
		Fluid.deobfuscate(classes, pool, mappings);
		FluidMemberRemapper fluidmemberremapper = Fluid.createMemberRemapper(mp);
		FluidClassRemapper fluidclsremapper = Fluid.createClassRemapper(mp);
		
		Fluid.remapClasses(fluidmemberremapper, fluidclsremapper, pool, classes);

		URL url = FluidTest.class.getProtectionDomain().getCodeSource().getLocation();
		for (Mapping<?> map : mappings.mappings) {
			URI u = url.toURI();
			byte[] byteCode = pool.getByteCode(map.name);
			BufferedOutputStream strm = new BufferedOutputStream(
					new FileOutputStream(new File(u.getPath(), map.name.replaceAll("\\.", "/") + ".class")));
			strm.write(byteCode);
			strm.flush();
			strm.close();
		}
		
		pool.close();
		Class<?> cls = Class.forName("org.asf.cyan.fluid.Tester");
		assertTrue((boolean) cls.getMethod("testIt", String.class, String.class).invoke(null, "Hello", "Goodbye"));
	}

	@Test
	public void descriptorTests() {
		String descs = "";
		ArrayList<String> descsArr = new ArrayList<String>();

		ConfigurationBuilder conf = ConfigurationBuilder.build(ClassLoader.getSystemClassLoader());
		for (Package p : getClass().getClassLoader().getDefinedPackages()) {
			String rname = p.getName().replace(".", "/");
			try {
				Enumeration<URL> roots = getClass().getClassLoader().getResources(rname);
				for (URL i : Collections.list(roots)) {
					conf.addUrls(i);
				}
			} catch (IOException ex) {
			}
		}
		Reflections reflections = new Reflections(conf);
		String[] types = reflections.getSubTypesOf(Map.class).stream().map(t -> t.getTypeName())
				.toArray(t -> new String[t]);

		int l = rnd.nextInt(160);
		for (int i = 0; i < l; i++) {
			int arrays = 0;
			for (int i2 = 0; i2 < rnd.nextInt(30); i2++) {
				arrays += (rnd.nextInt(4) == 1 ? 1 : 0);
			}
			String suffix = "";
			String prefix = "";
			for (int s = 0; s < arrays; s++) {
				suffix += "[]";
				prefix += "[";
			}
			int r = rnd.nextInt(Fluid.descriptors.keySet().size() + (l / 7));
			if (r >= Fluid.descriptors.keySet().size()) {
				String type = types[rnd.nextInt(types.length)] + suffix;
				descsArr.add(type);
				descs += Fluid.getDescriptor(type);
			} else {
				char c = Fluid.descriptors.keySet().toArray(t -> new Character[t])[r];
				descs += prefix + c;
				descsArr.add(Fluid.descriptors.get(c) + suffix);
			}
		}
		String[] args = Fluid.parseMultipleDescriptors(descs);
		for (int i = 0; i < descsArr.size(); i++) {
			assertTrue(args[i].equals(descsArr.get(i)));
		}
	}
}
