package com.cafe.backend;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		// All order times (estimatedReadyAt, createdAt, ...) are LocalDateTime values.
		// The customer's phone reads them as local time, so the server must use the
		// cafe's time zone even when it runs on a cloud machine set to UTC.
		// Override with the APP_TIMEZONE environment variable if a client is elsewhere.
		String zone = System.getenv().getOrDefault("APP_TIMEZONE", "Asia/Kolkata");
		TimeZone.setDefault(TimeZone.getTimeZone(zone));

		SpringApplication.run(BackendApplication.class, args);

		System.out.println("Application App");
	}

}
