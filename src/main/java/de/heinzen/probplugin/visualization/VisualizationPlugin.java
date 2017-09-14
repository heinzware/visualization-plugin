package de.heinzen.probplugin.visualization;

import de.prob2.ui.plugin.ProBPlugin;
import ro.fortsoft.pf4j.PluginWrapper;

/**
 * Description of class
 *
 * @author Christoph Heinzen
 * @version 0.1.0
 * @since 14.09.17
 */
public class VisualizationPlugin extends ProBPlugin {

    public VisualizationPlugin(PluginWrapper pluginWrapper) {
        super(pluginWrapper);
    }

    @Override
    public String getName() {
        return "Visualization Plugin";
    }

    @Override
    public void startPlugin() {

    }

    @Override
    public void stopPlugin() {

    }
}
