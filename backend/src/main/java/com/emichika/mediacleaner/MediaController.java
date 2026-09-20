package com.emichika.mediacleaner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
     * Reactから送られてきた写真ファイルを受け取り、重複チェック後に専用フォルダへ保存するAPI窓口
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile multipartFile) {
        log.info("[API] Received upload request from frontend: {}", multipartFile.getOriginalFilename());

        Map<String, Object> response = new HashMap<>();
        try {
            // 1. パソコンの中に「organized_photos（整理済み写真置き場）」フォルダの住所を作る
            String userHome = System.getProperty("user.home");
            Path organizedDir = Path.of(userHome, "Desktop", "organized_photos");

            if (!Files.exists(organizedDir)) {
                Files.createDirectories(organizedDir);
                log.info("[FOLDER CREATED] Created organized photos directory at: {}", organizedDir);
            }

            // 2. 画面から届いたファイルを、まずは解析用の一時ファイルとして保存
            File tempFile = File.createTempFile("upload-", multipartFile.getOriginalFilename());
            multipartFile.transferTo(tempFile);

            // ⚡【重要修正】サービスにファイルを渡す前に、「移動前の元のパス」を記憶しておく
            Path sourcePath = tempFile.toPath();

            // 3. 重複チェックを実行し、結果（"duplicate" または 「確定した新しいファイル名」）を受け取る
            String result = duplicateCheckService.processMediaFile(tempFile);
            String resultStatus = "duplicate".equals(result) || "error".equals(result) ? result : "success";

            // 4. 【新規ファイルの場合】確定したファイル名で「organized_photos」フォルダへ永久保存！
            if ("success".equals(resultStatus)) {
                String finalFileName = result;
                Path finalTargetPath = organizedDir.resolve(finalFileName);

                // ⚡【重要修正】すでにサービス側でリネーム（移動）された本物のファイルをコピー元に指定します
                // サービス内でファイルは同じ親フォルダ（/tmp）の新しい名前、または duplicates フォルダに動いています
                // ここでは、サービス内で移動した先のファイルを追跡してコピーします
                File renamedFile = new File(tempFile.getParent(), finalFileName);
                if (renamedFile.exists()) {
                    Files.copy(renamedFile.toPath(), finalTargetPath, StandardCopyOption.REPLACE_EXISTING);
                    log.info("[FILE ARCHIVED] Successfully saved new photo to storage: {}", finalTargetPath);
                    // 用が済んだのでリネーム後のファイルもお掃除
                    renamedFile.delete();
                }
            } else if ("duplicate".equals(resultStatus)) {
                // 重複ファイルだった場合は、サービス側ですでに「duplicates」フォルダへお引越しが完了しています
                log.info("[API PROCESS] Duplicate file moved to duplicates directory by service.");
            }

            // 万が一残っていた場合の一時ファイルのお掃除
            if (tempFile.exists()) {
                tempFile.delete();
            }

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
