package com.preen.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("local")
@EnabledIfEnvironmentVariable(named = "PREEN_VERIFY_LOCAL_DB", matches = "true")
class LocalDatabaseConnectionTests {
    @Autowired JdbcTemplate jdbc;

    @Test
    void connectsToLocalSchemaWithoutChangingData() {
        assertEquals(1, jdbc.queryForObject("SELECT 1", Integer.class));
        assertEquals(36, jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'",
            Integer.class));
    }
}
