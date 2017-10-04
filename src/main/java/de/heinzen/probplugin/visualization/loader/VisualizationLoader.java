package de.heinzen.probplugin.visualization.loader;

import com.google.common.io.Files;
import de.heinzen.probplugin.visualization.Visualization;
import de.heinzen.probplugin.visualization.loader.clazz.InMemoryCompiler;
import de.prob2.ui.internal.StageManager;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.PluginClassLoader;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Description of class
 *
 * @author Christoph Heinzen
 * @version 0.1.0
 * @since 23.09.17
 */
public class VisualizationLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(VisualizationLoader.class);

    private final StageManager stageManager;
    private final PluginClassLoader pluginClassLoader;
    private final String pluginClassPath;

    public VisualizationLoader(StageManager stageManager, PluginClassLoader pluginClassLoader) {
        this.stageManager = stageManager;
        this.pluginClassLoader = pluginClassLoader;

        // get classpath of the plugin from the pluginclassloader
        String classPath = "";
        if (pluginClassLoader != null) {
            URL[] classPathUrls = pluginClassLoader.getURLs();
            if (classPathUrls.length > 0) {
                StringBuilder stringBuilder = new StringBuilder(classPathUrls[0].getPath());
                for (int i = 1; i < classPathUrls.length; i++) {
                    stringBuilder.append(File.pathSeparator).append(classPathUrls[i].getPath());
                }
                classPath = stringBuilder.toString();
            }
        }
        this.pluginClassPath = classPath;
    }

    public Visualization loadVisualization(File selectedVisualization) {
        String selectedVisualizationExtension = Files.getFileExtension(selectedVisualization.getName());
        if (selectedVisualizationExtension.equals("java")) {
            LOGGER.debug("Try to open visualization-class {}.", selectedVisualization);
            return loadVisualizationClass(selectedVisualization);
        } else if (selectedVisualizationExtension.equals("jar")){
            LOGGER.debug("Try to open visualization-jar {}.", selectedVisualization);
            return loadVisualizationJar(selectedVisualization);
        }
        return null;
    }

    private Visualization loadVisualizationClass(File selectedVisualization) {
        String fileName = selectedVisualization.getName();
        try {
            LOGGER.debug("Try to compile file {} using plugin-classpath\n{}.", fileName, pluginClassPath);
            String className = fileName.replace(".java", "");

            Class visualizationClass = new InMemoryCompiler()
                    .compile(className, selectedVisualization, pluginClassPath, pluginClassLoader);

            LOGGER.debug("Successfully compiled class {}.", className);

            if (checkVisualizationClass(visualizationClass)) {
                LOGGER.debug("Class {} extends the abstract class Visualization. Create an instance of it.", className);
                return (Visualization) visualizationClass.newInstance();
            } else {
                LOGGER.warn("Class {} does not extend the abstract class Visualization.", className);
                showAlert(Alert.AlertType.WARNING,
                        "The class \"%s\" does not extend the abstract class Visualization. It is no valid visualization.",
                        className);
                return null;
            }

        } catch (Exception e) {
            LOGGER.warn("Exception while loading the visualization:\n{}", fileName, e);
            showAlert(Alert.AlertType.ERROR,
                    "Exception while loading the visualization.\n\nThe thrown exception is shown in the Log-file.");
            return null;
        }
    }

    private Visualization loadVisualizationJar(File selectedVisualization) {
        String fileName = selectedVisualization.getName();
        try {
            JarFile selectedVisualizationJar = new JarFile(selectedVisualization);
            URL[]  urls = new URL[]{ new URL("jar:file:" + selectedVisualizationJar.getName() +"!/") };
            URLClassLoader classLoader = URLClassLoader.newInstance(urls, pluginClassLoader);

            Class visualizationClass = null;
            String className = null;
            Enumeration<JarEntry> jarEntries = selectedVisualizationJar.entries();
            while (jarEntries.hasMoreElements()) {
                JarEntry jarEntry = jarEntries.nextElement();
                if (jarEntry.isDirectory() || !jarEntry.getName().endsWith(".class")) {
                    continue;
                }
                className = jarEntry.getName().substring(0, jarEntry.getName().length() - 6);
                className = className.replace('/', '.');
                visualizationClass = classLoader.loadClass(className);
                if (checkVisualizationClass(visualizationClass)) {
                    break;
                }
                visualizationClass = null;
            }
            if (visualizationClass != null) {
                LOGGER.debug("Found visualization-class {} in jar: {}", className, fileName);
                return (Visualization) visualizationClass.newInstance();

            } else {
                LOGGER.warn("No visualization-class found in jar: {}", fileName);
                showAlert(Alert.AlertType.WARNING,
                        "No visualization-class found!\n\nThe jar \"%s\" is not a valid visualization.",
                        fileName);
                return null;
            }
        } catch (Exception e) {
            LOGGER.warn("Exception while loading the visualization:\n{}", fileName, e);
            showAlert(Alert.AlertType.ERROR,
                    "Exception while loading the visualization.\n\nThe thrown exception is shown in the Log-file.");
            return null;
        }
    }

    private boolean checkVisualizationClass(Class visualizationClass) {
        return visualizationClass != null &&
                visualizationClass.getSuperclass() != null &&
                visualizationClass.getSuperclass().equals(Visualization.class);
    }

    private void showAlert(Alert.AlertType type, String text, Object... textParams) {
        Alert alert = stageManager.makeAlert(type,
                String.format(text, textParams),
                ButtonType.OK);
        alert.initOwner(stageManager.getCurrent());
        alert.show();
    }
}
