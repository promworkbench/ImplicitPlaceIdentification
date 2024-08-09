package org.processmining.implicitplaceidentification.plugins;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.implicitplaceidentification.algorithms.DecomposeAndConquerImplicitPlaceFinder;
import org.processmining.implicitplaceidentification.algorithms.FindMode;
import org.processmining.implicitplaceidentification.algorithms.YourAlgorithm;
import org.processmining.implicitplaceidentification.algorithms.util.PetriNetCopier;
import org.processmining.implicitplaceidentification.connections.YourConnection;
import org.processmining.implicitplaceidentification.dialogs.VariantSelectionDialog;
import org.processmining.implicitplaceidentification.dialogs.YourDialog;
import org.processmining.implicitplaceidentification.help.YourHelp;
import org.processmining.implicitplaceidentification.models.YourFirstInput;
import org.processmining.implicitplaceidentification.models.YourOutput;
import org.processmining.implicitplaceidentification.models.YourSecondInput;
import org.processmining.implicitplaceidentification.parameters.IPFinderParams;
import org.processmining.implicitplaceidentification.parameters.YourParameters;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.finalmarkingprovider.MarkingEditorPanel;

import java.util.Collection;
import java.util.Set;

@Plugin(name = "Implicit Place Identification", parameterLabels = {"Petri net", "Initial marking", "Event Log", "Accepting Petri net"},
	    returnLabels = { "Name of your output" }, returnTypes = { Petrinet.class }, help = YourHelp.TEXT)
public class ImplicitPlaceIdentificationPlugin extends YourAlgorithm {
	
	/**
	 * The plug-in variant that runs in a UI context and uses a dialog to get the parameters.
	 * 
	 * @param context The context to run in.
	 * @return The output.
	 */
	@UITopiaVariant(affiliation = "PADS Student", author = "Tobias Wirtz", email = "tobias.wirtz@rwth-aachen.de")
	@PluginVariant(variantLabel = "Your plug-in name, dialog", requiredParameterLabels = {0})
	public Petrinet runUI(UIPluginContext context, Petrinet petrinet) {
		// Get the default parameters.
	    IPFinderParams parameters = new IPFinderParams(false, FindMode.FIND_ALL_POTENTIAL_IPS);
	    // Get a dialog for this parameters.
	    VariantSelectionDialog dialog = new VariantSelectionDialog(context, parameters);
	    // Show the dialog. User can now change the parameters.
	    InteractionResult result = context.showWizard("Variant Selection", true, true, dialog);
	    // User has close the dialog.
	    if (result == InteractionResult.FINISHED) {
			// Apply the algorithm depending on whether a connection already exists.
	    	return runAlgo(context, petrinet, parameters);
	    }
	    // Dialog got canceled.
	    return null;
	}


	private Petrinet runAlgo(UIPluginContext context, Petrinet net, IPFinderParams params){
		Marking initialMarking = chooseInitialMarking(context, net);
		DecomposeAndConquerImplicitPlaceFinder finder = new DecomposeAndConquerImplicitPlaceFinder(net,
				initialMarking,params.getFindMode());

		//if(params.isRemovalFlag()){
			PetriNetCopier copier = new PetriNetCopier(net, net.getLabel()+ "removed IPs");
			return copier.colorPlaces(finder.findImplicitMinimalRegionsMaybeMore()).getDeepCopy();
		//}
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
