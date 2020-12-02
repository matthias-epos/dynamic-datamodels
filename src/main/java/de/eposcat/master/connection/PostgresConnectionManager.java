package de.eposcat.master.connection;

import java.util.Properties;


public class PostgresConnectionManager extends AbstractConnectionManager {

    public PostgresConnectionManager(RelationalApproach approach, String host, int port, String user, String password) {
        // DON'T SHARE NON LOCALHOST SERVERS AND CREDENTIALS ON GIT!!
        connectionString = "jdbc:postgresql://" + host + ":" + port;

        switch (approach) {
            case EAV:
                connectionString += "/eav_test";
            break;
            // remove this approach?
            case TABLE_PER_TYPE:
                throw new RuntimeException("Not supported type");
            case JSON:
                connectionString += "/json_test";
            break;
            case KEY_VALUE_STORE:
                throw new RuntimeException("Not supported type");

        }

        databaseProperties = new Properties();
        databaseProperties.put("user", user);
        databaseProperties.put("password", password);
    }
}
