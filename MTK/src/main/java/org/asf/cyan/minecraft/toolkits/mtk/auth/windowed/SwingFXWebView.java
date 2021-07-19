package org.asf.cyan.minecraft.toolkits.mtk.auth.windowed;

import com.sun.javafx.application.PlatformImpl;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.function.Consumer;

import javafx.beans.property.ReadOnlyProperty;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Callback;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.w3c.dom.Document;

/**
 * SwingFXWebView
 * 
 * @author anjackson
 */
public class SwingFXWebView extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Stage stage;
	private WebView browser;
	private JFXPanel jfxPanel;
	private WebEngine webEngine;

	public WebEngine getWebEngine() {
		return webEngine;
	}

	private ArrayList<Consumer<String>> listeners = new ArrayList<Consumer<String>>();

	private ArrayList<Consumer<Document>> loadListeners = new ArrayList<Consumer<Document>>();

	public void setPage(String page) {
		PlatformImpl.startup(() -> {
			webEngine.load(page);
		});
	}

	@SuppressWarnings("unchecked")
	public void addUrlListener(Consumer<String> listener) {
		if (webEngine == null) {
			listeners.add(listener);
			return;
		}
		webEngine.locationProperty().addListener(url -> {
			ReadOnlyProperty<String> prop = (ReadOnlyProperty<String>) url;
			listener.accept(prop.getValue());
		});
	}

	public void addPostLoadListener(Consumer<Document> listener) {
		if (webEngine == null) {
			loadListeners.add(listener);
			return;
		}
		webEngine.getLoadWorker().stateProperty().addListener(t -> {
			if (webEngine.getDocument() != null)
				listener.accept(webEngine.getDocument());
		});
	}

	public String startingPage = "https://google.com";

	public SwingFXWebView() {
		SwingUtilities.invokeLater(() -> {
			initComponents();
		});
	}

	private void initComponents() {
		jfxPanel = new JFXPanel();
		createScene();

		setLayout(new BorderLayout());
		add(jfxPanel, BorderLayout.CENTER);
	}

	/**
	 * createScene
	 * 
	 * Note: Key is that Scene needs to be created and run on "FX user thread" NOT
	 * on the AWT-EventQueue Thread
	 * 
	 */
	private void createScene() {
		PlatformImpl.startup(new Runnable() {
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				stage = new Stage();
				stage.setResizable(true);

				Group root = new Group();
				Scene scene = new Scene(root, getWidth(), getHeight());
				stage.setScene(scene);

				// Set up the embedded browser:
				browser = new WebView();
				browser.setContextMenuEnabled(false);
				browser.setPrefSize(getWidth(), getHeight());

				webEngine = browser.getEngine();
				webEngine.setCreatePopupHandler(new Callback<PopupFeatures, WebEngine>() {

					@Override
					public WebEngine call(PopupFeatures param) {
						return null;
					}

				});
				webEngine.onVisibilityChangedProperty().setValue(null);

				listeners.forEach(listener -> {
					webEngine.locationProperty().addListener(url -> {
						ReadOnlyProperty<String> prop = (ReadOnlyProperty<String>) url;
						listener.accept(prop.getValue());
					});
				});
				listeners.clear();

				loadListeners.forEach(listener -> {
					webEngine.getLoadWorker().stateProperty().addListener(t -> {
						if (webEngine.getDocument() != null)
							listener.accept(webEngine.getDocument());
					});
				});
				loadListeners.clear();

				ObservableList<Node> children = root.getChildren();
				children.add(browser);

				jfxPanel.setScene(scene);
				webEngine.setJavaScriptEnabled(true);
				webEngine.load(startingPage);
			}
		});
	}

	public void loadContent(String html) {
		PlatformImpl.startup(() -> {
			webEngine.loadContent(html);
		});
	}
}