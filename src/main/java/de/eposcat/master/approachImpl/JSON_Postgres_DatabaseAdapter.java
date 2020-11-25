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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.eposcat.master.connection.AbstractConnectionManager;
import de.eposcat.master.model.Attribute;
import de.eposcat.master.model.Page;


public class JSON_Postgres_DatabaseAdapter implements IDatabaseAdapter {

    private final Connection conn;
    private final Gson gson = new Gson();
    private final Type attributeType = new TypeToken<Map<String, Attribute>>() {}.getType();

    public JSON_Postgres_DatabaseAdapter(AbstractConnectionManager connectionManager) {
        this.conn = connectionManager.getConnection();
    }

    @Override
    public Page createPage(String typename) throws SQLException {
        return createPageWithAttributes(typename, new HashMap<>());
    }

    @Override
    public Page createPageWithAttributes(String typename, Map<String, Attribute> attributes) throws SQLException{
        PreparedStatement stCreatePage = conn.prepareStatement("INSERT INTO pages(type, attributes) VALUES (?,?::json)",
                Statement.RETURN_GENERATED_KEYS);
        stCreatePage.setString(1, typename);
        stCreatePage.setObject(2, mapToJSON(attributes));

        int affectedRows = stCreatePage.executeUpdate();

        if (affectedRows > 0) {
            ResultSet key = stCreatePage.getGeneratedKeys();
            key.next();

            return new Page(key.getInt(1), typename);
        }

        return null;
    }

    @Override
    public void updatePage(Page page) throws SQLException {

        PreparedStatement stUpdatePage = conn.prepareStatement("UPDATE pages SET type = ?, attributes = ?::json WHERE id = ?");
        stUpdatePage.setString(1, page.getTypeName());
        stUpdatePage.setString(2, mapToJSON(page.getAttributes()));
        stUpdatePage.setLong(3, page.getId());
        stUpdatePage.executeUpdate();

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
            throw new SQLException("Does not exist");
        }

    }

    @Override
    public List<Page> findPagesByType(String type) throws SQLException {
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
        PreparedStatement stFindByAttribute = conn.prepareStatement("SELECT * FROM pages WHERE attributes::jsonb ?? ?");
        stFindByAttribute.setString(1, attributeName);

        ResultSet rsFindPagesByAttribute = stFindByAttribute.executeQuery();
        List<Page> pages = new ArrayList<>();

        while (rsFindPagesByAttribute.next()) {
            Page page = new Page(rsFindPagesByAttribute.getInt(1), rsFindPagesByAttribute.getString(2));
            page.setAttributes(jsonToMap(rsFindPagesByAttribute.getString(3)));
            pages.add(page);
        }

        return pages;
    }

    @Override
    public List<Page> findPagesByAttributeValue(String attributeName, Object value) throws SQLException {
        PreparedStatement stFindByAttributeValue = conn.prepareStatement("SELECT * FROM pages WHERE attributes::jsonb @> ?::jsonb");
        stFindByAttributeValue.setObject(1, "[{\"" + attributeName + "\":\"" + value + "\" }]");

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
        return gson.toJson(attributes);
    }

    private Map<String, Attribute> jsonToMap(String json) {
        return gson.fromJson(json, attributeType);
    }
}
