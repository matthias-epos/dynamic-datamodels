package de.eposcat.master.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public abstract class AbstractConnectionManager {
    
    
    protected Connection connection;
    

    protected String connectionString;
    protected Properties databaseProperties;
    
    
    public AbstractConnectionManager() {
        connectionString = "Missing Connection URL";
    }
    
    public Connection createDatabaseConnection() {
        try {
            connection = DriverManager.getConnection(connectionString, databaseProperties);
            return connection;
        } catch (SQLException e) {

            e.printStackTrace();
            return null;
        }
    }

    
    public Connection getConnection() {
        if(connection != null) {
            return connection;
        } else {
            return createDatabaseConnection();
        }
    }
}
