package com.changha.notification.config;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.changha.notification.util.NotificationDispatchExecutor;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationDispatchExecutorSelectionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ApplicationInfrastructureConfig.class, TestDataSourceConfig.class);

    @Test
    void shouldCreatePlatformExecutorByDefault() {
        contextRunner.run(context -> {
            NotificationDispatchExecutor executor = context.getBean(NotificationDispatchExecutor.class);
            assertThat(executor.type()).isEqualTo("platform");
        });
    }

    @Test
    void shouldCreateVirtualExecutorWhenConfigured() {
        contextRunner.withPropertyValues("app.notification.executor.type=virtual")
                .run(context -> {
                    NotificationDispatchExecutor executor = context.getBean(NotificationDispatchExecutor.class);
                    assertThat(executor.type()).isEqualTo("virtual");
                });
    }

    @Configuration
    static class TestDataSourceConfig {

        @Bean
        DataSource dataSource() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
            dataSource.setUrl("jdbc:mysql://localhost:3306/test");
            dataSource.setUsername("test");
            dataSource.setPassword("test");
            return dataSource;
        }
    }
}
