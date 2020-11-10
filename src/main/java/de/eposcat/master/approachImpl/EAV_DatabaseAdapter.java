package de.eposcat.master.approachImpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.eposcat.master.connection.AbstractConnectionManager;
import de.eposcat.master.model.Attribute;
import de.eposcat.master.model.AttributeType;
import de.eposcat.master.model.Page;


public class EAV_DatabaseAdapter implements IDatabaseAdapter {

    private static final String CREATE_PAGE_QUERY = "INSERT INTO entities (typename) VALUES (?)";
    private static final String UPDATE_PAGE_QUERY = "UPDATE entities SET typename = ? WHERE id = ?";
    private static final String REMOVE_ATTRIBUTE_QUERY = "DELETE FROM public.'eav_values' WHERE ent_id = ? AND att_id = ?";
    private static final String UPDATE_ATTRIBUTE_VALUE_QUERY = "UPDATE public.'eav_values' SET value = ? WHERE ent_id = ? AND att_id = ?";
    private static final String CREATE_ATTRIBUTE_VALUE_QUERY = "INSERT INTO eav_values(ent_id, att_id, value) VALUES (?,?,?)";
    private static final String FIND_ATTRIBUTE_QUERY = "SELECT id FROM attributes WHERE datatype = ? AND name = ?";
    private static final String CREATE_ATTRIBUTE_QUERY = "INSERT INTO attributes (datatype, name) VALUES (?,?)";
    private static final String FIND_PAGE_BY_ID_QUERY = "SELECT * FROM entities WHERE id = ?";
    private static final String GET_PAGE_ATTRIBUTES_QUERY = "SELECT eav_values.att_id, value, datatype, name FROM eav_values INNER JOIN attributes ON eav_values.att_id = attributes.id WHERE ent_id = ?";
    private static final String GET_CURRENT_ATTRIBUTE_IDS_QUERY = "SELECT att_id FROM eav_values WHERE ent_id = ?";
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
        PreparedStatement st = conn.prepareStatement(CREATE_PAGE_QUERY, Statement.RETURN_GENERATED_KEYS);
        st.setString(1, typeName);

        int updatedRows = st.executeUpdate();

        if (updatedRows > 0) {
            //TODO? Cancel Transaction if getting generated keys fails?
            ResultSet keySet = st.getGeneratedKeys();
            return new Page(getIdFromGeneratedKeys(keySet, ENTITY_TABLE), typeName);
        }

        return null;
    }

    @Override
    public Page createPageWithAttributes(String typename, Map<String, Attribute> attributes) {
        return null;
    }

    @Override
    public void updatePage(Page page) throws SQLException{
        try {
            conn.setAutoCommit(false);

            //Check if dirty?
            PreparedStatement st = conn.prepareStatement(UPDATE_PAGE_QUERY);
            st.setString(1, page.getTypeName());
            st.setLong(2, page.getId());
            int affectedRows = st.executeUpdate();

            if(affectedRows != 1) {
                throw new SQLException();
            }

            //TODO Dont change while iterating
            for (String attributeName : page.getAttributes().keySet()) {
                page.getAttributes().put(attributeName, saveAttribute(attributeName, page.getAttributes().get(attributeName)));
            }

            final List<Long> currentAttributes = getCurrentAttributeIds(page.getId());

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
        PreparedStatement ps = conn.prepareStatement(CREATE_ATTRIBUTE_VALUE_QUERY);
        ps.setLong(1, id);
        ps.setLong(2, att.getId());
        ps.setString(3, att.getValue().toString());

        ps.executeUpdate();
    }

    private List<Long> getCurrentAttributeIds(long id) throws SQLException{
        PreparedStatement selectAllAttributes = conn.prepareStatement(GET_CURRENT_ATTRIBUTE_IDS_QUERY);
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
                int attributeId = attributes.getInt("att_id");
                attribute = new Attribute(attributeId, attribute.getType(), attribute.getValue());
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
            return new Attribute(getIdFromGeneratedKeys(keySet, ATTRIBUTE_TABLE), AttributeType.valueOf(attributeType), null);
        } else {
            System.out.println("Didnt create attribute...");
            throw new SQLException("Didnt create attribute...");
        }

    }

    @Override
    public Page loadPage(int pageId) throws SQLException{
        //load Page
        Page page;

        PreparedStatement stLoadPage = conn.prepareStatement(FIND_PAGE_BY_ID_QUERY);
        stLoadPage.setInt(1, pageId);

        ResultSet rsLoadPage = stLoadPage.executeQuery();

        if(rsLoadPage.next()) {
            page = new Page(rsLoadPage.getInt(1), rsLoadPage.getString(2));
        } else {
            throw new SQLException();
        }

        //load Attributes of page
        PreparedStatement stLoadAttributes = conn.prepareStatement(GET_PAGE_ATTRIBUTES_QUERY);
        stLoadAttributes.setInt(1, pageId);

        ResultSet attributesSet = stLoadAttributes.executeQuery();
        while(attributesSet.next()) {
            page.addAttribute( attributesSet.getString(4),
                               new Attribute(attributesSet.getInt(1),
                                             AttributeType.valueOf(attributesSet.getString(3)),
                                       attributesSet.getString(2)));
        }

        return page;
    }

    @Override
    public List<Page> findPagesByType(String Type) throws SQLException {
        return null;
    }

    //Add attributeType?
    @Override
    public List<Page> findPagesByAttribute(String attributeName) throws SQLException{
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
    private int getIdFromGeneratedKeys(ResultSet rs, String table) throws SQLException{
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
                return rsId.getInt(1);
            } else {
                return rs.getInt(1);
            }
        }

        return -1;
    }
}
