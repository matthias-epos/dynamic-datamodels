package de.eposcat.master.connection;

import oracle.jdbc.pool.OracleDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class H2ConnectionManager extends AbstractConnectionManager{

    public H2ConnectionManager(RelationalApproach approach) {
        // DON'T SHARE NON LOCALHOST SERVERS AND CREDENTIALS ON GIT!
        // Creates Schema and populates test data on connect
        connectionString = "jdbc:h2:mem:test;INIT=runscript from 'classpath:sql/h2.sql'\\;runscript from 'classpath:sql/h2Values.sql'";

        databaseProperties = new Properties();
        databaseProperties.put("user", "test");
        databaseProperties.put("password", "test");
    }
}
