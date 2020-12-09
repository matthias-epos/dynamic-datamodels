package de.eposcat.master.generators.data;

import de.eposcat.master.model.Page;

import java.util.ArrayList;

public class StartData {
    public String[] entityNames;
    public String[] attributeNames;
    public ArrayList<Page> pages;

    public StartData(String[] entityNames, String[] attributeNames, ArrayList<Page> pages) {
        this.entityNames = entityNames;
        this.attributeNames = attributeNames;
        this.pages = pages;
    }
}
