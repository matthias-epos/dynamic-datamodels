package de.eposcat.master.approachImpl;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.sun.istack.internal.NotNull;
import de.eposcat.master.model.Attribute;
import de.eposcat.master.model.Page;

public interface IDatabaseAdapter {
    /**
     * Creates a new page entry in the database and returns the representing page object.
     * This page has no attributes at creation.
     *
     * @param typename the type of the new page
     * @return a new page with a database-generated id
     * @throws SQLException if the implementation or database connection are malfunctioning
     */
    Page createPage(String typename) throws SQLException;

    /**
     * Creates a new page entry in the database and returns the representing page object.
     * This method simultaneously persists the given attributes of this page.
     *
     * @param typename the type of the new page
     * @param attributes the attributes of the new page
     * @return a new page with a database-generated id and the given attributes
     * @throws SQLException if the implementation or database connection are malfunctioning
     */
    Page createPageWithAttributes(String typename, Map<String, Attribute> attributes) throws SQLException;

    /**
     * Merges any changes to this object with the database entry with the corresponding id.
     * This method will not do any changes to the database if there are no matching database entries.
     * Acts similar to Hibernate's flush method, but we need to keep track of the objects ourself.
     *
     * @param page the page which database representation should be updated/persisted
     * @throws SQLException if the implementation or database connection are malfunctioning
     * @throws de.eposcat.master.exceptions.BlException - if no entry with the same id as the page exists in the database
     */
    void updatePage(@NotNull Page page) throws SQLException;

    /**
     * Retrieves the page with the given id from the database.
     *
     * @param pageId the id of the page to be retrieved
     * @return the page object with matching id, null if none exist
     * @throws SQLException if the implementation or database connection are malfunctioning
     */
    Page loadPage(long pageId) throws SQLException;

    /**
     * Returns all pages with matching type names.
     * @param type the name of the type of pages we are searching
     * @return a List of matching pages
     * @throws SQLException if the implementation or database connection are malfunctioning
     */
    List<Page> findPagesByType(String type) throws SQLException;

    /**
     * Returns all pages which have an attribute with the given name.
     *
     * @param attributeName the name of the attribute we are searching
     * @return a List of matching pages
     * @throws SQLException if the implementation or database connection are malfunctioning
     */
    List<Page> findPagesByAttributeName(String attributeName) throws SQLException;

    /**
     * Returns all pages which have an attribute with the given name and value.
     *
     * @param attributeName the name of the attribute we are searching
     * @param value the value of the attribute we are searching
     * @return a List of matching pages
     * @throws SQLException if the implementation or database connection are malfunctioning
     */
    List<Page> findPagesByAttributeValue(String attributeName, Object value) throws SQLException;
}
