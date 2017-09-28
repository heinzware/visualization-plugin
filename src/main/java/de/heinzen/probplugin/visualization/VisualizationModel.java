package de.heinzen.probplugin.visualization;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EventB;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.Trace;

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

    private Trace oldTrace;
    private Trace newTrace;
    private Map<String, EvalResult> oldStringToResult;
    private Map<String, EvalResult> newStringToResult;

    public void setTraces(Trace oldTrace, Trace newTrace) {
        this.oldTrace = oldTrace;
        this.newTrace = newTrace;
        if (oldTrace != null) {
            Map<IEvalElement, AbstractEvalResult> oldValues = oldTrace.getCurrentState().getValues();
            oldStringToResult = oldValues.keySet().stream()
                    .filter(element -> element instanceof EventB)
                    .map(element -> (EventB) element)
                    .collect(Collectors.toMap(EventB::toString, eventB -> (EvalResult) oldValues.get(eventB)));
        } else {
            oldStringToResult = null;
        }
        if (newTrace != null) {
            Map<IEvalElement, AbstractEvalResult> newValues = newTrace.getCurrentState().getValues();
            newStringToResult = newValues.keySet().stream()
                    .filter(element -> element instanceof EventB)
                    .map(element -> (EventB) element)
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
}
