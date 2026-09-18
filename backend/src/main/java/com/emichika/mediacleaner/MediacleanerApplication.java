package com.emichika.mediacleaner;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.File;

@SpringBootApplication
@RequiredArgsConstructor
public class MediacleanerApplication {

	private final DuplicateCheckService duplicateCheckService;

	public static void main(String[] args) {
		SpringApplication.run(MediacleanerApplication.class, args);
	}

	// ⚡実験のために一時的に復活させます！
	@Bean
	public CommandLineRunner runTest() {
		return args -> {
			// デスクトップの test フォルダにある本物の写真
			String testFilePath = "/home/emichika/Desktop/test/test_photo.jpg";
			File file = new File(testFilePath);

			if (file.exists()) {
				System.out.println("\n=== Run: Checking and cleaning file ===");
				duplicateCheckService.processMediaFile(file);
			} else {
				System.out.println("[ERROR] Test photo file not found at: " + testFilePath);
			}
		};
	}
}
