package org.processmining.implicitplaceidentification.algorithms.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetImpl;
import org.processmining.models.semantics.petrinet.Marking;

import java.awt.*;
import java.util.Collection;

/**
 * This class provides method to create and modify a deep copy of a Petri net.
 * Furthermore, there are methods to translate places/nodes between the copy and the original net.
 */
public class PetriNetCopier {

    private final Petrinet originalPetrinet;
    private Petrinet copy;
    private final BiMap<Place, Place> originalToCopyPlaceMap;
    private final BiMap<Transition, Transition> originalToCopyTransitionMap;


    public PetriNetCopier(Petrinet petrinet) {
        this.originalPetrinet = petrinet;
        this.originalToCopyPlaceMap = HashBiMap.create();
        this.originalToCopyTransitionMap = HashBiMap.create();
        deepCopy(petrinet.getLabel());
    }

    public PetriNetCopier(Petrinet petrinet, String label) {
        this.originalPetrinet = petrinet;
        this.originalToCopyPlaceMap = HashBiMap.create();
        this.originalToCopyTransitionMap = HashBiMap.create();
        deepCopy(label);
    }

    /**
     * Returns the deep copy of the net
     *
     * @return the deep copy of the net
     */
    public Petrinet getDeepCopy() {
        return copy;
    }


    private void deepCopy(String label) {
        copy = new PetrinetImpl(label);

        for (Transition t : originalPetrinet.getTransitions()) {
            Transition copyT = copy.addTransition(t.getLabel());
            originalToCopyTransitionMap.put(t, copyT);
            copyAttributeMapFromNodeToNode(t, copyT);
        }

        for (Place p : originalPetrinet.getPlaces()) {
            Place copyPlace = copy.addPlace(p.getLabel());
            copyAttributeMapFromNodeToNode(p, copyPlace);
            originalToCopyPlaceMap.put(p, copyPlace);

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : originalPetrinet.getInEdges(p)) {
                Arc originalArc = originalPetrinet.getArc((PetrinetNode) inEdge.getSource(), p);
                Arc copyArc = copy.addArc(originalToCopyTransitionMap.get(inEdge.getSource()), copyPlace);
                copyArc.setWeight(originalArc.getWeight());
            }

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge :
                    originalPetrinet.getOutEdges(p)) {
                Arc originalArc = originalPetrinet.getArc(p, (PetrinetNode) outEdge.getTarget());
                Arc copyArc = copy.addArc(copyPlace, originalToCopyTransitionMap.get(outEdge.getTarget()));
                copyArc.setWeight(originalArc.getWeight());
            }
        }
    }

    /**
     * Takes a {@link Marking} of the original net and returns the corresponding marking in the copy.
     *
     * @param m1 the Marking of the original net
     * @return the marking of the deep copy
     */
    public Marking originalToCopyMarking(Marking m1) {
        Marking m2 = new Marking();
        for (Place p : m1) {
            m2.add(getPlaceInCopyForOriginalPlace(p));
        }
        return m2;
    }

    /**
     * Takes a {@link PetrinetNode} in the deep copy and returns the corresponding node of the original net.
     *
     * @param node a node in the deep copy.
     * @return the node in the original net
     */
    public PetrinetNode getNodeInCopyForOriginalNode(PetrinetNode node) {
        if (node instanceof Transition) {
            return originalToCopyTransitionMap.get(node);
        } else {
            return originalToCopyPlaceMap.get(node);
        }
    }

    /**
     * Returns the place in the copy for a place in the original net.
     *
     * @param p a place from the original net
     * @return the place in the deep copy
     */
    public Place getPlaceInCopyForOriginalPlace(Place p) {
        return originalToCopyPlaceMap.get(p);
    }

    /**
     * Returns the place in the original net for a place in the deep copy.
     *
     * @param p a place in the deep copy
     * @return the place in the original net
     */
    public Place getPlaceInOriginalNetForPlaceInCopy(Place p) {
        return originalToCopyPlaceMap.inverse().get(p);
    }

    private static void copyAttributeMapFromNodeToNode(PetrinetNode node, PetrinetNode copyNode) {
        /*
        copyNode.getAttributeMap().put(AttributeMap.AUTOSIZE, node.getAttributeMap().get(AttributeMap.AUTOSIZE));
        copyNode.getAttributeMap().put(AttributeMap.BORDERWIDTH, node.getAttributeMap().get(AttributeMap.BORDERWIDTH));
        copyNode.getAttributeMap().put(AttributeMap.DASHOFFSET, node.getAttributeMap().get(AttributeMap.DASHOFFSET));
        copyNode.getAttributeMap().put(AttributeMap.DASHPATTERN, node.getAttributeMap().get(AttributeMap.DASHPATTERN));

        copyNode.getAttributeMap().put(AttributeMap.EDGESTART, node.getAttributeMap().get(AttributeMap.EDGESTART));
        copyNode.getAttributeMap().put(AttributeMap.EDGESTARTFILLED, node.getAttributeMap().get(AttributeMap
        .EDGESTARTFILLED));
        copyNode.getAttributeMap().put(AttributeMap.EDGEEND, node.getAttributeMap().get(AttributeMap.EDGEEND));
        copyNode.getAttributeMap().put(AttributeMap.EDGEENDFILLED, node.getAttributeMap().get(AttributeMap
        .EDGEENDFILLED));
        copyNode.getAttributeMap().put(AttributeMap.EDGEMIDDLE, node.getAttributeMap().get(AttributeMap.EDGEMIDDLE));
        copyNode.getAttributeMap().put(AttributeMap.EDGEMIDDLEFILLED, node.getAttributeMap().get(AttributeMap
        .EDGEMIDDLEFILLED));

        copyNode.getAttributeMap().put(AttributeMap.SHAPE, node.getAttributeMap().get(AttributeMap.SHAPE));
        copyNode.getAttributeMap().put(AttributeMap.SHAPEDECORATOR, node.getAttributeMap().get(AttributeMap
        .SHAPEDECORATOR));*/
        copyNode.getAttributeMap().put(AttributeMap.FILLCOLOR, node.getAttributeMap().get(AttributeMap.FILLCOLOR));
        //copyNode.getAttributeMap().put(AttributeMap.GRADIENTCOLOR, node.getAttributeMap().get(AttributeMap
        // .GRADIENTCOLOR));
        //copyNode.getAttributeMap().put(AttributeMap.ICON, node.getAttributeMap().get(AttributeMap.ICON));
        //copyNode.getAttributeMap().put(AttributeMap.BORDERWIDTH, node.getAttributeMap().get(AttributeMap
        // .BORDERWIDTH));
        //copyNode.getAttributeMap().put(AttributeMap.LABEL, node.getAttributeMap().get(AttributeMap.LABEL));
        //copyNode.getAttributeMap().put(AttributeMap.TOOLTIP, node.getAttributeMap().get(AttributeMap.TOOLTIP));
        //copyNode.getAttributeMap().put(AttributeMap.LABELVERTICALALIGNMENT, node.getAttributeMap().get(AttributeMap
        // .LABELVERTICALALIGNMENT));
        //copyNode.getAttributeMap().put(AttributeMap.EDGECOLOR, node.getAttributeMap().get(AttributeMap.EDGECOLOR));
        //copyNode.getAttributeMap().put(AttributeMap.STROKECOLOR, node.getAttributeMap().get(AttributeMap
        // .STROKECOLOR));
        //copyNode.getAttributeMap().put(AttributeMap.INSET, node.getAttributeMap().get(AttributeMap.INSET));
        //copyNode.getAttributeMap().put(AttributeMap.STROKE, node.getAttributeMap().get(AttributeMap.STROKE));
        //copyNode.getAttributeMap().put(AttributeMap.LABELCOLOR, node.getAttributeMap().get(AttributeMap.LABELCOLOR));
        //copyNode.getAttributeMap().put(AttributeMap.LABELALONGEDGE, node.getAttributeMap().get(AttributeMap
        // .LABELALONGEDGE));
        //copyNode.getAttributeMap().put(AttributeMap.LINEWIDTH, node.getAttributeMap().get(AttributeMap.LINEWIDTH));
        //copyNode.getAttributeMap().put(AttributeMap.NUMLINES, node.getAttributeMap().get(AttributeMap.NUMLINES));
        //copyNode.getAttributeMap().put(AttributeMap.STYLE, node.getAttributeMap().get(AttributeMap.STYLE));
        //copyNode.getAttributeMap().put(AttributeMap.POLYGON_POINTS, node.getAttributeMap().get(AttributeMap
        // .POLYGON_POINTS));
        //copyNode.getAttributeMap().put(AttributeMap.SQUAREBB, node.getAttributeMap().get(AttributeMap.SQUAREBB));
        //copyNode.getAttributeMap().put(AttributeMap.RESIZABLE, node.getAttributeMap().get(AttributeMap.RESIZABLE));
        //copyNode.getAttributeMap().put(AttributeMap.SHOWLABEL, node.getAttributeMap().get(AttributeMap.SHOWLABEL));
        //copyNode.getAttributeMap().put(AttributeMap.MOVEABLE, node.getAttributeMap().get(AttributeMap.MOVEABLE));
        //copyNode.getAttributeMap().put(AttributeMap.PREF_ORIENTATION, node.getAttributeMap().get(AttributeMap
        // .PREF_ORIENTATION));
        //copyNode.getAttributeMap().put(AttributeMap.LABELHORIZONTALALIGNMENT, node.getAttributeMap().get
        // (AttributeMap.LABELHORIZONTALALIGNMENT));
        //copyNode.getAttributeMap().put(AttributeMap.SIZE, node.getAttributeMap().get(AttributeMap.SIZE));
        //copyNode.getAttributeMap().put(AttributeMap.PORTOFFSET, node.getAttributeMap().get(AttributeMap.PORTOFFSET));
        //copyNode.getAttributeMap().put(AttributeMap.EXTRALABELPOSITIONS, node.getAttributeMap().get(AttributeMap
        // .EXTRALABELPOSITIONS));
        //copyNode.getAttributeMap().put(AttributeMap.EXTRALABELS, node.getAttributeMap().get(AttributeMap
        // .EXTRALABELS));
        //copyNode.getAttributeMap().put(AttributeMap.RENDERER, node.getAttributeMap().get(AttributeMap.RENDERER));
    }

    /**
     * Removes the given nodes from the deep copy.
     *
     * @param nodes of the original net to be removed in the deep copy
     * @return the {@link PetriNetCopier} instance
     */
    public PetriNetCopier removeNodes(Collection<PetrinetNode> nodes) {
        nodes.forEach(n -> copy.removeNode(getNodeInCopyForOriginalNode(n)));
        return this;
    }

    /**
     * Colors the given places in the deep copy.
     *
     * @param places of the original net that should be colored in the deep copy
     * @return the {@link PetriNetCopier} instance
     */
    public PetriNetCopier colorPlaces(Collection<Place> places) {
        places.forEach(n -> getNodeInCopyForOriginalNode(n).getAttributeMap().put(AttributeMap.FILLCOLOR,
                new Color(231, 110, 110)));
        return this;
    }
}
