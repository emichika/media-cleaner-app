package com.emichika.mediacleaner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
public class DuplicateCheckService {

    @Autowired
    private MediaService mediaService;

    @Autowired
    private MediaFileRepository mediaFileRepository;

    /**
     * Checks if the file is a duplicate. Saves to DB if new, skips if duplicate.
     */
    public void processMediaFile(File file) {
        log.info("------------------------------------------------------");
        log.info("[START] Analyzing file: {}", file.getName());

        String fileHash = mediaService.calculateFileHash(file);
        if (fileHash == null) {
            log.error("[ERROR] Canceled processing due to file analysis failure.");
            return;
        }

        Optional<MediaFile> existingFile = mediaFileRepository.findByFileHash(fileHash);

        if (existingFile.isPresent()) {
            MediaFile dbFile = existingFile.get();
            log.warn("[DUPLICATE DETECTED] This image already exists in the database.");
            log.warn("   -> Original File Name: {}", dbFile.getFileName());
            log.warn("   -> Registered ID: {}", dbFile.getId());
            log.info("[RESULT] File skipped.");
        } else {
            log.info("[NEW FILE] No duplicates found. Registering to the database.");

            Date shotAt = mediaService.getPhotoCreationDate(file);

            MediaFile newMediaFile = new MediaFile();
            newMediaFile.setFileName(file.getName());
            newMediaFile.setFileHash(fileHash);
            newMediaFile.setShotAt(shotAt);

            MediaFile saved = mediaFileRepository.save(newMediaFile);
            log.info("[SUCCESS] Saved to MySQL. Assigned ID: {}", saved.getId());
        }
        log.info("------------------------------------------------------");
    }
}
