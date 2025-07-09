package de.happybavarian07.coolstufflib.jpa.utils;

public class DatabaseProperties {
    private String host;
    private String port;
    private String database;
    private String username;
    private String password;
    private String driver;
    private String databasePrefix = "";
    private String connectionString;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getDatabasePrefix() {
        return databasePrefix;
    }

    public void setDatabasePrefix(String databasePrefix) {
        this.databasePrefix = databasePrefix;
    }

    public String getConnectionString() {
        if (connectionString != null) {
            return connectionString;
        }

        if ("sqlite".equalsIgnoreCase(driver)) {
            return "jdbc:sqlite:" + database;
        } else if ("mysql".equalsIgnoreCase(driver)) {
            String portStr = port != null ? ":" + port : ":3306";
            return "jdbc:mysql://" + host + portStr + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true";
        } else if ("postgresql".equalsIgnoreCase(driver)) {
            String portStr = port != null ? ":" + port : ":5432";
            return "jdbc:postgresql://" + host + portStr + "/" + database;
        }

        throw new IllegalArgumentException("Unsupported database driver: " + driver);
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }
}
