package com.emichika.mediacleaner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DuplicateCheckService {

    private final MediaService mediaService;
    private final MediaFileRepository mediaFileRepository;

    /**
     * Checks if the file is a duplicate. Saves to DB if new, moves to 'duplicates' folder if duplicate.
     * @return "success" if new, "duplicate" if moved to duplicate folder.
     */
    public String processMediaFile(File file) {
        log.info("------------------------------------------------------");
        log.info("[START] Analyzing file: {}", file.getName());

        String fileHash = mediaService.calculateFileHash(file);
        if (fileHash == null) {
            log.error("[ERROR] Canceled processing due to file analysis failure.");
            return "error";
        }

        Optional<MediaFile> existingFile = mediaFileRepository.findByFileHash(fileHash);

        if (existingFile.isPresent()) {
            MediaFile dbFile = existingFile.get();
            log.warn("[DUPLICATE DETECTED] This image already exists in the database. (ID: {})", dbFile.getId());

            // ⚡【物理お掃除機能】重複ファイルを「duplicates」フォルダへ自動移動させる
            try {
                // 1. 写真が置いてある親フォルダの場所を取得
                String parentDir = file.getParent();

                // 2. その中に「duplicates」という名前の移動先フォルダの住所を作る
                Path duplicateDir = Path.of(parentDir, "duplicates");

                // 3. フォルダがまだパソコン内になければ、Javaが自動で作成する
                if (!Files.exists(duplicateDir)) {
                    Files.createDirectories(duplicateDir);
                    log.info("[FOLDER CREATED] Created duplicates directory at: {}", duplicateDir);
                }

                // 4. 重複ファイルを「duplicates/写真名.jpg」へシュッと移動！
                Path targetPath = duplicateDir.resolve(file.getName());
                Files.move(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                log.info("[FILE MOVED] Successfully moved duplicate file to: {}", targetPath);
                log.info("------------------------------------------------------");
                return "duplicate";

            } catch (Exception e) {
                log.error("[ERROR] Failed to move duplicate file: ", e);
                return "error";
            }
        } else {
            log.info("[NEW FILE] No duplicates found. Registering to the database.");

            Date shotAt = mediaService.getPhotoCreationDate(file);

            MediaFile newMediaFile = new MediaFile();
            newMediaFile.setFileName(file.getName());
            newMediaFile.setFileHash(fileHash);
            newMediaFile.setShotAt(shotAt);

            MediaFile saved = mediaFileRepository.save(newMediaFile);
            log.info("[SUCCESS] Saved to MySQL. Assigned ID: {}", saved.getId());
            log.info("------------------------------------------------------");
            return "success";
        }
    }
}
