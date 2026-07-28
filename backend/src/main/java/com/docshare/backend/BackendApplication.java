package com.docshare.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the docshare backend monolith.
 *
 * <p>This is a single deployable application (Phase 0 of the PRD's phased delivery plan) organized
 * internally as a set of feature modules — see the package-info.java in each of {@code auth},
 * {@code users}, {@code documents}, {@code storage}, {@code sharing}, and {@code common} — so that
 * later phases can extract individual modules into independent services without a redesign.
 */
@SpringBootApplication
public class BackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(BackendApplication.class, args);
  }
}
