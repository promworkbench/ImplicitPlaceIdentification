package org.processmining.implicitplaceidentification.dialogs;

import com.fluxicon.slickerbox.components.NiceSlider;
import com.fluxicon.slickerbox.components.NiceSlider.Orientation;
import com.fluxicon.slickerbox.factory.SlickerFactory;
import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.util.ui.widgets.ProMComboBox;
import org.processmining.framework.util.ui.widgets.ProMList;
import org.processmining.implicitplaceidentification.algorithms.FindMode;
import org.processmining.implicitplaceidentification.models.YourFirstInput;
import org.processmining.implicitplaceidentification.models.YourSecondInput;
import org.processmining.implicitplaceidentification.parameters.IPFinderParams;
import org.processmining.implicitplaceidentification.parameters.YourParameters;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Anforderungen:
 * - JBox zur variant selection: (Replay-, Structure-, Language-based)
 * - color / remove
 * - list preconditions for input for variants
 */
public class VariantSelectionDialog extends JPanel {

    /**
     *
     */
    private static final long serialVersionUID = -60087716353524468L;

    /**
     * The JPanel that allows the user to set (a subset of) the parameters.
     */
    public VariantSelectionDialog(UIPluginContext context, final IPFinderParams parameters) {
        double size[][] = {{0.5, 0.5}, {150, 60, 60, 60}};
        setLayout(new TableLayout(size));
        Set<String> values = new HashSet<String>();
        values.add("Find all implicit places");
        values.add("Find a removable set of implicit places");

        DefaultListModel<String> listModel = new DefaultListModel<String>();
        for (String value : values) {
            listModel.addElement(value);
        }
        final ProMList<String> list = new ProMList<String>("Select option", listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        final String defaultValue = "Find all implicit places";
        list.setSelection(defaultValue);
        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                List<String> selected = list.getSelectedValuesList();
                if (selected.size() == 1) {
                    switch (selected.get(0)) {
                        case "Find all implicit places":
                            parameters.setFindMode(FindMode.FIND_ALL_POTENTIAL_IPS);
                            break;
                        case "Find a removable set of implicit places":
                                parameters.setFindMode(FindMode.GREEDY);
                                break;
                        default:
                            parameters.setFindMode(FindMode.FIND_ALL_POTENTIAL_IPS);
                    }
                } else {
                    /*
                     * Nothing selected. Revert to selection of default classifier.
                     */
                    list.setSelection(defaultValue);
                    parameters.setFindMode(FindMode.FIND_ALL_POTENTIAL_IPS);
                }
            }
        });
        list.setPreferredSize(new Dimension(100, 30));
        add(list, "0, 0");

/*
        DefaultComboBoxModel<String> comboBoxModel = new DefaultComboBoxModel<>();
        comboBoxModel.addElement("Structure based method");
        comboBoxModel.addElement("Replay based method");
        ProMComboBox<String> variantComboBox = new ProMComboBox<>(comboBoxModel);
        variantComboBox.setPreferredSize(new Dimension(100, 70));
        add(variantComboBox, "1, 1");
 */

/*
        final JCheckBox checkBox = SlickerFactory.instance().createCheckBox("Remove found places", false);
        checkBox.setSelected(parameters.isRemovalFlag());
        checkBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                parameters.setRemovalFlag(checkBox.isSelected());
            }

        });
        checkBox.setOpaque(false);
        checkBox.setPreferredSize(new Dimension(100, 30));
        add(checkBox, "0, 1");
 */
    }
}
