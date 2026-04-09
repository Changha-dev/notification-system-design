package com.changha.notification.testsupport;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.changha.notification.sender.LoggingEmailNotificationGateway;
import com.changha.notification.sender.LoggingInAppNotificationGateway;

import static org.mockito.Mockito.reset;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfig.class)
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractMySqlIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("notification_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected MutableClock mutableClock;

    @MockBean
    protected LoggingEmailNotificationGateway emailGateway;

    @MockBean
    protected LoggingInAppNotificationGateway inAppGateway;

    @BeforeEach
    void setUpDatabase() {
        jdbcTemplate.update("delete from notification_outbox");
        jdbcTemplate.update("delete from notification_schedules");
        jdbcTemplate.update("delete from notifications");
        jdbcTemplate.update("delete from shedlock");
        mutableClock.reset(MutableClock.seoulBaseline().instant());
        reset(emailGateway, inAppGateway);
    }
}
