package org.processmining.implicitplaceidentification.algorithms.plugins;

import org.processmining.implicitplaceidentification.algorithms.FindMode;
import org.processmining.implicitplaceidentification.algorithms.StructureBasedImplicitPlaceFinder;
import org.processmining.implicitplaceidentification.algorithms.util.PetriNetCopier;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.finalmarkingprovider.MarkingEditorPanel;

@Plugin(name = "IP-Finder: ILP-based",
        parameterLabels = {"Petri net", "Initial marking", "Accepting Petri net"},
        returnLabels = {"Petri net"},
        returnTypes = {Petrinet.class},
        userAccessible = true,
        categories = {},
        keywords = {"implicit places", "redundant places", "postprocessing"},
        help = "")
public class ILPBasedImplicitPlaceColorerPlugin {

    @PluginVariant(variantLabel = "IP-Finder: ILP-based, apn", requiredParameterLabels = {2})
    @UITopiaVariant(affiliation = "PADS Student", author = "Tobias Wirtz", email = "tobias.wirtz@rwth-aachen.de")
    public Petrinet colorAcceptingPetrinet(UIPluginContext context, AcceptingPetriNet acceptingPN) {

        StructureBasedImplicitPlaceFinder ipFinder = new StructureBasedImplicitPlaceFinder(acceptingPN.getNet(),
                acceptingPN.getInitialMarking(), FindMode.FIND_ALL_POTENTIAL_IPS);

        PetriNetCopier copier = new PetriNetCopier(acceptingPN.getNet(), acceptingPN.getNet().getLabel() + " with " +
                "colored IPs");
        return copier.colorPlaces(ipFinder.find()).getDeepCopy();
    }

    @PluginVariant(variantLabel = "IP-Finder: ILP-based", requiredParameterLabels = {0, 1})
    @UITopiaVariant(affiliation = "PADS Student", author = "Tobias Wirtz", email = "tobias.wirtz@rwth-aachen.de")
    public Petrinet color(UIPluginContext context, Petrinet net, Marking initialMarking) {

        StructureBasedImplicitPlaceFinder ipFinder = new StructureBasedImplicitPlaceFinder(net, initialMarking,
                FindMode.FIND_ALL_POTENTIAL_IPS);

        PetriNetCopier copier = new PetriNetCopier(net, net.getLabel() + " with colored IPs");
        return copier.colorPlaces(ipFinder.find()).getDeepCopy();
    }


    @PluginVariant(variantLabel = "IP-Finder: ILP-based, marking chooser", requiredParameterLabels =
            {0})
    @UITopiaVariant(affiliation = "PADS Student", author = "Tobias Wirtz", email = "tobias.wirtz@rwth-aachen.de")
    public Petrinet colorWithMarkingChooser(UIPluginContext context, Petrinet net) {
        Marking initialMarking = chooseInitialMarking(context, net);

        return color(context, net, initialMarking);
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
