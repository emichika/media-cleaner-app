package com.emichika.mediacleaner;

import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

@Entity
@Table(name = "media_files")
@Getter
@Setter
@NoArgsConstructor // Lombokが引数なしのコンストラクタを自動作成
@AllArgsConstructor // Lombokがすべての引数を持つコンストラクタを自動作成
public class MediaFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 背番号（自動連番）

    @Column(nullable = false)
    private String fileName; // ファイル名

    @Column(nullable = false, unique = true)
    private String fileHash; // ⚡重複削除の鍵：ファイル中身の指紋（SHA-256）

    @Temporal(TemporalType.TIMESTAMP)
    private Date shotAt; // EXIFから抜いた撮影日時
}
