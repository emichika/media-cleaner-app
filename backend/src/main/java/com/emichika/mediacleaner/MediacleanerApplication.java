package com.emichika.mediacleaner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.io.File;
import java.util.Date;
import java.util.UUID;

@SpringBootApplication
public class MediacleanerApplication {

	@Autowired
	private MediaService mediaService;

	@Autowired
	private MediaFileRepository mediaFileRepository; // リモコンを合体！

	public static void main(String[] args) {
		SpringApplication.run(MediacleanerApplication.class, args);
	}

	@Bean
	public CommandLineRunner runTest() {
		return args -> {
			System.out.println("============== 🐳 MySQL保存テストを開始します ==============");

			String testFilePath = "/home/emichika/Desktop/test/test_photo.jpg";
			File file = new File(testFilePath);

			if (file.exists()) {
				// 1. 撮影日を抜く
				Date shotAt = mediaService.getPhotoCreationDate(file);

				// 2. 指紋（ハッシュ）の代わりに、テスト用のランダムなIDを生成
				String fakeHash = UUID.randomUUID().toString();

				// 3. データの箱を組み立てる
				MediaFile mediaFile = new MediaFile();
				mediaFile.setFileName(file.getName());
				mediaFile.setFileHash(fakeHash);
				mediaFile.setShotAt(shotAt);

				// 4. リモコンを使ってMySQLへガツンと保存！
				MediaFile savedFile = mediaFileRepository.save(mediaFile);

				System.out.println("✨ MySQLへの保存に大成功しました！背番号(ID): " + savedFile.getId());

			} else {
				System.out.println("❌ 写真ファイルが見つかりません。");
			}
			System.out.println("============================================================");
		};
	}
}
