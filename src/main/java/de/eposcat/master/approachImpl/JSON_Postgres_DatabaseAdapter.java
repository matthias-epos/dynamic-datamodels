package de.eposcat.master.approachImpl;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JSON_Postgres_DatabaseAdapter implements IDatabaseAdapter {

    private final Connection conn;
    private final Gson gson;
    private final Type attributeType = new TypeToken<Map<String, Attribute>>() {}.getType();

    private static final Logger log = LoggerFactory.getLogger(JSON_Postgres_DatabaseAdapter.class);

    public JSON_Postgres_DatabaseAdapter(AbstractConnectionManager connectionManager) {
        this.conn = connectionManager.getConnection();

        try {
            conn.setAutoCommit(false);
        } catch (SQLException e){
            log.error("Could not set autocommit to false, terminating...");
            throw new RuntimeException("Could not set Autocommit to false...", e);
        }

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

            log.info("@@ Started creating page, PostgreSQL JSON SQL");
            Instant startCP = Instant.now();

            int affectedRows = stCreatePage.executeUpdate();
            conn.commit();

            Instant endCP = Instant.now();
            log.info("@@ Finished creating page, PostgreSQL JSON SQL, duration: {}ms", Duration.between(startCP,endCP).toMillis());


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

            log.info("@@ Started deleting page, PostgreSQL JSON SQL");
            Instant startDP = Instant.now();

            int affectedRows = st.executeUpdate();
            conn.commit();

            Instant endDP = Instant.now();
            log.info("@@ Finished deleting page, PostgreSQL JSON SQL, duration: {}ms", Duration.between(startDP,endDP).toMillis());


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
            log.info("@@ Started updating page, PostgreSQL JSON SQL");
            Instant startUP = Instant.now();

            int affectedRows = stUpdatePage.executeUpdate();
            conn.commit();

            Instant endUP = Instant.now();
            log.info("@@ Finished updating page, PostgreSQL JSON SQL, duration: {}ms", Duration.between(startUP,endUP).toMillis());

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

            log.info("@@ Started loading page, PostgreSQL JSON SQL");
            Instant start = Instant.now();

            rsLoadPage = stLoadPage.executeQuery();

            Instant end = Instant.now();
            log.info("@@ Finished loading page, PostgreSQL JSON SQL, duration: {}ms", Duration.between(start,end).toMillis());

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
            stFindByType.setFetchSize(100);

            log.info("@@ Started finding page by type, PostgreSQL JSON SQL");
            Instant startQPT = Instant.now();

            rs = stFindByType.executeQuery();

            Instant endQPT = Instant.now();
            log.info("@@ Finished finding page by type, PostgreSQL JSON SQL, duration: {}ms", Duration.between(startQPT,endQPT).toMillis());
            List<Page> pages = new ArrayList<>();


            int i = 0;
            while(rs.next() && i<getQueryPageSize()){
                long id = rs.getInt("ID");
                String resultType = rs.getString("type");
                String attributesJSON = rs.getString("attributes");

                Page page = new Page(id, resultType);
                page.setAttributes(jsonToMap(attributesJSON));
                pages.add(page);
                i++;
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
            stFindByAttribute.setFetchSize(100);


            log.info("@@ Started finding page by attribute name, PostgreSQL JSON SQL");
            Instant startQAN = Instant.now();

            rsFindPagesByAttribute = stFindByAttribute.executeQuery();

            Instant endQAN = Instant.now();
            log.info("@@ Finished finding page by attribute name, PostgreSQL JSON SQL, duration: {}ms", Duration.between(startQAN,endQAN).toMillis());
            List<Page> pages = new ArrayList<>();


            int i = 0;
            while (rsFindPagesByAttribute.next() && i < getQueryPageSize()) {
                Page page = new Page(rsFindPagesByAttribute.getInt(1), rsFindPagesByAttribute.getString(2));
                page.setAttributes(jsonToMap(rsFindPagesByAttribute.getString(3)));
                pages.add(page);
                i++;
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
            stFindByAttributeValue.setFetchSize(100);

            log.info("@@ Started finding pages by attribute vale, PostgreSQL JSON SQL");
            Instant startQAV = Instant.now();

            rsFindPagesByAttributeValue = stFindByAttributeValue.executeQuery();

            Instant endQAV = Instant.now();
            log.info("@@ Finished finding pages by attribute vale, PostgreSQL JSON SQL, duration: {}ms", Duration.between(startQAV,endQAV).toMillis());
            List<Page> pages = new ArrayList<>();

            int i = 0;

            while(rsFindPagesByAttributeValue.next() && i<getQueryPageSize()){
                Page page = new Page(rsFindPagesByAttributeValue.getInt(1), rsFindPagesByAttributeValue.getString(2));
                page.setAttributes(jsonToMap(rsFindPagesByAttributeValue.getString(3)));
                pages.add(page);
                i++;
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
