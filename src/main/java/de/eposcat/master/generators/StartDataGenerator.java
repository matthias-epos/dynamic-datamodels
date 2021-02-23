package de.eposcat.master.generators;

import de.eposcat.master.approachImpl.IDatabaseAdapter;
import de.eposcat.master.generators.data.FillerAttributesStats;
import de.eposcat.master.generators.data.PerformanceTestAttribute;
import de.eposcat.master.generators.data.StartData;
import de.eposcat.master.model.Attribute;
import de.eposcat.master.model.AttributeBuilder;
import de.eposcat.master.model.AttributeType;
import de.eposcat.master.model.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;

public class StartDataGenerator {
    private final Random random;

    private static final Logger log = LoggerFactory.getLogger(StartDataGenerator.class);

    public StartDataGenerator(int seed) {
        random = new Random(seed);
    }

    /**
     *
     * @param numberOfStartEntities number of entities that should be created
     * @param stats data for 'filler' attributes which have random names/values to create a good realistic environment for testing
     * @param perfAttributes list of certain attributes with certain names and values which will be used as query parameters
     * @param emptyDatabases
     * @return a StartData object containing the names of the created entities and attributes and all generated page objects.
     *          The page objects must be added to the database manually.
     */
    public void generateStartData(int numberOfStartEntities, FillerAttributesStats stats, List<PerformanceTestAttribute> perfAttributes, List<IDatabaseAdapter> emptyDatabases) {
        String[] attributeNames = new String[stats.getNumberOfStartAttributes()];
        fillWithRandomNames(attributeNames);

        for (int i = 0; i<numberOfStartEntities;i++) {
            Page page = new Page(getRandomEntityName());

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

            for(IDatabaseAdapter adapter : emptyDatabases){
                try {
                    adapter.createPageWithAttributes(page.getTypeName(), page.getAttributes());
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }

            if(i % (numberOfStartEntities / 100f) == 0){
                log.info("%%%");
                log.info("Progress: {}", i / (float) numberOfStartEntities);
                log.info("%%%");
            }
        }
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
