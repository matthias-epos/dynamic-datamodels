package de.eposcat.master.model;

import java.util.HashMap;
import java.util.Map;

public class Page {
    private long id;
    //TODO make typeName mandatory
    private String typeName;
    private Map<String, Attribute> attributes;
    
    private Page() {
        id = -1;
        attributes = new HashMap<>();
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
        StringBuilder pageString = new StringBuilder("Page #" + id);
        pageString.append("\n Type: ").append(typeName);
        for(String attributeName: attributes.keySet()) {
            pageString.append("\n").append(attributeName);
            pageString.append(": ").append(attributes.get(attributeName).getValue());
        }
        
        return pageString.toString();
    }
    
}
