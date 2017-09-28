package de.heinzen.probplugin.visualization;

import de.heinzen.probplugin.visualization.listener.EventListener;
import de.heinzen.probplugin.visualization.listener.FormulaListener;

/**
 * Description of clazz
 *
 * @author Christoph Heinzen
 * @version 0.1.0
 * @since 14.09.17
 */
public abstract class Visualization {

    private VisualizationPlugin controller;

    protected abstract String getName();

    protected abstract void initialize();

    protected abstract void registerFormulaListener();

    protected abstract void registerEventListener();

    public final void setController(VisualizationPlugin controller) {
        this.controller = controller;
    }

    protected final void registerFormulaListener(String[] formulas, FormulaListener listener) {
        controller.registerFormulaListener(formulas, listener);
    }

    protected final void registerEventListenerListener(String event, EventListener listener) {
        controller.registerEventListener(event, listener);
    }

}
