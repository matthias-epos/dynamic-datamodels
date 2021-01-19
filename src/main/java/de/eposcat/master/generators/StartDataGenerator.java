package de.eposcat.master.generators;

import de.eposcat.master.generators.data.FillerAttributesStats;
import de.eposcat.master.generators.data.PerformanceTestAttribute;
import de.eposcat.master.generators.data.StartData;
import de.eposcat.master.model.Attribute;
import de.eposcat.master.model.AttributeBuilder;
import de.eposcat.master.model.AttributeType;
import de.eposcat.master.model.Page;

import java.util.*;

public class StartDataGenerator {
    private final Random random;

    public StartDataGenerator(int seed) {
        random = new Random(seed);
    }

    /**
     *
     * @param numberOfStartEntities number of entities that should be created
     * @param stats data for 'filler' attributes which have random names/values to create a good realistic environment for testing
     * @param perfAttributes list of certain attributes with certain names and values which will be used as query parameters
     * @return a StartData object containing the names of the created entities and attributes and all generated page objects.
     *          The page objects must be added to the database manually.
     */
    public StartData generateStartData(int numberOfStartEntities, FillerAttributesStats stats, List<PerformanceTestAttribute> perfAttributes) {
        String[] entityNames = new String[numberOfStartEntities];
        String[] attributeNames = new String[stats.getNumberOfStartAttributes()];

        fillWithRandomNames(entityNames);
        fillWithRandomNames(attributeNames);

        ArrayList<Page> pages = new ArrayList<>();

        for (String entityName : entityNames) {
            Page page = new Page(entityName);

            //Generate filler Attributes
            int amountOfAttributes = numberOfAttributes(stats.getMeanNumberOfAttributes(), stats.getMaxNumberOfAttributes());

            for (int j = 0; j < amountOfAttributes; j++) {
                page.addAttribute(attributeNames[random.nextInt(stats.getNumberOfStartAttributes())],
                        new AttributeBuilder().setValue(getRandomEntityName())
                                .setType(AttributeType.values()[random.nextInt(AttributeType.values().length)])
                                .createAttribute());
            }

            //Generate Performance Test Attributes (Fixed names, certain probabilities, certain values)
            addPerformanceAttributes(perfAttributes, page);

            pages.add(page);
        }

        return new StartData(entityNames, attributeNames, pages);
    }

    public void addPerformanceAttributes(List<PerformanceTestAttribute> perfAttributes, Page page) {
        for (PerformanceTestAttribute testAttribute : perfAttributes) {
            if (random.nextDouble() < testAttribute.getOccurrencePercentage() / 100) {
                page.addAttribute(testAttribute.getAttributeName(), new Attribute(AttributeType.String, testAttribute.getValueGenerator().get()));
            }
        }
    }

    public void addPerformanceAttributesToPages(List<PerformanceTestAttribute> perfAttributes, Collection<Page> pages) {
        for (Page page : pages) {
            addPerformanceAttributes(perfAttributes, page);
        }
    }

    private void fillWithRandomNames(String[] entityNames) {
        for (int i = 0; i < entityNames.length; i++) {
            entityNames[i] = getRandomEntityName();
        }
    }

    public String getRandomEntityName() {
        byte[] token = new byte[16];
        random.nextBytes(token);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    public int numberOfAttributes(int mean, int max) {
        double number;

        do {
            number = random.nextGaussian() * 2 + mean;
        } while (number < 0 || number > max);

        return (int) Math.round(number);
    }

}
