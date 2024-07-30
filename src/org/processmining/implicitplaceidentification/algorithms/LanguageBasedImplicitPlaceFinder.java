package org.processmining.implicitplaceidentification.algorithms;

import org.processmining.implicitplaceidentification.algorithms.util.PetriNetCopier;
import org.processmining.models.graphbased.AbstractGraphEdge;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.analysis.WorkflowNetUtils;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.behavioralanalysis.woflan.WoflanDiagnosis;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class identifies implicit places in a petri net by generating the language of the net (as well as possible)
 * and comparing the language of a net without a particular place.
 */
public class LanguageBasedImplicitPlaceFinder {
    private final Petrinet petrinet;
    private final Marking initialMarking;
    private final Place sinkPlace;
    private final int maxTraceLength = 100;
    private final int loopExecutionLimit = 2;
    private final int markingBound = 5;
    PetriNetCopier petriNetCopier;
    Set<PetrinetNode> nonImplicitPlaces = new HashSet<>();

    public LanguageBasedImplicitPlaceFinder(Petrinet petrinet, Marking initialMarking) {
        //check if net is a sound WFN, if not, throw illegal argument exception
        //checkOnSoundWFNness(petrinet);

        petriNetCopier = new PetriNetCopier(petrinet);
        remove1LoopTransitionsForRuntimeOpt(petrinet);
        this.petrinet = petriNetCopier.getDeepCopy();
        this.initialMarking = petriNetCopier.originalToCopyMarking(initialMarking);
        this.sinkPlace = WorkflowNetUtils.getOutputPlace(this.petrinet);
    }


    /**
     * Finds IPs by generating and comparing the language of the net and a net without a place p.
     *
     * @param simulatedRemovalFlag if set to true, the algorithm follows a greedy (not necessarily optimal) approach.
     *                             All places returned by this approach can safely be removed. If set to false, it
     *                             returns all possible IPs. The latter approach returns a set of places that might
     *                             change the behavior of the net, when removed.
     * @return a/the set of IPs depending on the set parameter
     */
    public Set<Place> find(boolean simulatedRemovalFlag) {
        HashSet<Place> implicitPlaces = new HashSet<>();

        Set<Stack<String>> baseLanguage = new PetrinetLanguageGenerator().generate(petrinet, initialMarking,
                maxTraceLength, loopExecutionLimit, markingBound);

        List<Place> petrinetPlaces = new ArrayList<>(petrinet.getPlaces());

        for (Place p : petrinetPlaces) {
            if (isPlaceTheOnlyInputForOneTransition(p) || p.equals(sinkPlace) || nonImplicitPlaces.contains(petriNetCopier.getPlaceInOriginalNetForPlaceInCopy(p))) {
                System.out.println("Place " + p.getLabel() + " is not implicit because it is a sink place or the only" +
                        " input for a transition");
                continue;
            }
            System.out.println("Generating language for net without place " + p.getLabel());
            PetriNetCopier pCopier = new PetriNetCopier(petrinet);
            Petrinet copy = pCopier.getDeepCopy();
            copy.removePlace(pCopier.getPlaceInCopyForOriginalPlace(p));
            Set<Stack<String>> languageWithoutP;
            try {
                languageWithoutP = new PetrinetLanguageGenerator().generate(copy,
                        pCopier.originalToCopyMarking(initialMarking), maxTraceLength, loopExecutionLimit,
                        markingBound);
            } catch (IllegalArgumentException e) {
                continue;
            }

            if (baseLanguage.equals(languageWithoutP)) {
                implicitPlaces.add(petriNetCopier.getPlaceInOriginalNetForPlaceInCopy(p));
                System.out.println("Place " + p.getLabel() + " is implicit");

                if (simulatedRemovalFlag) {
                    petrinet.removePlace(p);
                    initialMarking.remove(p);
                }
            } else {
                Set<Stack<String>> baseLanguageWithoutNewLanguage = new HashSet<>(baseLanguage);
                baseLanguageWithoutNewLanguage.removeAll(languageWithoutP);
                Set<Stack<String>> newLanguageWithoutBase = new HashSet<>(languageWithoutP);
                newLanguageWithoutBase.removeAll(baseLanguage);
                System.out.println("Place " + p.getLabel() + " is not implicit because removing it would change the " +
                        "language with the following traces:");
                System.out.println(baseLanguageWithoutNewLanguage);
                System.out.println("and");
                System.out.println(newLanguageWithoutBase);
            }
        }
        return implicitPlaces;
    }

    private boolean isPlaceTheOnlyInputForOneTransition(Place p) {
        HashSet<PetrinetNode> transitions = new HashSet<>();
        petrinet.getOutEdges(p).forEach(e -> transitions.add(e.getTarget()));
        for (PetrinetNode t : transitions) {
            if (petrinet.getInEdges(t).size() == 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method is used to optimize runtime.
     * It removes all transitions that have only 1 ingoing and 1 outgoing arc to the same place.
     * Then it marks this place as non-implicit.
     *
     * @param petriNet a Petri net
     */
    private void remove1LoopTransitionsForRuntimeOpt(Petrinet petriNet) {

        petriNet.getTransitions().stream().filter(transition -> petriNet.getInEdges(transition).size() == 1
                && petriNet.getOutEdges(transition).size() == 1).forEach(transition -> {
            Set<PetrinetNode> ins =
                    petriNet.getInEdges(transition).stream().map(AbstractGraphEdge::getSource).collect(Collectors.toSet());
            Set<PetrinetNode> outs =
                    petriNet.getOutEdges(transition).stream().map(AbstractGraphEdge::getTarget).collect(Collectors.toSet());
            if (ins.equals(outs)) {
                System.out.println("[DEBUG] Removing 1-loop transition " + transition + " for the language generation" +
                        " process");
                ArrayList<PetrinetNode> list = new ArrayList<>();
                list.add(transition);
                petriNetCopier.removeNodes(list);
                nonImplicitPlaces.addAll(ins);
            }
        });
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
