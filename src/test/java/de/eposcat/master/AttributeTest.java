package de.eposcat.master;


import org.junit.jupiter.api.Test;

public class AttributeTest {

    @Test
    public void addAttribute(){
        //Attribute with same name gets replaced

        //What happens with null values? (don't add at all or save attribute without value/null value?)
        //fail on: missing name
        //Testcase for wrong Attributetype? -> Need logic in code which actually handles attribute type
    }

    @Test
    public void changeAttributeValue(){
        //

        //Testcase for wrong Attributetype? -> Need logic in code which actually handles attribute type
    }

    @Test
    public void removeAttribute(){
        //behavior when trying to remove non existing attribute?

        //null pointer exception on null key
    }
}
