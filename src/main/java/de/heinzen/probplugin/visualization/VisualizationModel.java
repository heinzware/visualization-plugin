package de.heinzen.probplugin.visualization;

import de.prob.animator.domainobjects.*;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentTrace;
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

    private Trace oldTrace;
    private Trace newTrace;
    private Map<String, EvalResult> oldStringToResult;
    private Map<String, EvalResult> newStringToResult;

    public VisualizationModel(CurrentTrace currentTrace) {
        this.currentTrace = currentTrace;
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
        if (oldStringToResult.containsKey(formula) && newStringToResult.containsKey(formula)) {
            //if the formula is just a variable or a constant, lookup the values in the maps
            String newValue = newStringToResult.get(formula).getValue();
            String oldValue = oldStringToResult.get(formula).getValue();
            return !oldValue.equals(newValue);
        }
        EvalResult oldValue = (EvalResult) oldTrace.evalCurrent(new EventB(formula));
        EvalResult newValue = (EvalResult) newTrace.evalCurrent(new EventB(formula));
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
        AbstractEvalResult evalResult = newTrace.evalCurrent(new EventB(formula, Collections.emptySet(), FormulaExpand.EXPAND));
        if (evalResult instanceof EvalResult) {
            EvalResult value = (EvalResult) evalResult;
            return value.translate().getValue();
        }
        return null;
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
