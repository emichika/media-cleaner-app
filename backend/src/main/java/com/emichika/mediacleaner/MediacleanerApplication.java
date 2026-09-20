package com.emichika.mediacleaner;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RequiredArgsConstructor
public class MediacleanerApplication {

	// ★起動時の自動テストブロック(@Bean)はすべて消去して、これだけですっきりさせます！
	public static void main(String[] args) {
		SpringApplication.run(MediacleanerApplication.class, args);
	}
}
