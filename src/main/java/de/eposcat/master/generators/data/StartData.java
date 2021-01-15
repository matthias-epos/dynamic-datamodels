package de.eposcat.master.generators.data;

import de.eposcat.master.model.Page;

import java.util.ArrayList;
import java.util.List;

public class StartData {
    public String[] entityNames;
    public String[] attributeNames;
    public List<Page> pages;

    public StartData(String[] entityNames, String[] attributeNames, List<Page> pages) {
        this.entityNames = entityNames;
        this.attributeNames = attributeNames;
        this.pages = pages;
    }
}
