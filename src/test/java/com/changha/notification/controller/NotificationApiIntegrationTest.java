package com.changha.notification.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.changha.notification.domain.Notification;
import com.changha.notification.dto.CreateNotificationRequest;
import com.changha.notification.dto.ReadFilter;
import com.changha.notification.repository.NotificationOutboxRepository;
import com.changha.notification.repository.NotificationRepository;
import com.changha.notification.repository.NotificationScheduleRepository;
import com.changha.notification.testsupport.AbstractMySqlIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationApiIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationOutboxRepository outboxRepository;

    @Autowired
    private NotificationScheduleRepository scheduleRepository;

    @Test
    void createNotificationShouldPersistNotificationAndOutbox() throws Exception {
        CreateNotificationRequest request = NotificationFixtures.createImmediateEmailRequest();

        mockMvc.perform(post("/api/notifications")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.notificationId").isNumber())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.accepted").value(true));

        assertThat(notificationRepository.count()).isEqualTo(1);
        assertThat(outboxRepository.count()).isEqualTo(1);
    }

    @Test
    void duplicateCreateShouldKeepSingleNotificationAndOutbox() throws Exception {
        CreateNotificationRequest request = NotificationFixtures.createImmediateEmailRequest();

        String firstBody = mockMvc.perform(post("/api/notifications")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andReturn().getResponse().getContentAsString();

        String secondBody = mockMvc.perform(post("/api/notifications")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andReturn().getResponse().getContentAsString();

        JsonNode firstResponse = objectMapper.readTree(firstBody);
        JsonNode secondResponse = objectMapper.readTree(secondBody);

        assertThat(firstResponse.get("notificationId").asLong()).isEqualTo(secondResponse.get("notificationId").asLong());
        assertThat(notificationRepository.count()).isEqualTo(1);
        assertThat(outboxRepository.count()).isEqualTo(1);
    }

    @Test
    void getDetailListAndReadFilterShouldWork() throws Exception {
        CreateNotificationRequest request = NotificationFixtures.createImmediateEmailRequest();
        String createResponse = mockMvc.perform(post("/api/notifications")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andReturn().getResponse().getContentAsString();

        long notificationId = objectMapper.readTree(createResponse).get("notificationId").asLong();

        mockMvc.perform(get("/api/notifications/{id}", notificationId)
                        .header("X-USER-ID", 1001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationId").value(notificationId))
                .andExpect(jsonPath("$.recipientId").value(1001))
                .andExpect(jsonPath("$.isRead").value(false))
                .andExpect(jsonPath("$.readAt").value(nullValue()));

        mockMvc.perform(patch("/api/notifications/{id}/read", notificationId)
                        .header("X-USER-ID", 1001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationId").value(notificationId))
                .andExpect(jsonPath("$.isRead").value(true));

        mockMvc.perform(get("/api/notifications")
                        .header("X-USER-ID", 1001L)
                        .param("read", ReadFilter.READ.name())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].notificationId").value(notificationId))
                .andExpect(jsonPath("$.content[0].isRead").value(true));
    }

    @Test
    void createScheduledNotificationShouldPersistOnlyScheduleBeforeDispatch() throws Exception {
        CreateNotificationRequest request = NotificationFixtures.createScheduledEmailRequest(mutableClock.now().plusMinutes(10));

        mockMvc.perform(post("/api/notifications")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.scheduleId").isNumber())
                .andExpect(jsonPath("$.notificationId").value(nullValue()));

        assertThat(scheduleRepository.count()).isEqualTo(1);
        assertThat(notificationRepository.count()).isZero();
        assertThat(outboxRepository.count()).isZero();
    }
}
