package com.docshare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.docshare.backend.AbstractPostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Proves the whole event-driven chain works end to end: uploading a document publishes a Kafka
 * event (via {@code DocumentEventPublisher}), which {@code DocumentEventNotificationConsumer} picks
 * up independently and turns into a persisted, queryable notification — all without {@code
 * DocumentService} knowing the notification module exists.
 *
 * <p>Kafka consumption is asynchronous, so this test polls (via Awaitility) rather than asserting
 * immediately after the upload call returns — the HTTP response completing only means the write
 * succeeded and the event was handed to the producer, not that any consumer has processed it yet
 * (that's the whole point of FR-21.4).
 */
class DocumentUploadNotificationIT extends AbstractPostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void uploadingADocumentEventuallyProducesANotification() throws Exception {
    String email = "notif-test-" + System.nanoTime() + "@docshare.local";
    mockMvc.perform(
        post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"email":"%s","password":"correct-horse-battery-staple","displayName":"Notif Test"}
                """
                    .formatted(email)));

    MvcResult loginResult =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"email":"%s","password":"correct-horse-battery-staple"}
                        """
                            .formatted(email)))
            .andExpect(status().isOk())
            .andReturn();
    String accessToken =
        objectMapper
            .readTree(loginResult.getResponse().getContentAsString())
            .get("accessToken")
            .asText();

    mockMvc
        .perform(
            multipart("/api/v1/documents/upload")
                .file(
                    new MockMultipartFile(
                        "file", "notify-me.txt", "text/plain", "trigger a notification".getBytes()))
                .param("folderId", "")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isCreated());

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              MvcResult listResult =
                  mockMvc
                      .perform(
                          get("/api/v1/notifications")
                              .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                      .andExpect(status().isOk())
                      .andReturn();

              JsonNode notifications =
                  objectMapper.readTree(listResult.getResponse().getContentAsString());
              assertThat(notifications).hasSize(1);
              assertThat(notifications.get(0).get("type").asText()).isEqualTo("UPLOAD_COMPLETE");
              assertThat(notifications.get(0).get("read").asBoolean()).isFalse();
            });
  }
}
