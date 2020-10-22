package de.eposcat.master.approachImpl;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.eposcat.master.connection.AbstractConnectionManager;
import de.eposcat.master.model.Attribute;
import de.eposcat.master.model.Page;


public class JSON_Oracle_DatabaseAdapter implements IDatabaseAdapter {
    
    private Connection conn;
    private Gson gson = new Gson();
    private Type attributeType = new TypeToken<Map<String, Attribute>>(){}.getType();
    
    public JSON_Oracle_DatabaseAdapter(AbstractConnectionManager connectionManager) {
        this.conn = connectionManager.getConnection();
    }

    @Override
    public Page createPage(String typename) throws SQLException{
        PreparedStatement stCreatePage = conn.prepareStatement("INSERT INTO pages(type, attributes) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS);
        stCreatePage.setString(1, typename);
        stCreatePage.setObject(2, "{}");
        
        int affectedRows = stCreatePage.executeUpdate();
        
        if(affectedRows > 0) {
            ResultSet newRow = stCreatePage.getGeneratedKeys();
            
            newRow.next();
            String query = "SELECT id FROM pages WHERE ROWID = ?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setString(1, newRow.getString(1));
            
            ResultSet rsId = st.executeQuery();
            rsId.next();
            
            
            return new Page(rsId.getInt(1), typename);
        }
            
        return null;
    }

    @Override
    public void updatePage(Page page) throws SQLException{
        PreparedStatement stUpdatePage = conn.prepareStatement("UPDATE pages SET type = ?, attributes= ? WHERE id = ?");
        stUpdatePage.setString(1, page.getTypeName());
        stUpdatePage.setString(2, mapToJSON(page.getAttributes()));
        stUpdatePage.setInt(3, page.getId());
        stUpdatePage.executeUpdate();
    }


    @Override
    public Page loadPage(int pageId) throws SQLException{
        PreparedStatement stLoadPage = conn.prepareStatement("SELECT * FROM pages WHERE id = ?");
        stLoadPage.setInt(1, pageId);
        
        ResultSet rsLoadPage = stLoadPage.executeQuery();
        
        if(rsLoadPage.next()) {
            Page page = new Page(rsLoadPage.getInt("id"), rsLoadPage.getString("type"));
            
            page.setAttributes(jsonToMap(rsLoadPage.getString("attributes")));
            
            return page;
        } else {
            throw new SQLException("Does not exist");
        }
    }

    @Override
    public List<Page> findPagesByAttribute(String attributeName) throws SQLException{
        // Oracle String literals are stupid -> sqlInjection would be possible here...
        // see https://stackoverflow.com/questions/56948001/how-to-use-oracles-json-value-function-with-a-preparedstatement
        String queryString = "SELECT * FROM pages WHERE json_exists(attributes, '$." + attributeName + "')";
        
        PreparedStatement stFindByAttribute = conn.prepareStatement(queryString);

        ResultSet rsFindPagesByAttribute = stFindByAttribute.executeQuery();
        List<Page> pages = new ArrayList<Page>();
        
        while(rsFindPagesByAttribute.next()) {
            Page page = new Page(rsFindPagesByAttribute.getInt(1), rsFindPagesByAttribute.getString(2));
            page.setAttributes(jsonToMap(rsFindPagesByAttribute.getString(3)));
            pages.add(page);
        }
        
        return pages;
    }

    @Override
    public List<Page> findPagesByAttributeValue(String attributeName, Object value) throws SQLException{
        // Oracle String literals are stupid -> sqlInjection would be possible here...
        // see https://stackoverflow.com/questions/56948001/how-to-use-oracles-json-value-function-with-a-preparedstatement
        String queryString = "SELECT * FROM pages WHERE json_value(attributes, '$."+ attributeName +".value') = ?";
        
        PreparedStatement stFindByAttributeValue = conn.prepareStatement(queryString);
        stFindByAttributeValue.setObject(1,  value);
        
        ResultSet rsFindPagesByAttributeValue = stFindByAttributeValue.executeQuery();
        List<Page> pages = new ArrayList<Page>();
        
        while(rsFindPagesByAttributeValue.next()) {
            Page page = new Page(rsFindPagesByAttributeValue.getInt(1), rsFindPagesByAttributeValue.getString(2));
            page.setAttributes(jsonToMap(rsFindPagesByAttributeValue.getString(3)));
            pages.add(page);
        }
        
        return pages;
    }

    private String mapToJSON(Map<String, Attribute> attributes) {
        return gson.toJson(attributes);
    }

    private Map<String, Attribute> jsonToMap(String json){
        return gson.fromJson(json, attributeType);
    }
}
