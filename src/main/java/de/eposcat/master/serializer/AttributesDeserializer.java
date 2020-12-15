package de.eposcat.master.serializer;

import com.google.gson.*;
import de.eposcat.master.model.Attribute;
import de.eposcat.master.model.AttributeType;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class AttributesDeserializer implements JsonDeserializer<Map<String, Attribute>> {
    @Override
    public Map<String, Attribute> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Map<String, Attribute> attributes = new HashMap<>();

        json.getAsJsonArray().forEach(attributeElement -> {
            JsonObject attribute = attributeElement.getAsJsonObject();
            String key = attribute.get("name").getAsString();

            JsonObject attributeObj = attribute.getAsJsonArray("values").get(0).getAsJsonObject();
            attributeObj.entrySet().forEach( entry ->
                    attributes.put(key, new Attribute(AttributeType.valueOf(entry.getKey()), entry.getValue().getAsString())));
                });

        return attributes;
    }
}
