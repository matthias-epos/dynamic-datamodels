package de.eposcat.master.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Page {
    private final long id;
    //TODO make typeName mandatory
    private String typeName;
    private Map<String, Attribute> attributes;
    
    private Page() {
        id = -1;
        attributes = new HashMap<>();
    }
    
    public Page(String typeName) {
        this();
        
        this.typeName = typeName;
    }
    
    public Page(long id, String typeName) {
        this.attributes = new HashMap<>();
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

    public Attribute getAttribute(String attributeName) {
        return attributes.get(attributeName);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Page page = (Page) o;
        return id == page.id &&
                typeName.equals(page.typeName) &&
                Objects.equals(attributes, page.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, typeName, attributes);
    }
}
