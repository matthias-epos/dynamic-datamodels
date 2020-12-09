package de.eposcat.master.generators.data;

public class Change {
    String entity;
    ChangeAction action;
    String attribute;
    String value;

    public Change(String entity, ChangeAction action, String attribute, String value) {
        this.entity = entity;
        this.action = action;
        this.attribute = attribute;
        this.value = value;
    }

    public String getEntity() {
        return entity;
    }

    public ChangeAction getAction() {
        return action;
    }

    public String getAttribute() {
        return attribute;
    }

    public String getValue() {
        return value;
    }
}
