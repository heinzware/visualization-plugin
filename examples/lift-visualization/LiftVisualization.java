import javafx.scene.Group;
import javafx.scene.control.Tab;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import de.heinzen.probplugin.visualization.Visualization;
import de.heinzen.probplugin.visualization.listener.FormulaListener;
//import de.prob.translator.types.BigInteger;


/**
 * Description of class
 *
 * @author Christoph Heinzen
 * @version 0.1.0
 * @since 05.10.17
 */
public class LiftVisualization extends Visualization{

    private Rectangle lift;
    private Text currentFloor;
    private Text moving;

    @Override
    protected String getName() {
        return "Lift Visualization";
    }

    @Override
    protected void initialize(Tab tab) {

        // ca 35 Minuten

        AnchorPane pane = new AnchorPane();
        pane.setMinWidth(400);
        pane.setMinHeight(400);
        pane.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));

        Group root = new Group();

        Rectangle puffer = new Rectangle(0,0,10,10);
        puffer.setFill(Color.WHITE);

        Rectangle frame = new Rectangle(192, 331, Color.WHITE);
        frame.setStroke(Color.BLACK);
        frame.setX(58);
        frame.setY(10);

        lift = new Rectangle(70, 80, Color.LIGHTGRAY);
        lift.setX(122);
        lift.setY(250);
        lift.setStroke(Color.BLACK);

        currentFloor = new Text(62, 362, "Current Floor: 0");
        currentFloor.setFont(Font.font("Helvetica", FontPosture.ITALIC, 15));

        Line line1 = new Line(19, 240, 289, 240);
        line1.setStroke(Color.BLACK);
        line1.getStrokeDashArray().addAll(4d);

        Line line2 = new Line(19, 130, 289, 130);
        line2.setStroke(Color.BLACK);
        line2.getStrokeDashArray().addAll(4d);


        Text floor2 = new Text(288, 68, "Floor 2");
        floor2.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
        Text floor1 = new Text(288, 190, "Floor 1");
        floor1.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
        Text floor0 = new Text(288, 295, "Floor 0");
        floor0.setFont(Font.font("Monospace", FontWeight.BOLD, 14));

        moving = new Text(58, 385, "Moving: idle");
        moving.setFont(Font.font("Helvetica", FontWeight.BOLD, 15));

        root.getChildren().addAll(puffer,frame, line1, line2, floor0, floor1, floor2, lift, currentFloor, moving);

        AnchorPane.setTopAnchor(root, 0.0);
        AnchorPane.setLeftAnchor(root, 0.0);
        pane.getChildren().add(root);

        tab.setContent(pane);

    }

    @Override
    protected void stop() {

    }

    @Override
    protected String[] getMachines() {
        return new String[]{"MLift"};
    }

    @Override
    protected void registerFormulaListener() {
        registerFormulaListener(new FormulaListener("cur_floor") {
            @Override
            public void variablesChanged(Object[] newValues) {
                int floor = translateToInt(newValues[0]);
                currentFloor.setText("Current Floor: " + (floor+1));
                lift.setY(250 - ((floor + 1 ) * 110));
            }
        });

        registerFormulaListener(new FormulaListener("door_open") {
            @Override
            public void variablesChanged(Object[] newValues) {
                boolean doorOpen = translateToBool(newValues[0]);
                lift.setFill(doorOpen ? Color.WHITE : Color.LIGHTGRAY);
            }
        });

        registerFormulaListener(new FormulaListener("direction_up") {
            @Override
            public void variablesChanged(Object[] newValues) {
                boolean up = translateToBool(newValues[0]);
                moving.setText("Moving: " + (up ? "Up" : "Down"));
            }
        });
    }

    @Override
    protected void registerEventListener() {

    }
}
