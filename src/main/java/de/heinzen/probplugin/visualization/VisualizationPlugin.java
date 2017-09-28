package de.heinzen.probplugin.visualization;

import de.heinzen.probplugin.visualization.listener.EventListener;
import de.heinzen.probplugin.visualization.listener.FormulaListener;
import de.heinzen.probplugin.visualization.loader.VisualizationLoader;
import de.heinzen.probplugin.visualization.menu.VisualizationMenu;
import de.prob.animator.domainobjects.EventB;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.model.eventb.EventBModel;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.plugin.ProBPlugin;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.Tab;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.PluginClassLoader;
import ro.fortsoft.pf4j.PluginWrapper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Description of clazz
 *
 * @author Christoph Heinzen
 * @version 0.1.0
 * @since 14.09.17
 */
public class VisualizationPlugin extends ProBPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(VisualizationMenu.class);

    private Tab visualizationTab;
    private Menu visualizationMenu;
    private Visualization visualization;
    private VisualizationModel visualizationModel;
    private VisualizationLoader visualizationLoader;

    private final ChangeListener<Trace> currentTraceChangeListener;
    private final StageManager stageManager;
    private final CurrentTrace currentTrace;

    private HashMap<String, List<FormulaListener>> formulaListenerMap;
    private HashMap<FormulaListener, String[]> formulasMap;
    private HashMap<String, EventListener> eventListenerMap;
    private EventBModel eventBModel;
    private SimpleBooleanProperty visualizationPossible = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty visualizationRunning  = new SimpleBooleanProperty(false);

    public VisualizationPlugin(PluginWrapper pluginWrapper) {
        super(pluginWrapper);

        stageManager = getInjector().getInstance(StageManager.class);
        currentTrace = getInjector().getInstance(CurrentTrace.class);
        visualizationModel = new VisualizationModel();

        if (currentTrace.getModel() != null &&
                currentTrace.getModel() instanceof EventBModel) {
            eventBModel = (EventBModel) currentTrace.getModel();
            visualizationPossible.set(true);
        }

        currentTrace.modelProperty().addListener((observable, oldModel, newModel) -> {
            if (newModel != null && newModel instanceof EventBModel) {
                if (!newModel.equals(eventBModel)) {
                    eventBModel = (EventBModel) newModel;
                    visualizationPossible.set(true);
                    if (visualizationRunning.get()) {
                        //TODO: show that the running visualization was stopped
                        stopVisualization();
                    }
                }
            } else {
                eventBModel = null;
                visualizationPossible.setValue(false);
                if (visualizationRunning.get()) {
                    //TODO: show that the running visualization was stopped
                    stopVisualization();
                }
            }
        });

        currentTraceChangeListener = (observable, oldTrace, newTrace) -> {
            if (newTrace != null) {
                if (newTrace.getCurrentState() != null && newTrace.getCurrentState().isInitialised()) {
                    if (newTrace.getPreviousState() == null || !newTrace.getPreviousState().isInitialised()) {
                        //the model was initialized in the last event, so constants could have changed
                        visualization.initialize();
                    }
                    visualizationModel.setTraces(oldTrace, newTrace);
                    updateVisualization();
                }
            }
        };

        currentTrace.addListener(currentTraceChangeListener);
    }

    @Override
    public String getName() {
        return "Visualization Plugin (BMotion)";
    }

    @Override
    public void startPlugin() {
        visualizationTab = new Tab("BMotion", createPlaceHolderContent());
        getProBConnection().addTab(visualizationTab);
        visualizationMenu = loadMenu();
        if (visualizationMenu != null) {
            getProBConnection().addMenu(visualizationMenu);
        }
    }

    @Override
    public void stopPlugin() {
        getProBConnection().removeTab(visualizationTab);
        getProBConnection().removeMenu(visualizationMenu);
    }

    public SimpleBooleanProperty visualizationPossibleProperty() {
        return visualizationPossible;
    }

    public SimpleBooleanProperty visualizationRunningProperty() {
        return visualizationRunning;
    }

    public void openVisualization() {
        LOGGER.info("Show filechooser to select a visualization.");
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a visualization");
        fileChooser.getExtensionFilters()
                .addAll(new FileChooser.ExtensionFilter("Visualization-JAR", "*.jar"),
                        new FileChooser.ExtensionFilter("Visualization-Class", "*.java"));
        File selectedVisualization = fileChooser.showOpenDialog(stageManager.getCurrent());

        if (selectedVisualization != null) {
            LOGGER.info("Try to load visualization from file {}.", selectedVisualization.getName());
            if (visualizationLoader == null) {
                visualizationLoader = new VisualizationLoader(stageManager,(PluginClassLoader) getWrapper().getPluginClassLoader());
            }
            Visualization loadedVisualization = visualizationLoader.loadVisualization(selectedVisualization);
            if (loadedVisualization != null) {
                startVisualization(loadedVisualization);
            }
        }
    }

    public void registerFormulaListener(String[] formulas, FormulaListener listener) {
        if (formulaListenerMap == null) {
            formulaListenerMap = new HashMap<>();
        }
        if (formulasMap == null) {
            formulasMap = new HashMap<>();
        }
        for(String formula : formulas) {
            if (formulaListenerMap.containsKey(formula)) {
                formulaListenerMap.get(formula).add(listener);
            } else {
                formulaListenerMap.put(formula, new ArrayList<>(Collections.singletonList(listener)));
            }
        }
        formulasMap.put(listener, formulas);
    }

    public void registerEventListener(String event, EventListener listener) {
        if (eventListenerMap == null) {
            eventListenerMap = new HashMap<>();
        }
        eventListenerMap.put(event, listener);
    }

    private Node createPlaceHolderContent() {
        AnchorPane anchorPane = new AnchorPane();
        Label noVisualizationLabel = new Label("No visualization selected");
        AnchorPane.setTopAnchor     (noVisualizationLabel, 10.0);
        AnchorPane.setBottomAnchor  (noVisualizationLabel, 10.0);
        AnchorPane.setLeftAnchor    (noVisualizationLabel, 10.0);
        AnchorPane.setRightAnchor   (noVisualizationLabel, 10.0);
        noVisualizationLabel.setAlignment(Pos.CENTER);
        anchorPane.getChildren().add(noVisualizationLabel);
        return anchorPane;
    }

    private Menu loadMenu() {
        try {
            URL menuUrl = getResource("visualization_menu.fxml");
            if (menuUrl != null) {
                FXMLLoader loader = new FXMLLoader(menuUrl);
                VisualizationMenu menu = new VisualizationMenu(this);
                loader.setRoot(menu);
                loader.setController(menu);
                return loader.load();
            }
        } catch (IOException e) {
            LOGGER.warn("Exception while loading the menu of the visualization plugin:", e);
        }
        return null;
    }

    private void startVisualization(Visualization loadedVisualization) {
        if (visualization != null) {
            stopVisualization();
        }
        visualization = loadedVisualization;
        visualization.setController(this);
        if (currentTrace.getCurrentState() != null && currentTrace.getCurrentState().isInitialised()) {
            visualization.initialize();
            visualization.registerFormulaListener();
            visualization.registerEventListener();
            visualizationModel.setTraces(null, currentTrace.get());
            updateVisualization();
        }
        currentTrace.addListener(currentTraceChangeListener);
        visualizationRunning.set(true);
    }

    private void stopVisualization() {
        currentTrace.removeListener(currentTraceChangeListener);
        if (formulaListenerMap != null && !formulaListenerMap.isEmpty()) {
            formulaListenerMap.clear();
        }
        if (formulasMap != null && !formulasMap.isEmpty()) {
            formulasMap.clear();
        }
        if (eventListenerMap != null && !eventListenerMap.isEmpty()) {
            eventListenerMap.clear();
        }
        visualization = null;
        visualizationRunning.set(false);
        //TODO: finish
    }

    private void updateVisualization() {
        //first check which formulas have changed
        List<String> test = currentTrace.getCurrentState().getValues().keySet().stream()
                .filter(new Predicate<IEvalElement>() {
                    @Override
                    public boolean test(IEvalElement iEvalElement) {
                        return iEvalElement instanceof EventB;
                    }
                }).map(iEvalElement -> ((EventB) iEvalElement).toString())
                .filter(formula -> visualizationModel.hasChanged(formula)).collect(Collectors.toList());

        System.out.println("The following values have changed:");
        for (String value : test) {
            System.out.println("\t" + value);
        }
    }

}
