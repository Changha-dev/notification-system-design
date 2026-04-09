package com.changha.notification.config;

import java.time.Clock;
import java.util.concurrent.Executors;

import javax.sql.DataSource;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.changha.notification.util.NotificationDispatchExecutor;

@Configuration
@EnableAsync
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT1M")
@EnableConfigurationProperties(NotificationProperties.class)
public class ApplicationInfrastructureConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    @ConditionalOnProperty(name = "app.notification.executor.type", havingValue = "platform", matchIfMissing = true)
    public NotificationDispatchExecutor platformDispatchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("notification-platform-");
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.initialize();
        return new NotificationDispatchExecutor("platform", new TaskExecutorAdapter(executor));
    }

    @Bean
    @ConditionalOnProperty(name = "app.notification.executor.type", havingValue = "virtual")
    public NotificationDispatchExecutor virtualDispatchExecutor() {
        AsyncTaskExecutor executor = new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
        return new NotificationDispatchExecutor("virtual", executor);
    }

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime()
                        .build()
        );
    }
}
