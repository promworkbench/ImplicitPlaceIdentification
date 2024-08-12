package org.processmining.implicitplaceidentification.algorithms.plugins;

import org.processmining.implicitplaceidentification.algorithms.LanguageBasedImplicitPlaceFinder;
import org.processmining.implicitplaceidentification.algorithms.util.PetriNetCopier;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.finalmarkingprovider.MarkingEditorPanel;

@Plugin(name = "IP-Finder: Language based",
        parameterLabels = {"Petri net", "Initial marking", "Accepting Petri net"},
        returnLabels = {"Petri net"},
        returnTypes = {Petrinet.class},
        userAccessible = true,
        categories = {},
        keywords = {"implicit places", "redundant places", "postprocessing"},
        help = "Colors implicit places in the Petri net by generating an approximation of the language. May have " +
                "unfeasibly high runtime and memory usage!")


public class LanguageBasedImplicitPlaceFinderPlugin {
    @PluginVariant(variantLabel = "IP-Finder: Language based, apn", requiredParameterLabels = {2})
    @UITopiaVariant(affiliation = "PADS Student", author = "Tobias Wirtz", email = "tobias.wirtz@rwth-aachen.de")
    public Petrinet colorAPN(UIPluginContext context, AcceptingPetriNet apn) {

        LanguageBasedImplicitPlaceFinder ipFinder = new LanguageBasedImplicitPlaceFinder(apn.getNet(),
                apn.getInitialMarking());

        PetriNetCopier copier = new PetriNetCopier(apn.getNet(), apn.getNet().getLabel() + " with " +
                "colored IPs");
        return copier.colorPlaces(ipFinder.find(false)).getDeepCopy();
    }

    @PluginVariant(variantLabel = "IP-Finder: Language based, initial marking", requiredParameterLabels = {0, 1})
    @UITopiaVariant(affiliation = "PADS Student", author = "Tobias Wirtz", email = "tobias.wirtz@rwth-aachen.de")
    public Petrinet color(UIPluginContext context, Petrinet net, Marking initialMarking) {

        LanguageBasedImplicitPlaceFinder ipFinder = new LanguageBasedImplicitPlaceFinder(net, initialMarking);

        PetriNetCopier copier = new PetriNetCopier(net, net.getLabel() + " with " +
                "colored IPs");
        return copier.colorPlaces(ipFinder.find(false)).getDeepCopy();
    }

    @PluginVariant(variantLabel = "IP-Finder: Language based, marking chooser", requiredParameterLabels = {0})
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
