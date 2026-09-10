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
@CrossOrigin(origins = "*", allowedHeaders = "*") // ⚡Reactからの通信を絶対に弾かないための最強の許可証
public class MediaController {

    private final DuplicateCheckService duplicateCheckService;

    /**
     * Reactから送られてきた写真ファイルを受け取るAPI窓口
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile multipartFile) {
        // ★確実にログが出るように、メソッドの最優先の1行目に配置しました！
        log.info("[API] Received upload request from frontend: {}", multipartFile.getOriginalFilename());

        Map<String, Object> response = new HashMap<>();
        try {
            // 1. 画面から送られてきたデータを、Javaが解析できる一時的な本物のファイルに変換
            File tempFile = File.createTempFile("upload-", multipartFile.getOriginalFilename());
            multipartFile.transferTo(tempFile);

            // 2. 重複チェックサービスを呼び出す
            duplicateCheckService.processMediaFile(tempFile);

            // 3. 一時ファイルを綺麗にお掃除
            tempFile.delete();

            // 4. 成功のお返事をReactへ返す
            response.put("status", "success");
            response.put("message", "File processed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[API ERROR] Failed to process uploaded file", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
