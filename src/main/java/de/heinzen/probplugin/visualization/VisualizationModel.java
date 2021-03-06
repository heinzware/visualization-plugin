package de.heinzen.probplugin.visualization;

import de.prob.animator.domainobjects.*;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
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

    /**
     * Sets the new and the old trace.
     *
     * @param oldTrace Trace before the last executed transition.
     * @param newTrace Current trace.
     */
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

    /**
     * The methods checks if the value of the given formula was changed through the last transition.
     * @param formula Formula that could have changed.
     * @return Returns true if the value has changed.
     */
    public boolean hasChanged(String formula) {
        LOGGER.debug("Look up if the formula \"{}\" has changed its value.", formula);
        if (oldTrace == null) {
            LOGGER.debug("The old trace is null, so the value of formula \"{}\" has changed.", formula);
            return true;
        }

        EvalResult oldValue;
        EvalResult newValue;

        if (oldStringToResult.containsKey(formula)) {
            oldValue = oldStringToResult.get(formula);
        } else {
            oldValue = evalState(oldTrace.getCurrentState(), formula);
        }

        if (newStringToResult.containsKey(formula)) {
            newValue = newStringToResult.get(formula);
        } else {
            newValue = evalState(newTrace.getCurrentState(), formula);
        }

        if (newValue == null) {
            LOGGER.debug("The value of formula \"{}\" couldn't be evaluated in the new trace. Returning false.", formula);
            return false;
        }

        if (oldValue == null) {
            LOGGER.debug("The value of formula \"{}\" couldn't be evaluated in the old trace, but in the new trace. Returning true.", formula);
            return true;
        }

        LOGGER.debug("The value of formula \"{}\" could be evaluated for the new and the old trace.", formula);
        return !oldValue.getValue().equals(newValue.getValue());
    }

    /**
     * Gets the expanded and translated value of the given formula.
     *
     * @param formula Formula to evaluate.
     * @return Returns the expanded and translated value of the formula. If the formula can't be evaluated the method
     *         will return {@code null}.
     */
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

    /**
     * Tries to execute the given event with the given predicates.
     *
     * @param event Name of the event
     * @param predicates Optional predicates of the event
     * @return Returns a boolean indicating whether the event was executed or not.
     */
    public boolean executeEvent(String event, String... predicates) {
        Trace currentTrace = this.currentTrace.get();
        LOGGER.debug("Try to execute event \"{}\" with predicates: {}.", event, String.join(" ", predicates));
        if (currentTrace.canExecuteEvent(event, predicates)) {
            LOGGER.debug("Event \"{}\" is executable. Execute it.");
            Trace resultTrace = currentTrace.execute(event, predicates);
            this.currentTrace.set(resultTrace);
            return true;
        }
        LOGGER.debug("Event \"{}\" is not executable.");
        return false;
    }

    private Object evalCurrent(String formula) {
        EvalResult value = evalState(newTrace.getCurrentState(), formula);
        return value != null ? value.translate().getValue() : null;
    }

    private EvalResult evalState(State state, String formula) {
        LOGGER.debug("Try to evaluate formula {}.", formula);
        try {
            AbstractEvalResult evalResult = state.eval(new EventB(formula, Collections.emptySet(), FormulaExpand.EXPAND));
            LOGGER.debug("Evaluated formula \"{}\" and got the result: {}", formula, evalResult);
            if (evalResult instanceof EvalResult) {
                return (EvalResult) evalResult;
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

    /**
     * Evaluates the given formula, but not on the current state.
     * It tries to evaluate it on the previous state of the current trace.
     *
     * @param formula Formula to evaluate.
     * @return The value of the formula or {@code null}.
     */
    public Object getPreviousValue(String formula) {
        LOGGER.debug("Try to get previous value of formula \"{}\".", formula);
        if (newTrace.getPreviousState() != null) {
            EvalResult value = evalState(newTrace.getPreviousState(), formula);
            LOGGER.debug("Evaluated previous value of formula \"{}\" and got the result: {}", formula, value);
            return value != null ? value.translate().getValue() : null;
        }
        LOGGER.debug("The previous state is null. Returning null");
        return null;
    }
}
