package de.eposcat.master.approachImpl;

import java.sql.SQLException;
import java.util.List;

import de.eposcat.master.model.Page;

public interface IDatabaseAdapter {
    public Page createPage(String typename) throws SQLException;
    
    public void updatePage(Page page) throws SQLException;
    public Page loadPage(int pageId) throws SQLException;
    
    public List<Page> findPagesByAttribute(String AttributeName) throws SQLException;
    public List<Page> findPagesByAttributeValue(String AttributeName, Object value) throws SQLException;
}
