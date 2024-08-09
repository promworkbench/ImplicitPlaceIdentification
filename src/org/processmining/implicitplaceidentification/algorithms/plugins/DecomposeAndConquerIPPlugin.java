package org.processmining.implicitplaceidentification.algorithms.plugins;

import org.deckfour.xes.model.XLog;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.implicitplaceidentification.algorithms.DecomposeAndConquerImplicitPlaceFinder;
import org.processmining.implicitplaceidentification.algorithms.FindMode;
import org.processmining.implicitplaceidentification.algorithms.util.PetriNetCopier;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.finalmarkingprovider.MarkingEditorPanel;

@Plugin(name = "AAIP-Finder: Decompose and Conquer",
        parameterLabels = {"Petri net", "Initial marking", "Event Log", "Accepting Petri net"},
        returnLabels = {"Petri net"},
        returnTypes = {Petrinet.class},
        userAccessible = true,
        categories = {},
        keywords = {"implicit places", "redundant places", "postprocessing"},
        help = "Colors places in the Petri net that are implicit.")
public class DecomposeAndConquerIPPlugin {

    @PluginVariant(variantLabel = "IP-Finder: Decompose and Conquer, Replay-based", requiredParameterLabels = {0, 2})
    @UITopiaVariant(affiliation = "PADS Student", author = "Tobias Wirtz", email = "tobias.wirtz@rwth-aachen.de")
    public Petrinet color(UIPluginContext context, Petrinet net, XLog log) {
        Marking initialMarking = chooseInitialMarking(context, net);
        return colorWithLog(net, log, initialMarking);
    }

    @PluginVariant(variantLabel = "IP-Finder: Decompose and Conquer, ILP-based, marking chooser",
            requiredParameterLabels = {0})
    @UITopiaVariant(affiliation = "PADS Student", author = "Tobias Wirtz", email = "tobias.wirtz@rwth-aachen.de")
    public Petrinet color(UIPluginContext context, Petrinet net) {
        Marking initialMarking = chooseInitialMarking(context, net);
        return colorILPbased(net, initialMarking);
    }

    @PluginVariant(variantLabel = "IP-Finder: Decompose and Conquer, apn, Replay-based", requiredParameterLabels = {3
            , 2})
    @UITopiaVariant(affiliation = "PADS Student", author = "Tobias Wirtz", email = "tobias.wirtz@rwth-aachen.de")
    public Petrinet color(UIPluginContext context, AcceptingPetriNet apn, XLog log) {
        return colorWithLog(apn.getNet(), log, apn.getInitialMarking());
    }

    @PluginVariant(variantLabel = "IP-Finder: Decompose and Conquer, apn, ILP-based", requiredParameterLabels = {3})
    @UITopiaVariant(affiliation = "PADS Student", author = "Tobias Wirtz", email = "tobias.wirtz@rwth-aachen.de")
    public Petrinet color(UIPluginContext context, AcceptingPetriNet apn) {
        return colorILPbased(apn.getNet(), apn.getInitialMarking());
    }

    private static Marking chooseInitialMarking(UIPluginContext context, Petrinet net) {
        MarkingEditorPanel editor = new MarkingEditorPanel("Initial Marking");
        Marking initialMarking = editor.getMarking(context, net);

        if (initialMarking == null) {
            initialMarking = new Marking();
        }
        return initialMarking;
    }

    private static Petrinet colorWithLog(Petrinet net, XLog log, Marking initialMarking) {
        DecomposeAndConquerImplicitPlaceFinder finder = new DecomposeAndConquerImplicitPlaceFinder(net,
                initialMarking, log, FindMode.FIND_ALL_POTENTIAL_IPS);

        PetriNetCopier copier = new PetriNetCopier(net, net.getLabel() + "colored IPs");
        return copier.colorPlaces(finder.findImplicitMinimalRegionsMaybeMore()).getDeepCopy();
    }

    private static Petrinet colorILPbased(Petrinet net, Marking initialMarking) {
        DecomposeAndConquerImplicitPlaceFinder finder = new DecomposeAndConquerImplicitPlaceFinder(net,
                initialMarking, FindMode.FIND_ALL_POTENTIAL_IPS);

        PetriNetCopier copier = new PetriNetCopier(net, net.getLabel() + "colored IPs");
        return copier.colorPlaces(finder.findImplicitMinimalRegionsMaybeMore()).getDeepCopy();
    }
}
