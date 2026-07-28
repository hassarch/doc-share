package com.docshare.backend.common.exception;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verifies each custom exception maps to the correct HTTP status and the shared {"error": {...}}
 * envelope — using a standalone MockMvc setup (no full Spring context needed) with a throwaway
 * controller that deliberately throws each exception type.
 */
class GlobalExceptionHandlerTest {

  @RestController
  static class ThrowingController {
    @GetMapping("/test/not-found")
    void notFound() {
      throw new NotFoundException("Document abc not found");
    }

    @GetMapping("/test/forbidden")
    void forbidden() {
      throw new ForbiddenException("Viewer cannot delete this document");
    }

    @GetMapping("/test/conflict")
    void conflict() {
      throw new ConflictException("Document version is stale");
    }

    @GetMapping("/test/unexpected")
    void unexpected() {
      throw new RuntimeException("boom - should never reach the client verbatim");
    }
  }

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new ThrowingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void notFoundExceptionMapsTo404WithCode() throws Exception {
    mockMvc
        .perform(get("/test/not-found"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code", is("NOT_FOUND")))
        .andExpect(jsonPath("$.error.message", is("Document abc not found")));
  }

  @Test
  void forbiddenExceptionMapsTo403WithCode() throws Exception {
    mockMvc
        .perform(get("/test/forbidden"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code", is("FORBIDDEN")));
  }

  @Test
  void conflictExceptionMapsTo409WithCode() throws Exception {
    mockMvc
        .perform(get("/test/conflict"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code", is("CONFLICT")));
  }

  @Test
  void unexpectedExceptionMapsTo500WithoutLeakingInternalMessage() throws Exception {
    mockMvc
        .perform(get("/test/unexpected"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.error.code", is("INTERNAL_ERROR")))
        .andExpect(jsonPath("$.error.message", is("An unexpected error occurred.")));
  }
}
