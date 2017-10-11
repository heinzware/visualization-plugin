package bridge.ui;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/**
 * Description of class
 *
 * @author Christoph Heinzen
 * @version 0.1.0
 * @since 10.10.17
 */
public class TrafficLight extends Group{

    private final Circle redLight;
    private final Circle greenLight;

    public TrafficLight(double x, double y) {

        Rectangle body = new Rectangle(0,0, 40, 80);
        body.setFill(Color.GRAY);
        body.setStroke(Color.BLACK);
        body.setStrokeWidth(1.5);

        greenLight = new Circle(20,20, 15);
        greenLight.setFill(Color.GREEN);
        greenLight.setStroke(Color.BLACK);
        greenLight.setStrokeWidth(1.5);
        redLight = new Circle(20,60,15);
        redLight.setFill(Color.RED);
        redLight.setStroke(Color.BLACK);
        redLight.setStrokeWidth(1.5);

        getChildren().addAll(body, greenLight, redLight);

        setTranslateX(x);
        setTranslateY(y);
    }

    public void setGreen(boolean green) {
        greenLight.setFill(green ? Color.GREEN : Color.WHITE);
        redLight.setFill(green? Color.WHITE : Color.RED);
    }
}
