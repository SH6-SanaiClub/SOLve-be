package com.shinhan.esg_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
@SpringBootApplication
public class EsgBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(EsgBeApplication.class, args);
	}

	@Bean
	public Clock clock() {
		return Clock.systemDefaultZone();
	}

	@Bean
	public DateTimeProvider auditingDateTimeProvider(Clock clock) {
		return () -> Optional.of(LocalDateTime.now(clock));
	}

}
