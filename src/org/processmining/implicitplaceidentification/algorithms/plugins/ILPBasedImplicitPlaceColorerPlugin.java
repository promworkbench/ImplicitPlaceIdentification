package org.processmining.implicitplaceidentification.algorithms.plugins;

import org.deckfour.uitopia.api.event.TaskListener;
import org.processmining.implicitplaceidentification.algorithms.FindMode;
import org.processmining.implicitplaceidentification.algorithms.StructureBasedImplicitPlaceFinder;
import org.processmining.implicitplaceidentification.algorithms.util.PetriNetCopier;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.implicitplaceidentification.dialogs.VariantSelectionDialog;
import org.processmining.implicitplaceidentification.parameters.IPFinderParams;
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
        Petrinet net = acceptingPN.getNet();
        Marking initialMarking = acceptingPN.getInitialMarking();

        return color(context, net, initialMarking);
    }

    @PluginVariant(variantLabel = "IP-Finder: ILP-based", requiredParameterLabels = {0, 1})
    @UITopiaVariant(affiliation = "PADS Student", author = "Tobias Wirtz", email = "tobias.wirtz@rwth-aachen.de")
    public Petrinet color(UIPluginContext context, Petrinet net, Marking initialMarking) {
        FindMode findmode = chooseFindmode(context);

        StructureBasedImplicitPlaceFinder ipFinder = new StructureBasedImplicitPlaceFinder(net, initialMarking,
                findmode);

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


    private static FindMode chooseFindmode(UIPluginContext context) {
        // Get the default parameters.
        IPFinderParams parameters = new IPFinderParams(false, FindMode.FIND_ALL_POTENTIAL_IPS);
        // Get a dialog for this parameters.
        VariantSelectionDialog dialog = new VariantSelectionDialog(context, parameters);
        // Show the dialog. User can now change the parameters.
        TaskListener.InteractionResult result = context.showWizard("Variant Selection", true, true, dialog);
        // User has close the dialog.
        if (result == TaskListener.InteractionResult.FINISHED) {
            // Apply the algorithm depending on whether a connection already exists.
            return parameters.getFindMode();
        }
        // Dialog got canceled.
        return null;
    }
}
