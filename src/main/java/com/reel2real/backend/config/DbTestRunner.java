package com.reel2real.backend.config;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

@Component
public class DbTestRunner {

    private final DataSource dataSource;

    public DbTestRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    @PostConstruct
    public void testConnection() {
        try (var conn = dataSource.getConnection()) {
            System.out.println("✅ Connected to Supabase PostgreSQL!");
        } catch (Exception e) {
            System.err.println("❌ Database connection failed");
            e.printStackTrace();
        }
    }
}