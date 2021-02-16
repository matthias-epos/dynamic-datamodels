package de.eposcat.master;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.eposcat.master.approachImpl.EAV_DatabaseAdapter;
import de.eposcat.master.approachImpl.IDatabaseAdapter;
import de.eposcat.master.connection.H2ConnectionManager;
import de.eposcat.master.connection.RelationalApproach;
import de.eposcat.master.exceptions.BlException;
import de.eposcat.master.model.Attribute;
import de.eposcat.master.model.AttributeBuilder;
import de.eposcat.master.model.AttributeType;
import de.eposcat.master.model.Page;
import de.eposcat.master.serializer.AttributesDeserializer;
import de.eposcat.master.serializer.AttributesSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;


public class PageTest {

    static IDatabaseAdapter dbAdapter;
    static Attribute defaultAttribute;

    static Attribute moreAttribute1 = new AttributeBuilder().setType(AttributeType.String).setValue("1").createAttribute();
    static Attribute moreAttribute2 = new AttributeBuilder().setType(AttributeType.String).setValue("2").createAttribute();
    static Attribute moreAttribute3 = new AttributeBuilder().setType(AttributeType.String).setValue("3").createAttribute();

    static List<Long> addedPages = new ArrayList<>();

    @BeforeAll
    static void initDataBase() {
        H2ConnectionManager connectionManager = new H2ConnectionManager(RelationalApproach.EAV);

        dbAdapter = new EAV_DatabaseAdapter(connectionManager);
        defaultAttribute = new AttributeBuilder().setType(AttributeType.String).setValue("A test value").createAttribute();
    }

    @AfterEach
    void removeAddedPages(){
        for(Long id : addedPages){
            try {
                dbAdapter.deletePage(id);
            } catch (SQLException exception){
                exception.printStackTrace();
            }
        }

        addedPages.clear();
    }

    @Test
    @Disabled
    public void geTestDataForJsonMethod(){
        //TODO Small helper to get json database data, probably should be placed somewhere else
        GsonBuilder builder = new GsonBuilder();

        Type attributeType = new TypeToken<Map<String, Attribute>>() {}.getType();

        builder.registerTypeAdapter(attributeType, new AttributesSerializer());
        builder.registerTypeAdapter(attributeType, new AttributesDeserializer());

        Gson gson = builder.create();

        for(int i = 1; i<99; i++){
            Page page;
            try {
                page = dbAdapter.loadPage(i);
                if(page == null){
                    break;
                }
                System.out.println("( \'"+page.getTypeName() +"\', \'" + gson.toJson(page.getAttributes(), attributeType) + "\' ),");
            } catch (SQLException exception
            ) {
                exception.printStackTrace();
            }
        }

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

            addedPages.add(page.getId());
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

            addedPages.add(page.getId());
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
    public void deletePage(){
        try {
            Page page = dbAdapter.createPage("deleteThisPageAgain");
            page.addAttribute("deleteAttribute", defaultAttribute);
            dbAdapter.updatePage(page);

            assertThat(dbAdapter.deletePage(page.getId()), is(true));
            assertNull(dbAdapter.loadPage(page.getId()));
        } catch (SQLException exception
        ) {
            fail();
            exception.printStackTrace();
        }
    }

    @Test
    public void deleteNonExistentPage(){
        try {
            assertThat(dbAdapter.deletePage(-1), is(false));
            assertThat(dbAdapter.deletePage(9999999), is(false));
        } catch (SQLException exception
        ) {
            fail();
            exception.printStackTrace();
        }
    }

    @Test
    public void loadPage(){
        try {
            Page page = dbAdapter.loadPage(1);
            assertThat(page.getId(), is(1l));
            assertThat(page.getTypeName(), is("single"));
            assertThat(page.getAttributes().isEmpty(), is(false));
            assertThat(page.getAttribute("singleAttribute").getValue(), is("test"));
        } catch (SQLException exception
        ) {
            fail();
            exception.printStackTrace();
        }
    }

    @Test
    public void loadPageWhichDoesNotExist(){
        try {
            Page page = dbAdapter.loadPage(-1);
            assertNull(page);
        } catch (SQLException exception
        ) {
            exception.printStackTrace();
            fail();
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

            addedPages.add(page.getId());
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

            //works with pages that have multiple attributes
            assertThat(dbAdapter.findPagesByAttributeName("moreAttribute3").size(), is(1));

            //empty name is allowed

            //fail on null attribute name
            assertThrows(IllegalArgumentException.class, () -> dbAdapter.findPagesByAttributeName(null));
        } catch (SQLException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void findPageByAttributeValue() {
        //how do we handle attribute Type?
        //fail when using wrong type to search?

        try {
            //one hit, multiple hits, no hit
            assertThat(dbAdapter.findPagesByAttributeValue("singleAttribute", new AttributeBuilder().setValue("test").setType(AttributeType.String).createAttribute()).size(), is(1));
            assertThat(dbAdapter.findPagesByAttributeValue("tenAttribute", new AttributeBuilder().setValue("firstHalf").setType(AttributeType.String).createAttribute()).size(), is(5));


            assertThat(dbAdapter.findPagesByAttributeValue("singleAttribute", new AttributeBuilder().setValue("non-existing value").setType(AttributeType.String).createAttribute()).size(), is(0));
            assertThat(dbAdapter.findPagesByAttributeValue("not-existing attribute", new AttributeBuilder().setValue("test").setType(AttributeType.String).createAttribute()).size(), is(0));

            //works with pages that have multiple attributes
            assertThat(dbAdapter.findPagesByAttributeValue("moreAttribute3", new AttributeBuilder().setValue("3").setType(AttributeType.String).createAttribute()).size(), is(1));

            //fail on null attribute name
            assertThrows(IllegalArgumentException.class, () -> dbAdapter.findPagesByAttributeValue(null, new AttributeBuilder().setValue("non-existing value").setType(AttributeType.String).createAttribute()));

        } catch (SQLException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void loadComplexPage(){
        try {
            Page page = dbAdapter.findPagesByType("complex").get(0);

            assertThat(page.getAttributes().size(), is(4));

            assertThat(page.getAttribute("moreAttribute1").getValue(), is("1"));
            assertThat(page.getAttribute("moreAttribute2").getValue(), is("2"));
            assertThat(page.getAttribute("moreAttribute3").getValue(), is("3"));
            assertThat(page.getAttribute("moreAttribute4").getValue(), is("4"));

        } catch (SQLException throwables) {
            fail();
            throwables.printStackTrace();
        }
    }

    @Test
    public void createComplexPage(){
        try {
            Page page = new Page("complex2");

            page.addAttribute("more1", moreAttribute1);
            page.addAttribute("more2", moreAttribute2);
            page.addAttribute("more3", moreAttribute3);

            Page dbPage = dbAdapter.createPageWithAttributes(page.getTypeName(), page.getAttributes());

            //Should stay the same, but for safety
            dbPage = dbAdapter.loadPage(dbPage.getId());

            assertThat(dbPage.getAttributes().size(), is(3));

            assertThat(dbPage.getAttribute("more1").getValue(), is("1"));
            assertThat(dbPage.getAttribute("more2").getValue(), is("2"));
            assertThat(dbPage.getAttribute("more3").getValue(), is("3"));


        } catch (SQLException throwables) {
            fail();
            throwables.printStackTrace();
        }
    }
}
