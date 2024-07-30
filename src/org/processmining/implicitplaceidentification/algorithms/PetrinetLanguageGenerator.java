package org.processmining.implicitplaceidentification.algorithms;

import org.processmining.models.graphbased.AbstractGraphNode;
import org.processmining.models.graphbased.NodeID;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.analysis.WorkflowNetUtils;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.behavioralanalysis.woflan.WoflanDiagnosis;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class can be used to generate a possibly large subset of the language a given sound Workflow net.
 */
public class PetrinetLanguageGenerator {

    // TODO use incidence matrices and methods from Algebra class (e.g. fire)! This also enables support for arc
    //  weights > 1

    private Set<Stack<String>> languageSet = new HashSet<>();
    private Stack<String> trace = new Stack<>();
    private Stack<String> previousTrace = new Stack<>();
    private Stack<Marking> markingTrace = new Stack<>();
    /**
     * List of all transition node-ids in the net
     */
    private List<NodeID> transitionNodeIds = new ArrayList<>();

    /**
     * This method generates the language a possibly large subset of a given sound Workflow net.
     * The additional parameters affect the size of the output but also the runtime.
     *
     * @param net                the Petri net for which the language shall be generated
     * @param initialMarking     the initial marking
     * @param maxTraceLength     the maximum trace length a word in the generated output can have
     * @param loopExecutionLimit the number how many times a loop gets executed during the generation of one trace
     * @param markingBound       the marking bound for all places in the net
     * @return an approximation of the language of the net
     */
    public Set<Stack<String>> generate(Petrinet net, Marking initialMarking, int maxTraceLength, int loopExecutionLimit,
                                       int markingBound) {

        //check if net is a sound WFN, if not, throw illegal argument exception
        //checkOnSoundWFNness(net);

        transitionNodeIds =
                net.getTransitions().stream().map(AbstractGraphNode::getId).sorted().collect(Collectors.toList());
        languageSet = new HashSet<>();
        trace = new Stack<>();
        previousTrace = new Stack<>();
        markingTrace = new Stack<>();
        markingTrace.push(initialMarking);

        return generateRecursively(net, initialMarking, maxTraceLength, loopExecutionLimit, markingBound);
    }

    private Set<Stack<String>> generateRecursively(Petrinet net, Marking initialMarking, int maxTraceLength,
                                                   int loopExecutionLimit,
                                                   int markingBound) {
        // TODO schreibe methode die check hat, ob final marking erreicht ist. wenn ja, return sofort (nur wenn ich
        //  in die richtung vertiefen will)

        for (NodeID id : transitionNodeIds) {
            Transition t = net.getTransitions().stream().filter(trans -> trans.getId().equals(id)).findFirst().get();
            if (isEnabled(initialMarking, net, t, markingBound) && trace.size() <= maxTraceLength && !loopLimitCheck(initialMarking
                    , t, loopExecutionLimit)) {
                previousTrace = (Stack<String>) trace.clone();
                trace.push(t.getLabel());
                Marking m = fire(initialMarking, net, t);
                markingTrace.push(m);
                generateRecursively(net, m, maxTraceLength, loopExecutionLimit, markingBound);
            }
        }

        if (trace.size() > previousTrace.size()) {
            languageSet.add((Stack<String>) trace.clone());
        }

        if (!trace.isEmpty()) {
            previousTrace = (Stack<String>) trace.clone();
            trace.pop();
            markingTrace.pop();
        }


        return languageSet;
    }

    private Marking fire(Marking m, Petrinet net, Transition t) {
        Marking newMarking = new Marking(m); // clone
        net.getOutEdges(t).forEach(e -> newMarking.add((Place) e.getTarget(), 1));
        Marking consumed = new Marking();
        net.getInEdges(t).forEach(e -> consumed.add((Place) e.getSource(), 1));
        newMarking.minus(consumed);
        return newMarking;
    }

    private boolean isEnabled(Marking m, Petrinet net, Transition t, int markingBound) {
        Marking minimumMarking = new Marking();
        net.getInEdges(t).forEach(e -> minimumMarking.add((Place) e.getSource(), 1));
        HashSet<Place> actualPlaces = new HashSet<>(m);
        HashSet<Place> minimumPlaces = new HashSet<>(minimumMarking);

        return actualPlaces.containsAll(minimumPlaces) && boundedMarkingCheck(m, net, t, markingBound);
    }

    private boolean boundedMarkingCheck(Marking m, Petrinet net, Transition t, int markingBound) {
        Marking inputPlaces = new Marking();
        Marking outputPlaces = new Marking();

        net.getInEdges(t).forEach(e -> inputPlaces.add((Place) e.getSource(), 1));
        net.getOutEdges(t).forEach(e -> outputPlaces.add((Place) e.getTarget(), 1));

        for (Place p : outputPlaces) {
            long tokensInMarking = m.stream().filter(place -> place.equals(p)).count();
            long tokensToBeConsumedFromP = inputPlaces.stream().filter(place -> place.equals(p)).count();
            long tokensInMarkingAfterFire = tokensInMarking - tokensToBeConsumedFromP;

            if (tokensInMarkingAfterFire >= markingBound) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks whether firing the transition with the current marking would exceed the loop limit.
     *
     * @param m marking
     * @param t transition to be fired
     * @return false if the transition has already been fired with this marking at least as often as the loop limit.
     * True otherwise.
     */
    private boolean loopLimitCheck(Marking m, Transition t, int loopExecutionLimit) {
        int timesAlreadyVisited = 0;
        for (int i = 0; i < trace.size(); i++) {
            if (markingTrace.get(i).equals(m) && trace.get(i).equals(t.getLabel())) {
                timesAlreadyVisited++;
            }
        }
        return loopExecutionLimit <= timesAlreadyVisited;
    }

    private static void checkOnSoundWFNness(Petrinet petrinet) {
        WoflanDiagnosis woflanDiagnosis = new WoflanDiagnosis(petrinet);
        if (!WorkflowNetUtils.isValidWFNet(petrinet) && !woflanDiagnosis.isSound()) {
            throw new IllegalArgumentException("Petri net is not a sound workflow net");
        } else if (!woflanDiagnosis.isSound()) {
            throw new IllegalArgumentException("Petri net is not sound");
        } else if (!WorkflowNetUtils.isValidWFNet(petrinet)) {
            throw new IllegalArgumentException("Petri net is not a workflow net");
        }
    }
}
