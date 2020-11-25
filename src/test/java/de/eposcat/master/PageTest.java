package de.eposcat.master;


import de.eposcat.master.approachImpl.EAV_DatabaseAdapter;
import de.eposcat.master.approachImpl.IDatabaseAdapter;
import de.eposcat.master.connection.H2ConnectionManager;
import de.eposcat.master.connection.RelationalApproach;
import de.eposcat.master.exceptions.BlException;
import de.eposcat.master.model.Attribute;
import de.eposcat.master.model.AttributeBuilder;
import de.eposcat.master.model.AttributeType;
import de.eposcat.master.model.Page;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;


public class PageTest {

    static IDatabaseAdapter dbAdapter;
    static Attribute defaultAttribute;

    @BeforeAll
    static void initDataBase() {
        H2ConnectionManager connectionManager = new H2ConnectionManager(RelationalApproach.EAV);

        dbAdapter = new EAV_DatabaseAdapter(connectionManager);
        defaultAttribute = new AttributeBuilder().setType(AttributeType.String).setValue("A test value").createAttribute();
    }

    @Test
    public void createPage() {
        try {
            //returned Pages must have an id, the given type and no attributes
            Page page = dbAdapter.createPage("testType");
            assertThat(page.getId(), not(-1));
            assertThat(page.getTypeName(), is("testType"));
            assertThat(page.getAttributes().isEmpty(), is(true));

            assertThat(dbAdapter.loadPage(page.getId()), is(page));
        } catch (SQLException exception) {
            //No test should ever throw a SQL exception
            fail();
            exception.printStackTrace();
        }
    }

    @Test
    public void createPageWithInvalidType(){
        //fails on  missing/empty type
        assertThrows(IllegalArgumentException.class, () -> dbAdapter.createPage(null));
        assertThrows(IllegalArgumentException.class, () -> dbAdapter.createPage(""));
    }

    @Test
    public void createWithAttributes() {
        //Currently we allow an empty string as attribute name..
        try {
            //returned Pages must have an id, the given type and the given attributes
            HashMap<String, Attribute> attributes = new HashMap<>();
            attributes.put("testAttr", new AttributeBuilder().setValue("test")
                    .setType(AttributeType.String)
                    .createAttribute());


            Page page = dbAdapter.createPageWithAttributes("testType", attributes);
            assertThat(page.getId(), not(-1));
            assertThat(page.getTypeName(), is("testType"));
            assertThat(page.getAttributes().isEmpty(), is(false));
            assertThat(page.getAttribute("testAttr").getType(), is(AttributeType.String));
            assertThat(page.getAttribute("testAttr").getValue(), is("test"));


         } catch (SQLException exception) {
            fail("No test should ever throw a SQL exception");
            exception.printStackTrace();
        }
    }

    @Test
    public void  createPageWithAttributesWithInvalidTypes(){
        HashMap<String, Attribute> attributes = new HashMap<>();
        attributes.put("testAttr", new AttributeBuilder().setValue("test")
                .setType(AttributeType.String)
                .createAttribute());

        //fails on  missing/empty type
        assertThrows(IllegalArgumentException.class, () -> dbAdapter.createPageWithAttributes(null, attributes));
        assertThrows(IllegalArgumentException.class, () -> dbAdapter.createPageWithAttributes("", attributes));

        //fails on null map
        assertThrows(IllegalArgumentException.class, () -> dbAdapter.createPageWithAttributes("test", null));
    }

    @Test
    public void loadPage(){
        try {
            Page page = dbAdapter.loadPage(1);
            assertThat(page.getId(), is(1l));
            assertThat(page.getTypeName(), is("single"));
            assertThat(page.getAttributes().isEmpty(), is(false));
            assertThat(page.getAttribute("singleAttribute").getValue(), is("test"));
        } catch (SQLException throwables) {
            fail();
            throwables.printStackTrace();
        }
    }

    @Test
    public void loadPageWhichDoesNotExist(){
        try {
            Page page = dbAdapter.loadPage(-1);
            assertNull(page);
        } catch (SQLException throwables) {
            fail();
            throwables.printStackTrace();
        }
    }

    //
    @Test
    @Disabled
    public void changePageType() {
        HashMap<String, Attribute> attributes = new HashMap<>();
        attributes.put("attName", defaultAttribute);

        //Since we don't associate a type with certain attributes the attributes should stay the same after changing the type
        try {
            Page page = dbAdapter.createPageWithAttributes("test", attributes);
            // TODO Add this method
//            page.setType = "new type";
            dbAdapter.updatePage(page);

        } catch (SQLException e) {
            fail("No test should ever throw a SQL exception");
            e.printStackTrace();
        }
    }

    @Test
    public void updatePage() {

        try {
            //entries with matching ids should be updated
            Page page = dbAdapter.createPage("updatePage");
            page.addAttribute("updatePageAtt", defaultAttribute);
            dbAdapter.updatePage(page);

            //cant do equals since the attribute will have a new (not -1) id.. (in the EAV approach..)
            Page dbPage = dbAdapter.loadPage(page.getId());
            assertThat(dbPage.getTypeName(), is(page.getTypeName()));
            assertThat(dbPage.getAttribute("updatePageAtt").getValue(), is(page.getAttribute("updatePageAtt").getValue()));
            assertThat(dbPage.getAttribute("updatePageAtt").getType(), is(page.getAttribute("updatePageAtt").getType()));
        } catch (SQLException e) {
            fail();
            e.printStackTrace();
        }
    }

    @Test
    public void updatePageNotTrackedByDatabase(){
        //fails when page id is -1 (-> not tracked by database)
        Page failPage1 = new Page("test");
        assertThrows(BlException.class, () -> dbAdapter.updatePage(failPage1));

        //entities with non -1 id which are not represented in the database should
        //fail --> catch sql exception and thrown custom exception / does sql even fail??
        //no, just returns no updated rows
        Page failPage2 = new Page(9999, "test");
        assertThrows(BlException.class, () -> dbAdapter.updatePage(failPage2));

        assertThrows(IllegalArgumentException.class, () -> dbAdapter.updatePage(null));
    }

    // Following tests are based on existing images/insert scripts
    @Test
    public void findPageByType() {
        try {
            //one hit, multiple hits, no hit
            assertThat(dbAdapter.findPagesByType("single").size(), is(1));
            assertThat(dbAdapter.findPagesByType("ten").size(), is(10));

            assertThat(dbAdapter.findPagesByType("this type does not exist").size(), is(0));
        } catch (SQLException e) {
            fail();
            e.printStackTrace();
        }
    }

    @Test
    public void findPageByTypeWithInvalidType(){
        //fail on empty/null type
        assertThrows(IllegalArgumentException.class, () -> dbAdapter.findPagesByType(""));
        assertThrows(IllegalArgumentException.class, () -> dbAdapter.findPagesByType(null));
    }

    @Test
    public void findPageByAttributeName() {
        try {
            //one hit, multiple hits, no hit
            assertThat(dbAdapter.findPagesByAttributeName("singleAttribute").size(), is(1));
            assertThat(dbAdapter.findPagesByAttributeName("tenAttribute").size(), is(10));
            assertThat(dbAdapter.findPagesByAttributeName("this attribute does not exist").size(), is(0));

            //empty name is allowed

            //fail on null attribute
            assertThrows(IllegalArgumentException.class, () -> dbAdapter.findPagesByAttributeName(null));
        } catch (SQLException e) {
            fail();
            e.printStackTrace();
        }
    }

    @Test
    public void findPageByAttributeValue() {
        //how do we handle attribute Type?
        //fail when using wrong type to search?

        try {
            //one hit, multiple hits, no hit
            assertThat(dbAdapter.findPagesByAttributeValue("singleAttribute", "test").size(), is(1));
            assertThat(dbAdapter.findPagesByAttributeValue("tenAttribute", "firstHalf").size(), is(5));


            assertThat(dbAdapter.findPagesByAttributeValue("singleAttribute", "non-existing value").size(), is(0));
            assertThat(dbAdapter.findPagesByAttributeValue("not-existing attribute", "test").size(), is(0));

            //fail on null attribute


        } catch (SQLException e) {
            e.printStackTrace();
        }


    }
}
