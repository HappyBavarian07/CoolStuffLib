package de.happybavarian07.coolstufflib.jpa;

import de.happybavarian07.coolstufflib.jpa.utils.DatabaseProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabasePropertiesTest {
    @Test
    void mariadbUsesTheMysqlUrlWithItsPort() {
        DatabaseProperties props = new DatabaseProperties();
        props.setDriver("mariadb");
        props.setHost("db.example");
        props.setPort("3307");
        props.setDatabase("panel");
        assertEquals("jdbc:mysql://db.example:3307/panel?useSSL=false&allowPublicKeyRetrieval=true", props.getConnectionString());
    }
}
