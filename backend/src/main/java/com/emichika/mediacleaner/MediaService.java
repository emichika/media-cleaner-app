package com.emichika.mediacleaner;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.mov.QuickTimeDirectory;
import com.drew.metadata.mp4.Mp4Directory;
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
     * 画像(JPEG/HEIC)や動画(MP4/MOV)から撮影日時・作成日時を引っこ抜くマルチメソッド
     */
    public Date getPhotoCreationDate(File file) {
        try {
            // ImageMetadataReaderは賢いので、ファイルの拡張子を見て自動で動画か写真かを判別してくれます
            Metadata metadata = ImageMetadataReader.readMetadata(file);

            // 1. 【写真ルート】通常のデジカメ写真(JPEG)やiPhoneの写真(HEIC)の場合
            ExifSubIFDDirectory exifDir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (exifDir != null) {
                Date date = exifDir.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (date != null) {
                    log.info("[METADATA] Found EXIF creation date: {}", date);
                    return date;
                }
            }

            // 2. 【標準動画ルート】MP4動画の場合
            Mp4Directory mp4Dir = metadata.getFirstDirectoryOfType(Mp4Directory.class);
            if (mp4Dir != null) {
                Date date = mp4Dir.getDate(Mp4Directory.TAG_CREATION_TIME);
                if (date != null) {
                    log.info("[METADATA] Found MP4 creation date: {}", date);
                    return date;
                }
            }

            // 3. 【Apple動画ルート】iPhoneのMOV動画などの場合
            QuickTimeDirectory qkDir = metadata.getFirstDirectoryOfType(QuickTimeDirectory.class);
            if (qkDir != null) {
                Date date = qkDir.getDate(QuickTimeDirectory.TAG_CREATION_TIME);
                if (date != null) {
                    log.info("[METADATA] Found QuickTime(MOV) creation date: {}", date);
                    return date;
                }
            }

            log.warn("[WARN] Creation date/time not found in metadata for file: {}", file.getName());
        } catch (Exception e) {
            log.error("[ERROR] Failed to analyze media metadata for: " + file.getName(), e);
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
