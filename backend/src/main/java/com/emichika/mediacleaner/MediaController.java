package com.emichika.mediacleaner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class MediaController {

    private final DuplicateCheckService duplicateCheckService;

    /**
     * Reactから送られてきた写真ファイルを受け取るAPI窓口
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile multipartFile) {
        log.info("[API] Received upload request from frontend: {}", multipartFile.getOriginalFilename());

        Map<String, Object> response = new HashMap<>();
        try {
            File tempFile = File.createTempFile("upload-", multipartFile.getOriginalFilename());
            multipartFile.transferTo(tempFile);

            // 1. ⚡サービスを実行して、本物の判定結果（"success" か "duplicate"）を受け取る！
            String resultStatus = duplicateCheckService.processMediaFile(tempFile);

            tempFile.delete();

            // 2. ⚡Reactがそのまま使えるように、結果のステータスをレスポンスにガチッと詰め込む！
            response.put("status", resultStatus);
            response.put("message", "File processed with status: " + resultStatus);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[API ERROR] Failed to process uploaded file", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
