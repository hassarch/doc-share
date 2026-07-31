import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("org.springframework.boot") version "3.3.4"
	id("io.spring.dependency-management") version "1.1.6"
	id("com.diffplug.spotless") version "6.25.0"
}

group = "com.docshare"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// --- Web / API ---
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")

	// --- Security / Auth ---
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

	// --- Persistence ---
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	runtimeOnly("org.postgresql:postgresql")
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")

	// --- Cache / Redis ---
	implementation("org.springframework.boot:spring-boot-starter-data-redis")

	// --- Messaging (Kafka — wired up starting Phase 3) ---
	implementation("org.springframework.kafka:spring-kafka")

	// --- WebSocket (real-time notifications) ---
	implementation("org.springframework.boot:spring-boot-starter-websocket")

	// --- Object storage (MinIO — wired up starting Phase 1) ---
	implementation("io.minio:minio:8.5.13")

	// --- Structured logging ---
	implementation("net.logstash.logback:logstash-logback-encoder:7.4")

	// --- Observability (metrics endpoint from day one, per ADR-0001 #7) ---
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	runtimeOnly("io.micrometer:micrometer-registry-prometheus")

	// --- Dev tooling ---
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	// --- Testing ---
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.springframework.kafka:spring-kafka-test")
	testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.2"))
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:postgresql")
	testImplementation("org.testcontainers:kafka")
	testImplementation("org.testcontainers:testcontainers")
	testImplementation("org.awaitility:awaitility:4.2.2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// Fast feedback loop: plain unit tests only, no Docker/Testcontainers
// needed. This is what `./gradlew test` runs by default, and what you'd
// run on every save while coding.
tasks.test {
	useJUnitPlatform {
		excludeTags("integration")
	}
}

// Full-system proof: everything tagged "integration" in
// AbstractPostgresIntegrationTest and its subclasses - real Postgres,
// Redis, MinIO, Kafka via Testcontainers. Needs Docker. Run explicitly
// with `./gradlew integrationTest`, and in CI as its own job/step.
tasks.register<Test>("integrationTest") {
	description = "Runs integration tests (real Postgres/Redis/MinIO/Kafka via Testcontainers)."
	group = "verification"
	useJUnitPlatform {
		includeTags("integration")
	}
	shouldRunAfter(tasks.test)
}

tasks.withType<BootJar> {
	archiveFileName.set("docshare-backend.jar")
}

spotless {
	java {
		googleJavaFormat()
		removeUnusedImports()
		trimTrailingWhitespace()
		endWithNewline()
	}
}
