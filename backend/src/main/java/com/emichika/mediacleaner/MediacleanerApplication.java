package com.emichika.mediacleaner;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.File;

@SpringBootApplication
@RequiredArgsConstructor // ★Lombokが推奨されるコンストラクタを自動生成します
public class MediacleanerApplication {

	// ★finalをつけることで、安全に自動で注入（インジェクション）されます
	private final DuplicateCheckService duplicateCheckService;

	public static void main(String[] args) {
		SpringApplication.run(MediacleanerApplication.class, args);
	}

//	@Bean
//	public CommandLineRunner runTest() {
//		return args -> {
//			String testFilePath = "/home/emichika/Desktop/test/test_photo.jpg";
//			File file = new File(testFilePath);
//
//			if (file.exists()) {
//				System.out.println("\n=== Run 1: Expecting New Registration ===");
//				duplicateCheckService.processMediaFile(file);
//
//				System.out.println("\n=== Run 2: Expecting Duplicate Detection ===");
//				duplicateCheckService.processMediaFile(file);
//			} else {
//				System.out.println("[ERROR] Test photo file not found at: " + testFilePath);
//			}
//		};
//	}
}
