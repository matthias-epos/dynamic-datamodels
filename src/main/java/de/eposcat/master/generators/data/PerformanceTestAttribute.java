package de.eposcat.master.generators.data;

import java.util.function.Function;
import java.util.function.Supplier;

public class PerformanceTestAttribute {
    String attributeName;
    double occurrencePercentage;
    Supplier<String> valueGenerator;

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
