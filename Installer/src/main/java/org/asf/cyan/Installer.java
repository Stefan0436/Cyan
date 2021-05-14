package org.asf.cyan;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

import java.awt.Dimension;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import java.awt.Font;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.core.CyanCore;
import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.implementation.CyanBytecodeExporter;
import org.asf.cyan.fluid.implementation.CyanReportBuilder;
import org.asf.cyan.fluid.implementation.CyanTransformer;
import org.asf.cyan.fluid.implementation.CyanTransformerMetadata;
import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftMappingsToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftModdingToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftVersionToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionType;

import java.awt.Color;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JCheckBox;

public class Installer extends CyanComponent {

	private static Installer impl;

	private static boolean init = false;

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

	/**
	 * Create the application.
	 */
	public Installer() throws IOException {
		initialize();
	}

	public static File APPDATA;
	private JCheckBox chckbxNewCheckBox;
	private JCheckBox chckbxNewCheckBox_1;

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

		CyanCore.initLoader();
		File cache = new File(APPDATA, ".kickstart");
		MinecraftInstallationToolkit.setMinecraftDirectory(cache);

		frmCyanInstaller = new JFrame();
		frmCyanInstaller.setTitle("Installer");
		frmCyanInstaller.setResizable(false);
		frmCyanInstaller.setBounds(100, 100, 640, 428);
		frmCyanInstaller.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmCyanInstaller.setLocationRelativeTo(null);

		ProjectConfig project = new ProjectConfig();
		frmCyanInstaller.setTitle(project.name + " Installer");

		JPanel panel = new JPanel();
		panel.setBorder(new LineBorder(new Color(0, 0, 0)));
		frmCyanInstaller.getContentPane().add(panel, BorderLayout.SOUTH);
		panel.setLayout(new BorderLayout(0, 0));

		JLabel lblNewLabel = new JLabel(" " + project.name + " " + project.version);
		panel.add(lblNewLabel, BorderLayout.WEST);
		lblNewLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
		lblNewLabel.setVerticalAlignment(SwingConstants.TOP);
		lblNewLabel.setPreferredSize(new Dimension(200, 18));

		JLabel lblKickstart = new JLabel("KickStart Installer 1.0 ");
		panel.add(lblKickstart, BorderLayout.EAST);
		lblKickstart.setFont(new Font("SansSerif", Font.BOLD, 12));
		lblKickstart.setHorizontalAlignment(SwingConstants.TRAILING);
		lblKickstart.setVerticalAlignment(SwingConstants.TOP);
		lblKickstart.setPreferredSize(new Dimension(200, 18));

		JPanel panel_1 = new JPanel();
		panel_1.setBorder(new LineBorder(new Color(0, 0, 0)));
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
		lblNewLabel_2.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel_2.setFont(new Font("SansSerif", Font.PLAIN, 16));
		panel_1.add(lblNewLabel_2, BorderLayout.SOUTH);

		JPanel panel_2 = new JPanel();
		panel_2.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		frmCyanInstaller.getContentPane().add(panel_2, BorderLayout.CENTER);
		panel_2.setLayout(null);

		textField = new JTextField();
		textField.setBounds(77, 130, 365, 26);
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
								String mfUri = project.manifest.replace("%wv", project.wrapper)
										.replace("%gv", project.game).replace("%pv", project.version);
								URL u = new URL(base + "/" + mfUri);
								u.openStream().close();
								manifest = u;
								break;
							} catch (IOException e) {
							}
						}

						logger.info("Creating fake version info for modloader...");
						MinecraftVersionInfo info = new MinecraftVersionInfo(
								project.inheritsFrom + "-cyan-" + project.version, MinecraftVersionType.UNKNOWN,
								manifest, OffsetDateTime.now());

						logger.info("Downloading modloader version manifest...");
						MinecraftInstallationToolkit.saveVersionManifest(info);

						logger.info("Finding vanilla version...");
						MinecraftVersionInfo version = MinecraftVersionToolkit.getVersion(project.game);

						logger.info("Downloading vanilla version manifest...");
						MinecraftInstallationToolkit.saveVersionManifest(version);

						int progressMax = 0;
						ProgressWindow.WindowAppender.addMax(progressMax);
						runInstaller(outputDir, version, info, cache, project, GameSide.CLIENT);
					} catch (Exception e) {
						logger.fatal(e);
						SwingUtilities.invokeLater(() -> {
							ProgressWindow.WindowAppender.fatalError();
							ProgressWindow.WindowAppender.closeWindow();
							frmCyanInstaller.dispose();
						});
					}
				}, "Installer").start();
			}
		});
		btnNewButton.setBounds(42, 93, 275, 25);
		panel_2.add(btnNewButton);

		JButton btnNewButton_1 = new JButton("Create Server");
		btnNewButton_1.setBounds(322, 93, 275, 25);
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
		btnNewButton_2.setBounds(445, 130, 117, 25);
		panel_2.add(btnNewButton_2);

		chckbxNewCheckBox = new JCheckBox("Create launcher profile");
		chckbxNewCheckBox.setSelected(true);
		chckbxNewCheckBox.setBounds(77, 156, 189, 23);
		panel_2.add(chckbxNewCheckBox);

		chckbxNewCheckBox_1 = new JCheckBox("Associate Mod Installer Extensions");
		chckbxNewCheckBox_1.setBounds(270, 156, 292, 23);
		panel_2.add(chckbxNewCheckBox_1);
	}

	private void runInstaller(File dest, MinecraftVersionInfo version, MinecraftVersionInfo modloader, File cache,
			ProjectConfig project, GameSide side) throws IOException {
		String suffix = "";
		if (!project.loader.isEmpty())
			suffix += "-" + project.loader;
		if (!project.loaderVersion.isEmpty())
			suffix += "-" + project.loaderVersion;
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

		progressMax++; // Resolve modloader libraries // TODO
		progressMax++; // Store rift libraries in memory for remapping // TODO
		progressMax++; // Download all regular libraries (and rift, but pre-mapped) // TODO
		progressMax++; // Build rift libraries // TODO
		progressMax++; // Generate new version manifest (use urls in old manifest, exclude rift urls
						// and CyanCore) // TODO
		progressMax++; // Build libraries folder structure, exclude fat server libs (if called for the
						// server) // TODO
		progressMax++; // Modify cyan.release.ccfg in the CyanCore to match the environment // TODO

		progressMax++; // Install libraries // TODO
		if (side == GameSide.CLIENT) {
			progressMax++; // Build version folder structure (client only) // TODO
			progressMax++; // Build version log folder (client only) // TODO

			if (chckbxNewCheckBox.isSelected())
				progressMax++; // Generate launcher profile or replace existing // TODO
		}
		if (side == GameSide.SERVER) {
			progressMax++; // Download platform dependencies and jar if needed // TODO
			progressMax++; // Build server fat jar // TODO
			progressMax++; // Build server jar manifest // TODO
			progressMax++; // Create server jar // TODO
		}
		if (chckbxNewCheckBox_1.isSelected())
			progressMax++; // Install the Mod Installer // TODO

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
			ProgressWindow.WindowAppender.increaseProgress();
			ProgressWindow.WindowAppender.increaseProgress();
		}

		logger.info("Resolving platform mappings...");
		if (!project.platform.equals("VANILLA") && (project.mappings == null || project.mappings.isEmpty())) {
			logger.info("Determining mappings version by modloader...");
			if (project.loader.equalsIgnoreCase("forge")) {
				File forgeInstaller = downloadForgeInstaller(project, cache);
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
				logger.info("");
			} else if (project.loader.equalsIgnoreCase("fabric-loader")) {
				logger.warn(
						"Auto-resolving YARN mappings is NOT recommended, please re-build the installer with a specified mappings version.");
				project.mappings = MinecraftMappingsToolkit.getLatestYarnVersion(version);
				logger.info("");
			} else {
				throw new IOException("Cannot resolve mappings version if not using forge or fabric");
			}
		}
		if (!project.platform.equals("VANILLA")) {
			logger.info("Preparing " + project.platform + " " + project.mappings + " mappings...");
			if (!MinecraftMappingsToolkit.areMappingsAvailable(suffix, project.platform.toLowerCase(), version, side)) {
				if (project.platform.equals("MCP")) {
					MinecraftMappingsToolkit.downloadMCPMappings(version, side, project.mappings);
				} else if (project.platform.equals("YARN")) {
					MinecraftMappingsToolkit.downloadYarnMappings(version, side, project.mappings);
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
		logger.info("for the " + project.game + " " + side.toString().toLowerCase() + " "
				+ " before, the existing jar will be used.");
		logger.info("");
		logger.info("");
		logger.info("");
		logger.info("Preparing to deobfuscate... (if needed)");
		MinecraftMappingsToolkit.loadMappings(version, side);
		logger.info("");
		ProgressWindow.WindowAppender.increaseProgress();
		if (side == GameSide.CLIENT && !MinecraftInstallationToolkit.checkVersion(version)) {
			logger.info("Downloading game libraries...");
			MinecraftInstallationToolkit.downloadVersionAndLibraries(version);
			ProgressWindow.WindowAppender.increaseProgress();
			logger.info("");
		} else {
			ProgressWindow.WindowAppender.increaseProgress();
		}
		logger.info("Running deobfuscation engine...");
		File lock = new File(cache, "deobf-" + version + "-" + side + "-" + project.platform + "-" + project.name + "-"
				+ project.version + ".lck");
		if (lock.exists())
			MinecraftModdingToolkit.deobfuscateJar(version, side).delete();
		if (!lock.exists())
			lock.createNewFile();
		MinecraftModdingToolkit.deobfuscateJar(version, side);
		lock.delete();
		ProgressWindow.WindowAppender.increaseProgress();
		logger.info("");

		logger.info("Resolving modloader libraries...");
		// TODO
	}

	private File downloadForgeInstaller(ProjectConfig project, File cache) throws IOException {
		String forgeurltemplate = "https://files.minecraftforge.net/maven/net/minecraftforge/forge/%game%-%forgeversion%/forge-%game%-%forgeversion%-installer.jar";

		URL url = new URL(forgeurltemplate.replaceAll("\\%game\\%", project.game).replaceAll("\\%forgeversion\\%",
				project.loaderVersion));
		File forgeInstaller = new File(cache,
				"forge-" + project.game + "-" + project.loaderVersion + "-installer.jar");

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
