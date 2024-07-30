package org.processmining.implicitplaceidentification.algorithms;

import org.processmining.models.graphbased.AbstractGraphEdge;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class TNetDecomposition {

    // when applying the t-net search, it is only needed to check transitions at choices
    public static Petrinet colorAllTNetsAtChoices(Petrinet net) {
        List<Place> placesBeforeChoices =
                net.getPlaces().stream().filter(p -> net.getOutEdges(p).size() > 1).collect(Collectors.toList());

        // adding the transitions to a set first, prevents constructing a t-net for one transition multiple times
        Set<PetrinetNode> transitionsAtChoices = new HashSet<>();
        for (Place p : placesBeforeChoices) {
            transitionsAtChoices.addAll(net.getOutEdges(p).stream().map(AbstractGraphEdge::getTarget).collect(Collectors.toList()));
        }

        for (PetrinetNode t : transitionsAtChoices) {
            Set<PetrinetNode> nodesOfInducedTNet = getInducedTNetNodes(net, (Transition) t);

            Random rand = new Random();
            Color randomColor = new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
            for (PetrinetNode node : nodesOfInducedTNet) {
                node.getAttributeMap().put(AttributeMap.FILLCOLOR, randomColor);
            }
        }
        return net;
    }

    public static Set<PetrinetNode> getInducedTNetNodes(Petrinet net, Transition t) {

        HashSet<PetrinetNode> tNetNodes = new HashSet<>();
        addTNetNodes(net, t, tNetNodes);

        return tNetNodes;
    }

    public static Set<Place> getSuccessorPlacesOfSubnet(Petrinet net, Set<PetrinetNode> nodes) {
        HashSet<Place> succeedingPlaces = new HashSet<>();
        for (PetrinetNode node : nodes) {
            if (node instanceof Transition) {
                Set<Place> successorPlaces =
                        net.getOutEdges(node).stream().map(petrinetEdge -> (Place) petrinetEdge.getTarget()).collect(Collectors.toSet());
                succeedingPlaces.addAll(successorPlaces.stream().filter(p -> !nodes.contains(p)).collect(Collectors.toSet()));
            }
        }
        return succeedingPlaces;
    }

    private static void addTNetNodes(Petrinet net, Transition t, HashSet<PetrinetNode> tNetNodes) {

        if (tNetNodes.contains(t)) {
            return;
        }
        
        // add t
        tNetNodes.add(t);

        // add all output places of t that have exactly 1 in- and 1 outgoing transition
        List<PetrinetNode> outputPlacesFiltered = net.getOutEdges(t).stream().map(AbstractGraphEdge::getTarget)
                .filter(pc -> net.getOutEdges(pc).size() == 1 && net.getInEdges(pc).size() == 1)
                .collect(Collectors.toList());

        for (PetrinetNode p : outputPlacesFiltered) {
            tNetNodes.add(p);
            // for every output place, all outgoing transitions are in t-net (recursive)
            List<PetrinetNode> outgoingTransitions =
                    net.getOutEdges(p).stream().map(AbstractGraphEdge::getTarget).collect(Collectors.toList());
            for (PetrinetNode outT : outgoingTransitions) {
                addTNetNodes(net, (Transition) outT, tNetNodes);
            }
            // TODO do this, also check about completeness (maximal t-nets required? -> can I skip all t-nets that are
            //  subnets of another t-net? Are those even t-nets?
        }
    }
}
