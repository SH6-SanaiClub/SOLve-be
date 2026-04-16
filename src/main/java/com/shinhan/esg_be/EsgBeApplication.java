package com.shinhan.esg_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.TimeZone;

@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
@EnableScheduling
@SpringBootApplication
@ConfigurationPropertiesScan
public class EsgBeApplication {

	private static final ZoneId APP_ZONE = ZoneId.of("Asia/Seoul");

	static {
		System.setProperty("user.timezone", APP_ZONE.getId());
		TimeZone.setDefault(TimeZone.getTimeZone(APP_ZONE));
	}

	public static void main(String[] args) {
		SpringApplication.run(EsgBeApplication.class, args);
	}

	@Bean
	public Clock clock() {
		return Clock.system(APP_ZONE);
	}

	@Bean
	public DateTimeProvider auditingDateTimeProvider(Clock clock) {
		return () -> Optional.of(LocalDateTime.now(clock));
	}

}
