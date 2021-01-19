package de.eposcat.master.generators.data;

import java.util.function.Function;
import java.util.function.Supplier;


public class PerformanceTestAttribute {
    String attributeName;
    double occurrencePercentage;
    Supplier<String> valueGenerator;

    /**
     * Creates a PerformanceTestAttribute which holds relevant data to create attributes we can query for
     *
     * @param attributeName The name of the attribute we want to create
     * @param occurrencePercentage The percentage of pages which should get this attribute
     * @param valueGenerator A method which generates the value of the attribute. It's a lambda function which takes no parameters and returns a String.
     *                       Using RNG we can create different values for different pages.
     */
    public PerformanceTestAttribute(String attributeName, double occurrencePercentage, Supplier<String> valueGenerator) {
        this.attributeName = attributeName;
        this.occurrencePercentage = occurrencePercentage;
        this.valueGenerator = valueGenerator;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public double getOccurrencePercentage() {
        return occurrencePercentage;
    }

    public Supplier<String> getValueGenerator() {
        return valueGenerator;
    }
}
