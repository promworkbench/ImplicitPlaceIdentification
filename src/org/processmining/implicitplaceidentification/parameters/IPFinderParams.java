package org.processmining.implicitplaceidentification.parameters;

import org.processmining.basicutils.parameters.impl.PluginParametersImpl;
import org.processmining.implicitplaceidentification.algorithms.FindMode;
import org.processmining.implicitplaceidentification.models.YourFirstInput;
import org.processmining.implicitplaceidentification.models.YourSecondInput;

public class IPFinderParams extends PluginParametersImpl {

    private boolean removalFlag;
    private FindMode findMode;

    public IPFinderParams(boolean input1, FindMode input2) {
        super();
        setRemovalFlag(input1);
        setFindMode(input2);
    }

    public IPFinderParams (IPFinderParams parameters) {
        super(parameters);
        setRemovalFlag(parameters.isRemovalFlag());
        setFindMode(parameters.getFindMode());
    }

    public boolean equals(Object object) {
        if (object instanceof IPFinderParams) {
            IPFinderParams parameters = (IPFinderParams) object;
            return super.equals(parameters) &&
                    isRemovalFlag() == parameters.isRemovalFlag() &&
                    getFindMode() == parameters.getFindMode();
        }
        return false;
    }

    public boolean isRemovalFlag() {
        return removalFlag;
    }

    public void setRemovalFlag(boolean removalFlag) {
        this.removalFlag = removalFlag;
    }

    public FindMode getFindMode() {
        return findMode;
    }

    public void setFindMode(FindMode findMode) {
        this.findMode = findMode;
    }

    public String toString() {
        return "(Removal Flag: " + isRemovalFlag() + ", Findmode:" + getFindMode() + ")";
    }
}
