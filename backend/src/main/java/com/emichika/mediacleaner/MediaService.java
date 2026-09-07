package com.emichika.mediacleaner;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Date;

@Slf4j
@Service
public class MediaService {

    /**
     * 画像ファイルからEXIFの撮影日時を抜き出すメソッド
     */
    public Date getPhotoCreationDate(File file) {
        try {
            // ファイルのメタデータ（EXIFなど）を丸ごと読み込む
            Metadata metadata = ImageMetadataReader.readMetadata(file);

            // EXIFの中の「撮影日時」が保管されているディレクトリを取得
            ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

            if (directory != null) {
                // 撮影日時（Original Date/Time）を抜き出す
                Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (date != null) {
                    log.info("撮影日時の抽出に成功しました: {}", date);
                    return date;
                }
            }
            log.warn("EXIFタグ内に撮影日時が見つかりませんでした。");
        } catch (Exception e) {
            log.error("画像の解析中にエラーが発生しました: ", e);
        }
        return null;
    }
}
