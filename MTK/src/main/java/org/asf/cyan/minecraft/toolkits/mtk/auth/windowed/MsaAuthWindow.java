package org.asf.cyan.minecraft.toolkits.mtk.auth.windowed;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JDialog;
import org.asf.cyan.api.classloading.DynamicClassLoader;

import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit.OsInfo;
import org.asf.cyan.minecraft.toolkits.mtk.auth.MsaAuthentication;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class MsaAuthWindow {

	public JDialog frame;

	public static class UserTokens {
		public String refreshToken;
		public String accessToken;
	}

	public MsaAuthWindow() {
		this(null, null);
	}

	private MsaAuthentication auth;
	private BiConsumer<UserTokens, BiConsumer<String, String>> callback;

	public MsaAuthWindow(MsaAuthentication auth, BiConsumer<UserTokens, BiConsumer<String, String>> callback) {
		this.callback = callback;
		this.auth = auth;
		try {
			EventQueue.invokeAndWait(new Runnable() {
				public void run() {
					initialize();
					frame.setModal(true);
					frame.setVisible(true);
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	private String[] libs = new String[] { "javafx-swing", "javafx-web", "javafx-controls", "javafx-base",
			"javafx-graphics", "javafx-media" };

	private String baseURL = "https://repo1.maven.org/maven2/org/openjfx/%proj%/%ver%/%proj%-%ver%-%platform%.jar";
	private String jfxVersion = "17-ea+15";

	/**
	 * @wbp.parser.entryPoint
	 */
	private void initialize() {
		frame = new JDialog();
		frame.setBounds(100, 100, 800, 700);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setTitle("MTK Microsoft Account Login");

		JLabel lblMinecraft = new JLabel("Minecraft Installation Toolkit - Microsoft Login");
		lblMinecraft.setPreferredSize(new Dimension(326, 60));
		lblMinecraft.setFont(new Font("Dialog", Font.BOLD, 20));
		lblMinecraft.setHorizontalAlignment(SwingConstants.CENTER);
		frame.getContentPane().add(lblMinecraft, BorderLayout.NORTH);

		JPanel panel = new JPanel();
		panel.setVisible(false);
		frame.getContentPane().add(panel, BorderLayout.SOUTH);
		panel.setLayout(new BorderLayout(0, 0));

		JLabel lblNewLabel = new JLabel(
				"Please wait, downloading OpenJFX... This is needed for the Microsoft login page...");
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		panel.add(lblNewLabel, BorderLayout.NORTH);

		JProgressBar progressBar = new JProgressBar();
		panel.add(progressBar, BorderLayout.CENTER);

		if (auth != null) {
			File jfxDir = new File(MinecraftInstallationToolkit.getMinecraftDirectory(),
					"caches/mtk-libs-auth/openjfx-" + OsInfo.getCurrent().toString());
			HashMap<String, File> missingLibs = new HashMap<String, File>();
			for (String lib : libs) {
				File output = new File(jfxDir, lib + ".jar");
				File outputL = new File(jfxDir, lib + ".jar.lck");
				if (outputL.exists() && output.exists()) {
					output.delete();
					outputL.delete();
				}
				if (!output.exists())
					missingLibs.put(baseURL.replace("%proj%", lib).replace("%ver%", jfxVersion).replace("%platform%",
							OsInfo.getCurrent() == OsInfo.osx ? "mac"
									: OsInfo.getCurrent() == OsInfo.windows ? "win" : OsInfo.getCurrent().toString()),
							output);
			}

			frame.addWindowListener(new WindowAdapter() {

				public void windowOpened(WindowEvent event) {
					File tmpDir = new File(System.getProperty("java.io.tmpdir"), "javafx-natives-mtk");
					if (!tmpDir.exists())
						tmpDir.mkdirs();

					if (missingLibs.size() == 0) {
						ArrayList<URL> urls = new ArrayList<URL>();
						for (String lib : libs) {
							File output = new File(jfxDir, lib + ".jar");
							try {
								extractNatives(output, tmpDir);
							} catch (IOException e1) {
							}
							try {
								urls.add(output.toURI().toURL());
							} catch (MalformedURLException e) {
							}
						}
						urls.add(getClass().getProtectionDomain().getCodeSource().getLocation());
						@SuppressWarnings("resource")
						DynamicClassLoader loader = new DynamicClassLoader();
						loader.addLoadRestriction(str -> {
							return str.contains("org.asf") && !str.contains("SwingFXWebView")
									&& !str.contains("AuthAdapter");
						});
						loader.setOptions(DynamicClassLoader.OPTION_ALLOW_DEFINE);
						loader.addUrls(urls);
						System.setProperty("java.library.path", System.getProperty("java.library.path")
								+ File.pathSeparator + tmpDir.getAbsolutePath());
						try {
							Class<?> cls = loader
									.loadClass("org.asf.cyan.minecraft.toolkits.mtk.auth.windowed.AuthAdapter");
							Object inst = cls.getConstructor().newInstance();
							cls.getMethod("load", MsaAuthentication.class, JDialog.class, MsaAuthWindow.class,
									BiConsumer.class).invoke(inst, auth, frame, MsaAuthWindow.this, callback);
						} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
								| InvocationTargetException | NoSuchMethodException | SecurityException
								| ClassNotFoundException e) {
							throw new RuntimeException(e);
						}
					} else {
						panel.setVisible(true);
						progressBar.setMaximum(missingLibs.size());
						new Thread(() -> {

							int i = 0;
							for (String url : missingLibs.keySet()) {
								try {
									URL u = new URL(url);
									File output = missingLibs.get(url);
									File outputL = new File(output.getPath() + ".lck");
									InputStream strm = u.openStream();
									if (!output.getParentFile().exists())
										output.getParentFile().mkdirs();
									outputL.createNewFile();
									FileOutputStream strm2 = new FileOutputStream(output);
									strm.transferTo(strm2);
									strm.close();
									strm2.close();
									outputL.delete();
									i++;
									int iF = i;
									SwingUtilities.invokeLater(() -> {
										progressBar.setValue(iF);
									});
								} catch (IOException e) {
									int iF = i;
									SwingUtilities.invokeLater(() -> {
										JOptionPane.showMessageDialog(frame,
												"Library download failed, cannot continue.\nThe following library failed to download: "
														+ libs[iF] + "\n\nThe MSA login window will now close...",
												"Download Failed", JOptionPane.ERROR_MESSAGE);
										frame.dispose();
									});
									return;
								}
							}
							SwingUtilities.invokeLater(() -> {
								panel.setVisible(false);
								ArrayList<URL> urls = new ArrayList<URL>();
								for (String lib : libs) {
									File output = new File(jfxDir, lib + ".jar");
									try {
										extractNatives(output, tmpDir);
									} catch (IOException e1) {
									}
									try {
										urls.add(output.toURI().toURL());
									} catch (MalformedURLException e) {
									}
								}
								urls.add(getClass().getProtectionDomain().getCodeSource().getLocation());
								@SuppressWarnings("resource")
								DynamicClassLoader loader = new DynamicClassLoader();
								loader.addLoadRestriction(str -> {
									return str.contains("org.asf") && !str.contains("SwingFXWebView")
											&& !str.contains("AuthAdapter");
								});
								loader.setOptions(DynamicClassLoader.OPTION_ALLOW_DEFINE);
								loader.addUrls(urls);
								System.setProperty("java.library.path", System.getProperty("java.library.path")
										+ File.pathSeparator + tmpDir.getAbsolutePath());
								try {
									Class<?> cls = loader
											.loadClass("org.asf.cyan.minecraft.toolkits.mtk.auth.windowed.AuthAdapter");
									Object inst = cls.getConstructor().newInstance();
									cls.getMethod("load", MsaAuthentication.class, JDialog.class, MsaAuthWindow.class,
											BiConsumer.class).invoke(inst, auth, frame, MsaAuthWindow.this, callback);
								} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
										| InvocationTargetException | NoSuchMethodException | SecurityException
										| ClassNotFoundException e) {
									throw new RuntimeException(e);
								}
							});
						}, "OpenJFX downloader").start();
					}
				}

				private void extractNatives(File jar, File tmpOut) throws IOException {
					ZipFile file = new ZipFile(jar);
					Enumeration<? extends ZipEntry> entries = file.entries();
					while (entries.hasMoreElements()) {
						ZipEntry entry = entries.nextElement();
						if (entry.isDirectory())
							continue;

						String path = entry.getName().replaceAll("\\\\", "/");
						if (path.startsWith("META-INF/") || path.endsWith(".git") || path.endsWith(".sha1")
								|| path.endsWith(".md5"))
							continue;

						File output = new File(tmpOut, path);
						if (output.exists())
							output.delete();

						FileOutputStream outstrm = new FileOutputStream(output);
						InputStream strm = file.getInputStream(entry);
						strm.transferTo(outstrm);
						strm.close();
						outstrm.close();
					}
					file.close();
				}

			});
		}
	}

}
