package de.heinzen.probplugin.visualization;

import de.heinzen.probplugin.visualization.listener.EventListener;
import de.heinzen.probplugin.visualization.listener.FormulaListener;
import de.heinzen.probplugin.visualization.loader.VisualizationLoader;
import de.heinzen.probplugin.visualization.menu.VisualizationMenu;
import de.prob.model.eventb.EventBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.plugin.ProBPlugin;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.PluginClassLoader;
import ro.fortsoft.pf4j.PluginWrapper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
    private final ChangeListener<AbstractModel> modelChangeListener;
    private final StageManager stageManager;
    private final CurrentTrace currentTrace;

    private HashMap<String, List<FormulaListener>> formulaListenerMap;
    private HashMap<FormulaListener, String[]> formulasMap;
    private HashMap<String, EventListener> eventListenerMap;
    private AbstractModel model;
    private SimpleBooleanProperty visualizationPossible = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty visualizationRunning  = new SimpleBooleanProperty(false);

    public VisualizationPlugin(PluginWrapper pluginWrapper) {
        super(pluginWrapper);

        stageManager = getInjector().getInstance(StageManager.class);
        currentTrace = getInjector().getInstance(CurrentTrace.class);
        visualizationModel = new VisualizationModel(currentTrace, stageManager);

        if (currentTrace.getModel() != null) {
            model = currentTrace.getModel();
            visualizationPossible.set(true);
        }

        modelChangeListener = (observable, oldModel, newModel) -> {
            if (newModel != null) {
                if (!newModel.equals(model)) {
                    model = newModel;
                    visualizationPossible.set(true);
                    if (visualizationRunning.get()) {
                        //TODO: better alert message
                        Alert alert = stageManager.makeAlert(Alert.AlertType.INFORMATION,
                                "Stopping the visualization because the used model changed.",
                                ButtonType.OK);
                        alert.initOwner(stageManager.getCurrent());
                        alert.show();
                        stopVisualization();
                    }
                }
            } else {
                model = null;
                visualizationPossible.setValue(false);
                if (visualizationRunning.get()) {
                    Alert alert = stageManager.makeAlert(Alert.AlertType.INFORMATION,
                            "Stopping the visualization because the used model changed.",
                            ButtonType.OK);
                    alert.initOwner(stageManager.getCurrent());
                    alert.show();
                    stopVisualization();
                }
            }
        };

        currentTraceChangeListener = (observable, oldTrace, newTrace) -> {
            if (newTrace != null) {
                if (newTrace.getCurrentState() != null && newTrace.getCurrentState().isInitialised()) {
                    visualizationModel.setTraces(oldTrace, newTrace);
                    if (newTrace.getPreviousState() == null || !newTrace.getPreviousState().isInitialised()) {
                        //the model was initialized in the last event, so constants could have changed
                        visualization.initialize(visualizationTab);
                    }
                    updateVisualization();
                }
            }
        };

        currentTrace.modelProperty().addListener(modelChangeListener);
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
        currentTrace.modelProperty().removeListener(modelChangeListener);
        stopVisualization();
    }

    public SimpleBooleanProperty visualizationPossibleProperty() {
        return visualizationPossible;
    }

    public SimpleBooleanProperty visualizationRunningProperty() {
        return visualizationRunning;
    }

    public void openVisualization() {
        LOGGER.debug("Show filechooser to select a visualization.");
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a visualization");
        fileChooser.getExtensionFilters()
                .addAll(new FileChooser.ExtensionFilter("Visualization-JAR", "*.jar"),
                        new FileChooser.ExtensionFilter("Visualization-Class", "*.java"));
        File selectedVisualization = fileChooser.showOpenDialog(stageManager.getCurrent());

        if (selectedVisualization != null) {
            LOGGER.debug("Try to load visualization from file {}.", selectedVisualization.getName());
            if (visualizationLoader == null) {
                visualizationLoader = new VisualizationLoader(stageManager,(PluginClassLoader) getWrapper().getPluginClassLoader());
            }
            Visualization loadedVisualization = visualizationLoader.loadVisualization(selectedVisualization);
            if (loadedVisualization != null) {
                startVisualization(loadedVisualization);
            }
        }
    }

    public void registerFormulaListener(FormulaListener listener) {
        String[] formulas = listener.getFormulas();
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

    public void registerEventListener(EventListener listener) {
        if (eventListenerMap == null) {
            eventListenerMap = new HashMap<>();
        }
        eventListenerMap.put(listener.getEvent(), listener);
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
        LOGGER.debug("Starting the visualization \"{}\"", loadedVisualization.getName());
        //TODO check if the new visualization is for the used model
        /*if (loadedVisualization.getModels() != null &&
                Arrays.asList(loadedVisualization.getModels()).contains(model)) {*/
            System.out.println(model + " from file " + model.getModelFile().getName());
        //}
        if (visualization != null) {
            stopVisualization();
        }
        visualization = loadedVisualization;
        visualization.setController(this);
        visualization.setModel(visualizationModel);
        visualizationTab.setText(visualization.getName());
        visualization.registerFormulaListener();
        visualization.registerEventListener();
        if (currentTrace.getCurrentState() != null && currentTrace.getCurrentState().isInitialised()) {
            LOGGER.debug("Start: The current state is initialised, call initialize() of visualization.");
            visualizationModel.setTraces(null, currentTrace.get());
            visualization.initialize(visualizationTab);
            updateVisualization();
        }
        currentTrace.addListener(currentTraceChangeListener);
        visualizationRunning.set(true);
    }

    public void stopVisualization() {
        if (visualizationRunning.get()) {
            LOGGER.debug("Stopping visualization \"{}\"!", visualization.getName());
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
            visualization.stop();
            visualization = null;
            visualizationRunning.set(false);
            visualizationTab.setContent(createPlaceHolderContent());
            visualizationTab.setText("BMotion");
        }
    }

    private void updateVisualization() {
        //first check which formulas have changed
        LOGGER.debug("Update visualization!");
        if (formulaListenerMap != null) {
            List<String> changedFormulas = formulaListenerMap.keySet().stream()
                    .filter(new Predicate<String>() {
                        @Override
                        public boolean test(String formula) {
                            return visualizationModel.hasChanged(formula);
                        }
                    }).collect(Collectors.toList());

            LOGGER.debug("The following formulas have changed their values: {}", String.join(" ", changedFormulas));

            Set<FormulaListener> listenersToTrigger = new HashSet<>();
            for (String formula : changedFormulas) {
                listenersToTrigger.addAll(formulaListenerMap.get(formula));
            }

            Map<String, Object> formulaValueMap = new HashMap<>(changedFormulas.size());
            for (FormulaListener listener : listenersToTrigger) {
                String[] formulas = formulasMap.get(listener);
                Object[] formulaValues = new Object[formulas.length];
                for (int i = 0; i < formulas.length; i++) {
                    if (formulaValueMap.containsKey(formulas[i])) {
                        formulaValues[i] = formulaValueMap.get(formulas[i]);
                    } else {
                        Object formulaValue = visualizationModel.getValue(formulas[i]);
                        formulaValues[i] = formulaValue;
                        formulaValueMap.put(formulas[i], formulaValue);
                    }
                }
                LOGGER.debug("Fire listener for formulas: {}", String.join(" ", formulas));
                listener.variablesChanged(formulaValues);
            }
        }

        if (eventListenerMap != null) {
            String lastEvent = currentTrace.get().getCurrentTransition().getName();
            if (eventListenerMap.containsKey(lastEvent)) {
                LOGGER.info("Last executed event is \"{}\". Call corresponding listener.");
                eventListenerMap.get(lastEvent).eventExcecuted();
            }
        }
    }
}
