package de.eposcat.master.connection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import oracle.jdbc.pool.OracleDataSource;

public class CustomOracleConnectionManager extends AbstractConnectionManager {

    public CustomOracleConnectionManager(RelationalApproach approach) {
        connectionString = "jdbc:oracle:thin:@//localhost:33333/xepdb1";
        
        
        
//        switch(approach) {
//            case EAV:
//                connectionString += "test_eav";
//                break;
//            //Wird gestrichen?
//            case TABLE_PER_TYPE:
//                throw new RuntimeException("Not supported type");
//            case JSON:
//                connectionString += "test_json";
//                break;
//            case KEY_VALUE_STORE:
//                throw new RuntimeException("Not supported type");
//               
//        }
        
        databaseProperties = new Properties();
        databaseProperties.put("user", "test");
        databaseProperties.put("password", "test");
//        databaseProperties.put("url", connectionString);
    }
    
    public Connection createDatabaseConnection() {
        try {
            OracleDataSource ods = new OracleDataSource();
            ods.setConnectionProperties(databaseProperties);
            ods.setURL(connectionString);
            connection = ods.getConnection();
            return connection;
        } catch (SQLException e) {

            e.printStackTrace();
            return null;
        }
    }

}
