package org.processmining.implicitplaceidentification.algorithms;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XLogImpl;
import org.deckfour.xes.model.impl.XTraceImpl;
import org.processmining.implicitplaceidentification.algorithms.util.PetriNetCopier;
import org.processmining.models.graphbased.AbstractGraphElement;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.analysis.WorkflowNetUtils;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.behavioralanalysis.woflan.WoflanDiagnosis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * This class implements an approach to identify implicit places in Petri nets that was developed by me (Tobias
 * Wirtz) in my master thesis.
 * This approach takes a sound workflow net (soundness is needed, workflow net might not be needed), decomposes it
 * into subnets and uses existing implicit place identification methods to find implicit places that cannot be found
 * by conventional approaches.
 * <p>
 * The approach in a nutshell:
 * - get T-nets induced by transitions at choices
 * - for every t-net:
 * - remove t-net from net
 * - use conventional IP-finder on rest of net
 * - record IPs of subnets
 * - the cut of IPs across subnets is also implicit in the full net
 */
public class DecomposeAndConquerImplicitPlaceFinder {
    // TODO add findmode
    
    private final Petrinet petrinet;
    private final XLog log;
    private final Marking initialMarking;

    public DecomposeAndConquerImplicitPlaceFinder(Petrinet petrinet, Marking initialMarking, XLog log) {
        //check if net is a sound WFN, if not, throw illegal argument exception
        checkOnSoundWFNness(petrinet);
        this.petrinet = petrinet;
        this.initialMarking = initialMarking;
        this.log = log;
    }

    public DecomposeAndConquerImplicitPlaceFinder(Petrinet petrinet, Marking initialMarking) {
        //check if net is a sound WFN, if not, throw illegal argument exception
        checkOnSoundWFNness(petrinet);
        this.petrinet = petrinet;
        this.initialMarking = initialMarking;
        this.log = null;
    }

    /**
     * Checks the output places of t-nets at choices for implicitness by decomposing the net.
     * This is meant as a post-processing tool, since it only checks places that might not be identifiable by
     * conventional approaches.
     *
     * @return a {@link Set} of implicit places
     */
    public Set<Place> findImplicitMinimalRegionsMaybeMore() {
        Set<Place> identifiedIPs = new HashSet<>();

        Set<Place> placesAtChoices =
                petrinet.getPlaces().stream().filter(p -> petrinet.getOutEdges(p).size() > 1).collect(Collectors.toSet());


        for (Place placeAtChoice : placesAtChoices) {
            System.out.println("[DEBUG] evaluating Tnets following place: " + placeAtChoice.getLabel());
            Set<Transition> transitionsAtChoice =
                    petrinet.getOutEdges(placeAtChoice).stream().map(e -> (Transition) e.getTarget()).collect(Collectors.toSet());

            // compute t-nets at choice
            HashMap<Transition, Set<PetrinetNode>> tNets = new HashMap<>();
            for (Transition t : transitionsAtChoice) {
                tNets.put(t, TNetDecomposition.getInducedTNetNodes(petrinet, t));
            }

            // compute implicit places for decomposed nets
            HashMap<Place, Set<Place>> implicitPlaceCandidates = new HashMap<>();
            Set<Place> successorPlacesOfTnets = TNetDecomposition.getSuccessorPlacesOfSubnet(petrinet,
                    tNets.values().stream().reduce(new HashSet<>(), (a, b) -> {
                        a.addAll(b);
                        return a;
                    }));
            for (Place sp : successorPlacesOfTnets) {
                implicitPlaceCandidates.put(sp, new HashSet<>());
            }

            // TODO use preprocessing to find duplicate places in decomposition
            for (Transition t : transitionsAtChoice) {
                System.out.println("[DEBUG] evaluating Tnet induced by transition: " + t.getLabel());
                PetriNetCopier copier = new PetriNetCopier(petrinet);

                // remove all but the current t-net from the copy
                for (Transition key : tNets.keySet()) {
                    if (!key.equals(t)) {
                        copier.removeNodes(tNets.get(key));
                    }
                }
                Petrinet subnet = copier.getDeepCopy();
                Set<Place> successorPlacesOfTNetsAtChoiceInCopy = successorPlacesOfTnets.stream()
                        .map(copier::getPlaceInCopyForOriginalPlace)
                        .collect(Collectors.toSet());

                for (Place sp : successorPlacesOfTNetsAtChoiceInCopy) {
                    // check whether sp is implicit and get implying places
                    Set<Place> implyingPlaces;
                    if (log == null) {
                        StructureBasedImplicitPlaceFinder ipFinder = new StructureBasedImplicitPlaceFinder(subnet,
                                copier.originalToCopyMarking(initialMarking),
                                StructureBasedImplicitPlaceFinder.FindMode.FIND_ALL_POTENTIAL_IPS);
                        implyingPlaces = ipFinder.getPlacesImplyingP(sp);
                    } else {
                        // TODO filter log to only contain transitions that are in net? Not doing so might currently
                        //  lead to a bug
                        ReplayBasedImplicitPlaceFinder ipFinder = new ReplayBasedImplicitPlaceFinder(subnet,
                                copier.originalToCopyMarking(initialMarking), log);
                        implyingPlaces = ipFinder.getPlacesImplyingP(sp);
                    }

                    Place spInOriginalNet = copier.getPlaceInOriginalNetForPlaceInCopy(sp);

                    if (implyingPlaces.isEmpty()) {
                        implicitPlaceCandidates.remove(spInOriginalNet);
                    } else if (implicitPlaceCandidates.containsKey(spInOriginalNet)) {
                        // add implying places of sp to candidate map
                        implicitPlaceCandidates.merge(spInOriginalNet,
                                implyingPlaces.stream().map(copier::getPlaceInOriginalNetForPlaceInCopy).collect(Collectors.toSet()),
                                (set, newValues) -> {
                                    set.addAll(newValues);
                                    return set;
                                });
                    }
                }
            }

            /* DEBUGGING */

            for (Place p : implicitPlaceCandidates.keySet()) {
                System.out.println("[DEBUG] " + p.getLabel() + "is implied by:" + implicitPlaceCandidates.get(p).stream().map(AbstractGraphElement::getLabel).collect(Collectors.toSet()));
            }

            /* DEBUGGING */

            // candidates are already the cut across t-nets, now check for dependencies between candidates
            // greedy version: go through hash map from start to finish, remove the first place, keep all implying
            // places
            // TODO optimal version (find combination to remove the max number of places)
            // TODO option to return all individually identifiable IPs

            // the following variable is used when finding a combination of IP candidates that can be safely removed
            Set<Place> placesThatCannotBeRemoved = new HashSet<>();

            Set<Place> potentiallyImplicitPlaces = new HashSet<>(implicitPlaceCandidates.keySet());
            for (Place ipCandidate : potentiallyImplicitPlaces) {
                if (!placesThatCannotBeRemoved.contains(ipCandidate)) {
                    placesThatCannotBeRemoved.addAll(implicitPlaceCandidates.get(ipCandidate));
                    implicitPlaceCandidates.remove(ipCandidate);
                }
            }
            identifiedIPs.addAll(implicitPlaceCandidates.keySet());
        }
        return identifiedIPs;
    }

    // TODO: Ist das Kunst oder kann das weg?
    private static Set<Place> findIPsUsingStructureBasedFinder(Petrinet subnet, Marking initialMarkingOnSubnet) {
        Set<Place> resultOnCopy;
        StructureBasedImplicitPlaceFinder ipFinder = new StructureBasedImplicitPlaceFinder(subnet,
                initialMarkingOnSubnet,
                StructureBasedImplicitPlaceFinder.FindMode.FIND_ALL_POTENTIAL_IPS);
        resultOnCopy = ipFinder.find();
        return resultOnCopy;
    }

    private Set<Place> findIPsUsingReplayBasedFinder(Set<PetrinetNode> nodesToBeRemoved, Petrinet subnet,
                                                     Marking initialMarkingOnSubnet) {
        XLog sublog = filterOutTracesThatContainEvents(log,
                nodesToBeRemoved.stream().filter(node -> node instanceof Transition).map(AbstractGraphElement::getLabel).collect(Collectors.toSet()));

        // use IP-Finder on copy without t-net
        ReplayBasedImplicitPlaceFinder ipFinder = new ReplayBasedImplicitPlaceFinder(subnet,
                initialMarkingOnSubnet,
                sublog);
        return ipFinder.find();
    }

    private XLog filterOutEvents(XLog log, Set<String> events) {
        XLog filteredLog = new XLogImpl(log.getAttributes());
        for (XTrace trace : log) {
            List<XEvent> filteredEvents = trace.stream().filter(e -> !events.contains(e.getAttributes().get("concept" +
                    ":name").toString())).collect(Collectors.toList());
            XTrace filteredTrace = new XTraceImpl(trace.getAttributes());
            filteredTrace.addAll(filteredEvents);
            filteredLog.add(filteredTrace);
        }
        return filteredLog;
    }

    /**
     * Filters out traces from the event log that contain certain events.
     *
     * @param log    an event log
     * @param events events to be filtered out
     * @return a copy of the log without the undesired events
     */
    private XLog filterOutTracesThatContainEvents(XLog log, Set<String> events) {
        XLog filteredLog = new XLogImpl(log.getAttributes());
        for (XTrace trace : log) {
            AtomicBoolean shouldBeInLog = new AtomicBoolean(true);
            trace.forEach(e -> {
                if (events.contains(e.getAttributes().get("concept" +
                        ":name").toString())) {
                    shouldBeInLog.set(false);
                }
            });
            if (shouldBeInLog.get()) {
                filteredLog.add(trace);
            }
        }
        return filteredLog;
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
