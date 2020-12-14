package de.eposcat.master.generators.data;

public class FillerAttributesStats {
    int numberOfStartAttributes;
    int meanNumberOfAttributes;
    int maxNumberOfAttributes;

    public FillerAttributesStats(int numberOfStartAttributes, int meanNumberOfAttributes, int maxNumberOfAttributes) {
        this.numberOfStartAttributes = numberOfStartAttributes;
        this.meanNumberOfAttributes = meanNumberOfAttributes;
        this.maxNumberOfAttributes = maxNumberOfAttributes;
    }

    public int getNumberOfStartAttributes() {
        return numberOfStartAttributes;
    }

    public int getMeanNumberOfAttributes() {
        return meanNumberOfAttributes;
    }

    public int getMaxNumberOfAttributes() {
        return maxNumberOfAttributes;
    }
}
