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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

public class JSON_Oracle_DatabaseAdapter implements IDatabaseAdapter {
    
    private final Connection conn;
    private final Gson gson;
    private final Type attributeType = new TypeToken<Map<String, Attribute>>(){}.getType();

    private static final Logger log = LoggerFactory.getLogger(JSON_Oracle_DatabaseAdapter.class);
    
    public JSON_Oracle_DatabaseAdapter(AbstractConnectionManager connectionManager) {
        this.conn = connectionManager.getConnection();

        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(attributeType, new AttributesSerializer());
        builder.registerTypeAdapter(attributeType, new AttributesDeserializer());
        gson = builder.create();
    }

    @Override
    public Page createPage(String typename) throws SQLException{
        return createPageWithAttributes(typename, new HashMap<>());
    }

    @Override
public Page createPageWithAttributes(String typename, Map<String, Attribute> attributes) throws SQLException{
        if(typename == null || typename.isEmpty() || attributes == null){
            throw new IllegalArgumentException();
        }

        PreparedStatement stCreatePage = null;
        ResultSet newRow = null;

        try{
            stCreatePage = conn.prepareStatement("INSERT INTO pages(type, attributes) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS);
            stCreatePage.setString(1, typename);
            stCreatePage.setObject(2, mapToJSON(attributes));

            log.info("@@ Started creating page, Oracle JSON SQL");
            Instant startCP = Instant.now();

            int affectedRows = stCreatePage.executeUpdate();

            Instant endCP = Instant.now();
            log.info("@@ Finished creating page, JSON SQL, duration: {}ms", Duration.between(startCP,endCP).toMillis());

            if(affectedRows > 0) {
                newRow = stCreatePage.getGeneratedKeys();

                Page page = new Page(getPageIdFromResultSet(newRow), typename);
                page.setAttributes(attributes);

                return page;
            }

            return null;
        } finally {
            DbUtils.close(stCreatePage);
            DbUtils.close(newRow);
        }
    }

    private long getPageIdFromResultSet(ResultSet rs) throws SQLException{
        rs.next();

        PreparedStatement st = null;
        ResultSet rsId = null;

        try {
            String query = "SELECT id FROM pages WHERE ROWID = ?";
            st = conn.prepareStatement(query);
            st.setRowId(1, rs.getRowId(1));

            rsId = st.executeQuery();
            rsId.next();

            long id = rsId.getLong(1);
            return id;
        } finally {
            DbUtils.close(rsId);
            DbUtils.close(st);
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

        try{
            st = conn.prepareStatement("DELETE FROM pages WHERE ID = ?");
            st.setLong(1, page.getId());

            log.info("@@ Started deleting page, Oracle JSON SQL");
            Instant startDP = Instant.now();

            int affectedRows = st.executeUpdate();

            Instant endDP = Instant.now();
            log.info("@@ Finished deleting page, Oracle JSON SQL, duration: {}ms", Duration.between(startDP,endDP).toMillis());


            return (affectedRows > 0);
        } finally {
            DbUtils.close(st);
        }
    }

    @Override
    public void updatePage(Page page) throws SQLException{
        if(page == null){
            throw new IllegalArgumentException("page must not be null");
        }

        PreparedStatement stUpdatePage = null;

        try {
            stUpdatePage= conn.prepareStatement("UPDATE pages SET type = ?, attributes= ? WHERE id = ?");
            stUpdatePage.setString(1, page.getTypeName());
            stUpdatePage.setString(2, mapToJSON(page.getAttributes()));
            stUpdatePage.setLong(3, page.getId());

            log.info("@@ Started updating page, Oracle JSON SQL");
            Instant startUP = Instant.now();

            int affectedRows = stUpdatePage.executeUpdate();

            Instant endUP = Instant.now();
            log.info("@@ Finished updating page, Oracle JSON SQL, duration: {}ms", Duration.between(startUP,endUP).toMillis());

            if(affectedRows != 1) {
                throw new BlException("Page with id= "+ page.getId()+", type= " + page.getTypeName()+" is not tracked by database, try create pageWithAttributes first");
            }
        } finally {
            DbUtils.close(stUpdatePage);
        }
    }


    @Override
    public Page loadPage(long pageId) throws SQLException{
        PreparedStatement stLoadPage = null;
        ResultSet rsLoadPage = null;

        try{
            stLoadPage = conn.prepareStatement("SELECT * FROM pages WHERE id = ?");
            stLoadPage.setLong(1, pageId);

            log.info("@@ Started loading page, Oracle JSON SQL");
            Instant startLP = Instant.now();

            rsLoadPage = stLoadPage.executeQuery();

            Instant endLP = Instant.now();
            log.info("@@ Finished loading page, Oracle JSON SQL, duration: {}ms", Duration.between(startLP,endLP).toMillis());

            if(rsLoadPage.next()) {
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

        try{
            stFindByType = conn.prepareStatement("SELECT * FROM pages WHERE type = ?");
            stFindByType.setString(1,type);

            log.info("@@ Started finding page by type, Oracle JSON SQL");
            Instant startFP = Instant.now();

            rs = stFindByType.executeQuery();

            Instant endFP = Instant.now();
            log.info("@@ Finished finding page by type, Oracle JSON SQL, duration: {}ms", Duration.between(startFP,endFP).toMillis());

            List<Page> pages = new ArrayList<>();

            int i = 0;
            while(rs.next() && i<100){
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

    /**
     * Returns all pages which have an attribute with the given name.
     *
     * WARNING: This implementation might be suspect of SQL injection attacks!
     * Sanitation of input is advised!
     * See: https://stackoverflow.com/questions/56948001/how-to-use-oracles-json-value-function-with-a-preparedstatement
     *
     * @param attributeName the name of the attribute we are searching
     * @return a List of matching pages
     * @throws SQLException if the implementation or database connection are malfunctioning
     */
    @Override
    public List<Page> findPagesByAttributeName(String attributeName) throws SQLException{
        if(attributeName == null){
            throw new IllegalArgumentException();
        }

        PreparedStatement stFindByAttribute = null;
        ResultSet rsFindPagesByAttribute = null;

        try {
            // Oracle String literals are stupid -> sqlInjection might be possible here...
            // see https://stackoverflow.com/questions/56948001/how-to-use-oracles-json-value-function-with-a-preparedstatement
            String queryString = "SELECT * FROM pages WHERE json_exists(attributes, '$[*]?(@.name == \"" + attributeName + "\")')";

            stFindByAttribute = conn.prepareStatement(queryString);

            log.info("@@ Started finding pages by attribute name, Oracle JSON SQL");
            Instant startQAN = Instant.now();

            rsFindPagesByAttribute = stFindByAttribute.executeQuery();

            Instant endQAN = Instant.now();
            log.info("@@ Finished finding pages by attribute name, Oracle JSON SQL, duration: {}ms", Duration.between(startQAN,endQAN).toMillis());

            List<Page> pages = new ArrayList<>();

            int i = 0;
            while(rsFindPagesByAttribute.next() && i<100) {
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

    /**
     * Returns all pages which have an attribute with the given name and value.
     *
     * WARNING: This implementation might be suspect of SQL injection attacks.
     * Sanitation of input is advised!
     * See: https://stackoverflow.com/questions/56948001/how-to-use-oracles-json-value-function-with-a-preparedstatement
     *
     * @param attributeName the name of the attribute we are searching
     * @param value the value of the attribute we are searching
     * @return a List of matching pages
     * @throws SQLException if the implementation or database connection are malfunctioning
     */
    @Override
    public List<Page> findPagesByAttributeValue(String attributeName, Attribute value) throws SQLException{
        if(attributeName == null){
            throw new IllegalArgumentException();
        }

        PreparedStatement stFindByAttributeValue = null;
        ResultSet rsFindPagesByAttributeValue = null;

        try {
            // Oracle String literals are stupid -> sqlInjection might be possible here...
            // see https://stackoverflow.com/questions/56948001/how-to-use-oracles-json-value-function-with-a-preparedstatement
            String queryString = "SELECT * FROM pages WHERE json_exists(attributes, '$[*]?(@.name == \""+ attributeName + "\" && @.values[*]."+ value.getType().toString()+" == \""+ value.getValue().toString() +"\")')";
            //Might need to look into performance of json_exists with filter vs json_value... (plus indezes for those)

            stFindByAttributeValue = conn.prepareStatement(queryString);

            log.info("@@ Started finding pages by attribute value, Oracle JSON SQL");
            Instant startQAV = Instant.now();

            rsFindPagesByAttributeValue = stFindByAttributeValue.executeQuery();

            Instant endQAV = Instant.now();
            log.info("@@ Finished finding pages by attribute value, Oracle JSON SQL, duration: {}ms", Duration.between(startQAV,endQAV).toMillis());

            List<Page> pages = new ArrayList<>();

            int i = 0;
            while(rsFindPagesByAttributeValue.next() && i<100) {
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

    private Map<String, Attribute> jsonToMap(String json){
        return gson.fromJson(json, attributeType);
    }
}
