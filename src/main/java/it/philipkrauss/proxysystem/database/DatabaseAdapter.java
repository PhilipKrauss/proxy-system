package it.philipkrauss.proxysystem.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.philipkrauss.proxysystem.ProxySystem;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseAdapter {

    public static DatabaseAdapter create() {
        return new DatabaseAdapter();
    }

    private HikariDataSource dataSource;

    private DatabaseAdapter() {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            HikariConfig config = new HikariConfig();
            // change mysql-information
            DatabaseCredentials credentials = DatabaseCredentials.create();
            credentials.setHostname("localhost");
            credentials.setPort(3306);
            credentials.setDatabase("database");
            credentials.setUsername("username");
            credentials.setPassword("password");
            config.setJdbcUrl(String.format("jdbc:mariadb://%s:%s/%s?serverTimezone=UTC&useUnicode=yes&characterEncoding=UTF-8",
                    credentials.getHostname(), credentials.getPort(), credentials.getDatabase()));
            config.setUsername(credentials.getUsername());
            config.setPassword(credentials.getPassword());
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            dataSource = new HikariDataSource(config);
        } catch (ClassNotFoundException e) {
            ProxySystem.getInstance().getLogger().warn("Couldn't setup the data-source, because of missing driver (org.mariadb.jdbc.Driver)");
        }
    }

    public Connection getConnection() throws SQLException {
        return this.dataSource.getConnection();
    }

}
