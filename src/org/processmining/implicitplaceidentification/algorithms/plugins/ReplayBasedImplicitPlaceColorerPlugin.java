package org.processmining.implicitplaceidentification.algorithms.plugins;

import org.deckfour.xes.model.XLog;
import org.processmining.implicitplaceidentification.algorithms.ReplayBasedImplicitPlaceFinder;
import org.processmining.implicitplaceidentification.algorithms.util.PetriNetCopier;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.finalmarkingprovider.MarkingEditorPanel;

@Plugin(name = "IP-Finder: Replay based",
        parameterLabels = {"Petri net", "Initial marking", "Event Log", "Accepting Petri net"},
        returnLabels = {"Petri net"},
        returnTypes = {Petrinet.class},
        userAccessible = true,
        categories = {},
        keywords = {"implicit places", "redundant places", "postprocessing"},
        help = "Colors implicit places using a log replay. It can only replay traces that perfectly fit the log. If " +
                "too few traces fit the log, it does not return reliable results.")
public class ReplayBasedImplicitPlaceColorerPlugin {

    @PluginVariant(variantLabel = "IP-Finder: Replay-based, apn", requiredParameterLabels = {3, 2})
    @UITopiaVariant(affiliation = "PADS Student", author = "Tobias Wirtz", email = "tobias.wirtz@rwth-aachen.de")
    public Petrinet color(UIPluginContext context, AcceptingPetriNet apn, XLog log) {

        ReplayBasedImplicitPlaceFinder ipFinder = new ReplayBasedImplicitPlaceFinder(apn.getNet(),
                apn.getInitialMarking(), log);
        PetriNetCopier copier = new PetriNetCopier(apn.getNet(), apn.getNet().getLabel() + " IP colored");
        return copier.colorPlaces(ipFinder.find()).getDeepCopy();
    }

    @PluginVariant(variantLabel = "IP-Finder: Replay-based", requiredParameterLabels = {0, 1, 2})
    @UITopiaVariant(affiliation = "PADS Student", author = "Tobias Wirtz", email = "tobias.wirtz@rwth-aachen.de")
    public Petrinet color(UIPluginContext context, Petrinet net, Marking initialMarking, XLog log) {

        ReplayBasedImplicitPlaceFinder ipFinder = new ReplayBasedImplicitPlaceFinder(net, initialMarking, log);

        PetriNetCopier copier = new PetriNetCopier(net, net.getLabel() + " IP colored");
        return copier.colorPlaces(ipFinder.find()).getDeepCopy();
    }

    @PluginVariant(variantLabel = "IP-Finder: Replay-based, marking chooser", requiredParameterLabels = {0, 2})
    @UITopiaVariant(affiliation = "PADS Student", author = "Tobias Wirtz", email = "tobias.wirtz@rwth-aachen.de")
    public Petrinet colorWithMarkingChooser(UIPluginContext context, Petrinet net, XLog log) {
        Marking initialMarking = chooseInitialMarking(context, net);

        return color(context, net, initialMarking, log);
    }

    private static Marking chooseInitialMarking(UIPluginContext context, Petrinet net) {
        MarkingEditorPanel editor = new MarkingEditorPanel("Initial Marking");
        Marking initialMarking = editor.getMarking(context, net);

        if (initialMarking == null) {
            initialMarking = new Marking();
        }
        return initialMarking;
    }

}
