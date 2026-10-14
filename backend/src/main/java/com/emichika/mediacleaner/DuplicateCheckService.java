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
     * Checks if the file is a duplicate or already formatted.
     * Moves to 'duplicates' if duplicate, renames if new, skips if already formatted.
     */
    public String processMediaFile(File file) {
        log.info("------------------------------------------------------");
        log.info("[START] Analyzing file: {}", file.getName());

        // ファイル名から拡張子を切り離す
        String originalName = file.getName();
        int dotIndex = originalName.lastIndexOf('.');
        String baseName = dotIndex >= 0 ? originalName.substring(0, dotIndex) : originalName;
        String extension = dotIndex >= 0 ? originalName.substring(dotIndex) : "";

        // ⚡【新仕様】もしすでにファイル名が「数字14桁（yyyyMMddHHmmss）」の形式なら、リネーム不要として大至急スキップ！
        if (baseName.matches("^\\d{14}$")) {
            log.info("[SKIP] File name '{}' is already formatted as yyyyMMddHHmmss. Skipping processing.", originalName);
            log.info("------------------------------------------------------");
            return originalName; // 今の名前のまま終了させる
        }

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

                Path targetPath = duplicateDir.resolve(originalName);
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

            Date shotAt = mediaService.getPhotoCreationDate(file);
            String baseFileName;

            // ⚡撮影日時が抜けたら「yyyyMMddHHmmss」にする。無ければ「unknown_元の名前」の迷子ルートへ
            if (shotAt != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                baseFileName = sdf.format(shotAt);
            } else {
                log.warn("[WARN] Could not determine creation date. Route to UNKNOWN file name.");
                baseFileName = "unknown_" + baseName;
            }

            try {
                String parentDir = file.getParent();
                String finalFileName = baseFileName + extension;
                Path targetPath = Path.of(parentDir, finalFileName);

                int count = 1;
                while (Files.exists(targetPath)) {
                    finalFileName = baseFileName + "_" + count + extension;
                    targetPath = Path.of(parentDir, finalFileName);
                    count++;
                }

                Files.move(file.toPath(), targetPath);
                log.info("[FILE RENAMED] Successfully renamed file to: {}", finalFileName);

                MediaFile newMediaFile = new MediaFile();
                newMediaFile.setFileName(finalFileName);
                newMediaFile.setFileHash(fileHash);
                newMediaFile.setShotAt(shotAt);

                mediaFileRepository.save(newMediaFile);
                log.info("[SUCCESS] Saved to MySQL.");
                log.info("------------------------------------------------------");
                return finalFileName;

            } catch (Exception e) {
                log.error("[ERROR] Failed to rename and save new file: ", e);
                return "error";
            }
        }
    }
}
