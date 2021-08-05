package org.asf.cyan;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.asf.cyan.CtcUtil.HttpCredential;
import org.asf.cyan.security.TrustContainerBuilder;

public class CtcCLI {
	static int max = 0;
	private static Object[] credentials = null;
	private static Scanner in = new Scanner(System.in);
	private static URL dest;
	private static String lastGroup = "";

	private static HttpBasicCredential cred1;
	private static HttpBearerCredential cred2;

	public static class HttpBasicCredential extends HttpCredential {

		@Override
		public String buildHeader(String group, String type) {
			Object[] cred = getCredentials(group);
			if (cred == null)
				return null;
			return Base64.getEncoder().encodeToString((cred[0] + ":" + cred[1]).getBytes());
		}

		@Override
		public boolean supportsAuthMethod(String type) {
			return type.equals("Basic");
		}

		@Override
		public boolean supportsGroup(String group) {
			return true;
		}

	}

	public static class HttpBearerCredential extends HttpCredential {

		@Override
		public String buildHeader(String group, String type) {
			System.out.print("Authorization bearer token (often a JWT): ");
			char[] token = System.console().readPassword();
			return "Bearer " + new String(token);
		}

		@Override
		public boolean supportsGroup(String group) {
			return true;
		}

		@Override
		public boolean supportsAuthMethod(String type) {
			return type.equals("Bearer");
		}

	}

	public static void main(String[] args) throws IOException {
		if (cred1 == null)
			cred1 = new HttpBasicCredential();
		if (cred2 == null)
			cred2 = new HttpBearerCredential();

		if (args.length < 3 && args.length >= 1 && args[0].equalsIgnoreCase("manual")) {
			startManual();
			return;
		} else if (args.length < 3 && args.length >= 1 && args[0].equalsIgnoreCase("connectiveurl")) {
			Scanner sc = new Scanner(System.in);
			System.out.print("Server protocol (http/https): ");
			String protocol = sc.nextLine();
			System.out.print("Server host (without port): ");
			String host = sc.nextLine();
			System.out.print("Server port (empty for default): ");
			String port = sc.nextLine();
			if (!port.isEmpty())
				host += ":" + port;

			System.out.print("Mod group (eg. org.example): ");
			String modgroup = sc.nextLine();
			System.out.print("Mod id (eg. examplemod): ");
			String modid = sc.nextLine();

			System.out.print("Mod version (eg. 1.0.0.A1): ");
			String ver = sc.nextLine();
			System.out.print("Trust container name (eg My Trust): ");
			String cont = sc.nextLine();

			System.out.println(protocol + "://" + host + "/cyan/trust/upload/" + modgroup + "/" + modid + "?trustname="
					+ URLEncoder.encode(cont, "UTF-8") + "&modversion=" + URLEncoder.encode(ver, "UTF-8") + "&file=");
			sc.close();
			return;
		}
		if (args.length < 3 || (!args[0].equals("zctc-create") && !args[0].equals("zctc-convert")
				&& !args[0].equals("pack") && !args[0].equals("unpack") && !args[0].equals("publish"))) {
			System.err.println("Usage: ctc pack/unpack/publish/zctc-create/zctc-convert <input> <output>");
			System.err.println("Use 'ctc manual' for an in-depth manual on how to use ctc.");
			System.out.println("");
			System.out.println("Copyright(c) 2021 AerialWorks Software Foundation,");
			System.out.println("The ClassTrust, CTC and Cyan projects are Free (Libre) Open Source Software.");
			System.out.println("");
			System.out.println("CTC and ClassTrust are licensed LGPL v3. Feel free to use these projects, as long");
			System.out.println("as you give credit to the AerialWorks Software Foundation and its contributors.");
			System.out.println("Full license can be found here: https://www.gnu.org/licenses/lgpl-3.0.txt");
			System.exit(-1);
			return;
		}
		if (args[0].equals("unpack")) {
			CtcUtil.unpack(new File(args[1]), new File(args[2]), (num) -> max = num, (num) -> {
				System.out.println("Extracted: " + num + " / " + max);
			});
		} else if (args[0].equals("pack")) {
			CtcUtil.pack(new File(args[1]), new File(args[2]), (num) -> max = num, (num) -> {
				System.out.println("Added: " + num + " / " + max);
			});
		} else if (args[0].equals("publish")) {
			dest = new URL(args[2]);
			CtcUtil.publish(new File(args[1]), dest, List.of(cred1, cred2), (published) -> {
				if (published.startsWith("/"))
					published = published.substring(1);
				if (!args[2].endsWith("/"))
					args[2] = args[2] + "/";
				System.out.println("Published: " + published + " -> " + args[2] + published);
			});
			in.close();
		} else if (args[0].equals("zctc-create")) {
			File output = File.createTempFile("zctc-tmp-", "-uctc");
			output.delete();
			CtcUtil.unpack(new File(args[1]), output, (num) -> max = num, (num) -> {
				System.out.println("Extracted to temporary directory: " + num + " / " + max);
			});
			System.out.println("Building ZCTC...");

			try {
				ZipOutputStream strm = new ZipOutputStream(new FileOutputStream(new File(args[2])));
				zipDir(output, "", strm, countFiles(output), 1);
				strm.close();
			} finally {
				delDir(output);
			}
		} else if (args[0].equals("zctc-convert")) {
			String name = null;
			ZipInputStream strm = new ZipInputStream(new FileInputStream(new File(args[1])));
			ZipEntry entry = strm.getNextEntry();

			int amount = 0;
			while (entry != null) {
				String path = entry.getName().replace("\\", "/");
				if (path.startsWith("/"))
					path = path.substring(1);

				if (path.equals("main.header")) {
					amount++;
					String cont = new String(strm.readAllBytes());
					for (String line : cont.replace("\r", "").split("\n")) {
						if (line.startsWith("Name: ")) {
							name = line.substring("Name: ".length());
						}
					}
				} else if ((path.endsWith("/") && !path.substring(0, path.length() - 1).contains("/"))
						|| path.endsWith(".cls")) {
					amount++;
				} else {
					throw new IOException("Invalid ZCTC archive, invalid files/directories detected.");
				}

				entry = strm.getNextEntry();
			}
			strm.close();
			strm = new ZipInputStream(new FileInputStream(new File(args[1])));

			if (name == null)
				throw new IOException("Invalid ZCTC archive, missing header.");

			int count = 0;
			entry = strm.getNextEntry();
			TrustContainerBuilder builder = new TrustContainerBuilder(name);

			while (entry != null) {
				String path = entry.getName().replace("\\", "/");
				if (path.startsWith("/"))
					path = path.substring(1);

				if (path.equals("main.header")) {
					count++;
				} else if ((path.endsWith("/") && !path.substring(0, path.length() - 1).contains("/"))
						|| path.endsWith(".cls")) {
					if (path.endsWith(".cls")) {
						String packageName = path.substring(0, path.indexOf("/"));
						String clsName = path.substring(path.indexOf("/") + 1, path.indexOf(".cls"));

						ArrayList<String> hashes = new ArrayList<String>();
						String content = new String(strm.readAllBytes());
						for (String line : content.replace("\r", "").split("\n")) {
							line = line.trim().replace("\t", "    ");
							if (line.contains(" "))
								line = line.substring(0, line.indexOf(" "));

							if (!line.isEmpty()) {
								hashes.add(line);
							}
						}
						builder.addClass(packageName, clsName, hashes.toArray(t -> new String[t]));
					}
					count++;
				}
				System.out.println("Converted: " + count + " / " + amount);

				entry = strm.getNextEntry();
			}

			builder.build().exportContainer(new File(args[2]));
		}
	}

	private static int zipDir(File dir, String prefix, ZipOutputStream strm, int max, int num) throws IOException {
		for (File file : dir.listFiles((f) -> !f.isDirectory())) {
			ZipEntry entry = new ZipEntry(prefix + file.getName());
			strm.putNextEntry(entry);
			FileInputStream strmIn = new FileInputStream(file);
			strmIn.transferTo(strm);
			strmIn.close();
			strm.closeEntry();
			System.out.println("Added: " + num++ + " / " + max);
		}
		for (File subdir : dir.listFiles((d) -> d.isDirectory())) {
			ZipEntry entry = new ZipEntry(prefix + subdir.getName() + "/");
			strm.putNextEntry(entry);
			strm.closeEntry();
			num = zipDir(subdir, prefix + subdir.getName() + "/", strm, max, num);
			System.out.println("Added: " + num++ + " / " + max);
		}
		return num;
	}

	private static int countFiles(File dir) {
		int c = 0;
		for (@SuppressWarnings("unused")
		File file : dir.listFiles((f) -> !f.isDirectory()))
			c += 1;
		for (File subdir : dir.listFiles((d) -> d.isDirectory()))
			c += countFiles(subdir) + 1;
		return c;
	}

	private static void delDir(File dir) {
		for (File file : dir.listFiles((f) -> !f.isDirectory()))
			file.delete();
		for (File subdir : dir.listFiles((d) -> d.isDirectory()))
			delDir(subdir);

		dir.delete();
	}

	private static ArrayList<Runnable> pages = new ArrayList<Runnable>();

	private static void startManual() {
		pages.clear();
		pages.add(() -> {
			System.out.println("Table of contents:");
			System.out.println("Page 1      - Table of contents");
			System.out.println("Page 2      - Basic interface guide");
			System.out.println("Page 3      - Unpacking CTC containers");
			System.out.println("Page 4      - Packing CTC containers");
			System.out.println("Page 5      - Publishing CTC containers");
			System.out.println("Page 6      - ZCTC archive files.");
			System.out.println("Pages 7-8   - The CTC format");
			System.out.println("Pages 9-11  - UCTC trust folders.");
			System.out.println("Pages 12-13 - Using ClassTrust in java");
		});
		pages.add(() -> {
			System.out.println("Basic interface guide:");
			System.out.println("CTC's interface is not too difficult to understand.");
			System.out.println("");
			System.out.println("The base syntax always has 3 arguments:");
			System.out.println("- The method to use for CTC");
			System.out.println("- The input file/folder");
			System.out.println("- The output file/folder/url");
			System.out.println("");
			System.out.println("CTC has the following methods:");
			System.out.println("- pack         - creates a ctc container file");
			System.out.println("- unpack       - extracts a ctc container file into a UCTC folder structure");
			System.out.println("- zctc-create  - converts a ctc container file to ZCTC (zipped UCTC folder)");
			System.out.println("- zctc-convert - extracts a zctc container file and reconstructs it in ctc format");
			System.out.println("- publish      - publishes a ctc container file to a remote server (HTTP/HTTPS only)");
			System.out.println("");
			System.out.println("Each method has it's own manual page, use page 1 to view the table of contents");
		});
		pages.add(() -> {
			System.out.println("Unpacking CTC containers:");
			System.out.println("CTC can unpack compiled container files, whether they were created through the CLI");
			System.out.println("or programmatically through the ClassTrust library.");
			System.out.println("");
			System.out.println("Use the following command to unpack a ctc file:");
			System.out.println("ctc unpack \"<input file>\" \"<output directory>\"");
			System.out.println("");
			System.out.println("Examples:");
			System.out.println("- ctc unpack hello.ctc world/");
			System.out.println("- ctc unpack cyan/test.ctc /home/user/uctc/test");
			System.out.println("");
			System.out.println("The first example unpacks a container named 'hello.ctc' to the world folder.");
			System.out.println("The second unpacks the test.ctc file in the cyan directory to /home/user/uctc/test.");
		});
		pages.add(() -> {
			System.out.println("Packing CTC containers:");
			System.out.println(
					"CTC can reconstruct extracted ctc files, if they are in the UCTC format (ctc-cli uses this)");
			System.out.println("");
			System.out
					.println("You can read more about UCTC on page 10 and 11. This includes a guide on how to create");
			System.out.println("UCTC folder structures yourself and how to properly edit them.");
			System.out.println("");
			System.out.println("How to pack CTC containers; you can use the following command to pack UCTC folders:");
			System.out.println("ctc pack \"<input uctc>\" \"<output ctc\">");
			System.out.println("");
			System.out.println("Examples:");
			System.out.println("- ctc pack world/ hello.new.ctc");
			System.out.println("- ctc pack /home/user/uctc/test cyan/test.new.ctc");
			System.out.println("");
			System.out.println("The examples re-create the CTC files extracted in the examples of page 3.");
			System.out.println(
					"The original CTC is left untouched, because we create '.new.ctc' files instead of '.ctc'");
			System.out.println(
					"If you want to overwrite the original CTC file, use something like hello.ctc instead of hello.new.ctc");
			System.out.println("");
			System.out.println("");
			System.out.println("NOTICE:");
			System.out.println(
					"The ClassTrust library ignores manually assigned timestamps when packing, this is to prevent some issues");
			System.out.println("with remote servers. You can read more about this in the publishing guide. (page 5)");
		});
		pages.add(() -> {
			System.out.println("Publishing CTC containers:");
			System.out.println("The CTC Command Line Interface provides a way to upload your containers");
			System.out.println("to any remote HTTP/HTTPS server.");
			System.out.println("");
			System.out.println("");
			System.out.println("Publishing to a Connective HTTP server with the CTC module installed:");
			System.out.println("1. You need to create your upload base url, you can use the following template:");
			System.out.println("   http(s)://<server>[:<port>]/cyan/trust/upload/<mod group>/<mod id>");
			System.out.println("   ?trustname=<trust name>&modversion=<mod version>&file=");
			System.out.println("");
			System.out.println("   Run ctc connectiveurl to get an interactive setup.");
			System.out.println("");
			System.out.println("2. Create a SHA-256 hash file of your container:");
			System.out.println("   For Linux: sha256sum \"<container-file>\" > \"<container-file>.sha256\"");
			System.out.println(
					"   For PowerShell: echo $(Get-FileHash \"<container-file>\" SHA256).Hash > \"<container-file>.sha256\"");
			System.out.println("");
			System.out.println("3. Run 'ctc publish \"<input ctc>\" \"<url>\"' to upload your container file.");
			System.out.println("");
			System.out.println("");
			System.out.println("Publishing to a regular server:");
			System.out.println("1. Create a SHA-256 hash file of your container:");
			System.out.println("   For Linux: sha256sum \"<container-file>\" > \"<container-file>.sha256\"");
			System.out.println(
					"   For PowerShell: echo $(Get-FileHash \"<container-file>\" SHA256).Hash > \"<container-file>.sha256\"");
			System.out.println("");
			System.out.println("2. Run 'ctc publish \"<input ctc>\" \"<url>\"' to upload your container file.");
			System.out.println("");
			System.out.println("");
			System.out.println(
					"This will send a PUT request to <url>/<trust-filename>.ctc and ask for credentials when needed.");
			System.out.println("If possible, ctc also uploads the sha256 hash to <url>/<trust-filename>.ctc.sha256.");
			System.out.println("");
			System.out.println(
					"If you are using ClassTrust in your own projects, it is always recommended to use remote servers for");
			System.out.println("downloading (and verifying) trust containers. Read page 12 for information on how to");
			System.out.println(
					"include ClassTrust in java, page 13 will explain how to load containers and how to verify classes.");
		});

		pages.add(() -> {
			System.out.println("ZCTC Archive Files:");
			System.out.println("ZCTC is UCTC but zipped, the CTC CLI can directly create ZCTC files as");
			System.out
					.println("as well as convert ZCTC to CTC (ZCTC to CTC is direct, CTC to ZCTC needs a temp folder)");
			System.out.println("");
			System.out.println("");
			System.out.println(
					"This page explains how to use the ctc cli to create ZCTC files and how to conver them back.");
			System.out.println("");
			System.out.println("Creating a ZCTC file from a normal CTC file:");
			System.out.println("ctc zctc-create \"<input ctc>\" \"<output zctc>\"");
			System.out.println("");
			System.out.println("Converting ZCTC to CTC:");
			System.out.println("ctc zctc-convert \"<input zctc>\" \"<output ctc>\"");
			System.out.println("");
			System.out.println("");
			System.out.println("Examples:");
			System.out.println("ctc zctc-create hello.ctc world.zctc");
			System.out.println("ctc zctc-convert world.zctc hello.new.ctc");
			System.out.println("");
			System.out.println("The first example creates the zctc, the second converts it back to ctc.");
			System.out.println(
					"If you want to overwrite the original file, you can change 'hello.new.ctc' to just 'hello.ctc'");
		});

		pages.add(() -> {
			System.out.println("CTC containers are based on CyanUtil Packets. The system was originally intended for");
			System.out.println("network transport but does work as an archive format. These packets cannot be");
			System.out.println("extracted like tar or zip files as the packets do not provided files.");
			System.out.println("");
			System.out.println("Instead, CyanUtil packets are serialized object entries.");
			System.out.println("");
			System.out.println("CyanUtil packets are composed of a header and body. The header includes:");
			System.out.println("- The 8 byte version (long integer)");
			System.out.println("- The 4 byte summary count. (32-bit signed integer)");
			System.out.println("- Content summary entries");
			System.out.println("");
			System.out.println("The summary format goes as following (repeated for each entry):");
			System.out.println("- The 8 byte entry type (long integer, NUMID encoded string, see aos-util for NUMID)");
			System.out.println("- The 8 byte content length (long integer)");
			System.out.println("");
			System.out.println("After the header, all content is transferred to the output stream at once.");
			System.out
					.println("There are no breaks in between entries for the body, the content length from the header");
			System.out.println("is used to deserialize the packet.");
			System.out.println("");
			System.out.println("Head to page 7 for CTC's format.");
		});
		pages.add(() -> {
			System.out.println("On page 6, we described the format of CyanUtil packets.");
			System.out.println("In this page, we will describe the CTC format itself.");
			System.out.println("");
			System.out.println("CTC is split up in 2 parts, a header and body.");
			System.out.println("The body is split up in packages, containing the class hashes.");
			System.out.println("");
			System.out.println("");
			System.out.println("Each CTC container packet is composed of a header that contains:");
			System.out.println("- The container name");
			System.out.println("- The container timestamp");
			System.out.println("- The entry count (32-bit signed integer)");
			System.out.println("");
			System.out.println("Each package entry is build as following:");
			System.out.println("- The name of package");
			System.out.println("- The amount of classes for said package.");
			System.out.println("- The class entries");
			System.out.println("");
			System.out.println("Each class entry is build as following:");
			System.out.println("- The class simple name");
			System.out.println("- The count of valid hashes for the class");
			System.out.println("- All hashes are added one after another");
		});

		pages.add(() -> {
			System.out.println("UCTC Trust Folders:");
			System.out.println("UCTC -- Unpacked Cyan Trust Container Folder.");
			System.out.println("");
			System.out.println("UCTC folders are folders containing editable class trust.");
			System.out.println("Because the nature of CTC files differs from regular archives,");
			System.out.println("you cannot simply open a CTC in, eg. 7zip or ark.");
			System.out.println("");
			System.out.println("With this utility, you can unpack ctc files into a destination");
			System.out.println("(UCTC) directory, and then edit the files with any text editor you like.");
			System.out.println("");
			System.out.println("CTC can re-pack the container to save your changes after editing.");
			System.out.println("");
			System.out.println("This page will explain UCTC's format (and by extension, ZCTC's format)");
			System.out.println("Page 9 will go into the structure of UCTC trust folders.");
		});
		pages.add(() -> {
			System.out.println("UCTC Folder Structure:");
			System.out.println("The following pages explain the structure of a valid UCTC folder.");
			System.out.println("");
			System.out.println("If any file/folder does not follow the specification, the archive");
			System.out.println("will be rendered invalid and won't compile into ctc.");
			System.out.println("");
			System.out.println("");
			System.out.println("Each UCTC folder contains a main.header file, it contains two entries:");
			System.out.println("- Name       - trust name");
			System.out.println("- Timestamp  - trust timestamp/version");
			System.out.println("");
			System.out.println("The Name entry specifies the software-based trust container name,");
			System.out.println("It does not need to match the trust file, it is by the ClassTrust library.");
			System.out.println("");
			System.out.println("The Timestamp entry specifies the container version, though it will not");
			System.out.println("be written to the CTC file, as it gets generated when packing.");
			System.out.println("");
			System.out.println("The timespamp entry only exists to show the version of the unpacked CTC file,");
			System.out.println("it does not need to be present in order to build the package.");
		});
		pages.add(() -> {
			System.out.println("This page will explain how packages and classes are saved:");
			System.out.println("");
			System.out.println(
					"Packages are saved in directories, all directory contain .cls files representing classes.");
			System.out.println(
					"A package directory name is always absolute, sub-directories invalidate the UCTC folder.");
			System.out.println("");
			System.out.println("A valid directory is named something like:   org.example.examplemod");
			System.out.println("Example of an invalid directory:             org/example/examplemod");
			System.out.println("");
			System.out.println("");
			System.out.println("A class (.cls) file must be created with the simple name of a class, eg. ExampleMod.");
			System.out.println("All lines inside the class represent the valid SHA-256 hashes, classes are allowed");
			System.out.println("to have multiple hashes. This is useful for modders that have multiple jarfiles");
			System.out.println("containing remapped code.");
			System.out.println("");
			System.out.println("Utilities such as Cyan RIFT remap jars for multiple platforms, RIFT allows CYAN mods");
			System.out.println("to run on platforms like forge, fabric, paper and also vanilla minecraft.");
			System.out.println("");
			System.out.println("Such processes invalidate hashes, for each remapped class, you will need to have an");
			System.out.println("alternate (backup) hash saved, ClassTrust will reject the remapped classes otherwise.");
			System.out.println("");
			System.out.println("");
			System.out.println("Use utilities such as sha256sum to get the hash of a class file,");
			System.out.println("Powershell users have access to the Get-FileHash command, which outputs the same");
			System.out.println("if you select SHA256 as the hashing algorithm.");
			System.out.println("");
			System.out.println("");
			System.out.println("Examples for getting a class file hash:");
			System.out.println("");
			System.out.println(
					"GNU/Linux:\nsha256sum org/example/examplemod/ExampleMod.class | sed \"s/  .*$//\" > trust/org.example.examplemod/ExampleMod.cls");
			System.out.println("");
			System.out.println(
					"PowerShell:\necho $(Get-FileHash org/example/examplemod/ExampleMod.class SHA256).Hash > trust/org.example.examplemod/ExampleMod.cls");
			System.out.println("");
			System.out.println(
					"The 'sed' part of the linux command strips the filename from the hash output, powershell doesn't output the file name.");
		});

		pages.add(() -> {
			System.out.println("Including ClassTrust in java projects:");
			System.out.println("This page will explain how to add ClassTrust to java projects");
			System.out.println("");
			System.out.println("");
			System.out.println("For gradle projects (fully tested)");
			System.out.println("1. Add the ASF maven server: (if you haven't done so already)");
			System.out.println("   repositories {");
			System.out.println("       maven { name = \"AerialWorks\"; url = \"https://aerialworks.ddns.net/maven\" }");
			System.out.println("   }");
			System.out.println("");
			System.out.println("2. Add the class trust dependency: (latest version is alpha 5)");
			System.out.println("   dependencies {");
			System.out.println("       implementation 'org.asf.cyan:ClassTrust:1.0.0.A5'");
			System.out.println("   }");
			System.out.println("");
			System.out.println("");
			System.out.println("For maven projects (untested):");
			System.out.println("1. Add the ASF maven server: (if you haven't done so already)");
			System.out.println("   <repository>");
			System.out.println("     <id>asf</id>");
			System.out.println("     <url>https://aerialworks.ddns.net/maven</url>");
			System.out.println("   </repository>");
			System.out.println("");
			System.out.println("2. Add the class trust dependency: (latest version is alpha 5)");
			System.out.println("   <dependency>");
			System.out.println("     <groupId>org.asf.cyan</groupId>");
			System.out.println("     <artifactId>ClassTrust</artifactId>");
			System.out.println("     <version>1.0.0.A5</version>");
			System.out.println("   <dependency>");
			System.out.println("");
			System.out.println("");
			System.out.println("Other projects:");
			System.out.println("If you cannot use maven repositories, you can find the latest ClassTrust jar");
			System.out.println("in /usr/lib/ctc-libs (C:\\Program Files (x86)\\ctc for windows), just know that");
			System.out.println("you will be missing javadoc and source jars.");
			System.out.println("");
			System.out.println("Remote javadoc: https://aerialworks.ddns.net/javadoc/Cyan/ClassTrust");
		});
		pages.add(() -> {
			System.out.println("ClassTrust Java Guide:");
			System.out.println(
					"This is a simple java guide on how to use ClassTrust, you can find the full javadoc here:");
			System.out.println("Remote javadoc: https://aerialworks.ddns.net/javadoc/Cyan/ClassTrust");
			System.out.println("");
			System.out.println(
					"// This page will mostly use java comments, so it is easy to copy paste this into an actual program.");
			System.out
					.println("// The following code loads a trust container, you should not depend on absoulte paths,");
			System.out.println("// instead it is best to use a configuration or program directory.");
			System.out.println("//");
			System.out.println(
					"// It is also recommended to download the trust containers, and verify using a remote container hash.");
			System.out.println(
					"TrustContainer container = TrustContainer.importContainer(new File(\"path/to/container.ctc\");");
			System.out.println("");
			System.out.println("// Validate a class");
			System.out.println("int result = container.validateClass(ExampleMod.class); // validates ExampleMod");
			System.out.println("");
			System.out.println("// The result is either 0, 1 or 2:");
			System.out.println("// 0 = class validated successfully.");
			System.out.println("// 1 = class hash mismatch.");
			System.out.println("// 2 = class not found in container.");
			System.out.println("");
			System.out.println(
					"// If you have a trust directory, you should ignore 2 unless it cannot be found in any container");
			System.out.println(
					"// If the result is 1, the program should immediately terminate and warn the end-user about possible tampering.");
		});

		showPage(1);
		System.out.println("Use 'page number' to switch pages, use exit to close the manual.");
		Scanner sc = new Scanner(System.in);
		while (true) {
			System.out.print("CTC-MANUAL> ");
			String cmd = sc.nextLine().trim();
			while (cmd.contains("  "))
				cmd = cmd.replace("  ", " ");

			String[] args = cmd.split(" ");
			if (args.length >= 1) {
				if (args[0].equalsIgnoreCase("exit") || args[0].equalsIgnoreCase("quit"))
					break;
				else if (args[0].equalsIgnoreCase("page") && args.length >= 2) {
					int page = 1;
					try {
						page = Integer.valueOf(args[1]);
						if (pages.size() <= page - 1 || page <= 0) {
							System.err.println("Invalid page");
							continue;
						}
					} catch (Exception e) {
						System.err.println("Invalid page");
						continue;
					}
					showPage(page);
				}
			}
		}
		sc.close();
	}

	private static void showPage(int i) {
		System.out.println("");
		System.out.println("");
		System.out.println("");
		System.out.println("----------------");
		System.out.println("CTC Manual Page:");
		System.out.println("");
		pages.get(i - 1).run();
		System.out.println("");
	}

	private static Object[] getCredentials(String group) {
		if (credentials != null && lastGroup.equals(group)) {
			return credentials;
		}

		lastGroup = group;
		System.out.println("Please log in to your " + group + " account at " + dest.getHost() + "...");
		System.out.print("Username: ");
		String username = in.nextLine();
		System.out.print("Password for " + username + "@" + dest.getHost() + ": ");
		char[] password = System.console().readPassword();
		credentials = new Object[] { username, password };
		return credentials;
	}

}
