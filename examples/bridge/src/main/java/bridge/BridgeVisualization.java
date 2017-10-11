package bridge;

import bridge.ui.Car;
import bridge.ui.TrafficLight;
import de.heinzen.probplugin.visualization.Visualization;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;

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
    private TrafficLight mainlandTrafficLight;
    private TrafficLight islandTrafficLight;

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

        Rectangle bridge = new Rectangle(250, 177.5, 350,100);
        bridge.setFill(Color.WHITE);
        bridge.setStroke(Color.BLACK);
        bridge.setStrokeWidth(STROKE_WIDTH);

        Line line = new Line(250, 227.5, 700, 227.5);
        line.setStroke(Color.BLACK);
        line.getStrokeDashArray().addAll(10.0,10.0);
        line.setStrokeWidth(2);

        SVGPath mainland = new SVGPath();
        mainland.setContent("M 765,0 L 700,0 A 250 319 0 0 0 700,555 L 765,555");
        mainland.setStroke(Color.BLACK);
        mainland.setStrokeWidth(STROKE_WIDTH);
        mainland.setFill(Color.WHITE);

        Ellipse island = new Ellipse(130, 227.5, 130, 146);
        island.setFill(Color.WHITE);
        island.setStroke(Color.BLACK);
        island.setStrokeWidth(STROKE_WIDTH);

        mainlandTrafficLight = new TrafficLight(540, 87.5);
        islandTrafficLight = new TrafficLight(260, 287.5);

        root.getChildren().addAll(sea, bridge, line, island, mainland, mainlandTrafficLight, islandTrafficLight);

        for (int i = 0; i < 3; i++) {
            toIslandCars[i] = new Car(262 + (i * 85), 182.5);
            fromIslandCars[i] = new Car(490 - (i * 85), 235);
            fromIslandCars[i].setScaleX(-1 * fromIslandCars[i].getScaleX());
            islandCars[i] = new Car(50, 162.5 + (i * 45));
        }

        root.getChildren().addAll(toIslandCars);
        root.getChildren().addAll(fromIslandCars);
        root.getChildren().addAll(islandCars);

        AnchorPane.setTopAnchor(root, 0.0);
        AnchorPane.setLeftAnchor(root, 0.0);
        AnchorPane.setRightAnchor(root, 0.0);
        pane.getChildren().add(root);

        //scaleVisualization(pane.widthProperty().get());

        return pane;
    }

    @Override
    protected void stop() {}

    @Override
    protected void registerFormulaListener() {

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
