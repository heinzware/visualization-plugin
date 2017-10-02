package de.heinzen.probplugin.visualization.menu;

import de.heinzen.probplugin.visualization.VisualizationPlugin;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Description of clazz
 *
 * @author Christoph Heinzen
 * @version 0.1.0
 * @since 21.09.17
 */
public class VisualizationMenu extends Menu{

    private static final Logger LOGGER = LoggerFactory.getLogger(VisualizationMenu.class);

    @FXML
    private MenuItem openVisualizationItem;

    @FXML
    private MenuItem stopVisualizationItem;

    private final VisualizationPlugin plugin;

    public VisualizationMenu(VisualizationPlugin plugin) {
        this.plugin = plugin;
    }

    @FXML
    public void initialize() {
        openVisualizationItem.disableProperty().bind(Bindings.not(plugin.visualizationPossibleProperty()));
        stopVisualizationItem.disableProperty().bind(Bindings.not(plugin.visualizationRunningProperty()));
    }

    @FXML
    private void stopVisualization(){
        plugin.stopVisualization();
    }

    @FXML
    private void openVisualization(){
        plugin.openVisualization();
    }

}
