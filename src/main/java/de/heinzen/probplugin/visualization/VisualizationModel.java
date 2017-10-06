package de.heinzen.probplugin.visualization;

import de.prob.animator.domainobjects.*;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Description of class
 *
 * @author Christoph Heinzen
 * @version 0.1.0
 * @since 25.09.17
 */
public class VisualizationModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(VisualizationModel.class);

    private final CurrentTrace currentTrace;
    private final StageManager stageManager;

    private Trace oldTrace;
    private Trace newTrace;
    private Map<String, EvalResult> oldStringToResult;
    private Map<String, EvalResult> newStringToResult;

    public VisualizationModel(CurrentTrace currentTrace, StageManager stageManager) {
        this.currentTrace = currentTrace;
        this.stageManager = stageManager;
    }

    public void setTraces(Trace oldTrace, Trace newTrace) {
        this.oldTrace = oldTrace;
        this.newTrace = newTrace;
        if (oldTrace != null) {
            Map<IEvalElement, AbstractEvalResult> oldValues = oldTrace.getCurrentState().getValues();
            oldStringToResult = oldValues.keySet().stream()
                    .filter(element -> element instanceof EventB)
                    .map(element -> (EventB) element)
                    .filter(eventB -> oldValues.get(eventB) instanceof EvalResult)
                    .collect(Collectors.toMap(EventB::toString, eventB -> (EvalResult) oldValues.get(eventB)));
        } else {
            oldStringToResult = null;
        }
        if (newTrace != null) {
            StringBuilder sb = new StringBuilder();
            for (Transition trans : newTrace.getNextTransitions()) {
                sb.append(trans.getName()).append(" ").append(trans.getParameterPredicate()).append(" ");
            }
            LOGGER.info("The following transitions are available: {}", sb);
            Map<IEvalElement, AbstractEvalResult> newValues = newTrace.getCurrentState().getValues();
            newStringToResult = newValues.keySet().stream()
                    .filter(element -> element instanceof EventB)
                    .map(element -> (EventB) element)
                    .filter(eventB -> newValues.get(eventB) instanceof EvalResult)
                    .collect(Collectors.toMap(EventB::toString, eventB -> (EvalResult) newValues.get(eventB)));
        } else {
            newStringToResult = null;
        }
    }

    public boolean hasChanged(String formula) {
        if (oldTrace == null) {
            return true;
        }

        EvalResult oldValue = null;
        EvalResult newValue = null;

        if (oldStringToResult.containsKey(formula)) {
            oldValue = oldStringToResult.get(formula);
        } else {
            AbstractEvalResult oldResult = oldTrace.evalCurrent(new EventB(formula));
            if (oldResult instanceof EvalResult) {
                oldValue = (EvalResult) oldResult;
            }
        }
        if (newStringToResult.containsKey(formula)) {
           newValue = newStringToResult.get(formula);
        } else {
            AbstractEvalResult newResult = newTrace.evalCurrent(new EventB(formula));
            if (newResult instanceof EvalResult) {
                newValue = (EvalResult) newResult;
            }
        }
        if (newValue == null) return false;
        if (oldValue == null) return true;

        return !oldValue.getValue().equals(newValue.getValue());
    }

    public Object getValue(String formula) {
        LOGGER.debug("Get value for formula \"{}\".", formula);
        if (newStringToResult.containsKey(formula)) {
            LOGGER.debug("Using map to get value of formula \"{}\".", formula);
            try {
                EvalResult value = newStringToResult.get(formula);
                TranslatedEvalResult translatedValue = value.translate();
                return translatedValue.getValue();
            } catch (Exception  e) {
                LOGGER.debug("Exception while trying to get the value for formula \"{}\" out of the map. Try to eval the trace.", formula);
                return evalCurrent(formula);
            }
        }
        LOGGER.debug("Eval trace to get value of formula \"{}\".", formula);
        return evalCurrent(formula);
    }

    public boolean executeEvent(String event, String... predicates) {
        Trace currentTrace = this.currentTrace.get();
        if (currentTrace.canExecuteEvent(event, predicates)) {
            Trace resultTrace = currentTrace.execute(event, predicates);
            this.currentTrace.set(resultTrace);
            return true;
        }
        return false;
    }

    private Object evalCurrent(String formula) {
        try {
            AbstractEvalResult evalResult = newTrace.evalCurrent(new EventB(formula, Collections.emptySet(), FormulaExpand.EXPAND));
            LOGGER.debug("Evaluated formula {} and got the result: {}", formula, evalResult);
            if (evalResult instanceof EvalResult) {
                EvalResult value = (EvalResult) evalResult;
                return value.translate().getValue();
            }
            return null;
        } catch (EvaluationException evalException) {
            Alert alert = stageManager.makeAlert(Alert.AlertType.WARNING,
                    "EvaluationException while evaluating the formula \"" + formula +
                            "\".\nThe message of the thrown exception is:\n\n\"" + evalException.getMessage() + "\"\n\n" +
                            "More details are in the log.", ButtonType.OK);
            alert.initOwner(stageManager.getCurrent());
            alert.show();
            LOGGER.warn("EvaluationException while evaluating the formula \"" + formula +"\".", evalException);
            return null;
        }

    }

    public Object getPreviousValue(String formula) {
        if (newTrace.getPreviousState() != null) {
            AbstractEvalResult evalResult = newTrace.getPreviousState()
                    .eval(new EventB(formula, Collections.emptySet(), FormulaExpand.EXPAND));
            if (evalResult instanceof EvalResult) {
                EvalResult value = (EvalResult) evalResult;
                TranslatedEvalResult translatedValue = value.translate();
                return translatedValue.getValue();
            }
        }
        return null;
    }
}
