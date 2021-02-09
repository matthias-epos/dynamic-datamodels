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
import org.apache.commons.dbutils.DbUtils;


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

        PreparedStatement stCreatePage = null;
        ResultSet key = null;

        try {
            stCreatePage = conn.prepareStatement("INSERT INTO pages(type, attributes) VALUES (?,?::json)",
                    Statement.RETURN_GENERATED_KEYS);
            stCreatePage.setString(1, typename);
            stCreatePage.setObject(2, mapToJSON(attributes));

            int affectedRows = stCreatePage.executeUpdate();

            if (affectedRows > 0) {
                key = stCreatePage.getGeneratedKeys();
                key.next();

                Page pageWithId = new Page(key.getInt(1), typename);
                pageWithId.setAttributes(attributes);
                return pageWithId;
            }

            return null;
        } finally {
            DbUtils.close(stCreatePage);
            DbUtils.close(key);
        }


    }

    @Override
    public boolean deletePage(long pageId) throws SQLException{
        if(pageId == -1){
            return false;
        }

        return deletePage(loadPage(pageId));
    }

    public boolean deletePage(Page page) throws SQLException{
        if(page == null || page.getId() == -1){
            return false;
        }

        PreparedStatement st = null;

        try {
            st = conn.prepareStatement("DELETE FROM pages WHERE ID = ?");
            st.setLong(1, page.getId());

            int affectedRows = st.executeUpdate();

            return (affectedRows > 0);
        } finally {
            DbUtils.close(st);
        }
    }

    @Override
    public void updatePage(Page page) throws SQLException {
        if(page == null){
            throw new IllegalArgumentException("page must not be null");
        }

        PreparedStatement stUpdatePage = null;

        try {
            stUpdatePage = conn.prepareStatement("UPDATE pages SET type = ?, attributes = ?::json WHERE id = ?");
            stUpdatePage.setString(1, page.getTypeName());
            stUpdatePage.setString(2, mapToJSON(page.getAttributes()));
            stUpdatePage.setLong(3, page.getId());
            int affectedRows = stUpdatePage.executeUpdate();

            if(affectedRows != 1) {
                throw new BlException("Page with id= "+ page.getId()+", type= " + page.getTypeName()+" is not tracked by database, try create pageWithAttributes first");
            }
        } finally {
            DbUtils.close(stUpdatePage);
        }
    }

    @Override
    public Page loadPage(long pageId) throws SQLException {
        PreparedStatement stLoadPage = null;
        ResultSet rsLoadPage = null;

        try {
            stLoadPage = conn.prepareStatement("SELECT * FROM pages WHERE id = ?");
            stLoadPage.setLong(1, pageId);

            rsLoadPage = stLoadPage.executeQuery();

            if (rsLoadPage.next()) {
                Page page = new Page(rsLoadPage.getInt("id"), rsLoadPage.getString("type"));

                page.setAttributes(jsonToMap(rsLoadPage.getString("attributes")));

                return page;
            } else {
                return null;
            }
        } finally {
            DbUtils.close(stLoadPage);
            DbUtils.close(rsLoadPage);
        }
    }

    @Override
    public List<Page> findPagesByType(String type) throws SQLException {
        if(type == null || type.isEmpty()){
            throw new IllegalArgumentException();
        }

        PreparedStatement stFindByType = null;
        ResultSet rs = null;

        try {
            stFindByType = conn.prepareStatement("SELECT * FROM pages WHERE type = ?");
            stFindByType.setString(1,type);

            rs = stFindByType.executeQuery();
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
        } finally {
            DbUtils.close(stFindByType);
            DbUtils.close(rs);
        }
    }

    @Override
    public List<Page> findPagesByAttributeName(String attributeName) throws SQLException {
        if(attributeName == null){
            throw new IllegalArgumentException();
        }

        PreparedStatement stFindByAttribute = null;
        ResultSet rsFindPagesByAttribute = null;

        try {
            JsonArray jsonArray = getAttributeArray(attributeName, null);

            stFindByAttribute = conn.prepareStatement("SELECT * FROM pages WHERE attributes::jsonb @> ?::jsonb");
            stFindByAttribute.setString(1, gson.toJson(jsonArray));

            rsFindPagesByAttribute = stFindByAttribute.executeQuery();
            List<Page> pages = new ArrayList<>();

            while (rsFindPagesByAttribute.next()) {
                Page page = new Page(rsFindPagesByAttribute.getInt(1), rsFindPagesByAttribute.getString(2));
                page.setAttributes(jsonToMap(rsFindPagesByAttribute.getString(3)));
                pages.add(page);
            }

            return pages;
        } finally {
            DbUtils.close(stFindByAttribute);
            DbUtils.close(rsFindPagesByAttribute);
        }
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
        if(attributeName == null){
            throw new IllegalArgumentException();
        }

        JsonArray jsonArray = getAttributeArray(attributeName, value);

        PreparedStatement stFindByAttributeValue = null;
        ResultSet rsFindPagesByAttributeValue = null;

        try {
            stFindByAttributeValue = conn.prepareStatement("SELECT * FROM pages WHERE attributes::jsonb @> ?::jsonb");
            stFindByAttributeValue.setObject(1, gson.toJson(jsonArray));

            rsFindPagesByAttributeValue = stFindByAttributeValue.executeQuery();
            List<Page> pages = new ArrayList<>();

            while (rsFindPagesByAttributeValue.next()) {
                Page page = new Page(rsFindPagesByAttributeValue.getInt(1), rsFindPagesByAttributeValue.getString(2));
                page.setAttributes(jsonToMap(rsFindPagesByAttributeValue.getString(3)));
                pages.add(page);
            }

            return pages;
        } finally {
            DbUtils.close(stFindByAttributeValue);
            DbUtils.close(rsFindPagesByAttributeValue);
        }


    }

    private String mapToJSON(Map<String, Attribute> attributes) {
        return gson.toJson(attributes, attributeType);
    }

    private Map<String, Attribute> jsonToMap(String json) {
        return gson.fromJson(json, attributeType);
    }
}
