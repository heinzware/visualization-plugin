package bridge.ui;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Transform;

/**
 * Description of class
 *
 * @author Christoph Heinzen
 * @version 0.1.0
 * @since 05.10.17
 */
public class Car extends Group {


    private static final int INITIAL_WIDTH = 697;
    private static final int INITIAL_HEIGHT = 310;

    public Car(double x, double y) {

        SVGPath body = new SVGPath();
        body.setContent("M 52.5 527.362 L 677.5 527.362 " +
                "C 677.5 527.362 717.5 294.862 322.5 294.862 " +
                "C 258.75 292.362 246.242 395.237 189.688 393.612 " +
                "C 20.1352 388.814 52.5 527.362 52.5 527.362 Z");
        body.setFillRule(FillRule.EVEN_ODD);
        body.setFill(Color.BLACK);
        body.setStroke(Color.BLACK);

        SVGPath window = new SVGPath();
        window.setContent("M 237.5 389.862 " +
                "C 305 309.862 301.68 310.16 332.5 309.862 " +
                "C 469.661 308.42 475.833 325.695 492.5 339.862 " +
                "C 527.5 394.862 517.5 387.362 405 384.862 " +
                "C 242.5 389.862 237.5 389.862 237.5 389.862 Z");
        window.setFillRule(FillRule.EVEN_ODD);
        window.setFill(Color.WHITE);
        window.setStroke(Color.BLACK);

        Group frontWheel = new Group();
        Ellipse outerFrontWheel = new Ellipse(213.75, 561.112, 71.25, 71.25);
        outerFrontWheel.setFill(Color.BLACK);
        outerFrontWheel.getTransforms().add(Transform.translate(158.297, -34.9311));
        Ellipse innerFrontWheel = new Ellipse(135 ,479.862, 47.5, 47.5);
        innerFrontWheel.setFill(Color.valueOf("#c1bfbf"));
        innerFrontWheel.getTransforms().add(Transform.translate(237.047, 46.3189));
        frontWheel.getTransforms().add(Transform.translate(-160, -2.50003));
        frontWheel.getChildren().addAll(outerFrontWheel,innerFrontWheel);

        Group rearWheel = new Group();
        Ellipse outerRearWheel = new Ellipse(213.75, 561.112, 71.25, 71.25);
        outerRearWheel.setFill(Color.BLACK);
        outerRearWheel.getTransforms().add(Transform.translate(158.297, -34.9311));
        Ellipse innerRearWheel = new Ellipse(135 ,479.862, 47.5, 47.5);
        innerRearWheel.setFill(Color.valueOf("#c1bfbf"));
        innerRearWheel.getTransforms().add(Transform.translate(237.047, 46.3189));
        rearWheel.getTransforms().add(Transform.translate(162.5, 4.99997));
        rearWheel.getChildren().addAll(outerRearWheel,innerRearWheel);

        getChildren().addAll(body, window, frontWheel, rearWheel);
        double scale = 40.0/310.0;
        setScaleX(scale);
        setScaleY(scale);
        setTranslateX(x + (((INITIAL_WIDTH * scale) - INITIAL_WIDTH) / 2.0) - 20);
        setTranslateY(y + (((INITIAL_HEIGHT * scale) - INITIAL_HEIGHT) / 2.0) - 294);
        //getTransforms().addAll(Transform.scale(, 40.0/310.0),
        //      Transform.translate(-6.0, -38.0));


    }

}
