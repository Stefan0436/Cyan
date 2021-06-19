package org.asf.cyan;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.fluid.bytecode.BytecodeExporter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public class PcCLI extends CyanComponent {
	public static void main(String[] args) throws IOException {
		MtkBootstrap.strap();

		if (args.length >= 2) {
			File output = new File(args[args.length - 1]);
			ArrayList<File> input = new ArrayList<File>();
			args = Arrays.copyOf(args, args.length - 1);
			for (String str : args) {
				File file = new File(str);
				addFile(file, input);
			}
			if (!output.exists() || !output.isDirectory()) {
				System.err.println("Output is not a directory.");
				System.exit(1);
			}
			for (File f : input) {
				FileInputStream strm = new FileInputStream(f);
				ClassReader reader = new ClassReader(strm);
				strm.close();
				ClassNode cls = new ClassNode();
				reader.accept(cls, 0);

				System.out.println("Decompiling " + cls.name + "...");
				File out = new File(output, cls.name + ".pc.java");
				if (!out.getParentFile().exists())
					out.getParentFile().mkdirs();
				try {
					Files.write(out.toPath(), BytecodeExporter.classToString(cls).getBytes());
					System.out.println("+-- Done.");
				} catch (IOException e) {
					System.err.println("!-- Error: " + e);
					e.printStackTrace();
				}
			}
			return;
		}

		error();
	}

	private static void addFile(File file, ArrayList<File> input) {
		if (!file.exists()) {
			System.err.println("Warning: file " + file.getPath() + " does not exist.");
		} else if (file.isDirectory()) {
			for (File f : file.listFiles())
				addFile(f, input);
		} else if (!file.getName().endsWith(".class")) {
			System.err.println("Warning: file " + file.getPath() + " is not a class file.");
		} else {
			input.add(file);
		}
	}

	public static void error() {
		System.err.println("Usage: pseudocode <input file(s)>... <output directory>");
		System.err.println();
		System.err.println("The MTK and its CLI are licensed LGPL v3. Feel free to use these projects, as long");
		System.err.println("as you give credit to the AerialWorks Software Foundation and its contributors.");
		System.err.println("Full license can be found here: https://www.gnu.org/licenses/lgpl-3.0.txt");
		System.exit(1);
		return;
	}

}
