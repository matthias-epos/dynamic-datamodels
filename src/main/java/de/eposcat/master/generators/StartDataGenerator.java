package de.eposcat.master.generators;

import de.eposcat.master.generators.data.StartData;
import de.eposcat.master.model.AttributeBuilder;
import de.eposcat.master.model.AttributeType;
import de.eposcat.master.model.Page;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Random;

public class StartDataGenerator {
    private final Random random;

    public StartDataGenerator(int seed) {
        random = new Random(seed);
    }

    public StartData generateData(int numberOfStartEntities, int numberOfStartAttributes, int meanNumberOfAttributes, int maxNumberOfAttributes) {
        String[] entityNames = new String[numberOfStartEntities];
        String[] attributeNames = new String[numberOfStartAttributes];

        fillWithRandomNames(entityNames);
        fillWithRandomNames(attributeNames);

        ArrayList<Page> pages = new ArrayList<>();

        for (String entityName : entityNames) {
            Page page = new Page(entityName);
            int amountOfAttributes = numberOfAttributes(meanNumberOfAttributes, maxNumberOfAttributes);

            for (int j = 0; j < amountOfAttributes; j++) {
                page.addAttribute(attributeNames[random.nextInt(numberOfStartAttributes)],
                        new AttributeBuilder().setValue(getRandomEntityName())
                                .setType(AttributeType.values()[random.nextInt(AttributeType.values().length)])
                                .createAttribute());
            }

            pages.add(page);
        }


//        for(Page page : pages){
//            System.out.println(page.toString());
//        }

        return new StartData(entityNames, attributeNames, pages);
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
        double number = random.nextGaussian() * 3 + mean;
        number = Math.max(number, 0);
        number = Math.min(number, max);
        return (int) Math.round(number);
    }

}
