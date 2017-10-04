package de.heinzen.probplugin.visualization;

import de.heinzen.probplugin.visualization.listener.EventListener;
import de.heinzen.probplugin.visualization.listener.FormulaListener;
import javafx.scene.control.Tab;

/**
 * Description of clazz
 *
 * @author Christoph Heinzen
 * @version 0.1.0
 * @since 14.09.17
 */
public abstract class Visualization {

    private VisualizationPlugin controller;
    protected VisualizationModel model;

    protected abstract String getName();

    protected abstract void initialize(Tab tab);

    protected abstract void registerFormulaListener();

    protected abstract void registerEventListener();

    public final void setController(VisualizationPlugin controller) {
        this.controller = controller;
    }

    public final void setModel(VisualizationModel model) {
        this.model = model;
    }

    protected final void registerFormulaListener(String[] formulas, FormulaListener listener) {
        controller.registerFormulaListener(formulas, listener);
    }

    protected final void registerEventListener(String event, EventListener listener) {
        controller.registerEventListener(event, listener);
    }

}
