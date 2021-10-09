package org.asf.cyan;

import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

import java.awt.Dimension;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import java.awt.Font;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.asf.aos.util.service.extra.slib.util.ArrayUtil;
import org.asf.cyan.api.classloading.DynamicClassLoader;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.config.Configuration;
import org.asf.cyan.api.config.serializing.ObjectSerializer;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.bytecode.sources.FileClassSourceProvider;
import org.asf.cyan.fluid.implementation.CyanBytecodeExporter;
import org.asf.cyan.fluid.implementation.CyanReportBuilder;
import org.asf.cyan.fluid.implementation.CyanTransformer;
import org.asf.cyan.fluid.implementation.CyanTransformerMetadata;
import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftMappingsToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit.OsInfo;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftModdingToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftVersionToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.rift.SimpleRift;
import org.asf.cyan.minecraft.toolkits.mtk.rift.SimpleRiftBuilder;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionType;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.Color;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JCheckBox;

public class Installer extends CyanComponent {

	private static final String version = "5.10";

	private static Installer impl;

	private static boolean init = false;

	private static boolean cli = false;

	public static boolean isInitialized() {
		return init;
	}

	protected static void initComponent() {
		init = true;
		try {
			debug("Closing FLUID API...");
			Fluid.closeFluidLoader();
		} catch (IllegalStateException e) {
			error("Failed to close FLUID!", e);
		}
	}

	@Override
	protected void setupComponents() {
		if (init)
			throw new IllegalStateException("Cyan components have already been initialized.");
		if (LOG == null)
			initLogger();
	}

	@Override
	protected void preInitAllComponents() {
		trace("OPEN FluidAPI Mappings Loader, caller: " + CallTrace.traceCallName());
		try {
			debug("Opening FLUID API...");
			Fluid.openFluidLoader();
		} catch (IllegalStateException e) {
			error("Failed to open FLUID!", e);
		}

		trace("INITIALIZE all components, caller: " + CallTrace.traceCallName());
		trace("CREATE ConfigurationBuilder instance, caller: " + CallTrace.traceCallName());
	}

	@Override
	protected void finalizeComponents() {
	}

	@Override
	protected Class<?>[] getComponentClasses() {
		return new Class<?>[] { CyanTransformer.class, CyanTransformerMetadata.class, CyanBytecodeExporter.class,
				Installer.class, CyanReportBuilder.class, MinecraftToolkit.class, MinecraftVersionToolkit.class,
				MinecraftModdingToolkit.class, MinecraftInstallationToolkit.class, MinecraftMappingsToolkit.class };
	}

	public static void setDebugLog() {
		if (LOG == null)
			initLogger();
		trace(CallTrace.traceCallName() + " set the log level to DEBUG.");
		Configurator.setLevel("CYAN", Level.DEBUG);
	}

	public static void initializeComponents() throws IllegalStateException {
		impl.initializeComponentClasses();
	}

	private JFrame frmCyanInstaller;
	private JTextField textField;
	private Logger logger;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) throws IOException {
		if (args.length == 3 && args[0].equals("install")) {
			cli = true;
			Installer window = new Installer();
			String type = args[1];
			if (type.equals("client")) {
				ProjectConfig project = new ProjectConfig();
				try {
					window.installClient(new File(args[2]), MinecraftInstallationToolkit.getMinecraftDirectory(),
							project, false);
				} catch (Exception e) {
					fatal("Fatal error during installation", e);
					System.exit(1);
				}
			} else if (type.equals("server")) {
				ProjectConfig project = new ProjectConfig();
				try {
					window.installServer(new File(args[2]), MinecraftInstallationToolkit.getMinecraftDirectory(),
							project, false);
				} catch (Exception e) {
					fatal("Fatal error during installation", e);
					System.exit(1);
				}
			} else if (type.equals("gui-client")) {
				ProjectConfig project = new ProjectConfig();
				File outputDir = new File(args[2]);
				if (!outputDir.exists()) {
					JOptionPane.showMessageDialog(null, "Game directory does not exist.", "Cannot install",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				ProgressWindow.WindowAppender.showWindow();
				try {
					window.installClient(outputDir, MinecraftInstallationToolkit.getMinecraftDirectory(), project,
							false);
					ProgressWindow.WindowAppender.closeWindow();
					return;
				} catch (Exception e) {
					window.logger.fatal(e);
					SwingUtilities.invokeLater(() -> {
						ProgressWindow.WindowAppender.fatalError();
						ProgressWindow.WindowAppender.closeWindow();
						System.exit(1);
					});
				}
			} else if (type.equals("gui-server")) {
				ProjectConfig project = new ProjectConfig();
				File outputDir = new File(args[2]);
				if (outputDir.equals(new File(APPDATA, ".minecraft"))) {
					if (JOptionPane.showConfirmDialog(null,
							"You have selected the default client (.minecraft) installation directory for installing the server,\nThis is a bit unusual, are you sure you want to continue?",
							"", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
						return;
					}
				}
				if (!outputDir.exists()) {
					outputDir.mkdirs();
				}
				ProgressWindow.WindowAppender.showWindow();
				try {
					window.installServer(outputDir, MinecraftInstallationToolkit.getMinecraftDirectory(), project,
							false);
					ProgressWindow.WindowAppender.closeWindow();
					return;
				} catch (Exception e) {
					window.logger.fatal(e);
					SwingUtilities.invokeLater(() -> {
						ProgressWindow.WindowAppender.fatalError();
						ProgressWindow.WindowAppender.closeWindow();
						System.exit(1);
					});
				}
			}
			return;
		}
		if (args.length >= 1) {
			String file = args[0];
			final File install;
			if (args.length >= 2)
				install = new File(args[1]);
			else
				install = SelectionWindow.showWindow(new File(file));
			if (install == null)
				return;

			if (install.exists() && new File(file).exists())
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						try {
							ModInstaller window = new ModInstaller();
							window.mod = new File(file);
							window.cyanDataDir = install;
							window.load();
							window.setVisible(true);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			return;
		}
		ProjectConfig project = new ProjectConfig();
		if (project.loader.equalsIgnoreCase("forge")
				&& Version.fromString(project.game).isLessThan(Version.fromString("1.17"))) {
			Version java = Version.fromString(System.getProperty("java.version"));
			if (Version.fromString("12").isLessOrEqualTo(java)) {
				JOptionPane.showMessageDialog(null,
						"Forge (Pre-1.17) is not compatible with versions above Java 11, please use Java 11 to install.",
						"Cannot install", JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			}
		}
		Version java = Version.fromString(System.getProperty("java.version"));
		if (Version.fromString(project.game).isGreaterOrEqualTo(Version.fromString("1.17"))
				&& Version.fromString("16").isGreaterThan(java)) {
			JOptionPane.showMessageDialog(null,
					project.name
							+ " 1.17+ is not compatible with versions below Java 16, please use Java 16+ to install.",
					"Cannot install", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
		EventQueue.invokeLater(new Runnable() {

			public void run() {
				try {
					Installer window = new Installer();
					window.frmCyanInstaller.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		});
	}

	private void installClient(File outputDir, File cache, ProjectConfig project, boolean interactive)
			throws IOException {

		if (project.loader.equals("fabric"))
			project.loader = "fabric-loader";

		logger.info("");
		logger.info("KickStart Installer Version " + version + ".");
		logger.info("For " + project.name + " " + project.version + ".");
		logger.info("");
		logger.info("Preparing...");
		initializeComponents();
		MinecraftToolkit.resetServerConnectionState();
		MinecraftToolkit.resolveVersions();
		logger.info("");

		logger.info("Resolving manifest...");
		URL manifest = null;
		for (String repoID : project.repositories.keySet()) {
			String base = project.repositories.get(repoID);
			try {
				String mfUri = project.manifest.replace("%wv", project.wrapper).replace("%gv", project.game)
						.replace("%pv", project.version);
				URL u = new URL(base + "/" + mfUri);
				u.openStream().close();
				manifest = u;
				break;
			} catch (IOException e) {
			}
		}
		if (manifest == null)
			throw new IOException("Missing modloader manifest");

		logger.info("Creating fake version info for modloader...");
		MinecraftVersionInfo info = new MinecraftVersionInfo(
				project.inheritsFrom + "-" + project.name.toLowerCase() + "-" + project.version,
				MinecraftVersionType.UNKNOWN, manifest, OffsetDateTime.now());

		logger.info("Downloading modloader version manifest...");
		MinecraftInstallationToolkit.saveVersionManifest(info, true);

		logger.info("Finding vanilla version...");
		MinecraftVersionInfo version = MinecraftVersionToolkit.getVersion(project.game);

		logger.info("Downloading vanilla version manifest...");
		MinecraftInstallationToolkit.saveVersionManifest(version);

		int progressMax = 0;
		ProgressWindow.WindowAppender.addMax(progressMax);
		runInstaller(outputDir, version, info, cache, project, GameSide.CLIENT, interactive);
	}

	/**
	 * Create the application.
	 */
	public Installer() throws IOException {
		initialize();
	}

	private void installServer(File outputDir, File cache, ProjectConfig project, boolean interactive)
			throws IOException {

		if (project.loader.equals("fabric"))
			project.loader = "fabric-loader";

		logger.info("");
		logger.info("KickStart Installer Version " + version + ".");
		logger.info("For " + project.name + " " + project.version + ".");
		logger.info("");
		logger.info("Preparing...");
		initializeComponents();
		MinecraftToolkit.resetServerConnectionState();
		MinecraftToolkit.resolveVersions();
		logger.info("");

		logger.info("Resolving manifest...");
		URL manifest = null;
		for (String repoID : project.repositories.keySet()) {
			String base = project.repositories.get(repoID);
			try {
				String mfUri = project.manifest.replace("%wv", project.wrapper).replace("%gv", project.game)
						.replace("%pv", project.version);
				URL u = new URL(base + "/" + mfUri);
				u.openStream().close();
				manifest = u;
				break;
			} catch (IOException e) {
			}
		}
		if (manifest == null)
			throw new IOException("Missing modloader manifest");

		logger.info("Creating fake version info for modloader...");
		MinecraftVersionInfo info = new MinecraftVersionInfo(project.inheritsFrom + "-cyan-" + project.version,
				MinecraftVersionType.UNKNOWN, manifest, OffsetDateTime.now());

		logger.info("Downloading modloader version manifest...");
		MinecraftInstallationToolkit.saveVersionManifest(info);

		logger.info("Finding vanilla version...");
		MinecraftVersionInfo version = MinecraftVersionToolkit.getVersion(project.game);

		logger.info("Downloading vanilla version manifest...");
		MinecraftInstallationToolkit.saveVersionManifest(version);

		int progressMax = 0;
		ProgressWindow.WindowAppender.addMax(progressMax);
		runInstaller(outputDir, version, info, cache, project, GameSide.SERVER, interactive);
	}

	public static File APPDATA;
	private JCheckBox chckbxNewCheckBox;
	private JCheckBox chckbxNewCheckBox_1;

	private String forgeUniversalURL = "https://maven.minecraftforge.net/net/minecraftforge/forge/%game%-%forgeversion%/forge-%game%-%forgeversion%-universal.jar";

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() throws IOException {
		logger = LogManager.getLogger("Installer");
		impl = this;
		assignImplementation();

		String dir = System.getenv("APPDATA");
		if (dir == null)
			dir = System.getProperty("user.home");
		APPDATA = new File(dir);

		File cache = new File(APPDATA, ".kickstart");
		MinecraftInstallationToolkit.setMinecraftDirectory(cache);

		if (!cli) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
					| UnsupportedLookAndFeelException e1) {
			}
			frmCyanInstaller = new JFrame();
			frmCyanInstaller.setTitle("Installer");
			frmCyanInstaller.setResizable(false);
			frmCyanInstaller.setBounds(100, 100, 640, 428);
			frmCyanInstaller.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frmCyanInstaller.setLocationRelativeTo(null);
		}

		ProjectConfig project = new ProjectConfig();

		if (!cli)
			frmCyanInstaller.setTitle(project.name + " Installer");

		JPanel panel = new JPanel();
		panel.setBorder(new LineBorder(new Color(0, 0, 0)));

		if (!cli)
			frmCyanInstaller.getContentPane().add(panel, BorderLayout.SOUTH);
		panel.setLayout(new BorderLayout(0, 0));

		JLabel lblNewLabel = new JLabel(" " + project.name + " " + project.version);
		panel.add(lblNewLabel, BorderLayout.WEST);
		lblNewLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
		lblNewLabel.setVerticalAlignment(SwingConstants.TOP);
		lblNewLabel.setPreferredSize(new Dimension(200, 18));

		JLabel lblKickstart = new JLabel("KickStart Installer " + version + " ");
		panel.add(lblKickstart, BorderLayout.EAST);
		lblKickstart.setFont(new Font("SansSerif", Font.BOLD, 12));
		lblKickstart.setHorizontalAlignment(SwingConstants.TRAILING);
		lblKickstart.setVerticalAlignment(SwingConstants.TOP);
		lblKickstart.setPreferredSize(new Dimension(200, 18));

		JPanel panel_1 = new JPanel();
		panel_1.setBorder(new LineBorder(new Color(0, 0, 0)));

		if (!cli)
			frmCyanInstaller.getContentPane().add(panel_1, BorderLayout.NORTH);
		panel_1.setLayout(new BorderLayout(0, 0));

		JLabel lblNewLabel_1 = new JLabel(project.name + " Modloader " + project.version);
		lblNewLabel_1.setAlignmentY(6.0f);
		panel_1.add(lblNewLabel_1, BorderLayout.NORTH);
		lblNewLabel_1.setPreferredSize(new Dimension(70, 55));
		lblNewLabel_1.setFont(new Font("SansSerif", Font.ITALIC, 35));
		lblNewLabel_1.setHorizontalAlignment(SwingConstants.CENTER);

		JLabel lblNewLabel_2 = new JLabel("For " + "Minecraft " + project.game
				+ (project.loader.isEmpty() ? ""
						: ", " + project.loader.substring(0, 1).toUpperCase() + project.loader.substring(1)
								+ (project.loaderVersion.isEmpty() ? "" : " " + project.loaderVersion)));

		if (project.loader.equals("fabric"))
			project.loader = "fabric-loader";

		lblNewLabel_2.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel_2.setFont(new Font("SansSerif", Font.PLAIN, 16));
		panel_1.add(lblNewLabel_2, BorderLayout.SOUTH);

		JPanel panel_3 = new JPanel();
		panel_3.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));

		JPanel panel_2 = new JPanel();
		panel_2.setPreferredSize(new Dimension(555, 200));
		panel_3.add(panel_2);

		if (!cli)
			frmCyanInstaller.getContentPane().add(panel_3, BorderLayout.CENTER);
		panel_3.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
		panel_2.setLayout(null);

		textField = new JTextField();
		textField.setBounds(35, 120, 365, 26);
		textField.setText(new File(dir, ".minecraft").getAbsolutePath());
		JButton btnNewButton = new JButton("Install Client");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				File outputDir = new File(textField.getText());
				if (!outputDir.exists()) {
					JOptionPane.showMessageDialog(frmCyanInstaller, "Game directory does not exist.", "Cannot install",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				frmCyanInstaller.setVisible(false);
				ProgressWindow.WindowAppender.showWindow();
				new Thread(() -> {
					try {
						installClient(outputDir, cache, project, true);
						frmCyanInstaller.dispose();
						ProgressWindow.WindowAppender.closeWindow();
						return;
					} catch (Exception e) {
						logger.fatal(e);
						SwingUtilities.invokeLater(() -> {
							ProgressWindow.WindowAppender.fatalError();
							ProgressWindow.WindowAppender.closeWindow();
							frmCyanInstaller.dispose();
							System.exit(1);
						});
					}
				}, "Installer").start();
			}
		});
		btnNewButton.setBounds(0, 83, 275, 25);
		panel_2.add(btnNewButton);

		JButton btnNewButton_1 = new JButton("Create Server");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				File outputDir = new File(textField.getText());
				if (outputDir.equals(new File(APPDATA, ".minecraft"))) {
					if (JOptionPane.showConfirmDialog(frmCyanInstaller,
							"You have selected the default client (.minecraft) installation directory for installing the server,\nThis is a bit unusual, are you sure you want to continue?",
							"", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
						return;
					}
				}
				if (!outputDir.exists()) {
					outputDir.mkdirs();
				}
				frmCyanInstaller.setVisible(false);
				ProgressWindow.WindowAppender.showWindow();
				new Thread(() -> {
					try {
						installServer(outputDir, cache, project, true);
						frmCyanInstaller.dispose();
						ProgressWindow.WindowAppender.closeWindow();
						return;
					} catch (Exception e) {
						logger.fatal(e);
						SwingUtilities.invokeLater(() -> {
							ProgressWindow.WindowAppender.fatalError();
							ProgressWindow.WindowAppender.closeWindow();
							frmCyanInstaller.dispose();
							System.exit(1);
						});
					}
				}, "Installer").start();
			}
		});
		btnNewButton_1.setBounds(280, 83, 275, 25);
		panel_2.add(btnNewButton_1);

		panel_2.add(textField);
		textField.setColumns(10);

		JButton btnNewButton_2 = new JButton("Select...");
		btnNewButton_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser f = new JFileChooser(textField.getText());
				f.setDialogTitle("Select installation directory...");
				f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				f.showSaveDialog(frmCyanInstaller);
				if (f.getSelectedFile() != null)
					textField.setText(f.getSelectedFile().getAbsolutePath());
			}
		});
		btnNewButton_2.setBounds(403, 119, 117, 28);
		panel_2.add(btnNewButton_2);

		chckbxNewCheckBox = new JCheckBox("Create launcher profile");
		chckbxNewCheckBox.setSelected(true);
		chckbxNewCheckBox.setBounds(35, 146, 189, 23);
		panel_2.add(chckbxNewCheckBox);

		chckbxNewCheckBox_1 = new JCheckBox("Associate Mod Installer Extensions");
		chckbxNewCheckBox_1.setBounds(228, 146, 292, 23);
		if (project.platform.equals("SPIGOT")) {
			btnNewButton.setVisible(false);
			btnNewButton_1.setSize(275 + 280, 25);
			btnNewButton_1.setLocation(btnNewButton.getLocation());
			chckbxNewCheckBox_1.setLocation(chckbxNewCheckBox.getLocation());
			chckbxNewCheckBox.setVisible(false);
			textField.setText(new File(new File(dir, ".minecraft"), "server").getAbsolutePath());
		}
		panel_2.add(chckbxNewCheckBox_1);
	}

	private static HashMap<String, Class<?>> loaded = new HashMap<String, Class<?>>();

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void runInstaller(File dest, MinecraftVersionInfo version, MinecraftVersionInfo modloader, File cache,
			ProjectConfig project, GameSide side, boolean interactive) throws IOException {
		logger.info("Determining progress...");

		int progressMax = 0;

		progressMax++; // Download vanilla mappings (if needed)
		progressMax++;

		progressMax++; // Download platform mappings (if needed)
		progressMax++;

		progressMax++; // Download vanilla jar (if needed)

		progressMax++; // Deobfuscate vanilla jar (if needed)
		progressMax++;
		progressMax++;

		progressMax++; // Resolve modloader libraries
		progressMax++;

		progressMax++; // Apply artifactModifications
		progressMax++; // Install libraries
		if (side == GameSide.CLIENT) {
			progressMax++; // Generate new version manifest
			progressMax++; // Install version file
			progressMax++; // Download version log file

			if (chckbxNewCheckBox.isSelected())
				progressMax++; // Generate launcher profile or replace existing
		}
		if (side == GameSide.SERVER) {
			progressMax++; // Download platform dependencies and jar if needed
			progressMax++; // Build server fat jar
			progressMax++; // Build server jar manifest
			progressMax++; // Create server jar
		}
		if (chckbxNewCheckBox_1.isSelected())
			progressMax++; // Install the Mod Installer
		progressMax++; // Create .kickstart-installer.ccfg

		ProgressWindow.WindowAppender.addMax(progressMax);

		logger.info("Resolving mappings...");
		Mapping<?> vanillaMappings = null;
		if (!MinecraftMappingsToolkit.areMappingsAvailable(version, side)) {
			vanillaMappings = MinecraftMappingsToolkit.downloadVanillaMappings(version, side);
			ProgressWindow.WindowAppender.increaseProgress();

			MinecraftMappingsToolkit.saveMappingsToDisk(version, side);
			ProgressWindow.WindowAppender.increaseProgress();
			logger.info("");
		} else {
			vanillaMappings = MinecraftMappingsToolkit.loadMappings(version, side);
			ProgressWindow.WindowAppender.increaseProgress();
			ProgressWindow.WindowAppender.increaseProgress();
		}

		logger.info("Resolving platform mappings...");
		if (!project.platform.equals("VANILLA") && !project.platform.equals("INTERMEDIARY")
				&& (project.mappings == null || project.mappings.isEmpty())) {
			logger.info("Determining mappings version by modloader...");
			if (project.loader.equalsIgnoreCase("forge")) {
				File forgeInstaller = downloadForgeInstaller(project, cache);
				if (Version.fromString(project.game).isLessThan(Version.fromString("1.17"))) {
					logger.info("Searching for MCP version in forge jar...");
					String mcpVersion = "";
					ZipFile installerZip = new ZipFile(forgeInstaller);
					ZipEntry forgeJar = installerZip.getEntry("maven/net/minecraftforge/forge/" + project.game + "-"
							+ project.loaderVersion + "/forge-" + project.game + "-" + project.loaderVersion + ".jar");
					ZipInputStream forgeStrm = new ZipInputStream(installerZip.getInputStream(forgeJar));
					while (forgeStrm.available() != 0) {
						ZipEntry entry = forgeStrm.getNextEntry();
						if (entry.getName().equals("META-INF/MANIFEST.MF")) {
							Manifest manifest = new Manifest(forgeStrm);
							Attributes MCP = manifest.getAttributes("net/minecraftforge/versions/mcp/");
							mcpVersion = MCP.getValue("Implementation-Version");
							break;
						}
					}
					forgeStrm.close();
					installerZip.close();
					project.mappings = mcpVersion;
				} else {
					URL url = new URL(forgeUniversalURL.replaceAll("\\%game\\%", project.game)
							.replaceAll("\\%forgeversion\\%", project.loaderVersion));
					File forgeJar = new File(cache,
							"forge-" + project.game + "-" + project.loaderVersion + "-universal.jar");
					File forgeJarDownloadMarker = new File(cache, forgeJar.getName() + ".lck");
					if (!forgeJar.exists() || forgeJarDownloadMarker.exists()) {
						if (!forgeJar.getParentFile().exists())
							forgeJar.getParentFile().mkdirs();
						if (forgeJarDownloadMarker.exists())
							forgeJarDownloadMarker.delete();
						forgeJarDownloadMarker.createNewFile();

						logger.info("Downloading forge jar to cache...");
						if (forgeJar.exists())
							forgeJar.delete();
						FileOutputStream strm = new FileOutputStream(forgeJar);
						InputStream inp = url.openStream();
						inp.transferTo(strm);
						strm.close();
						inp.close();
						forgeJarDownloadMarker.delete();
					}
					FileInputStream forgeJarStrm = new FileInputStream(forgeJar);
					ZipInputStream forgeStrm = new ZipInputStream(forgeJarStrm);
					while (forgeStrm.available() != 0) {
						ZipEntry entry = forgeStrm.getNextEntry();
						if (entry.getName().equals("META-INF/MANIFEST.MF")) {
							Manifest manifest = new Manifest(forgeStrm);
							Attributes MCP = manifest.getAttributes("net/minecraftforge/versions/mcp/");
							project.mappings = MCP.getValue("Implementation-Version");
							break;
						}
					}
					forgeStrm.close();
					forgeJarStrm.close();
				}
				logger.info("");
			} else {
				throw new IOException("Cannot resolve mappings version if not using forge");
			}
		}

		String suffix = "";
		if (!project.loader.isEmpty())
			suffix += "-" + project.loader;
		if (project.mappings != null && !project.mappings.isEmpty())
			suffix += "-" + project.mappings.replaceAll("[^A-Za-z0-9.-]", "-");
		if (!project.loaderVersion.isEmpty())
			suffix += "-" + project.loaderVersion;

		if (!project.platform.equals("VANILLA")) {
			logger.info("Preparing " + project.platform + " " + project.mappings + " mappings...");
			if (!MinecraftMappingsToolkit.areMappingsAvailable(suffix, project.platform.toLowerCase(), version, side)) {
				if (project.platform.equals("MCP")) {
					MinecraftMappingsToolkit.downloadMCPMappings(version, side, project.mappings);
				} else if (project.platform.equals("INTERMEDIARY")) {
					MinecraftMappingsToolkit.downloadIntermediaryMappings(version, side);
				} else if (project.platform.equals("SPIGOT")) {
					MinecraftMappingsToolkit.downloadSpigotMappings(vanillaMappings, version, project.mappings);
				}
				ProgressWindow.WindowAppender.increaseProgress();

				MinecraftMappingsToolkit.saveMappingsToDisk(suffix, project.platform.toLowerCase(), version, side);
				ProgressWindow.WindowAppender.increaseProgress();
				logger.info("");
			} else {
				ProgressWindow.WindowAppender.increaseProgress();
				ProgressWindow.WindowAppender.increaseProgress();
			}
		} else {
			ProgressWindow.WindowAppender.increaseProgress();
			ProgressWindow.WindowAppender.increaseProgress();
		}

		logger.info("Preparing vanilla jar...");
		if (project.platform.equals("MCP")) {
			if (MinecraftInstallationToolkit.getVersionJar(version, GameSide.CLIENT) == null) {
				logger.info("Downloading vanilla client jar...");
				MinecraftInstallationToolkit.downloadVersionJar(version, GameSide.CLIENT);
				ProgressWindow.WindowAppender.increaseProgress();
				logger.info("");
			} else {
				ProgressWindow.WindowAppender.increaseProgress();
			}
		}
		if (MinecraftInstallationToolkit.getVersionJar(version, side) == null) {
			logger.info("Downloading vanilla jar...");
			MinecraftInstallationToolkit.downloadVersionJar(version, side);
			ProgressWindow.WindowAppender.increaseProgress();
			logger.info("");
		} else {
			ProgressWindow.WindowAppender.increaseProgress();
		}

		logger.info("");
		logger.info("");
		logger.info("");
		logger.info("Quick note:");
		logger.info("If you are installing for this version of the game (not the modloader) for the first time,");
		logger.info("the KickStart Installer will need to deobfuscate the game.");
		logger.info("");
		logger.info("This process will take A LOT OF TIME, if you have installed " + project.name);
		logger.info("for the " + project.game + " " + side.toString().toLowerCase()
				+ " before, the existing jar will be used.");
		logger.info("");
		logger.info("");
		logger.info("");
		logger.info("Preparing to deobfuscate... (if needed)");
		MinecraftMappingsToolkit.loadMappings(version, (project.platform.equals("MCP") ? GameSide.CLIENT : side));
		logger.info("");
		ProgressWindow.WindowAppender.increaseProgress();
		if (side == GameSide.CLIENT && !MinecraftInstallationToolkit.checkInstallation(version, false)) {
			logger.info("Downloading game libraries...");
			MinecraftInstallationToolkit.downloadVersionFiles(version, false);
			ProgressWindow.WindowAppender.increaseProgress();
			logger.info("");
		} else {
			ProgressWindow.WindowAppender.increaseProgress();
		}
		logger.info("Running deobfuscation engine...");
		File lock = new File(cache, "deobf-" + version + "-" + side + "-" + project.platform + "-" + project.name + "-"
				+ project.version + ".lck");
		if (lock.exists())
			MinecraftModdingToolkit.deobfuscateJar(version, (project.platform.equals("MCP") ? GameSide.CLIENT : side))
					.delete();
		if (!lock.exists())
			lock.createNewFile();
		MinecraftModdingToolkit.deobfuscateJar(version, (project.platform.equals("MCP") ? GameSide.CLIENT : side));
		lock.delete();
		ProgressWindow.WindowAppender.increaseProgress();
		logger.info("");

		logger.info("Resolving modloader libraries...");
		ArrayList<String> libs = new ArrayList<String>();
		ArrayList<String> rift = new ArrayList<String>();

		if (MinecraftInstallationToolkit.getVersionManifest(modloader).has("inheritsFrom"))
			MinecraftInstallationToolkit.getVersionManifest(modloader).remove("inheritsFrom");
		for (String lib : MinecraftInstallationToolkit.getLibrariesMavenFormat(modloader, false)) {
			String[] info = lib.split(":");
			String group = info[0];
			String name = info[1];
			String ver = info[2];

			if (name.contains("-RIFT") | ver.contains("-RIFT")) {
				if (name.contains("-RIFT")) {
					name = name.substring(0, name.indexOf("-RIFT"));
				}
				if (ver.contains("-RIFT")) {
					ver = ver.substring(0, ver.indexOf("-RIFT"));
				}
				if (!rift.contains(group + ":" + name + ":" + ver))
					rift.add(group + ":" + name + ":" + ver);
			} else {
				if (!libs.contains(lib))
					libs.add(lib);
			}
		}

		ProgressWindow.WindowAppender.increaseProgress();
		logger.info("Downloading regular libraries...");

		HashMap<String, String> libraryPaths = new HashMap<String, String>();
		HashMap<String, File> libraryFiles = new HashMap<String, File>();
		for (String lib : libs) {
			libraryFiles.put(lib, download(lib, cache, project, libraryPaths));
		}
		for (String lib : rift) {
			logger.info("");
			File riftJar = download(lib, cache, project, libraryPaths);

			logger.info("Remapping RIFT jar...");
			File riftOut = new File(cache,
					"caches/rift/" + riftJar.getName().substring(0, riftJar.getName().lastIndexOf(".jar")) + "-rift-"
							+ project.platform.toLowerCase() + "-" + project.game + "-"
							+ project.mappings.replaceAll("[^A-Za-z0-9-.]", "-")
							+ (project.loader.isEmpty() ? "" : "-" + project.loader)
							+ (project.loaderVersion.isEmpty() ? "" : "-" + project.loaderVersion) + ".jar");
			if (!riftOut.getParentFile().exists())
				riftOut.getParentFile().mkdirs();
			if (riftOut.exists())
				riftOut.delete();

			if (project.platform.equals("DEOBFUSCATED")) {
				ProgressWindow.WindowAppender.increaseProgress();
				ProgressWindow.WindowAppender.increaseProgress();
				ProgressWindow.WindowAppender.increaseProgress();
				Files.copy(riftJar.toPath(), riftOut.toPath());
				continue;
			}

			SimpleRiftBuilder builder = new SimpleRiftBuilder();
			builder.appendRiftProvider(SimpleRiftBuilder.getProviderForPlatform(
					LaunchPlatform.valueOf(project.platform), version, side, project.loaderVersion, project.mappings));
			builder.appendSources(new FileClassSourceProvider(riftJar));
			builder.setIdentifier("kickstart-" + project.platform.toLowerCase() + "-" + project.game + "-"
					+ project.mappings.replaceAll("[^A-Za-z0-9-.]", "-")
					+ (project.loader.isEmpty() ? "" : "-" + project.loader)
					+ (project.loaderVersion.isEmpty() ? "" : "-" + project.loaderVersion));

			FileInputStream fin = new FileInputStream(riftJar);
			ZipInputStream strm = new ZipInputStream(fin);
			ZipEntry ent = strm.getNextEntry();
			while (ent != null) {
				if (ent.getName().endsWith(".class")) {
					String pth = ent.getName().replace("\\", "/");
					if (pth.startsWith("/"))
						pth = pth.substring(1);
					pth = pth.substring(0, pth.lastIndexOf(".class")).replace("/", ".");
					builder.addClass(pth);
				}
				ent = strm.getNextEntry();
			}
			strm.close();
			fin.close();

			SimpleRift riftUtil;
			try {
				riftUtil = builder.build();
			} catch (ClassNotFoundException | IOException e) {
				builder.close();
				throw new IOException(e);
			}
			fin = new FileInputStream(riftJar);
			strm = new ZipInputStream(fin);
			ent = strm.getNextEntry();
			while (ent != null) {
				if (!ent.getName().endsWith(".class")) {
					riftUtil.addFile(ent.getName(), strm);
				}
				ent = strm.getNextEntry();
			}
			strm.close();
			fin.close();
			try {
				riftUtil.apply();
			} catch (ClassNotFoundException | IOException e) {
				builder.close();
				riftUtil.close();
				throw new IOException(e);
			}

			riftUtil.export(riftOut);
			builder.close();
			riftUtil.close();
			String group = lib.split(":")[0];
			String name = lib.split(":")[1];
			String libver = lib.split(":")[2];
			libraryFiles.put(group + ":" + name + "-RIFT-" + side.toString() + ":" + libver + "-RIFT-" + side.toString()
					+ "-" + project.platform + "-" + project.mappings.replaceAll("[^A-Za-z0-9.-]", "-"), riftOut);
			libraryPaths.remove(lib);
			libs.add(group + ":" + name + "-RIFT-" + side.toString() + ":" + libver + "-RIFT-" + side.toString() + "-"
					+ project.platform + "-" + project.mappings.replaceAll("[^A-Za-z0-9.-]", "-"));
		}

		logger.info("");
		ProgressWindow.WindowAppender.increaseProgress();

		logger.info("Applying modifications...");
		for (String artifact : project.artifactModifications.keySet()) {
			String group = artifact.split(":")[0];
			String name = artifact.split(":")[1];
			File output = new File(cache, "modified/" + group + "/" + name + "/" + project.name + "-" + project.version
					+ "-" + project.platform + "-" + project.mappings.replaceAll("[^A-Za-z0-9-.]", "-") + ".jar");
			if (!output.getParentFile().exists())
				output.getParentFile().mkdirs();
			if (output.exists())
				output.delete();

			String source = null;
			for (String lib : libs) {
				if (lib.startsWith(artifact + ":")) {
					source = lib;
					break;
				}
			}
			if (source == null)
				throw new IOException("Cannot find artifact " + artifact + ", unable to apply modification");

			File input = libraryFiles.get(source);
			logger.info("Applying modifications to " + artifact + "...");
			FileInputStream fin = new FileInputStream(input);
			ZipInputStream inp = new ZipInputStream(fin);
			ZipEntry ent = inp.getNextEntry();
			FileOutputStream outf = new FileOutputStream(output);
			ZipOutputStream outp = new ZipOutputStream(outf);
			while (ent != null) {
				String pth = ent.getName().replace("\\", "/");
				if (pth.startsWith("/"))
					pth = pth.substring(1);

				outp.putNextEntry(new ZipEntry(ent.getName()));
				if (project.artifactModifications.get(artifact).containsKey(pth) && !pth.endsWith("/")) {
					String patch = project.artifactModifications.get(artifact).get(pth);
					String method = patch.substring(0, patch.indexOf("\n")).replace("\r", "");
					patch = patch.substring(patch.indexOf("\n") + 1).replace("%pv", project.version)
							.replace("%i", project.inheritsFrom).replace("%pv", project.version)
							.replace("%gv", project.game)
							.replace("%lv",
									(project.loader.isEmpty() ? "" : project.loader)
											+ (project.loaderVersion.isEmpty() ? "" : "-" + project.loaderVersion))
							.replace("%mv", project.mappings);

					String pos = "";
					String arg = "";
					if (method.contains("//")) {
						arg = method.substring(method.indexOf("//") + 2);
						method = method.substring(0, method.indexOf("//"));
					}
					if (method.contains(":")) {
						pos = method.substring(method.indexOf(":") + 1);
						method = method.substring(0, method.indexOf(":"));
					}
					if (pos.isEmpty())
						pos = "0";

					if (method.equals("ccfg-edit")) {
						String inputFile = new String(inp.readAllBytes());
						DynamicClassLoader tmpLoader = new DynamicClassLoader();
						tmpLoader.setOptions(DynamicClassLoader.OPTION_ALLOW_DEFINE);
						tmpLoader.addUrl(input.toURI().toURL());
						Class<?> confClass;
						try {
							if (loaded.containsKey(arg))
								confClass = loaded.get(arg);
							else
								confClass = tmpLoader.loadClass(arg);
						} catch (ClassNotFoundException e1) {
							tmpLoader.close();
							throw new RuntimeException(e1);
						}
						loaded.put(confClass.getTypeName(), confClass);
						Configuration<?> ccfg = ObjectSerializer
								.deserialize(inputFile, (Class<? extends Configuration>) confClass).readAll(patch);
						String outputStr = ccfg.toString();
						tmpLoader.close();
						outp.write(outputStr.getBytes());
					} else if (method.equals("replace")) {
						byte[] newContent = patch.getBytes();
						if (arg.equals("binary")) {
							newContent = Base64.getDecoder().decode(newContent);
						}
						outp.write(newContent);
					} else if (method.equals("append")) {
						inp.transferTo(outp);
						byte[] newContent = patch.getBytes();
						if (arg.equals("binary")) {
							newContent = Base64.getDecoder().decode(newContent);
						}
						outp.write(newContent);
					} else if (method.equals("insert")) {
						byte[] inputBytes = inp.readAllBytes();
						byte[] newContent = patch.getBytes();
						if (arg.equals("binary")) {
							newContent = Base64.getDecoder().decode(newContent);
						}
						outp.write(ArrayUtil.insert(inputBytes, Integer.valueOf(pos), newContent));
					}
				} else {
					if (!pth.endsWith("/"))
						inp.transferTo(outp);
				}
				outp.closeEntry();

				ent = inp.getNextEntry();
			}
			inp.close();
			fin.close();
			outp.close();
			outf.close();
			libraryPaths.remove(source);
			libraryFiles.remove(source);
			libs.remove(source);
			libraryFiles.put(source + "-" + project.inheritsFrom, output);
			libs.add(source + "-" + project.inheritsFrom);
		}
		logger.info("");
		ProgressWindow.WindowAppender.increaseProgress();

		logger.info("Installing libraries...");
		installLibs(dest, libs, libraryFiles, project, side);
		logger.info("");

		boolean missingParentVersion = false;
		ProgressWindow.WindowAppender.increaseProgress();

		if (side == GameSide.CLIENT) {
			logger.info("Updating version manifest...");
			JsonObject manifest = MinecraftInstallationToolkit.getVersionManifest(modloader).deepCopy();
			if (manifest.has("libraries"))
				manifest.remove("libraries");
			if (manifest.has("inheritsFrom"))
				manifest.remove("inheritsFrom");
			if (manifest.has("id"))
				manifest.remove("id");
			if (manifest.has("mainClass"))
				manifest.remove("mainClass");

			manifest.addProperty("id",
					project.id.replace("%pv", project.version).replace("%i", project.inheritsFrom)
							.replace("%pv", project.version).replace("%gv", project.game)
							.replace("%ml", (project.loader.isEmpty() ? "" : project.loader)
									+ (project.loaderVersion.isEmpty() ? "" : "-" + project.loaderVersion)));
			manifest.addProperty("mainClass", project.clientMain);
			manifest.addProperty("inheritsFrom", project.inheritsFrom);

			if (project.loader.equals("forge")
					&& Version.fromString(project.game).isGreaterOrEqualTo(Version.fromString("1.17"))) {
				if (!manifest.has("arguments"))
					manifest.add("arguments", new JsonObject());
				JsonObject args = manifest.get("arguments").getAsJsonObject();
				if (!args.has("jvm"))
					args.add("jvm", new JsonArray());

				JsonArray jvm = args.get("jvm").getAsJsonArray();
				jvm.add("--add-exports=java.base/sun.security.util=ALL-UNNAMED");
				jvm.add("--add-opens=java.base/java.util.jar=ALL-UNNAMED");
				jvm.add("--add-exports=cpw.mods.bootstraplauncher/cpw.mods.bootstraplauncher=ALL-UNNAMED");
			}

			JsonArray libArray = new JsonArray();
			libs.forEach(lib -> {
				JsonObject artifact = new JsonObject();
				artifact.addProperty("name", lib);
				artifact.addProperty("url", libraryPaths.getOrDefault(lib, ""));
				libArray.add(artifact);
			});
			manifest.add("libraries", libArray);

			File manFile = new File(cache,
					"new-manifests/" + manifest.get("id").getAsString() + "-" + project.name + "-" + project.version
							+ "-" + project.platform + "-" + project.mappings.replaceAll("[^A-Za-z0-9-.]", "-")
							+ ".json");
			if (!manFile.getParentFile().exists())
				manFile.getParentFile().mkdirs();
			if (manFile.exists())
				manFile.delete();
			logger.info("Saving manifest...");
			Files.writeString(manFile.toPath(), new Gson().toJson(manifest));
			ProgressWindow.WindowAppender.increaseProgress();
			logger.info("Done");
			logger.info("");

			logger.info("Installing new version manifest...");
			File destManifest = new File(dest,
					"versions/" + manifest.get("id").getAsString() + "/" + manifest.get("id").getAsString() + ".json");
			if (destManifest.exists())
				destManifest.delete();
			if (!destManifest.getParentFile().exists())
				destManifest.getParentFile().mkdirs();
			Files.copy(manFile.toPath(), destManifest.toPath());
			logger.info("Done");
			logger.info("");
			ProgressWindow.WindowAppender.increaseProgress();

			logger.info("Downloading log configuration...");
			if (manifest.has("logging") && manifest.get("logging").getAsJsonObject().has("client")) {
				JsonObject logClient = manifest.get("logging").getAsJsonObject().get("client").getAsJsonObject()
						.get("file").getAsJsonObject();

				File logOut = new File(dest, "assets/log_configs/" + logClient.get("id").getAsString());
				if (!logOut.getParentFile().exists())
					logOut.getParentFile().mkdirs();
				if (logOut.exists())
					logOut.delete();

				FileOutputStream outp = new FileOutputStream(logOut);
				String url = logClient.get("url").getAsString();
				try {
					URL logURL = new URL(url);
					InputStream strm = logURL.openStream();
					logger.info("Installing...");
					strm.transferTo(outp);
					strm.close();
				} catch (IOException e) {
					String pth = null;
					for (String repoID : project.repositories.keySet()) {
						String base = project.repositories.get(repoID);
						if (!base.endsWith("/"))
							base += "/";
						if (url.startsWith(base)) {
							pth = url.substring(base.length());
							break;
						}
					}
					if (pth == null)
						throw new IOException("Failed to download log config from any repository.");

					boolean found = false;
					for (String repoID : project.repositories.keySet()) {
						String base = project.repositories.get(repoID);
						if (!base.endsWith("/"))
							base += "/";
						try {
							URL u = new URL(base + pth);
							InputStream strm = u.openStream();
							strm.transferTo(outp);
							logger.info("Installing...");
							strm.close();
							found = true;
							break;
						} catch (IOException e2) {
						}
					}
					if (!found)
						throw new IOException("Failed to download log config from any repository.");
				}
				outp.close();
			}
			logger.info("");
			ProgressWindow.WindowAppender.increaseProgress();

			if (!new File(dest, "versions/" + project.inheritsFrom).exists())
				missingParentVersion = true;

			if (chckbxNewCheckBox.isSelected()) {
				logger.info("Updating minecraft launcher profiles...");
				JsonObject profiles = new JsonObject();
				profiles.add("profiles", new JsonObject());
				if (new File(dest, "launcher_profiles.json").exists()) {
					profiles = JsonParser
							.parseString(Files.readString(new File(dest, "launcher_profiles.json").toPath()))
							.getAsJsonObject();
				}
				JsonObject profileList = profiles.get("profiles").getAsJsonObject();
				JsonObject profile = new JsonObject();
				if (profileList
						.has(project.name + "-" + project.version + "-" + project.game + "-" + project.platform)) {
					profile = profileList
							.get(project.name + "-" + project.version + "-" + project.game + "-" + project.platform)
							.getAsJsonObject();
					profile.remove("lastVersionId");
					profile.remove("created");
					profile.remove("javaDir");
					profile.remove("name");
					profileList
							.remove(project.name + "-" + project.version + "-" + project.game + "-" + project.platform);
				} else {
					profile.addProperty("type", "custom");
					profile.addProperty("icon", project.profileIcon);
				}
				profile.addProperty("name",
						project.profileName.replace("%v", project.version).replace("%gv", project.game));
				profile.addProperty("javaDir", ProcessHandle.current().info().command().get());
				profile.addProperty("created", new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.ms'Z'").format(new Date()));
				profile.addProperty("lastVersionId", manifest.get("id").getAsString());

				profileList.add(project.name + "-" + project.version + "-" + project.game + "-" + project.platform,
						profile);
				Files.writeString(new File(dest, "launcher_profiles.json").toPath(), new Gson().toJson(profiles));

				logger.info("Done");
				logger.info("");
				ProgressWindow.WindowAppender.increaseProgress();
			}
		} else if (side == GameSide.SERVER) {
			logger.info("Resolving server dependencies...");

			String cp = "";
			String bootcp = "";

			if (project.loader.equals("forge")) {
				project.loadFirst = ArrayUtil.insert(project.loadFirst, 0, new String[] { "log4j-api", "log4j-core" });
			}
			for (String lib : libs) {
				String group = lib.split(":")[0];
				String name = lib.split(":")[1];
				String ver = lib.split(":")[2];
				if (!Stream.of(project.fatServer).anyMatch(t -> t.equals(group + ":" + name))
						&& Stream.of(project.loadFirst).anyMatch(t -> t.equals(name))) {
					if (!cp.isEmpty())
						cp += " ";
					cp += "libraries/" + group.replace(".", "/") + "/" + name + "/" + ver + "/" + name + "-" + ver
							+ ".jar";
				} else if (!Stream.of(project.fatServer).anyMatch(t -> t.equals(group + ":" + name))
						&& Stream.of(project.bootLibs).anyMatch(t -> t.equals(name))) {
					if (!bootcp.isEmpty())
						bootcp += " ";
					bootcp += "libraries/" + group.replace(".", "/") + "/" + name + "/" + ver + "/" + name + "-" + ver
							+ ".jar";
				}
			}
			if (project.loader.equals("paper")) {
				if (!cp.isEmpty())
					cp += " ";
				cp += "paper-server-" + project.loaderVersion + ".jar";
				cp += " cache/patched_" + project.game + ".jar";
			}
			if (!cp.isEmpty())
				cp += " ";
			cp += "vanilla-server.jar";

			HashMap<String, ArrayList<File>> filePaths = new HashMap<String, ArrayList<File>>();
			HashMap<String, ArrayList<File>> outputPaths = new HashMap<String, ArrayList<File>>();
			HashMap<String, URL> remoteLibs = new HashMap<String, URL>();
			for (String lib : libs) {
				String group = lib.split(":")[0];
				String name = lib.split(":")[1];
				String ver = lib.split(":")[2];
				if (!Stream.of(project.fatServer).anyMatch(t -> t.equals(group + ":" + name))
						&& !Stream.of(project.loadFirst).anyMatch(t -> t.equals(name))
						&& !Stream.of(project.bootLibs).anyMatch(t -> t.equals(name)))
					cp += " libraries/" + group.replace(".", "/") + "/" + name + "/" + ver + "/" + name + "-" + ver
							+ ".jar";
			}

			logger.info("Installing vanilla server...");
			File vanillaJar = new File(dest, "vanilla-server.jar");
			if (vanillaJar.exists())
				vanillaJar.delete();
			Files.copy(MinecraftInstallationToolkit.getVersionJar(version, side).toPath(), vanillaJar.toPath());

			String loaderArti = "";
			String intermediaryArti = "";

			logger.info("Processing platform dependencies...");
			if (project.loader.equals("paper")) {

				logger.info("");
				logger.info("");
				logger.info("");
				logger.info("DISCLAIMER!");
				logger.info("Spigot/Paper is not owned by the AerialWorks Software Foundation,");
				logger.info("We only use their files to create compatible servers.");
				logger.info("");
				logger.info("");
				logger.info("");
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
				}

				logger.info("Downloading paper server...");
				URL u = new URL("https://papermc.io/api/v2/projects/paper/versions/" + project.game + "/builds/"
						+ project.loaderVersion + "/downloads/paper-" + project.game + "-" + project.loaderVersion
						+ ".jar");

				File paperJar = new File(dest, "paper-server-" + project.loaderVersion + ".jar");
				InputStream strm = u.openStream();
				FileOutputStream strmOut = new FileOutputStream(paperJar);
				strm.transferTo(strmOut);
				strm.close();
				strmOut.close();

				logger.info("Patching vanilla server with the paper patches...");
				ProcessBuilder builder = new ProcessBuilder();
				builder.directory(dest);
				builder.command(ProcessHandle.current().info().command().get(), "-Dpaperclip.patchonly=true", "-jar",
						paperJar.getCanonicalPath());
				Process proc = builder.start();
				attachLog(proc);
				try {
					proc.waitFor();
				} catch (InterruptedException e) {
				}
				if (proc.exitValue() != 0)
					throw new IOException("Paper exited with non-zero exit code.");

			} else if (project.loader.equals("forge")) {

				logger.info("");
				logger.info("");
				logger.info("");
				logger.info("DISCLAIMER!");
				logger.info("Forge is not owned by the AerialWorks Software Foundation,");
				logger.info("We only use their files to create compatible servers/clienst.");
				logger.info("");
				logger.info("");
				logger.info("");
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
				}

				logger.info("Resolving Forge server libraries...");
				File forgeInstaller = downloadForgeInstaller(project, cache);
				File serverFolder = new File(cache, "forge-" + project.game + "-" + project.loaderVersion + "-server");
				File serverFolderTmp = new File(cache,
						"forge-" + project.game + "-" + project.loaderVersion + "-server.tmp");
				if (!serverFolder.exists()) {
					logger.info("Installing forge server for dependency resolution...");
					ProcessBuilder builder = new ProcessBuilder();
					builder.directory(cache);
					builder.command(ProcessHandle.current().info().command().get(), "-jar",
							forgeInstaller.getCanonicalPath(), "--installServer", serverFolderTmp.getAbsolutePath());
					Process proc = builder.start();
					attachLog(proc);
					try {
						proc.waitFor();
					} catch (InterruptedException e) {
					}
					if (proc.exitValue() != 0)
						throw new IOException("Forge installer exited with non-zero exit code.");
					moveDir(serverFolderTmp, serverFolder);
				}
				logger.info("Processing Forge server files...");
				HashMap<String, File> files = scanFiles(new File(serverFolder, "libraries"), "libraries/", "jar",
						"jar.cache", "zip", "txt");

				for (String str : files.keySet()) {
					String[] information = str.split("/");
					String group = "";
					String name = "";
					String ver = "";

					for (int i = information.length - 2; i >= 1; i--) {
						if (ver.isEmpty())
							ver = information[i];
						else if (name.isEmpty())
							name = information[i];
						else {
							if (group.isEmpty())
								group = information[i];
							else
								group = information[i] + "." + group;
						}
					}

					String groupstr = group.replaceAll("\\.", "/");
					if (!groupstr.isEmpty())
						groupstr += "/";
					final String groupPath = groupstr;
					boolean newer = false;
					for (String libfile : files.keySet()) {
						if (libfile.startsWith("libraries/" + groupstr + name + "/")) {
							String newversion = libfile.substring(("libraries/" + groupstr + name + "/").length());
							newversion = newversion.substring(0, newversion.indexOf("/"));
							if (!ver.equals(newversion)) {
								newversion = newversion.replaceAll("[^0-9.]", "");
								String oldver = ver.replaceAll("[^0-9.]", "");
								int ind = 0;
								String[] old = oldver.split("\\.");
								for (String vn : newversion.split("\\.")) {
									if (ind < old.length) {
										String vnold = old[ind];
										if (Integer.valueOf(vn) > Integer.valueOf(vnold)) {
											newer = true;
											break;
										} else if (Integer.valueOf(vn) < Integer.valueOf(vnold)) {
											break;
										}
										ind++;
									} else
										break;
								}
								if (newer)
									break;
							}
						}
					}

					if (!newer) {
						String pth = "libraries/" + groupPath + name + "/" + ver + "/";
						String[] srvlibs = files.keySet().stream().filter(t -> t.startsWith(pth))
								.toArray(t -> new String[t]);

						String artifact = group + ":" + name;
						if (!filePaths.keySet().stream().anyMatch(t -> t.startsWith(artifact + ":"))) {
							for (String lib : srvlibs) {
								if (lib.endsWith(".jar")
										&& !(lib.contains("net/minecraft/server")
												&& (lib.contains("-slim.jar") || lib.contains("-srg.jar")))
										&& !(lib.contains("net/minecraftforge/forge") && lib.contains("-server.jar")))
									cp += " " + lib;

								ArrayList<File> files1 = filePaths.get(group + ":" + name + ":" + ver);
								if (files1 == null)
									files1 = new ArrayList<File>();
								ArrayList<File> files2 = outputPaths.get(group + ":" + name + ":" + ver);
								if (files2 == null)
									files2 = new ArrayList<File>();

								if (!files1.contains(files.get(lib)))
									files1.add(files.get(lib));
								if (!files2.contains(new File(dest, lib)))
									files2.add(new File(dest, lib));

								filePaths.put(group + ":" + name + ":" + ver, files1);
								outputPaths.put(group + ":" + name + ":" + ver, files2);
							}
						}
					}
				}
			} else if (project.loader.equals("fabric-loader")) {

				logger.info("");
				logger.info("");
				logger.info("");
				logger.info("DISCLAIMER!");
				logger.info("Fabric is not owned by the AerialWorks Software Foundation,");
				logger.info("We only use their files to create compatible servers.");
				logger.info("");
				logger.info("");
				logger.info("");
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
				}

				logger.info("Resolving fabric dependencies...");
				URL mdataURL = new URL(
						"https://meta.fabricmc.net/v2/versions/loader/" + project.game + "/" + project.loaderVersion);
				InputStreamReader rd = new InputStreamReader(mdataURL.openStream());
				JsonElement ele = JsonParser.parseReader(rd);
				JsonObject metadata = (ele.isJsonArray() ? ele.getAsJsonArray().get(0).getAsJsonObject()
						: ele.getAsJsonObject());
				rd.close();

				for (JsonElement dependency : metadata.get("launcherMeta").getAsJsonObject().get("libraries")
						.getAsJsonObject().get("common").getAsJsonArray()) {
					JsonObject obj = dependency.getAsJsonObject();
					String[] information = obj.get("name").getAsString().split(":");
					String url = obj.get("url").getAsString();

					String group = "";
					String name = information[0];
					String versionstr = "";

					if (information.length == 3) {
						group = information[0];
						name = information[1];
						versionstr = information[2];
					} else if (information.length >= 2) {
						name = information[0];
						versionstr = information[1];
					}

					url = url + "/" + group.replaceAll("\\.", "/") + "/" + name + "/" + versionstr + "/" + name + "-"
							+ versionstr + ".jar";
					String id = group + ":" + name + ":";
					String idFull = id + versionstr;
					if (!filePaths.keySet().stream().anyMatch(t -> t.startsWith(id))) {
						if (!cp.isEmpty())
							cp += " ";

						String lib = "libraries/" + group.replaceAll("\\.", "/");

						lib += "/" + name + "/" + versionstr + "/" + name + "-" + versionstr + ".jar";
						remoteLibs.put(idFull, new URL(url));

						ArrayList<File> files = outputPaths.get(group + ":" + name + ":" + versionstr);
						if (files == null)
							files = new ArrayList<File>();
						if (!files.contains(new File(dest, lib)))
							files.add(new File(dest, lib));
						outputPaths.put(group + ":" + name + ":" + versionstr, files);

						cp += lib;
					}
				}

				for (JsonElement dependency : metadata.get("launcherMeta").getAsJsonObject().get("libraries")
						.getAsJsonObject().get("server").getAsJsonArray()) {
					JsonObject obj = dependency.getAsJsonObject();
					String[] information = obj.get("name").getAsString().split(":");
					String url = obj.get("url").getAsString();

					String group = "";
					String name = information[0];
					String versionstr = "";

					if (information.length == 3) {
						group = information[0];
						name = information[1];
						versionstr = information[2];
					} else if (information.length >= 2) {
						name = information[0];
						versionstr = information[1];
					}

					url = url + "/" + group.replaceAll("\\.", "/") + "/" + name + "/" + versionstr + "/" + name + "-"
							+ versionstr + ".jar";
					String id = group + ":" + name + ":";
					String idFull = id + versionstr;
					if (!filePaths.keySet().stream().anyMatch(t -> t.startsWith(id))) {
						if (!cp.isEmpty())
							cp += " ";

						String lib = "libraries/" + group.replaceAll("\\.", "/");

						lib += "/" + name + "/" + versionstr + "/" + name + "-" + versionstr + ".jar";
						remoteLibs.put(idFull, new URL(url));

						ArrayList<File> files1 = filePaths.get(idFull);
						if (files1 == null)
							files1 = new ArrayList<File>();
						if (!files1.contains(new File(dest, lib)))
							files1.add(new File(dest, lib));

						ArrayList<File> files2 = outputPaths.get(group + ":" + name + ":" + versionstr);
						if (files2 == null)
							files2 = new ArrayList<File>();
						if (!files2.contains(new File(dest, lib)))
							files2.add(new File(dest, lib));

						filePaths.put(idFull, files1);
						outputPaths.put(group + ":" + name + ":" + versionstr, files2);

						cp += lib;
					}
				}

				String[] information = metadata.get("loader").getAsJsonObject().get("maven").getAsString().split(":");
				loaderArti = metadata.get("loader").getAsJsonObject().get("maven").getAsString();
				String url = "https://maven.fabricmc.net/";

				String group = "";
				String name = information[0];
				String versionstr = "";

				if (information.length == 3) {
					group = information[0];
					name = information[1];
					versionstr = information[2];
				} else if (information.length >= 2) {
					name = information[0];
					versionstr = information[1];
				}

				url = url + "/" + group.replaceAll("\\.", "/") + "/" + name + "/" + versionstr + "/" + name + "-"
						+ versionstr + ".jar";
				String id = group + ":" + name + ":";
				String idFull = id + versionstr;
				if (!filePaths.keySet().stream().anyMatch(t -> t.startsWith(id))) {
					if (!cp.isEmpty())
						cp += " ";

					String lib = "libraries/" + group.replaceAll("\\.", "/");

					lib += "/" + name + "/" + versionstr + "/" + name + "-" + versionstr + ".jar";
					remoteLibs.put(idFull, new URL(url));
					cp += lib;
				}

				information = metadata.get("intermediary").getAsJsonObject().get("maven").getAsString().split(":");
				intermediaryArti = metadata.get("intermediary").getAsJsonObject().get("maven").getAsString();
				url = "https://maven.fabricmc.net/";

				group = "";
				name = information[0];
				versionstr = "";

				if (information.length == 3) {
					group = information[0];
					name = information[1];
					versionstr = information[2];
				} else if (information.length >= 2) {
					name = information[0];
					versionstr = information[1];
				}

				url = url + "/" + group.replaceAll("\\.", "/") + "/" + name + "/" + versionstr + "/" + name + "-"
						+ versionstr + ".jar";
				String id2 = group + ":" + name + ":";
				String idFull2 = id2 + versionstr;
				if (!filePaths.keySet().stream().anyMatch(t -> t.startsWith(id2))) {
					if (!cp.isEmpty())
						cp += " ";

					String lib = "libraries/" + group.replaceAll("\\.", "/");

					lib += "/" + name + "/" + versionstr + "/" + name + "-" + versionstr + ".jar";
					remoteLibs.put(idFull2, new URL(url));
					cp += lib;
				}
				logger.info("Done.");
			}
			logger.info("Processed " + filePaths.size() + " libraries.");
			logger.info("");
			logger.info("Downloading remote libraries...");
			for (String id : remoteLibs.keySet()) {
				URL v = remoteLibs.get(id);

				String group = id.split(":")[0];
				String name = id.split(":")[1];
				String versionstr = id.split(":")[2];

				String k = "libraries/" + group.replaceAll("\\.", "/") + "/" + name + "/" + versionstr + "/" + name
						+ "-" + versionstr + ".jar";

				File inputcache = new File(cache, k);
				File downloadmarker = new File(cache, k + ".lck");
				if (!inputcache.exists() || downloadmarker.exists()) {
					if (!inputcache.getParentFile().exists())
						inputcache.getParentFile().mkdirs();
					if (downloadmarker.exists())
						downloadmarker.delete();
					downloadmarker.createNewFile();
					logger.info("Downloading library into cache... file: " + inputcache.getName());

					InputStream strm = v.openStream();
					if (inputcache.exists())
						inputcache.delete();
					FileOutputStream strm2 = new FileOutputStream(inputcache);
					strm.transferTo(strm2);
					strm2.close();
					strm.close();
					downloadmarker.delete();
				}

				logger.info("Adding cached libary to install list... file: " + inputcache.getName());
				ArrayList<File> files1 = filePaths.get(id);
				if (files1 == null)
					files1 = new ArrayList<File>();
				if (!files1.contains(new File(cache, k)))
					files1.add(new File(cache, k));

				ArrayList<File> files2 = outputPaths.get(id);
				if (files2 == null)
					files2 = new ArrayList<File>();
				if (!files2.contains(new File(dest, k)))
					files2.add(new File(dest, k));

				filePaths.put(id, files1);
				outputPaths.put(id, files2);
			}

			logger.info("Installing local libraries...");
			for (String lib : filePaths.keySet()) {
				logger.info("Installing " + lib + "...");

				int i = 0;
				ArrayList<File> inF = filePaths.get(lib);
				ArrayList<File> outF = outputPaths.get(lib);
				for (File in : inF) {
					File out = outF.get(i);

					if (!out.getParentFile().exists())
						out.getParentFile().mkdirs();
					if (out.exists())
						out.delete();
					Files.copy(in.toPath(), out.toPath());

					i++;
				}
			}

			if (project.loader.equals("fabric-loader")) {
				logger.info("Merging fabric loader and intermediary jars...");
				File loader = null;
				for (File f : outputPaths.get(loaderArti)) {
					if (f.getName().endsWith("-" + loaderArti.split(":")[2] + ".jar")) {
						loader = f;
						break;
					}
				}
				File intermediary = null;
				for (File f : outputPaths.get(intermediaryArti)) {
					if (f.getName().endsWith("-" + loaderArti.split(":")[2] + ".jar")) {
						intermediary = f;
						break;
					}
				}
				File output = new File(loader.getCanonicalPath() + ".intermediary.tmp");
				if (output.exists())
					output.delete();

				ZipOutputStream strm = new ZipOutputStream(new FileOutputStream(output));
				ZipFile loaderJar = new ZipFile(loader);
				ZipFile intermediaryJar = new ZipFile(intermediary);

				Enumeration<? extends ZipEntry> entries = loaderJar.entries();
				ArrayList<String> knownentries = new ArrayList<String>();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					String path = entry.getName().replaceAll("\\\\", "/");
					if (knownentries.contains(path))
						continue;
					knownentries.add(path);

					strm.putNextEntry(entry);
					loaderJar.getInputStream(entry).transferTo(strm);
					strm.closeEntry();
				}

				entries = intermediaryJar.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					String path = entry.getName().replaceAll("\\\\", "/");
					if (knownentries.contains(path))
						continue;
					knownentries.add(path);

					strm.putNextEntry(entry);
					intermediaryJar.getInputStream(entry).transferTo(strm);
					strm.closeEntry();
				}
				strm.close();
				intermediaryJar.close();
				loaderJar.close();

				logger.info("Installing output jar...");
				loader.delete();
				Files.move(output.toPath(), loader.toPath());
			}

			logger.info("Done.");
			ProgressWindow.WindowAppender.increaseProgress();
			logger.info("");

			logger.info("Generating manifest...");
			Manifest manifest = new Manifest();
			Attributes main = manifest.getMainAttributes();
			project.jarManifest.forEach((k, v) -> {
				project.jarManifest.put(k,
						v.replace("%time", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date()))
								.replace("%gv", project.game).replace("%i", project.inheritsFrom)
								.replace("%pv", project.version).replace("%ln", project.loader)
								.replace("%lv", project.loaderVersion))
						.replace("%pl", project.platform);
			});

			main.put(Attributes.Name.MANIFEST_VERSION, "1.0");
			project.jarManifest.forEach((k, v) -> {
				project.jarManifest.put(k, v.replace("%time",
						new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date()).replace("%gv", project.game)
								.replace("%i", project.inheritsFrom).replace("%pv", project.version)
								.replace("%ln", project.loader).replace("%lv", project.loaderVersion)));
			});
			project.jarManifest.forEach((k, v) -> {
				main.put(new Attributes.Name(k), v);
			});
			main.put(new Attributes.Name("Boot-Class-Path"), bootcp);
			main.put(Attributes.Name.CLASS_PATH, cp);
			main.put(Attributes.Name.MAIN_CLASS, project.serverMain);

			File jarOutputFile = new File(dest, project.serverOutput.replace("%wv", project.wrapper)
					.replace("%gv", project.game).replace("%pv", project.version).replace("%i", project.inheritsFrom));
			if (jarOutputFile.exists())
				jarOutputFile.delete();

			FileOutputStream outputFileStream = new FileOutputStream(jarOutputFile);
			JarOutputStream output = new JarOutputStream(outputFileStream, manifest);

			ProgressWindow.WindowAppender.increaseProgress();
			logger.info("Building server jar...");
			ArrayList<String> entries = new ArrayList<String>();
			for (String lib : libs) {
				if (Stream.of(project.fatServer).anyMatch(t -> lib.startsWith(t + ":"))) {
					FileInputStream fin = new FileInputStream(libraryFiles.get(lib));
					ZipInputStream zip = new ZipInputStream(fin);
					ZipEntry ent = zip.getNextEntry();
					while (ent != null) {
						String pth = ent.getName().replace("\\", "");
						if (pth.startsWith("/"))
							pth = pth.substring(1);

						if (!entries.contains(pth) && !pth.equalsIgnoreCase("META-INF/MANIFEST.MF")) {
							output.putNextEntry(new ZipEntry(ent.getName()));
							if (!pth.endsWith("/"))
								zip.transferTo(output);
							output.closeEntry();
							entries.add(pth);
						}

						ent = zip.getNextEntry();
					}
					zip.close();
					fin.close();
				}
			}

			ProgressWindow.WindowAppender.increaseProgress();
			logger.info("");

			logger.info("Saving...");
			output.flush();
			outputFileStream.flush();
			output.close();
			outputFileStream.close();
			ProgressWindow.WindowAppender.increaseProgress();
			logger.info("");

		}

		if (chckbxNewCheckBox_1.isSelected()) {
			logger.info("Installing the KickStart Mod Installer...");
			File outp = new File(APPDATA, ".minecraft/kickstart.jar");

			if (outp.exists())
				outp.delete();
			if (!outp.getParentFile().exists())
				outp.getParentFile().mkdirs();

			try {
				Files.copy(new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).toPath(),
						outp.toPath());
			} catch (IOException | URISyntaxException e) {
				throw new IOException(e);
			}
			logger.info("Registering installer...");
			if (MinecraftInstallationToolkit.OsInfo.getCurrent() == OsInfo.windows) {
				File reg = new File(APPDATA, ".minecraft/kickstart.reg");
				if (!reg.exists()) {
					InputStream strm = getClass().getClassLoader().getResourceAsStream("kickstart.reg");
					String cont = new String(strm.readAllBytes())
							.replace("%appdata%", APPDATA.getCanonicalPath().replace("\\", "\\\\"))
							.replace("%java%", ProcessHandle.current().info().command().get().replace("\\", "\\\\"));
					Files.writeString(reg.toPath(), cont);
					strm.close();
				}
				InputStream strm = getClass().getClassLoader().getResourceAsStream("winsudo");
				File winsudo = File.createTempFile("winsudo", ".bat");
				Files.write(winsudo.toPath(),
						new String(strm.readAllBytes()).replace("\r", "").replace("\n", "\r\n").getBytes());
				strm.close();
				logger.info("Installing as admin...");
				ProcessBuilder builder = new ProcessBuilder();
				builder.command(winsudo.getCanonicalPath(), "reg", "import", reg.getCanonicalPath());
				Process proc = builder.start();
				try {
					proc.waitFor();
				} catch (InterruptedException e) {
				}
				if (proc.exitValue() != 0)
					throw new IOException("Registry script exited with non-zero exit code");
			} else {
				File xml = new File(APPDATA, ".minecraft/cyan-kickstart.xml");
				InputStream strm = getClass().getClassLoader().getResourceAsStream("cyan-kickstart.xml");
				String cont = new String(strm.readAllBytes()).replace("%appdata%", APPDATA.getCanonicalPath())
						.replace("%java%", ProcessHandle.current().info().command().get());
				Files.writeString(xml.toPath(), cont);
				strm.close();
				File desktop = new File(APPDATA, ".minecraft/cyan-kickstart.desktop");
				strm = getClass().getClassLoader().getResourceAsStream("kickstart.desktop");
				cont = new String(strm.readAllBytes()).replace("%appdata%", APPDATA.getCanonicalPath())
						.replace("%java%", ProcessHandle.current().info().command().get());
				Files.writeString(desktop.toPath(), cont);
				strm.close();
				logger.info("Creating registry script...");

				File bashFile = File.createTempFile("kickstart-install", ".bash");
				StringBuilder bashScript = new StringBuilder();
				bashScript.append("#!/bin/bash").append("\n");
				bashScript.append("xdg-mime install \"" + xml.getCanonicalPath() + "\" || exit 1").append("\n");
				bashScript.append("update-mime-database ~/.local/share/mime || exit 1").append("\n");
				bashScript.append(
						"xdg-mime default \"" + desktop.getCanonicalPath() + "\" application/x-kickstart-cmf || exit 1")
						.append("\n");
				bashScript.append("cp  \"" + desktop.getCanonicalPath() + "\" ~/.local/share/applications")
						.append("\n");
				bashScript.append("").append("\n");
				bashScript.append("rm -- \"$0\"").append("\n");
				Files.writeString(bashFile.toPath(), bashScript.toString());

				logger.info("Starting script...");
				ProcessBuilder builder = new ProcessBuilder();
				builder.command("bash", bashFile.getCanonicalPath());
				Process proc = builder.start();
				try {
					proc.waitFor();
				} catch (InterruptedException e) {
				}
				if (proc.exitValue() != 0)
					throw new IOException("Registry script exited with non-zero exit code");
			}
			logger.info("Done.");
			ProgressWindow.WindowAppender.increaseProgress();
			logger.info("");
		}

		logger.info("Finalizing...");
		if (!new File(APPDATA, ".kickstart-installer.ccfg").exists())
			new File(APPDATA, ".kickstart-installer.ccfg").createNewFile();
		ProgressWindow.WindowAppender.increaseProgress();
		logger.info("Installation completed.\nInstalled in: " + dest.getCanonicalPath());

		if (missingParentVersion && interactive) {
			JOptionPane.showMessageDialog(frmCyanInstaller, project.name
					+ " has been installed into the launcher.\nPlease know that it cannot be launched until the '"
					+ project.inheritsFrom + "' version has been installed.", "Installation completed",
					JOptionPane.INFORMATION_MESSAGE);
		} else if (side == GameSide.CLIENT && interactive) {
			JOptionPane.showMessageDialog(frmCyanInstaller,
					project.name + " has been installed into the launcher.\nAll requirements are present.",
					"Installation completed", JOptionPane.INFORMATION_MESSAGE);
		} else if (side == GameSide.SERVER && interactive) {
			JOptionPane.showMessageDialog(frmCyanInstaller,
					project.name + " has been installed.\nAll requirements are present.", "Installation completed",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private HashMap<String, File> scanFiles(File inp, String start, String... extensions) {
		HashMap<String, File> collection = new HashMap<String, File>();
		for (File f : inp.listFiles(f2 -> {
			for (String extension : extensions) {
				if (f2.getName().endsWith("." + extension))
					return true;
			}
			return false;
		})) {
			collection.put(start + f.getName(), f);
		}
		for (File f : inp.listFiles(f2 -> {
			return f2.isDirectory();
		})) {
			scanFiles(f, start + f.getName() + "/", extensions).forEach((p, f2) -> {
				collection.put(p, f2);
			});
		}
		return collection;
	}

	private void moveDir(File in, File out) throws IOException {
		out.mkdirs();
		for (File dir : in.listFiles(t -> t.isDirectory()))
			moveDir(dir, new File(out, dir.getName()));
		for (File file : in.listFiles(t -> !t.isDirectory())) {
			if (new File(out, file.getName()).exists())
				new File(out, file.getName()).delete();
			Files.move(file.toPath(), new File(out, file.getName()).toPath());
		}
		in.delete();
	}

	private void attachLog(Process proc) {
		new Thread(() -> {
			try {
				while (proc.isAlive()) {
					String buffer = "";
					while (true) {
						int b = proc.getInputStream().read();
						if (b == -1)
							return;
						char ch = (char) b;
						if (ch == '\r')
							continue;
						else if (ch == '\n')
							break;
						buffer += ch;
					}
					logger.info(buffer);
				}
			} catch (IOException e) {

			}
		}, "Process Logger").start();
		new Thread(() -> {
			try {
				while (proc.isAlive()) {
					String buffer = "";
					while (true) {
						int b = proc.getErrorStream().read();
						if (b == -1)
							return;
						char ch = (char) b;
						if (ch == '\r')
							continue;
						else if (ch == '\n')
							break;
						buffer += ch;
					}
					logger.error(buffer);
				}
			} catch (IOException e) {

			}
		}, "Process Logger").start();
	}

	private void installLibs(File dest, ArrayList<String> libs, HashMap<String, File> libraryFiles,
			ProjectConfig project, GameSide side) {
		libs.forEach(lib -> {
			File file = libraryFiles.get(lib);
			String group = lib.split(":")[0];
			String name = lib.split(":")[1];
			String libver = lib.split(":")[2];

			if (side == GameSide.SERVER && Stream.of(project.fatServer).anyMatch(t -> t.equals(group + ":" + name))) {
				return;
			}

			logger.info("Installing library " + lib + "...");
			File output = new File(dest, "libraries/" + group.replace(".", "/") + "/" + name + "/" + libver + "/" + name
					+ "-" + libver + ".jar");
			if (output.exists())
				output.delete();
			if (!output.getParentFile().exists())
				output.getParentFile().mkdirs();
			try {
				Files.copy(file.toPath(), output.toPath());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private File download(String lib, File cache, ProjectConfig project, HashMap<String, String> libraryPaths)
			throws IOException {
		String[] info = lib.split(":");
		String group = info[0];
		String name = info[1];
		String ver = info[2];
		String libPath = group.replace(".", "/") + "/" + name + "/" + ver + "/" + name + "-" + ver + ".jar";
		for (String repoID : project.repositories.keySet()) {
			String base = project.repositories.get(repoID);
			URL loc = null;
			try {
				URL u = new URL(base + "/" + libPath);
				u.openStream().close();
				libraryPaths.put(lib, base);
				loc = u;
			} catch (IOException e) {
			}
			if (loc != null) {
				String expectedHash = null;
				boolean uptodate = false;
				try {
					URL u2 = new URL(base + "/" + libPath + ".sha1");
					InputStream strm = u2.openStream();
					expectedHash = new String(strm.readAllBytes());
					strm.close();
				} catch (IOException e) {
				}
				File outputFile = new File(cache, "caches/libraries/" + libPath);
				if (!outputFile.getParentFile().exists())
					outputFile.getParentFile().mkdirs();

				if (expectedHash != null && outputFile.exists()) {
					if (expectedHash.equals(sha1HEX(Files.readAllBytes(outputFile.toPath()))))
						uptodate = true;
				}

				if (!uptodate) {
					logger.info("Downloading " + outputFile.getName() + "...");
					InputStream strm = loc.openStream();
					FileOutputStream outp = new FileOutputStream(outputFile);
					strm.transferTo(outp);
					strm.close();
					outp.close();
				} else {
					logger.info("Skipping file " + outputFile.getName() + " as it is up to date.");
				}

				return outputFile;
			}
		}
		throw new IOException("Could not download library " + lib + " from any repository.");
	}

	private static String sha1HEX(byte[] array) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
		byte[] sha = digest.digest(array);
		StringBuilder result = new StringBuilder();
		for (byte aByte : sha) {
			result.append(String.format("%02x", aByte));
		}
		return result.toString();
	}

	private File downloadForgeInstaller(ProjectConfig project, File cache) throws IOException {
		String forgeurltemplate = "https://files.minecraftforge.net/maven/net/minecraftforge/forge/%game%-%forgeversion%/forge-%game%-%forgeversion%-installer.jar";

		URL url = new URL(forgeurltemplate.replaceAll("\\%game\\%", project.game).replaceAll("\\%forgeversion\\%",
				project.loaderVersion));
		File forgeInstaller = new File(cache, "forge-" + project.game + "-" + project.loaderVersion + "-installer.jar");

		File downloadmarker = new File(cache, forgeInstaller.getName() + ".lck");
		if (!forgeInstaller.exists() || downloadmarker.exists()) {
			if (!forgeInstaller.getParentFile().exists())
				forgeInstaller.getParentFile().mkdirs();
			if (downloadmarker.exists())
				downloadmarker.delete();
			downloadmarker.createNewFile();

			logger.info("Downloading forge installer to cache...");
			if (forgeInstaller.exists())
				forgeInstaller.delete();
			FileOutputStream strm = new FileOutputStream(forgeInstaller);
			InputStream inp = url.openStream();
			inp.transferTo(strm);
			strm.close();
			inp.close();
			downloadmarker.delete();
		}
		return forgeInstaller;
	}
}
