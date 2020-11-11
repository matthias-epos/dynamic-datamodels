package de.eposcat.master;

import org.junit.Test;

public class PageTest {

    @Test
    public void createPage(){
        //returned Pages must have an id

        //fails on  missing/empty type
    }

    @Test
    public void createWithAttributes(){
        //same as createPage, but with values for attributes
    }

    @Test
    public void changePageType(){
        //What behavior when changing type? purge Attributes?
    }

    @Test
    public void findPageByType(){
        //one hit, multiple hits, no hit

        //fail on empty/null type
    }

    @Test
    public void findPageByAttribute(){
        //one hit, multiple hits, no hit

        //fail on empty/null attribute
    }

    @Test
    public void findPageByAttributeValue(){
        //one hit, multiple hits, no hit

        //fail on empty/null attribute

        //how do we handle attribute Type?
        //fail when using wrong type to search?
    }
}
