package de.eposcat.master.connection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import oracle.jdbc.pool.OracleDataSource;

public class CustomOracleConnectionManager extends AbstractConnectionManager {

    public CustomOracleConnectionManager(RelationalApproach approach) {
        // DON'T SHARE NON LOCALHOST SERVERS AND CREDENTAILS ON GIT!
        connectionString = "jdbc:oracle:thin:@//localhost:33333/xepdb1";
        
        databaseProperties = new Properties();
        databaseProperties.put("user", "test");
        databaseProperties.put("password", "test");
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
