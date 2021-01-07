package de.eposcat.master.approachImpl;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import de.eposcat.master.model.Attribute;
import de.eposcat.master.model.Page;

public interface IDatabaseAdapter {
    Page createPage(String typename) throws SQLException;
    Page createPageWithAttributes(String typename, Map<String, Attribute> attributes) throws SQLException;

    void updatePage(Page page) throws SQLException;
    Page loadPage(long pageId) throws SQLException;
    
    List<Page> findPagesByType(String type) throws SQLException;
    List<Page> findPagesByAttributeName(String attributeName) throws SQLException;
    List<Page> findPagesByAttributeValue(String attributeName, Attribute value) throws SQLException;
}
