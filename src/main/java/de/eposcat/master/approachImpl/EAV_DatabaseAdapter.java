package de.eposcat.master.approachImpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.eposcat.master.connection.AbstractConnectionManager;
import de.eposcat.master.connection.CustomOracleConnectionManager;
import de.eposcat.master.connection.PostgresConnectionManager;
import de.eposcat.master.exceptions.BlException;
import de.eposcat.master.model.Attribute;
import de.eposcat.master.model.AttributeBuilder;
import de.eposcat.master.model.AttributeType;
import de.eposcat.master.model.Page;


public class EAV_DatabaseAdapter implements IDatabaseAdapter {

    private static final String CREATE_PAGE_QUERY = "INSERT INTO entities (typename) VALUES (?)";
    private static final String UPDATE_PAGE_QUERY = "UPDATE entities SET typename = ? WHERE id = ?";
    private static final String REMOVE_ATTRIBUTE_QUERY = "DELETE FROM eav_values WHERE ent_id = ? AND att_id = ?";
    private static final String UPDATE_ATTRIBUTE_VALUE_QUERY = "UPDATE eav_values SET value = ? WHERE ent_id = ? AND att_id = ?";
    private static final String CREATE_ATTRIBUTE_VALUE_QUERY = "INSERT INTO eav_values(ent_id, att_id, value) VALUES (?,?,?)";
    private static final String FIND_ATTRIBUTE_QUERY = "SELECT id FROM attributes WHERE datatype = ? AND name = ?";
    private static final String CREATE_ATTRIBUTE_QUERY = "INSERT INTO attributes (datatype, name) VALUES (?,?)";
    private static final String FIND_PAGE_BY_ID_QUERY = "SELECT * FROM entities WHERE id = ?";
    private static final String FIND_PAGE_BY_TYPE_QUERY = "SELECT id FROM entities WHERE typename = ?";
    private static final String GET_PAGE_ATTRIBUTES_QUERY = "SELECT eav_values.att_id, value, datatype, name FROM eav_values INNER JOIN attributes ON eav_values.att_id = attributes.id WHERE ent_id = ?";
    private static final String GET_LAST_ATTRIBUTE_IDS_QUERY = "SELECT att_id FROM eav_values WHERE ent_id = ?";
    private static final String ORACLE_FIND_PAGE_BY_ATTRIBUTE_VALUE_QUERY = "SELECT eav_values.ent_id FROM attributes Inner JOIN eav_values ON eav_values.att_id = attributes.id WHERE attributes.name = ? AND DBMS_LOB.INSTR(eav_values.value, ?) > 0";
    private static final String FIND_PAGE_BY_ATTRIBUTE_VALUE_QUERY = "SELECT eav_values.ent_id FROM attributes Inner JOIN eav_values ON eav_values.att_id = attributes.id WHERE attributes.name = ? AND eav_values.value = ?";
    private static final String FIND_PAGE_BY_ATTRIBUTE_QUERY = "SELECT eav_values.ent_id FROM attributes Inner JOIN eav_values ON eav_values.att_id = attributes.id WHERE attributes.name = ?";
    private static final String REMOVE_PAGE_QUERY = "DELETE FROM entities WHERE id = ?";

    private final Connection conn;
    private final static String ENTITY_TABLE = "entityTable";
    private final static String ATTRIBUTE_TABLE = "attributeTable";

    private static final Logger log = LoggerFactory.getLogger(EAV_DatabaseAdapter.class);
    private String dbContext = "";

    public EAV_DatabaseAdapter(AbstractConnectionManager connectionManager) {

        //really ugly, but comparing clobs in oracle needs a custom query....
        if (connectionManager instanceof CustomOracleConnectionManager) {
            dbContext = "oracle";
        } else if (connectionManager instanceof PostgresConnectionManager) {
            dbContext = "postgres";
        }
        this.conn = connectionManager.getConnection();

        try {
            conn.setAutoCommit(false);
        } catch (SQLException e){
            log.error("Could not set autocommit to false, terminating...");
            throw new RuntimeException("Could not set Autocommit to false...", e);
        }

    }

    @Override
    public Page createPage(String typeName) throws SQLException {
        if (typeName == null || typeName.isEmpty()) {
            throw new IllegalArgumentException("Typename has to be a non empty String");
        }

        PreparedStatement st = null;

        try {
            st = conn.prepareStatement(CREATE_PAGE_QUERY, Statement.RETURN_GENERATED_KEYS);
            st.setString(1, typeName);

            long startTime = System.currentTimeMillis();
            int updatedRows = st.executeUpdate();
            conn.commit();
            long endTime = System.currentTimeMillis();
            log.debug("@@ Finished Creation of page, EAV SQL, duration: {} ms", endTime - startTime);

            if (updatedRows > 0) {
                //TODO? Cancel Transaction if getting generated keys fails?
                ResultSet keySet = st.getGeneratedKeys();
                return new Page(getIdFromGeneratedKeys(keySet, ENTITY_TABLE), typeName);
            }

            throw new SQLException("Could not insert a new page in the entity table. This should never happen!");
        } finally {
            DbUtils.close(st);
        }
    }

    @Override
    public Page createPageWithAttributes(String typename, Map<String, Attribute> attributes) throws SQLException {
        if (attributes == null) {
            throw new IllegalArgumentException("Argument map must not be null");
        }

        //Currently implementation in two transactions
        //We cant get much efficiency out of making an extra sql instruction, since it's always a multi step process
        Page page = createPage(typename);
        page.setAttributes(attributes);
        updatePage(page);
        return page;
    }

    @Override
    public boolean deletePage(long pageId) throws SQLException {
        if (pageId == -1) {
            return false;
        }

        return deletePage(loadPage(pageId));
    }

    public boolean deletePage(Page page) throws SQLException {
        if (page == null || page.getId() == -1) {
            return false;
        }

        //Remove Attribute references
        //Attributes in the Attribute table do not get removed, even if this was the last page referencing this attribute
        page.getAttributes().values().forEach(attribute -> {
            try {
                removeAttributeValue(page.getId(), attribute.getId());
            } catch (SQLException exception) {
                //Streams and exceptions......
                log.error(exception.getMessage());
            }
        });

        PreparedStatement st = null;

        try {
            //Remove page
            st = conn.prepareStatement(REMOVE_PAGE_QUERY);
            st.setLong(1, page.getId());

            long startTime = System.currentTimeMillis();
            int affectedRows = st.executeUpdate();
            conn.commit();
            long endTime = System.currentTimeMillis();
            log.debug("@@ Finished deleting page, EAV SQL, duration: {}ms", endTime - startTime);

            return (affectedRows > 0);
        } finally {
            DbUtils.close(st);
        }
    }

    @Override
    public void updatePage(Page page) throws SQLException {
        if (page == null) {
            throw new IllegalArgumentException("page must not be null");
        }

        PreparedStatement st = null;

        try {

            //Check if dirty?
            st = conn.prepareStatement(UPDATE_PAGE_QUERY);
            st.setString(1, page.getTypeName());
            st.setLong(2, page.getId());

            long startTime = System.currentTimeMillis();
            int affectedRows = st.executeUpdate();
            long endTime = System.currentTimeMillis();
            log.debug("@@ Finished saving page entity, EAV SQL, duration: {} ms", endTime - startTime);

            if (affectedRows != 1) {
                throw new BlException("Page with id= " + page.getId() + ", type= " + page.getTypeName() + " is not tracked by database, try create pageWithAttributes first");
            }

            HashMap<String, Attribute> persistedAttributesWithIds = new HashMap<>();
            for (String attributeName : page.getAttributes().keySet()) {
                persistedAttributesWithIds.put(attributeName, saveAttribute(attributeName, page.getAttributes().get(attributeName)));
            }

            page.setAttributes(persistedAttributesWithIds);

            // the interesting measure
            startTime = System.currentTimeMillis();
            final List<Long> currentAttributes = getLastAttributeIds(page.getId());
            for (Attribute att : page.getAttributes().values()) {
                long attId = att.getId();
                if (currentAttributes.contains(attId)) {
                    currentAttributes.remove(attId);
                    updateAttributeValue(page.getId(), att);
                } else {
                    createAttributeValue(page.getId(), att);
                }
            }

            for (long id : currentAttributes) {
                removeAttributeValue(page.getId(), id);
            }

            conn.commit();
            endTime = System.currentTimeMillis();
            log.info("+++ Finished updating page, EAV SQL, duration: {} ms", endTime - startTime);

        } finally {
            DbUtils.close(st);
        }

    }

    private void removeAttributeValue(long id, Long attId) throws SQLException {

        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(REMOVE_ATTRIBUTE_QUERY);

            ps.setLong(1, id);
            ps.setLong(2, attId);

            long startTime = System.currentTimeMillis();
            ps.executeUpdate();
            conn.commit();
            long endTime = System.currentTimeMillis();
            log.debug("@@ Finished removing attribute value, EAV SQL, duration: {} ms", endTime - startTime);
        } finally {
            DbUtils.close(ps);
        }

    }

    private void updateAttributeValue(long id, Attribute att) throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(UPDATE_ATTRIBUTE_VALUE_QUERY);

            ps.setString(1, att.getValue().toString());
            ps.setLong(2, id);
            ps.setLong(3, att.getId());

            long startTime = System.currentTimeMillis();
            ps.executeUpdate();
            long endTime = System.currentTimeMillis();
            log.debug("@@ Finished updating attribute value, EAV SQL, duration: {} ms", endTime - startTime);
        } finally {
            DbUtils.close(ps);
        }

    }

    private void createAttributeValue(long id, Attribute att) throws SQLException {
        if (att.getId() == -1) {
            throw new IllegalArgumentException();
        }

        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(CREATE_ATTRIBUTE_VALUE_QUERY);
            ps.setLong(1, id);
            ps.setLong(2, att.getId());
            ps.setString(3, att.getValue().toString());

            long startTime = System.currentTimeMillis();
            int affectedRows = ps.executeUpdate();
            long endTime = System.currentTimeMillis();
            log.debug("@@ Finished creating attribute value, EAV SQL, duration: {}ms", endTime - startTime);

            if (affectedRows == 0) {
                throw new RuntimeException("Did not create Attribute Value!!");
            }
        } finally {
            DbUtils.close(ps);
        }
    }

    private List<Long> getLastAttributeIds(long id) throws SQLException {
        PreparedStatement selectAllAttributes = null;
        ResultSet rsAttributes = null;

        try {
            selectAllAttributes = conn.prepareStatement(GET_LAST_ATTRIBUTE_IDS_QUERY);
            selectAllAttributes.setLong(1, id);
            rsAttributes = selectAllAttributes.executeQuery();

            List<Long> attributes = new ArrayList<>();

            while (rsAttributes.next()) {
                attributes.add(rsAttributes.getLong(1));
            }

            return attributes;
        } finally {
            DbUtils.close(selectAllAttributes);
            DbUtils.close(rsAttributes);
        }
    }

    // compute the time and subtract it from the total time
    private Attribute saveAttribute(String attributeName, Attribute attribute) throws SQLException { // should be cached
        if (attribute.getId() == -1) {
            PreparedStatement psFindAttribute = null;
            ResultSet attributes = null;

            try {
                psFindAttribute = conn.prepareStatement(FIND_ATTRIBUTE_QUERY);
                psFindAttribute.setString(1, attribute.getType().toString()); // is the type needed? no
                psFindAttribute.setString(2, attributeName);
                attributes = psFindAttribute.executeQuery();

                if (attributes.next()) {
                    long attributeId = attributes.getLong("id");
                    attribute = new AttributeBuilder().setId(attributeId).setType(attribute.getType()).setValue(attribute.getValue()).createAttribute();
                } else {
                    Attribute att = createAttribute(attributeName, attribute.getType().toString());
                    att.setValue(attribute.getValue());

                    attribute = att;
                }
            } finally {
                DbUtils.close(psFindAttribute);
                DbUtils.close(attributes);
            }
        }

        return attribute;
    }


    private Attribute createAttribute(String attributeName, String attributeType) throws SQLException {
        PreparedStatement createAttributeDB = null;
        ResultSet keySet = null;

        try {
            createAttributeDB = conn.prepareStatement(CREATE_ATTRIBUTE_QUERY, Statement.RETURN_GENERATED_KEYS);
            createAttributeDB.setString(1, attributeType);
            createAttributeDB.setString(2, attributeName);

            long startTime = System.currentTimeMillis();
            int numberOfNewEntries = createAttributeDB.executeUpdate();
            long endTime = System.currentTimeMillis();
            log.debug("@@ Finished creating new Attribute, EAV SQL, duration: {} ms", endTime - startTime);

            if (numberOfNewEntries == 1) {
                keySet = createAttributeDB.getGeneratedKeys();
                return new AttributeBuilder().setId(getIdFromGeneratedKeys(keySet, ATTRIBUTE_TABLE)).setType(AttributeType.valueOf(attributeType)).setValue(null).createAttribute();
            } else {
                System.out.println("Didnt create attribute...");
                throw new SQLException("Didnt create attribute...");
            }
        } finally {
            DbUtils.close(createAttributeDB);
            DbUtils.close(keySet);
        }
    }

    @Override
    public Page loadPage(long pageId) throws SQLException {
        //load Page
        Page page;

        PreparedStatement stLoadPage = null;
        ResultSet rsLoadPage = null;
        PreparedStatement stLoadAttributes = null;
        ResultSet attributesSet = null;

        try {
            stLoadPage = conn.prepareStatement(FIND_PAGE_BY_ID_QUERY);
            stLoadPage.setLong(1, pageId);

            long startTime = System.currentTimeMillis();
            rsLoadPage = stLoadPage.executeQuery();
            long endTime = System.currentTimeMillis();
            log.debug("@@ Finished loading page, EAV SQL, duration: {}ms", endTime - startTime);

            if (rsLoadPage.next()) {
                page = new Page(rsLoadPage.getInt(1), rsLoadPage.getString(2));
            } else {
                return null;
            }

            //load Attributes of page
            stLoadAttributes = conn.prepareStatement(GET_PAGE_ATTRIBUTES_QUERY);
            stLoadAttributes.setLong(1, pageId);

            startTime = System.currentTimeMillis();
            attributesSet = stLoadAttributes.executeQuery();
            endTime = System.currentTimeMillis();
            log.debug("Finished loading page attributes, EAV SQL, duration: {}ms", endTime - startTime);

            while (attributesSet.next()) {
                page.addAttribute(attributesSet.getString(4),
                        new AttributeBuilder().setId(attributesSet.getLong(1)).setType(AttributeType.valueOf(attributesSet.getString(3))).setValue(attributesSet.getString(2)).createAttribute());
            }

            return page;
        } finally {
            DbUtils.close(stLoadPage);
            DbUtils.close(rsLoadPage);
            DbUtils.close(stLoadAttributes);
            DbUtils.close(attributesSet);
        }


    }

    @Override
    public List<Page> findPagesByType(String type) throws SQLException {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Typename has to be a non empty String");
        }

        PreparedStatement stFindPagesByAttribute = null;
        ResultSet rsPages = null;

        try {
            stFindPagesByAttribute = conn.prepareStatement(FIND_PAGE_BY_TYPE_QUERY);
            stFindPagesByAttribute.setString(1, type);
            stFindPagesByAttribute.setFetchSize(100);

            long startTime = System.currentTimeMillis();
            rsPages = stFindPagesByAttribute.executeQuery();
            long endTime = System.currentTimeMillis();
            log.debug("@@ Finished finding pages by type, EAV SQL, duration: {}ms", endTime - startTime);

            List<Page> pageList = new ArrayList<>();
            int i = 0;
            while (rsPages.next() && i < getQueryPageSize()) {
                pageList.add(loadPage(rsPages.getInt(1)));
                i++;
            }

            return pageList;
        } finally {
            DbUtils.close(stFindPagesByAttribute);
            DbUtils.close(rsPages);
        }
    }

    //Add attributeType?
    @Override
    public List<Page> findPagesByAttributeName(String attributeName) throws SQLException {
        if (attributeName == null) {
            throw new IllegalArgumentException("attributeName must not be null");
        }

        PreparedStatement stFindPagesByAttribute = null;
        ResultSet rsPages = null;

        try {
            stFindPagesByAttribute = conn.prepareStatement(FIND_PAGE_BY_ATTRIBUTE_QUERY);
            stFindPagesByAttribute.setString(1, attributeName);
            stFindPagesByAttribute.setFetchSize(100);

            long startTime = System.currentTimeMillis();
            rsPages = stFindPagesByAttribute.executeQuery();
            long endTime = System.currentTimeMillis();
            log.debug("@@ Finished finding pages by attribute name, EAV SQL, duration: {}ms", endTime - startTime);

            List<Page> pageList = new ArrayList<>();
            int i = 0;
            while (rsPages.next() && i < getQueryPageSize()) {
                pageList.add(loadPage(rsPages.getInt(1)));
                i++;
            }

            return pageList;
        } finally {
            DbUtils.close(stFindPagesByAttribute);
            DbUtils.close(rsPages);
        }


    }

    /**
     * @param attributeName the name of the attribute we are searching
     * @param value         the attribute including value we are searching, id and type are ignored in the EAV approach,
     *                      since attribute names are unique and every attribute has a certain type already saved in the db
     * @return a List of Page Objects which have the matching attribute
     * @throws SQLException
     */
    @Override
    public List<Page> findPagesByAttributeValue(String attributeName, Attribute value) throws SQLException {
        if (attributeName == null) {
            throw new IllegalArgumentException();
        }

        PreparedStatement stFindPagesByAttributeValue = null;
        ResultSet rsPages = null;

        try {
            String dbDependentQuery = "";

            if (dbContext.equals("oracle")) {
                dbDependentQuery = ORACLE_FIND_PAGE_BY_ATTRIBUTE_VALUE_QUERY;
            } else {
                dbDependentQuery = FIND_PAGE_BY_ATTRIBUTE_VALUE_QUERY;
            }
            stFindPagesByAttributeValue = conn.prepareStatement(dbDependentQuery);
            stFindPagesByAttributeValue.setString(1, attributeName);
            stFindPagesByAttributeValue.setString(2, value.getValue().toString());
            stFindPagesByAttributeValue.setFetchSize(100);

            log.info("@@ Started finding by attribute value, EAV SQL");

            long startTime = System.currentTimeMillis();
            rsPages = stFindPagesByAttributeValue.executeQuery();
            long endTime = System.currentTimeMillis();
            log.debug("@@ Finished finding by attribute value, EAV SQL, duration: {} ms", endTime - startTime);

            List<Page> pageList = new ArrayList<>();
            int i = 0;
            while (rsPages.next() && i < getQueryPageSize()) {
                pageList.add(loadPage(rsPages.getInt(1)));
                i++;
            }

            return pageList;
        } finally {
            DbUtils.close(stFindPagesByAttributeValue);
            DbUtils.close(rsPages);
        }


    }

    //Helper method since Oracle and Postgres handle generated keys differently
    private long getIdFromGeneratedKeys(ResultSet rs, String table) throws SQLException {
        if (rs.next()) {
            if (rs.getMetaData().getColumnName(1).equals("ROWID")) {
                //Oracle returns a rowid for generated keys...
                String query = "";
                if (table.equals(ENTITY_TABLE)) {
                    query = "SELECT id FROM entities WHERE ROWID = ?";
                } else if (table.equals(ATTRIBUTE_TABLE)) {
                    query = "SELECT id FROM attributes WHERE ROWID = ?";
                }

                PreparedStatement st = null;
                ResultSet rsId = null;

                try {
                    st = conn.prepareStatement(query);
                    st.setString(1, rs.getString(1));

                    rsId = st.executeQuery();
                    rsId.next();
                    return rsId.getLong(1);
                } finally {
                    DbUtils.close(st);
                    DbUtils.close(rsId);
                }
            } else {
                return rs.getLong(1);
            }
        }

        return -1;
    }
}
