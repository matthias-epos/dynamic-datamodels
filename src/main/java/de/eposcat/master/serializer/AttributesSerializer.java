package de.eposcat.master.serializer;

import java.lang.reflect.Type;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import de.eposcat.master.model.Attribute;

public class AttributesSerializer implements JsonSerializer<Map<String, Attribute>> {

    @Override
    public JsonElement serialize(Map<String, Attribute> src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray base = new JsonArray();

        for (String key : src.keySet()) {
            JsonObject attribute = new JsonObject();
            attribute.addProperty("name", key);

            Attribute attr = src.get(key);

            JsonArray valuesArray = new JsonArray();
            JsonObject value = new JsonObject();
            value.addProperty(attr.getType().toString(), attr.getValue().toString());

            valuesArray.add(value);

            attribute.add("values", valuesArray);

            base.add(attribute);
        }

        return base;
    }
}
