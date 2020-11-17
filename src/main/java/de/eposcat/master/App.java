package de.eposcat.master;

import java.sql.SQLException;

import de.eposcat.master.approachImpl.IDatabaseAdapter;
import de.eposcat.master.approachImpl.JSON_Oracle_DatabaseAdapter;
import de.eposcat.master.connection.AbstractConnectionManager;
import de.eposcat.master.connection.CustomOracleConnectionManager;
import de.eposcat.master.connection.RelationalApproach;
import de.eposcat.master.model.AttributeBuilder;
import de.eposcat.master.model.AttributeType;
import de.eposcat.master.model.Page;

/**
 * Hello world!
 *
 */
public class App
{

//    private final static RelationalApproach approach = RelationalApproach.EAV;
    private final static RelationalApproach approach = RelationalApproach.JSON;

//    private final static AbstractConnectionManager connManager = new PostgresConnectionManager(approach);
    private final static AbstractConnectionManager connManager = new CustomOracleConnectionManager(approach);
//
//    private final static IDatabaseAdapter dbAdapter = new  JSON_Postgres_DatabaseAdapter(connManager);
//    private final static IDatabaseAdapter dbAdapter = new EAV_DatabaseAdapter(connManager);
    private final static IDatabaseAdapter dbAdapter = new JSON_Oracle_DatabaseAdapter(connManager);

    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );

        Page page;
        try {
            page = dbAdapter.createPage("test");
            page.getAttributes().put("t8", new AttributeBuilder().setType(AttributeType.String).setValue("").createAttribute());
            page.getAttributes().put("t9", new AttributeBuilder().setType(AttributeType.String).setValue(null).createAttribute());
            dbAdapter.updatePage(page);

//            page = dbAdapter.loadPage(1);
//            System.out.println(page);

//            for(Page foundByAttPage: dbAdapter.findPagesByAttribute("t8")) {
//                System.out.println(foundByAttPage);
//            }

//            for(Page foundByValPage: dbAdapter.findPagesByAttributeValue("t9", "value2")) {
//                System.out.println(foundByValPage);
//            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
