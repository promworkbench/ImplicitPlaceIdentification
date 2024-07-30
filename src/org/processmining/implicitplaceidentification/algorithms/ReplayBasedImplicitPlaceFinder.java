package org.processmining.implicitplaceidentification.algorithms;

import com.google.common.collect.BiMap;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.implicitplaceidentification.algorithms.util.AlgebraClass;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class identifies implicit places in Petri net by replaying an event log on the net and performing
 * a pairwise comparison of the token histories of places.
 * Requirements for the input:
 * - bijective mapping between log events and transitions (no label splitting or silent transitions)
 * - no self loops allowed
 * <p>
 * This finder is not exclusive to inputs mined with the eST-Miner and also supports arc weights > 1
 * TODO places empty in beginning and end of replay?
 */
public class ReplayBasedImplicitPlaceFinder {
    private final Petrinet petrinet;
    private final Marking initialMarking;
    private final XLog log;
    private final HashSet<Place> foundImplicitPlaces = new HashSet<>();
    private final BiMap<Place, Integer> placeToRowMap;
    private final BiMap<Transition, Integer> transitionToColumnMap;
    private final HashMap<ArrayList<Transition>, int[][]> markingSequences;

    public ReplayBasedImplicitPlaceFinder(Petrinet petrinet, Marking initialMarking, XLog eventLog) {
        this.petrinet = petrinet;
        this.initialMarking = initialMarking;
        this.log = eventLog;
        placeToRowMap = AlgebraClass.createPlaceToIndexBiMap(this.petrinet);
        transitionToColumnMap = AlgebraClass.createTransitionToIndexBiMap(this.petrinet);

        // replay variants on the net (uniquely labeled, no silent transitions)
        // before: map events to transitions (simplifies adding potential support for silent transitions)
        HashSet<ArrayList<Transition>> variants = extractVariantsFromLogAsTransitionSequences();

        // get incidence matrix of net
        int[][] pre = AlgebraClass.computePreIncidenceMatrix(petrinet, placeToRowMap, transitionToColumnMap);
        int[][] incidenceMatrix = AlgebraClass.computeIncidenceMatrix(petrinet, placeToRowMap, transitionToColumnMap);

        markingSequences = replayVariantsOnNet(variants, pre, incidenceMatrix);
    }

    /**
     * Finds implicit places in the Petri net.
     *
     * @return a set of implicit places in the Petri net
     */
    public Set<Place> find() {
        foundImplicitPlaces.clear();

        // pairwise comparison of markings in places
        for (Place place : placeToRowMap.keySet()) {
            if (!getPlacesImplyingP(place).isEmpty()) {
                foundImplicitPlaces.add(place);
            }
        }
        return foundImplicitPlaces;
    }

    /**
     * Determines the implicitness via a pairwise comparison of marking histories of all places
     * if there exists a p2 s.t. markings in p1 > markings in p2, compute p3=p1-p2.
     * If p3 exists in the net, p1 is implicit
     *
     * @param place the place for which is determined whether is it implicit
     * @return The set of places that make the given place implicit. If the place is not implicit, the set is empty.
     */
    public Set<Place> getPlacesImplyingP(Place place) {
        int p1 = placeToRowMap.get(place);
        Set<Place> placesThatMakeP1implicit = new HashSet<>();

        // Optimization: only compare places that share input transitions with p1
        // 1. get input transitions for p1
        Set<Transition> transitionsPrecedingP1 =
                petrinet.getInEdges(place).stream().map(edge -> (Transition) edge.getSource()).collect(Collectors.toSet());
        // 2. put followplaces of all these transitions in a set -> map them to row values and use below as source
        // for p2
        Set<Place> placesThatShareInputTransitionWithP1 = new HashSet<>();
        transitionsPrecedingP1.forEach(transition -> placesThatShareInputTransitionWithP1.addAll(petrinet.getOutEdges(transition).stream().map(edge -> (Place) edge.getTarget()).collect(Collectors.toSet())));

        for (int p2 :
                Collections.unmodifiableSet(placesThatShareInputTransitionWithP1.stream().map(placeToRowMap::get).collect(Collectors.toSet()))) {
            if (p1 == p2) {
                continue;
            }
            // check whether marking histories of p1 > p2 or p1 == p2
            boolean hasPotential = true;
            boolean isEqual = true;
            for (int[][] markingHistory : markingSequences.values()) {
                //TODO might change greater or equal to to equal and at least in one point greater to
                if (!AlgebraClass.arrayIsGreaterOrEqualTo(markingHistory[p1], markingHistory[p2])) {
                    hasPotential = false;
                    break;
                }
                if (!Arrays.equals(markingHistory[p1], markingHistory[p2])) {
                    isEqual = false;
                }
            }
            if (isEqual) {
                placesThatMakeP1implicit.add(placeToRowMap.inverse().get(p2));
            }
            if (hasPotential) {
                // compute marking history of p3 and check whether p3 exists
                HashSet<Integer> placeIndicesThatMatchP3 =
                        IntStream.range(0, placeToRowMap.size()).boxed().collect(Collectors.toCollection(HashSet::new));
                placeIndicesThatMatchP3.remove(p1);
                placeIndicesThatMatchP3.remove(p2);
                for (int[][] markingHistory : markingSequences.values()) {
                    if (placeIndicesThatMatchP3.isEmpty()) {
                        break;
                    } else {
                        int[] markingHistoryP1 = markingHistory[p1];
                        int[] markingHistoryP2 = markingHistory[p2];
                        int[] difference =
                                IntStream.range(0, markingHistory[0].length).map(index -> markingHistoryP1[index] - markingHistoryP2[index]).toArray();
                        for (int k = 0; k < markingHistory.length; k++) {
                            if (!Arrays.equals(markingHistory[k], difference)) {
                                placeIndicesThatMatchP3.remove(k);
                            }
                        }
                    }
                }
                if (!placeIndicesThatMatchP3.isEmpty()) {
                    System.out.println("Place " + placeToRowMap.inverse().get(p1).getLabel() + " is implicit");
                    placesThatMakeP1implicit.addAll(placeIndicesThatMatchP3.stream().map(index -> placeToRowMap.inverse().get(index)).collect(Collectors.toSet()));
                }
            }
        }
        return placesThatMakeP1implicit;
    }

    /**
     * Replays the variants on the Petri net and returns the marking sequences
     *
     * @param variants        the variants to be replayed on the net
     * @param pre             the preincidence matrix of the Petri net on which the replay is performed
     * @param incidenceMatrix the incidence matrix of the Petri net
     * @return the marking sequences of the replay as hash map, where the transition sequence is the key and the
     * marking sequences of the places presented in a matrix is the value. The matrix has a row for every place and a
     * column for every transition, so one row is the marking history of one particular place.
     */
    private HashMap<ArrayList<Transition>, int[][]> replayVariantsOnNet(HashSet<ArrayList<Transition>> variants,
                                                                        int[][] pre,
                                                                        int[][] incidenceMatrix) {
        // initial marking vector
        int[] m0 = new int[placeToRowMap.size()];
        this.initialMarking.forEach(p -> m0[placeToRowMap.get(p)]++);


        HashMap<ArrayList<Transition>, int[][]> markingSequences = new HashMap<>();
        for (ArrayList<Transition> trace : variants) {
            int[] mx = m0;
            // marking sequences as matrix, where one column holds the marking history of one place
            int[][] markingSequenceTransposed = new int[trace.size() + 1][petrinet.getPlaces().size()];

            // add initial marking to marking sequence first:
            markingSequenceTransposed[0] = mx;

            for (int i = 0; i < trace.size(); i++) {

                Transition t = trace.get(i);
                int[] minimumMarking = AlgebraClass.getColumnOfMatrix(pre, transitionToColumnMap.get(t));

                // check whether transition is enabled
                if (AlgebraClass.arrayIsGreaterOrEqualTo(mx, minimumMarking)) {
                    // fire transition
                    mx = AlgebraClass.fireTransitionOnIncidenceMatrix(mx, incidenceMatrix,
                            transitionToColumnMap.get(t));

                } else {
                    System.out.println("Variant does not match Petri net: " + trace);
                    break;
                }
                markingSequenceTransposed[i + 1] = mx;
            }
            markingSequences.put(trace, AlgebraClass.transpose(markingSequenceTransposed));
        }
        return markingSequences;
    }

    private HashSet<ArrayList<Transition>> extractVariantsFromLogAsTransitionSequences() {
        // extract variants from log
        HashSet<ArrayList<String>> variants = extractVariantsFromLog();

        // map traces to transition sequences
        HashMap<String, Transition> transitionLabelsToTransitionMap = new HashMap<>();
        petrinet.getTransitions().forEach(t -> transitionLabelsToTransitionMap.put(t.getLabel(), t));
        return mapTracesToTransitionSequences(variants,
                transitionLabelsToTransitionMap);
    }

    private static HashSet<ArrayList<Transition>> mapTracesToTransitionSequences(HashSet<ArrayList<String>> variants,
                                                                                 HashMap<String, Transition> transitionLabelsToTransitionMap) {
        HashSet<ArrayList<Transition>> transitionSequences = new HashSet<>();
        for (ArrayList<String> v : variants) {
            ArrayList<Transition> transitionseq = new ArrayList<>();
            for (String eventName : v) {
                if (!transitionLabelsToTransitionMap.containsKey(eventName)) {
                    transitionseq = null;
                    break;
                } else {
                    transitionseq.add(transitionLabelsToTransitionMap.get(eventName));
                }
            }
            if (transitionseq != null) {
                transitionSequences.add(transitionseq);
            }
        }
        return transitionSequences;
    }

    private HashSet<ArrayList<String>> extractVariantsFromLog() {
        HashSet<ArrayList<String>> variants = new HashSet<>();
        for (XTrace trace : log) {
            ArrayList<String> traceAsList = new ArrayList<>();
            for (org.deckfour.xes.model.XEvent xEvent : trace) {
                traceAsList.add(xEvent.getAttributes().get("concept:name").toString());
            }
            variants.add(traceAsList);
        }
        return variants;
    }

}
