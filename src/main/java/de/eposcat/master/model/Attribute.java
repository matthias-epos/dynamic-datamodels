package de.eposcat.master.model;


public class Attribute {
    // Only used in eav approach
    private transient long id;
    private AttributeType type;
    private Object value;

    private Attribute() {
        this.id = -1;
    }

    public Attribute(AttributeType type, Object value) {
        this();
        this.type = type;
        this.value = value;
    }

    public Attribute(long id, AttributeType type, Object value) {
        this();
        this.id = id;
        this.type = type;
        this.value = value;
    }

    public AttributeType getType() {
        return type;
    }

    public void setType(AttributeType type) {
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public long getId() {
        return id;
    }

}
