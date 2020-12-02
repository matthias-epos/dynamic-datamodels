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

import de.eposcat.master.connection.AbstractConnectionManager;
import de.eposcat.master.model.AttributeBuilder;
import de.eposcat.master.model.Page;
import de.eposcat.master.exceptions.BlException;
import de.eposcat.master.model.Attribute;
import de.eposcat.master.model.AttributeType;


public class EAV_DatabaseAdapter implements IDatabaseAdapter {

    private static final String CREATE_PAGE_QUERY = "INSERT INTO entities (typename) VALUES (?)";
    private static final String UPDATE_PAGE_QUERY = "UPDATE entities SET typename = ? WHERE id = ?";
    private static final String REMOVE_ATTRIBUTE_QUERY = "DELETE FROM public.'eav_values' WHERE ent_id = ? AND att_id = ?";
    private static final String UPDATE_ATTRIBUTE_VALUE_QUERY = "UPDATE public.'eav_values' SET value = ? WHERE ent_id = ? AND att_id = ?";
    private static final String CREATE_ATTRIBUTE_VALUE_QUERY = "INSERT INTO eav_values(ent_id, att_id, value) VALUES (?,?,?)";
    private static final String FIND_ATTRIBUTE_QUERY = "SELECT id FROM attributes WHERE datatype = ? AND name = ?";
    private static final String CREATE_ATTRIBUTE_QUERY = "INSERT INTO attributes (datatype, name) VALUES (?,?)";
    private static final String FIND_PAGE_BY_ID_QUERY = "SELECT * FROM entities WHERE id = ?";
    private static final String FIND_PAGE_BY_TYPE_QUERY = "SELECT id FROM entities WHERE typename = ?";
    private static final String GET_PAGE_ATTRIBUTES_QUERY = "SELECT eav_values.att_id, value, datatype, name FROM eav_values INNER JOIN attributes ON eav_values.att_id = attributes.id WHERE ent_id = ?";
    private static final String GET_LAST_ATTRIBUTE_IDS_QUERY = "SELECT att_id FROM eav_values WHERE ent_id = ?";
    private static final String FIND_PAGE_BY_ATTRIBUTE_VALUE_QUERY = "SELECT eav_values.ent_id FROM attributes Inner JOIN eav_values ON eav_values.att_id = attributes.id WHERE attributes.name = ? AND eav_values.value = ?";
    private static final String FIND_PAGE_BY_ATTRIBUTE_QUERY = "SELECT eav_values.ent_id FROM attributes Inner JOIN eav_values ON eav_values.att_id = attributes.id WHERE attributes.name = ?";

    private final Connection conn;
    private final static String ENTITY_TABLE =  "entityTable";
    private final static String ATTRIBUTE_TABLE = "attributeTable";

    public EAV_DatabaseAdapter(AbstractConnectionManager connectionManager) {
        this.conn = connectionManager.getConnection();
    }

    @Override
    public Page createPage(String typeName) throws SQLException {
        if(typeName == null || typeName.isEmpty()){
            throw new IllegalArgumentException("Typename has to be a non empty String");
        }

        PreparedStatement st = conn.prepareStatement(CREATE_PAGE_QUERY, Statement.RETURN_GENERATED_KEYS);
        st.setString(1, typeName);

        int updatedRows = st.executeUpdate();

        if (updatedRows > 0) {
            //TODO? Cancel Transaction if getting generated keys fails?
            ResultSet keySet = st.getGeneratedKeys();
            return new Page(getIdFromGeneratedKeys(keySet, ENTITY_TABLE), typeName);
        }

        throw new SQLException("Could not insert a new page in the entity table. This should never happen!");
    }

    @Override
    public Page createPageWithAttributes(String typename, Map<String, Attribute> attributes) throws SQLException{
        if(attributes == null){
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
    public void updatePage(Page page) throws SQLException{
        if(page == null){
            throw new IllegalArgumentException("page must not be null");
        }

        try {
            conn.setAutoCommit(false);

            //Check if dirty?
            PreparedStatement st = conn.prepareStatement(UPDATE_PAGE_QUERY);
            st.setString(1, page.getTypeName());
            st.setLong(2, page.getId());
            int affectedRows = st.executeUpdate();

            if(affectedRows != 1) {
                throw new BlException("Page with id= "+ page.getId()+", type= " + page.getTypeName()+" is not tracked by database, try create pageWithAttributes first");
            }

            HashMap<String, Attribute> persistedAttributesWithIds = new HashMap<>();
            for (String attributeName : page.getAttributes().keySet()) {
                persistedAttributesWithIds.put(attributeName, saveAttribute(attributeName, page.getAttributes().get(attributeName)));
            }

            page.setAttributes(persistedAttributesWithIds);

            final List<Long> currentAttributes = getLastAttributeIds(page.getId());

            for(Attribute att : page.getAttributes().values()){
                long attId = att.getId();
                if(currentAttributes.contains(attId)) {
                    currentAttributes.remove(attId);
                    updateAttributeValue(page.getId(), att);
                } else {
                    createAttributeValue(page.getId(), att);
                }
            }

            for(long id : currentAttributes){
                removeAttributeValue(page.getId(), id);
            }

            conn.commit();
        } finally {
            conn.setAutoCommit(true);
        }

    }

    private void removeAttributeValue(long id, Long attId) throws SQLException{
        PreparedStatement ps = conn.prepareStatement(REMOVE_ATTRIBUTE_QUERY);

        ps.setLong(1, id);
        ps.setLong(2, attId);

        ps.executeUpdate();
    }

    private void updateAttributeValue(long id, Attribute att) throws SQLException{
        PreparedStatement ps = conn.prepareStatement(UPDATE_ATTRIBUTE_VALUE_QUERY);

        ps.setString(1, att.getValue().toString());
        ps.setLong(2, id);
        ps.setLong(3, att.getId());

        ps.executeUpdate();

    }

    private void createAttributeValue(long id, Attribute att) throws SQLException {
        if(att.getId() == -1){
            throw new IllegalArgumentException();
        }

        PreparedStatement ps = conn.prepareStatement(CREATE_ATTRIBUTE_VALUE_QUERY);
        ps.setLong(1, id);
        ps.setLong(2, att.getId());
        ps.setString(3, att.getValue().toString());

        int affectedRows = ps.executeUpdate();

        if(affectedRows == 0){
            throw new RuntimeException("Did not create Attribute Value!!");
        }
    }

    private List<Long> getLastAttributeIds(long id) throws SQLException{
        PreparedStatement selectAllAttributes = conn.prepareStatement(GET_LAST_ATTRIBUTE_IDS_QUERY);
        selectAllAttributes.setLong(1, id);
        ResultSet rsAttributes = selectAllAttributes.executeQuery();

        List<Long> attributes = new ArrayList<>();

        while(rsAttributes.next()) {
            attributes.add(rsAttributes.getLong(1));
        }

        return attributes;
    }

    private Attribute saveAttribute(String attributeName, Attribute attribute) throws SQLException{
        if(attribute.getId() == -1) {
            PreparedStatement psFindAttribute = conn.prepareStatement(FIND_ATTRIBUTE_QUERY);
            psFindAttribute.setString(1, attribute.getType().toString());
            psFindAttribute.setString(2, attributeName);
            ResultSet attributes = psFindAttribute.executeQuery();

            if(attributes.next()){
                long attributeId = attributes.getLong("id");
                attribute = new AttributeBuilder().setId(attributeId).setType(attribute.getType()).setValue(attribute.getValue()).createAttribute();
            } else {
                Attribute att = createAttribute(attributeName, attribute.getType().toString());
                att.setValue(attribute.getValue());

                attribute = att;
            }
        }

        return attribute;
    }


    private Attribute createAttribute(String attributeName, String attributeType) throws SQLException{
        PreparedStatement createAttributeDB = conn.prepareStatement(CREATE_ATTRIBUTE_QUERY, Statement.RETURN_GENERATED_KEYS);
        createAttributeDB.setString(1, attributeType);
        createAttributeDB.setString(2, attributeName);

        if(createAttributeDB.executeUpdate() == 1) {
            ResultSet keySet = createAttributeDB.getGeneratedKeys();
            return new AttributeBuilder().setId(getIdFromGeneratedKeys(keySet, ATTRIBUTE_TABLE)).setType(AttributeType.valueOf(attributeType)).setValue(null).createAttribute();
        } else {
            System.out.println("Didnt create attribute...");
            throw new SQLException("Didnt create attribute...");
        }

    }

    @Override
    public Page loadPage(long pageId) throws SQLException{
        //load Page
        Page page;

        PreparedStatement stLoadPage = conn.prepareStatement(FIND_PAGE_BY_ID_QUERY);
        stLoadPage.setLong(1, pageId);

        ResultSet rsLoadPage = stLoadPage.executeQuery();

        if(rsLoadPage.next()) {
            page = new Page(rsLoadPage.getInt(1), rsLoadPage.getString(2));
        } else {
            return null;
        }

        //load Attributes of page
        PreparedStatement stLoadAttributes = conn.prepareStatement(GET_PAGE_ATTRIBUTES_QUERY);
        stLoadAttributes.setLong(1, pageId);

        ResultSet attributesSet = stLoadAttributes.executeQuery();
        while(attributesSet.next()) {
            page.addAttribute( attributesSet.getString(4),
                    new AttributeBuilder().setId(attributesSet.getLong(1)).setType(AttributeType.valueOf(attributesSet.getString(3))).setValue(attributesSet.getString(2)).createAttribute());
        }

        return page;
    }

    @Override
    public List<Page> findPagesByType(String type) throws SQLException {
        if(type == null || type.isEmpty()){
            throw new IllegalArgumentException("Typename has to be a non empty String");
        }

        PreparedStatement stFindPagesByAttribute = conn.prepareStatement(FIND_PAGE_BY_TYPE_QUERY);
        stFindPagesByAttribute.setString(1, type);
        ResultSet rsPages = stFindPagesByAttribute.executeQuery();

        List<Page> pageList= new ArrayList<>();

        while(rsPages.next()) {
            pageList.add(loadPage(rsPages.getInt(1)));
        }

        return pageList;
    }

    //Add attributeType?
    @Override
    public List<Page> findPagesByAttributeName(String attributeName) throws SQLException{
        if(attributeName == null){
            throw new IllegalArgumentException("attributeName must not be null");
        }

        PreparedStatement stFindPagesByAttribute = conn.prepareStatement(FIND_PAGE_BY_ATTRIBUTE_QUERY);
        stFindPagesByAttribute.setString(1, attributeName);
        ResultSet rsPages = stFindPagesByAttribute.executeQuery();

        List<Page> pageList= new ArrayList<>();

        while(rsPages.next()) {
            pageList.add(loadPage(rsPages.getInt(1)));
        }

        return pageList;
    }

    @Override
    public List<Page> findPagesByAttributeValue(String attributeName, Object value) throws SQLException{
        PreparedStatement stFindPagesByAttributeValue = conn.prepareStatement(FIND_PAGE_BY_ATTRIBUTE_VALUE_QUERY);
        stFindPagesByAttributeValue.setString(1, attributeName);
        stFindPagesByAttributeValue.setString(2, value.toString());
        ResultSet rsPages = stFindPagesByAttributeValue.executeQuery();

        List<Page> pageList= new ArrayList<>();

        while(rsPages.next()) {
            pageList.add(loadPage(rsPages.getInt(1)));
        }

        return pageList;
    }

    //Helper method since Oracle and Postgres handle generated keys differently
    private long getIdFromGeneratedKeys(ResultSet rs, String table) throws SQLException{
        if(rs.next()) {
            if(rs.getMetaData().getColumnName(1).equals("ROWID")) {
                //Oracle returns a rowid for generated keys...
                String query = "";
                if(table.equals(ENTITY_TABLE)) {
                    query = "SELECT id FROM entities WHERE ROWID = ?";
                } else if(table.equals(ATTRIBUTE_TABLE)) {
                    query = "SELECT id FROM attributes WHERE ROWID = ?";
                }

                PreparedStatement st = conn.prepareStatement(query);
                st.setString(1, rs.getString(1));

                ResultSet rsId = st.executeQuery();
                rsId.next();
                return rsId.getLong(1);
            } else {
                return rs.getLong(1);
            }
        }

        return -1;
    }
}
