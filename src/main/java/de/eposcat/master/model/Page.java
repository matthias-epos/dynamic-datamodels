package de.eposcat.master.model;

import java.util.HashMap;
import java.util.Map;

public class Page {
    private long id;
    private String typeName;
    private Map<String, Attribute> attributes;
    
    private Page() {
        id = -1;
        attributes = new HashMap<String, Attribute>();
    }
    
    public Page(String pageName) {
        this();
        
        typeName = pageName;
    }
    
    public Page(long id, String typeName) {
        this();
        this.id = id;
        this.typeName = typeName;
    }

    public long getId() {
        return id;
    }

    
    public String getTypeName() {
        return typeName;
    }

    
    public void setAttributes(Map<String, Attribute> attributes) {
        this.attributes = attributes;
    }

    public void addAttribute(String name, Attribute attribute) {
        attributes.put(name, attribute);
    }
    
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    
    public Map<String, Attribute> getAttributes() {
        return attributes;
    }
    
    @Override
    public String toString() {
        String pageString = "Page #" + id;
        pageString += "\n Type: " + typeName;
        for(String attributeName: attributes.keySet()) {
            pageString += "\n" + attributeName;
            pageString += ": " + attributes.get(attributeName).getValue();
        }
        
        return pageString;
    }
    
}
