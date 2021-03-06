package de.eposcat.master.generators;

import com.google.gson.Gson;
import de.eposcat.master.generators.data.Change;
import de.eposcat.master.generators.data.ChangeAction;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

public class ChangesGenerator {
    String[] startEntityNames;
    String[] startAttributeNames;

    Random random;

    Gson gson;
    String targetPath;

    /**
     * Class used to generate a list of changes and save them in a textfile. Use {@link de.eposcat.master.generators.ChangeRunner} to execute the changes.
     *
     * @param startEntityNames  unique text identifiers of entities in the database
     * @param startAttributeNames   name of filler attribute names
     * @param targetPath    path where to save the textfile
     * @param seed  RNG-seed
     */
    public ChangesGenerator(String[] startEntityNames, String[] startAttributeNames, String targetPath, int seed) {
        this.startEntityNames = startEntityNames;
        this.startAttributeNames = startAttributeNames;
        this.targetPath = targetPath;

        random = new Random(seed);
        gson = new Gson();
    }

    public void generateChangeSets(int numberOfAttributeChanges) throws IOException {
        FileWriter writer = new FileWriter(Paths.get(targetPath).toFile(), false);

        for(int i=0;i<=numberOfAttributeChanges; i++){
            Change change;
            String entityName = getRandomEntry(startEntityNames);

            double actionRoll = random.nextDouble();

            if(actionRoll > 0.95){
                change = new Change(entityName, ChangeAction.REMOVE, "", "");
            } else if(actionRoll > 0.7){
                String attr;

                if(random.nextDouble() > 0.8){
                    attr = getRandomEntry(startAttributeNames);
                } else {
                    attr = "newAtt" + entityName;
                }

                change = new Change(entityName, ChangeAction.ADD, attr, "new" + getRandomAttributeValue());
            } else {
                change = new Change(entityName, ChangeAction.CHANGE, "", "chg" + getRandomAttributeValue());
            }

            writer.write(gson.toJson(change) + System.lineSeparator());
        }

        writer.flush();
    }

    public String getRandomEntry(String[] array){
        return array[random.nextInt(array.length)];
    }

    public String getRandomAttributeValue(){
        return random.nextInt(1000) + "val";
    }

}
