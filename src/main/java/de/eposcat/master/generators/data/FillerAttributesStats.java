package de.eposcat.master.generators.data;

/**
 * Holds Parameters for creation of filler data for performance tests
 */
public class FillerAttributesStats {
    int numberOfStartAttributes;
    int meanNumberOfAttributes;
    int maxNumberOfAttributes;

    /**
     * Holds Parameters for creation of filler data for performance tests
     *
     * @param numberOfStartAttributes Total number of different attributes that are available and will be randomly used
     * @param meanNumberOfAttributes Mean number of attributes a page should have,
     *                               distribution of attributes is based on 'normal distribution'
     * @param maxNumberOfAttributes The maximum number of attributes a page can have
     */
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
