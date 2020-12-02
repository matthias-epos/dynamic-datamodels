package de.eposcat.master.connection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import oracle.jdbc.pool.OracleDataSource;

public class CustomOracleConnectionManager extends AbstractConnectionManager {

    public CustomOracleConnectionManager(RelationalApproach approach, String host, int port, String user, String password) {
        // DON'T SHARE NON LOCALHOST SERVERS AND CREDENTAILS ON GIT!
        connectionString = "jdbc:oracle:thin:@//" + host +":"+port+"/xepdb1";
        
        databaseProperties = new Properties();
        databaseProperties.put("user", user);
        databaseProperties.put("password", password);
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
