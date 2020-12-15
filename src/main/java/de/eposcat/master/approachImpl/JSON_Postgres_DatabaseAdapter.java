package de.eposcat.master.approachImpl;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import de.eposcat.master.connection.AbstractConnectionManager;
import de.eposcat.master.exceptions.BlException;
import de.eposcat.master.model.Attribute;
import de.eposcat.master.model.Page;
import de.eposcat.master.serializer.AttributesDeserializer;
import de.eposcat.master.serializer.AttributesSerializer;


public class JSON_Postgres_DatabaseAdapter implements IDatabaseAdapter {

    private final Connection conn;
    private final Gson gson;
    private final Type attributeType = new TypeToken<Map<String, Attribute>>() {}.getType();

    public JSON_Postgres_DatabaseAdapter(AbstractConnectionManager connectionManager) {
        this.conn = connectionManager.getConnection();
        GsonBuilder builder = new GsonBuilder();

        builder.registerTypeAdapter(attributeType, new AttributesSerializer());
        builder.registerTypeAdapter(attributeType, new AttributesDeserializer());
        gson = builder.create();
    }

    @Override
    public Page createPage(String typename) throws SQLException {
        return createPageWithAttributes(typename, new HashMap<>());
    }

    @Override
    public Page createPageWithAttributes(String typename, Map<String, Attribute> attributes) throws SQLException{
        if(typename == null || typename.isEmpty() || attributes == null){
            throw new IllegalArgumentException();
        }

        PreparedStatement stCreatePage = conn.prepareStatement("INSERT INTO pages(type, attributes) VALUES (?,?::json)",
                Statement.RETURN_GENERATED_KEYS);
        stCreatePage.setString(1, typename);
        stCreatePage.setObject(2, mapToJSON(attributes));

        int affectedRows = stCreatePage.executeUpdate();

        if (affectedRows > 0) {
            ResultSet key = stCreatePage.getGeneratedKeys();
            key.next();

            Page pageWithId = new Page(key.getInt(1), typename);
            pageWithId.setAttributes(attributes);
            return pageWithId;
        }

        return null;
    }

    @Override
    public void updatePage(Page page) throws SQLException {
        if(page == null){
            throw new IllegalArgumentException("page must not be null");
        }

        PreparedStatement stUpdatePage = conn.prepareStatement("UPDATE pages SET type = ?, attributes = ?::json WHERE id = ?");
        stUpdatePage.setString(1, page.getTypeName());
        stUpdatePage.setString(2, mapToJSON(page.getAttributes()));
        stUpdatePage.setLong(3, page.getId());
        int affectedRows = stUpdatePage.executeUpdate();

        if(affectedRows != 1) {
            throw new BlException("Page with id= "+ page.getId()+", type= " + page.getTypeName()+" is not tracked by database, try create pageWithAttributes first");
        }
    }

    @Override
    public Page loadPage(long pageId) throws SQLException {
        PreparedStatement stLoadPage = conn.prepareStatement("SELECT * FROM pages WHERE id = ?");
        stLoadPage.setLong(1, pageId);

        ResultSet rsLoadPage = stLoadPage.executeQuery();

        if (rsLoadPage.next()) {
            Page page = new Page(rsLoadPage.getInt("id"), rsLoadPage.getString("type"));

            page.setAttributes(jsonToMap(rsLoadPage.getString("attributes")));

            return page;
        } else {
            return null;
        }

    }

    @Override
    public List<Page> findPagesByType(String type) throws SQLException {
        if(type == null || type.isEmpty()){
            throw new IllegalArgumentException();
        }

        PreparedStatement stFindByType = conn.prepareStatement("SELECT * FROM pages WHERE type = ?");
        stFindByType.setString(1,type);

        ResultSet rs = stFindByType.executeQuery();
        List<Page> pages = new ArrayList<>();

        while(rs.next()){
            long id = rs.getInt("ID");
            String resultType = rs.getString("type");
            String attributesJSON = rs.getString("attributes");

            Page page = new Page(id, resultType);
            page.setAttributes(jsonToMap(attributesJSON));
            pages.add(page);
        }

        return pages;
    }

    @Override
    public List<Page> findPagesByAttributeName(String attributeName) throws SQLException {
        if(attributeName == null){
            throw new IllegalArgumentException();
        }

        JsonArray jsonArray = getAttributeArray(attributeName, null);

        PreparedStatement stFindByAttribute = conn.prepareStatement("SELECT * FROM pages WHERE attributes::jsonb @> ?::jsonb");
        stFindByAttribute.setString(1, gson.toJson(jsonArray));

        ResultSet rsFindPagesByAttribute = stFindByAttribute.executeQuery();
        List<Page> pages = new ArrayList<>();

        while (rsFindPagesByAttribute.next()) {
            Page page = new Page(rsFindPagesByAttribute.getInt(1), rsFindPagesByAttribute.getString(2));
            page.setAttributes(jsonToMap(rsFindPagesByAttribute.getString(3)));
            pages.add(page);
        }

        return pages;
    }


    private JsonArray getAttributeArray(String attributeName, Attribute attribute) {
        JsonObject nameHelper = new JsonObject();
        nameHelper.addProperty("name", attributeName);

        if(attribute != null){
            JsonArray values =  new JsonArray();
            JsonObject valueObject = new JsonObject();
            valueObject.addProperty(attribute.getType().toString(), attribute.getValue().toString());
            values.add(valueObject);
            nameHelper.add("values", values);
        }

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(nameHelper);
        return jsonArray;
    }

    @Override
    public List<Page> findPagesByAttributeValue(String attributeName, Attribute value) throws SQLException {

        JsonArray jsonArray = getAttributeArray(attributeName, value);
        PreparedStatement stFindByAttributeValue = conn.prepareStatement("SELECT * FROM pages WHERE attributes::jsonb @> ?::jsonb");
        stFindByAttributeValue.setObject(1, gson.toJson(jsonArray));

        ResultSet rsFindPagesByAttributeValue = stFindByAttributeValue.executeQuery();
        List<Page> pages = new ArrayList<>();

        while (rsFindPagesByAttributeValue.next()) {
            Page page = new Page(rsFindPagesByAttributeValue.getInt(1), rsFindPagesByAttributeValue.getString(2));
            page.setAttributes(jsonToMap(rsFindPagesByAttributeValue.getString(3)));
            pages.add(page);
        }

        return pages;
    }

    private String mapToJSON(Map<String, Attribute> attributes) {
        return gson.toJson(attributes, attributeType);
    }

    private Map<String, Attribute> jsonToMap(String json) {
        return gson.fromJson(json, attributeType);
    }
}
