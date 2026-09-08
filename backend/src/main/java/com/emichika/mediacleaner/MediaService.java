package com.emichika.mediacleaner;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.Date;

@Slf4j
@Service
public class MediaService {

    /**
     * Extracts the photo creation date from the EXIF metadata.
     */
    public Date getPhotoCreationDate(File file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

            if (directory != null) {
                Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (date != null) {
                    return date;
                }
            }
            log.warn("[WARN] Original date/time not found in EXIF tags: {}", file.getName());
        } catch (Exception e) {
            log.error("[ERROR] Failed to analyze image metadata: ", e);
        }
        return null;
    }

    /**
     * Calculates the SHA-256 hash value from the file content.
     */
    public String calculateFileHash(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] byteArray = new byte[1024];
            int bytesCount;

            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }

            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }

            String hash = sb.toString();
            log.info("[SUCCESS] Calculated file hash: {}", hash);
            return hash;
        } catch (Exception e) {
            log.error("[ERROR] Failed to calculate file hash: ", e);
            return null;
        }
    }
}
