package de.heinzen.probplugin.visualization;

import de.heinzen.probplugin.visualization.listener.EventListener;
import de.heinzen.probplugin.visualization.listener.FormulaListener;
import de.prob.translator.types.BigInteger;
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

    protected abstract String[] getModels();

    protected abstract void initialize(Tab tab);

    protected abstract void stop();

    protected abstract void registerFormulaListener();

    protected abstract void registerEventListener();

    public final void setController(VisualizationPlugin controller) {
        this.controller = controller;
    }

    public final void setModel(VisualizationModel model) {
        this.model = model;
    }

    protected final void registerFormulaListener(FormulaListener listener) {
        controller.registerFormulaListener(listener);
    }

    protected final void registerEventListener(EventListener listener) {
        controller.registerEventListener(listener);
    }

    protected final Integer translateToInt(Object intObj) {
        if (intObj instanceof BigInteger) {
            return ((BigInteger) intObj).intValue();
        }
        return null;
    }

    protected final Boolean translateToBool(Object boolObj) {
        if (boolObj instanceof de.prob.translator.types.Boolean) {
            return ((de.prob.translator.types.Boolean) boolObj).booleanValue();
        }
        return null;
    }



}
