package com.emichika.mediacleaner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DuplicateCheckService {

    private final MediaService mediaService;
    private final MediaFileRepository mediaFileRepository;

    /**
     * Checks if the file is a duplicate.
     * Moves to 'duplicates' if duplicate, renames with yyyyMMddHHmmss and auto-sequence if new.
     * @return "success" if new, "duplicate" if moved.
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

            try {
                String parentDir = file.getParent();
                Path duplicateDir = Path.of(parentDir, "duplicates");
                if (!Files.exists(duplicateDir)) {
                    Files.createDirectories(duplicateDir);
                    log.info("[FOLDER CREATED] Created duplicates directory at: {}", duplicateDir);
                }

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
            log.info("[NEW FILE] No duplicates found. Proceeding with auto-rename.");

            // 1. 撮影日時を抜き出す
            Date shotAt = mediaService.getPhotoCreationDate(file);
            String baseFileName;
            String extension = "";

            // ファイル名から拡張子（.jpgなど）を切り離す
            String originalName = file.getName();
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = originalName.substring(dotIndex); // 例: ".jpg"
            }

            // 2. ⚡リネームのベース名を「yyyyMMddHHmmss」にする（無ければ元の名前をそのまま使用）
            if (shotAt != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                baseFileName = sdf.format(shotAt); // 例: "20050228124729"
            } else {
                log.warn("[WARN] Could not determine creation date. Using original name as base.");
                baseFileName = dotIndex >= 0 ? originalName.substring(0, dotIndex) : originalName;
            }

            try {
                String parentDir = file.getParent();
                String finalFileName = baseFileName + extension;
                Path targetPath = Path.of(parentDir, finalFileName);

                // 3. ⚡【安全連番ループ機能】もし同名ファイルがすでに存在する場合、空く番号（_1, _2...）を探す
                int count = 1;
                while (Files.exists(targetPath)) {
                    finalFileName = baseFileName + "_" + count + extension; // 例: "20050228124729_1.jpg"
                    targetPath = Path.of(parentDir, finalFileName);
                    count++;
                }

                // 実際にパソコンの物理ファイルの名前を書き換える
                Files.move(file.toPath(), targetPath);
                log.info("[FILE RENAMED] Successfully renamed file to: {}", finalFileName);

                // MySQLへは「連番を考慮した最終的な新しい名前」で保存！
                MediaFile newMediaFile = new MediaFile();
                newMediaFile.setFileName(finalFileName);
                newMediaFile.setFileHash(fileHash);
                newMediaFile.setShotAt(shotAt);

                MediaFile saved = mediaFileRepository.save(newMediaFile);
                log.info("[SUCCESS] Saved to MySQL. Assigned ID: {}", saved.getId());
                log.info("------------------------------------------------------");
                return finalFileName;

            } catch (Exception e) {
                log.error("[ERROR] Failed to rename and save new file: ", e);
                return "error";
            }
        }
    }
}
