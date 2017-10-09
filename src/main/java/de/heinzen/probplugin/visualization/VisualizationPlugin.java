package de.heinzen.probplugin.visualization;

import de.heinzen.probplugin.visualization.listener.EventListener;
import de.heinzen.probplugin.visualization.listener.FormulaListener;
import de.heinzen.probplugin.visualization.loader.VisualizationLoader;
import de.heinzen.probplugin.visualization.menu.VisualizationMenu;
import de.prob.model.representation.AbstractModel;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.plugin.ProBPlugin;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import java.util.function.Consumer;
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
    private VisualizationModel visualizationModel;
    private VisualizationLoader visualizationLoader;

    private final ChangeListener<Trace> currentTraceChangeListener;
    private final ChangeListener<AbstractModel> modelChangeListener;
    private final StageManager stageManager;
    private final CurrentTrace currentTrace;

    private HashMap<String, List<FormulaListener>> formulaListenerMap;
    private HashMap<String, EventListener> eventListenerMap;
    private AbstractModel model;
    private SimpleObjectProperty<Visualization> visualization = new SimpleObjectProperty<>(null);
    private SimpleBooleanProperty visualizationPossible = new SimpleBooleanProperty(false);

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
                    if (visualization.isNotNull().get()) {
                        boolean start = checkModel(newModel.getModelFile().getName(), visualization.get().getModels());
                        if (start) {
                            visualization.get().initialize(visualizationTab);
                        } else {
                            showAlert(Alert.AlertType.INFORMATION,
                                    "A new model was loaded and " +
                                            "this model does not work with the loaded visualization. So the visualization will be stopped.",
                                    ButtonType.OK);
                            stopVisualization();
                        }
                    }
                }
            } else {
                model = null;
                visualizationPossible.setValue(false);
                if (visualization.isNotNull().get()) {
                    showAlert(Alert.AlertType.INFORMATION,
                            "The animation was stopped, so the visualization is also stopped.",
                            ButtonType.OK);
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
                        visualization.get().initialize(visualizationTab);
                    }
                    updateVisualization();
                }
            }
        };

        currentTrace.modelProperty().addListener(modelChangeListener);
    }

    @Override
    public String getName() {
        return "VisualizationFX";
    }

    @Override
    public void startPlugin() {
        visualizationTab = new Tab("VisualizationFX", createPlaceHolderContent());
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

    public SimpleObjectProperty<Visualization> visualizationProperty() {
        return visualization;
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
            } else {
                visualizationLoader.closeClassloader();
            }
        }
    }

    public void registerFormulaListener(FormulaListener listener) {
        String[] formulas = listener.getFormulas();
        if (formulaListenerMap == null) {
            formulaListenerMap = new HashMap<>();
        }
        for(String formula : formulas) {
            if (formulaListenerMap.containsKey(formula)) {
                formulaListenerMap.get(formula).add(listener);
            } else {
                formulaListenerMap.put(formula, new ArrayList<>(Collections.singletonList(listener)));
            }
        }
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
        boolean start = checkModel(model.getModelFile().getName(), loadedVisualization.getModels());
        if (!start) {
            showAlert(Alert.AlertType.INFORMATION, "The visualization \"" + loadedVisualization.getName() +
                    "\" does not work with the the model.\n\n" +
                    "The visualization won't be loaded.",
                    ButtonType.OK);
            return;
        }
        if (visualization.isNotNull().get()) {
            Alert alert = stageManager.makeAlert(Alert.AlertType.CONFIRMATION,
                    "The visualization \"" + visualization.get().getName() +
                            "\" is already loaded.\n\nDo you want to replace it with the visualization \"" +
                            loadedVisualization.getName() + "\"?",
                    ButtonType.YES, ButtonType.NO);
            alert.setTitle("VisualizationFX");
            alert.initOwner(stageManager.getCurrent());
            Optional<ButtonType> alertResult = alert.showAndWait();
            if (alertResult.isPresent() && alertResult.get() == ButtonType.YES) {
                stopVisualization();
            } else {
                return;
            }
        }
        visualization.set(loadedVisualization);
        loadedVisualization.setController(this);
        loadedVisualization.setModel(visualizationModel);
        visualizationTab.setText(loadedVisualization.getName());
        loadedVisualization.registerFormulaListener();
        loadedVisualization.registerEventListener();
        if (currentTrace.getCurrentState() != null && currentTrace.getCurrentState().isInitialised()) {
            LOGGER.debug("Start: The current state is initialised, call initialize() of visualization.");
            visualizationModel.setTraces(null, currentTrace.get());
            loadedVisualization.initialize(visualizationTab);
            updateVisualization();
        }
        currentTrace.addListener(currentTraceChangeListener);
    }

    public void stopVisualization() {
        if (visualization.isNotNull().get()) {
            LOGGER.debug("Stopping visualization \"{}\"!", visualization.get().getName());
            currentTrace.removeListener(currentTraceChangeListener);
            if (formulaListenerMap != null && !formulaListenerMap.isEmpty()) {
                formulaListenerMap.clear();
            }
            if (eventListenerMap != null && !eventListenerMap.isEmpty()) {
                eventListenerMap.clear();
            }
            visualization.get().stop();
            visualizationLoader.closeClassloader();
            visualization.set(null);
            visualizationTab.setContent(createPlaceHolderContent());
            visualizationTab.setText("VisualizationFX");
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
                String[] formulas = listener.getFormulas();
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
                try {
                    listener.variablesChanged(formulaValues);
                } catch (Exception e) {
                    Alert alert = stageManager.makeExceptionAlert(Alert.AlertType.WARNING,
                            "Exception while calling the formula listener for the formulas:\n\"" +
                                    String.join(" ", formulas) + "\"\n", e);
                    alert.initOwner(stageManager.getCurrent());
                    alert.show();
                    LOGGER.warn("Exception while calling the formula listener for the formulas:\n\"" +
                            String.join(" ", formulas), e);
                }
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

    private boolean checkModel(String modelFile, String[] models) {
        LOGGER.debug("Checking the model. Model-file is \"{}\" and models are \"{}\"", modelFile, models);
        boolean start = true;
        if (models != null && models.length != 0) {
            start = Arrays.asList(models).contains(modelFile);
        }
        return start;
    }

    private void showAlert(Alert.AlertType type, String content, ButtonType... buttons) {
        Alert alert = stageManager.makeAlert(type, content, buttons);
        alert.setTitle("VisualizationFX");
        alert.initOwner(stageManager.getCurrent());
        alert.show();
    }
}
