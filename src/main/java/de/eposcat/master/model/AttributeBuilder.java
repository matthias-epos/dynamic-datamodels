package de.eposcat.master.model;

public class AttributeBuilder {
    private AttributeType type;
    private Object value;
    private long id = -1;

    public AttributeBuilder setType(AttributeType type) {
        this.type = type;
        return this;
    }

    public AttributeBuilder setValue(Object value) {
        this.value = value;
        return this;
    }

    public Attribute createAttribute() {
        return new Attribute(id, type, value);
    }

    public AttributeBuilder setId(long attributeId) {
        if (attributeId == -1) {
            throw new IllegalArgumentException();
        }

        this.id = attributeId;
        return this;
    }
}