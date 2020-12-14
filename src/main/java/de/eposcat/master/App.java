package de.eposcat.master;

import java.io.IOException;

import de.eposcat.master.connection.RelationalApproach;
import de.eposcat.master.generators.ChangesGenerator;
import de.eposcat.master.generators.StartDataGenerator;
import de.eposcat.master.generators.data.StartData;

public class App
{

//    private final static RelationalApproach approach = RelationalApproach.EAV;
    private final static RelationalApproach approach = RelationalApproach.JSON;

//    private final static AbstractConnectionManager connManager = new PostgresConnectionManager(approach);
//    private final static AbstractConnectionManager connManager = new CustomOracleConnectionManager(approach);
//
//    private final static IDatabaseAdapter dbAdapter = new  JSON_Postgres_DatabaseAdapter(connManager);
//    private final static IDatabaseAdapter dbAdapter = new EAV_DatabaseAdapter(connManager);
//    private final static IDatabaseAdapter dbAdapter = new JSON_Oracle_DatabaseAdapter(connManager);

    public static void main( String[] args )
    {
        System.out.println(String.valueOf(true));
    }
}
