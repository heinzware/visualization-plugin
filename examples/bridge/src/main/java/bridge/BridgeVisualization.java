package bridge;

import bridge.ui.Car;
import bridge.ui.TrafficLight;
import de.heinzen.probplugin.visualization.Visualization;
import de.heinzen.probplugin.visualization.listener.FormulaListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * @author Christoph Heinzen
 * @version 0.1.0
 * @since 02.10.17
 */
public class BridgeVisualization extends Visualization{

    private static final int INITIAL_WIDTH = 765;
    private static final int INITIAL_HEIGHT = 555;
    private static final double STROKE_WIDTH = 1.5;

    private Group root;

    private Car[] toIslandCars = new Car[3];
    private Car[] fromIslandCars = new Car[3];
    private Car[] islandCars = new Car[3];
    private Car[] mainlandCars = new Car[3];
    private TrafficLight mainlandTrafficLight;
    private TrafficLight islandTrafficLight;
    private Text textN;
    private Text textA;
    private Text textB;
    private Text textC;

    @Override
    protected String getName() {
        return "Bridge Visualization";
    }

    @Override
    protected String[] getMachines() {
        return new String[0];
    }

    @Override
    protected Node initialize() {

        AnchorPane pane = new AnchorPane();
        pane.setMinWidth(100);
        pane.setBackground(new Background(new BackgroundFill(Color.BLACK, null ,null)));

        root = new Group();

        pane.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
               scaleVisualization(newValue.doubleValue());
            }
        });

        Rectangle sea = new Rectangle(0, 0, 765, 555);
        sea.setFill(Color.DARKBLUE);

        Rectangle bridge = new Rectangle(250, 197.5, 350,60);
        bridge.setFill(Color.WHITE);
        bridge.setStroke(Color.BLACK);
        bridge.setStrokeWidth(STROKE_WIDTH);

        SVGPath mainland = new SVGPath();
        mainland.setContent("M 765,0 L 700,0 A 250 319 0 0 0 700,555 L 765,555");
        mainland.setStroke(Color.BLACK);
        mainland.setStrokeWidth(STROKE_WIDTH);
        mainland.setFill(Color.WHITE);

        Ellipse island = new Ellipse(130, 227.5, 130, 146);
        island.setFill(Color.WHITE);
        island.setStroke(Color.BLACK);
        island.setStrokeWidth(STROKE_WIDTH);

        mainlandTrafficLight = new TrafficLight(540, 107.5);
        mainlandTrafficLight.setOpacity(0);
        islandTrafficLight = new TrafficLight(260, 267.5);
        islandTrafficLight.setOpacity(0);

        textN = new Text(20,40, "n = 0");
        textN.setFont(Font.font("Helvetica", FontWeight.BOLD, 25));
        textN.setFill(Color.WHITE);
        textA = new Text(260,187.5, "a = 0");
        textA.setFont(Font.font("Helvetica", FontWeight.BOLD, 25));
        textA.setFill(Color.WHITE);
        textA.setOpacity(0);
        textB = new Text(95,135, "b = 0");
        textB.setFont(Font.font("Helvetica", FontWeight.BOLD, 25));
        textB.setOpacity(0);
        textC = new Text(495,285.5, "c = 0");
        textC.setFont(Font.font("Helvetica", FontWeight.BOLD, 25));
        textC.setFill(Color.WHITE);
        textC.setOpacity(0);

        root.getChildren().addAll(sea, bridge, island, mainland, mainlandTrafficLight, islandTrafficLight, textA, textB, textC, textN);

        for (int i = 0; i < 3; i++) {
            toIslandCars[i] = new Car(262 + (i * 85), 207.5);
            toIslandCars[i].setOpacity(0);
            fromIslandCars[i] = new Car(490 - (i * 85), 207.5);
            fromIslandCars[i].setScaleX(-1 * fromIslandCars[i].getScaleX());
            fromIslandCars[i].setOpacity(0);
            islandCars[i] = new Car(50, 162.5 + (i * 45));
            islandCars[i].setOpacity(0);
            mainlandCars[i] = new Car(650, 162.5 + (i * 45));
        }

        root.getChildren().addAll(toIslandCars);
        root.getChildren().addAll(fromIslandCars);
        root.getChildren().addAll(islandCars);
        root.getChildren().addAll(mainlandCars);

        AnchorPane.setTopAnchor(root, 0.0);
        AnchorPane.setLeftAnchor(root, 0.0);
        AnchorPane.setRightAnchor(root, 0.0);
        pane.getChildren().add(root);

        return pane;
    }

    @Override
    protected void stop() {}

    @Override
    protected void registerFormulaListener() {
        registerFormulaListener(new FormulaListener("n", "b") {
            @Override
            public void variablesChanged(Object[] newValues) throws Exception {
                for (int i = 0; i < 3; i++) {
                    islandCars[i].setOpacity(0);
                    mainlandCars[i].setOpacity(0);
                }
                Integer n = translateToInt(newValues[0]);
                textN.setText("n = " + n);
                for (int i = 0; i < (3 - n); i++) {
                    mainlandCars[i].setOpacity(1);
                }
                Integer b = translateToInt(newValues[1]);
                if (b != null) {
                    textB.setOpacity(1);
                    textB.setText("b = " + b);
                    for (int i = 0; i < b; i++) {
                        islandCars[i].setOpacity(1);
                    }
                } else {
                    for (int i = 0; i < n; i++) {
                        islandCars[i].setOpacity(1);
                    }
                }
            }
        });

        registerFormulaListener(new FormulaListener("a") {
            @Override
            public void variablesChanged(Object[] newValues) throws Exception {
                for (Car toIslandCar : toIslandCars) {
                    toIslandCar.setOpacity(0);
                }
                Integer a = translateToInt(newValues[0]);
                if (a != null) {
                    textA.setOpacity(1);
                    textA.setText("a = " + a);
                    for (int i = 0; i < a; i++) {
                        toIslandCars[i].setOpacity(1);
                    }
                }
            }
        });

        registerFormulaListener(new FormulaListener("c") {
            @Override
            public void variablesChanged(Object[] newValues) throws Exception {
                for (Car fromIslandCar : fromIslandCars) {
                    fromIslandCar.setOpacity(0);
                }
                Integer c = translateToInt(newValues[0]);
                if (c != null) {
                    textC.setOpacity(1);
                    textC.setText("c = " + c);
                    for (int i = 0; i < c; i++) {
                        fromIslandCars[i].setOpacity(1);
                    }
                }
            }
        });

        registerFormulaListener(new FormulaListener("ml_tl") {
            @Override
            public void variablesChanged(Object[] newValues) throws Exception {
                if (newValues[0] != null) {
                    mainlandTrafficLight.setOpacity(1);
                    mainlandTrafficLight.setGreen(newValues[0].toString().equals("green"));
                }
            }
        });

        registerFormulaListener(new FormulaListener("il_tl") {
            @Override
            public void variablesChanged(Object[] newValues) throws Exception {
                if (newValues[0] != null) {
                    islandTrafficLight.setOpacity(1);
                    islandTrafficLight.setGreen(newValues[0].toString().equals("green"));
                }
            }
        });

    }

    @Override
    protected void registerEventListener() {}

    private void scaleVisualization(double width) {
        double scale = width / INITIAL_WIDTH;
        root.setScaleX(scale);
        root.setScaleY(scale);

        double translateX = ((INITIAL_WIDTH * scale) - INITIAL_WIDTH) / 2.0;
        double translateY = ((INITIAL_HEIGHT * scale) - INITIAL_HEIGHT) / 2.0;
        root.setTranslateX(translateX);
        root.setTranslateY(translateY);
    }
}
