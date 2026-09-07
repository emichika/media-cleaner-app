package com.emichika.mediacleaner;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {
    // ⚡「同じハッシュ値（中身）の画像がすでにDBにあるか」を一瞬で探す魔法のメソッド
    Optional<MediaFile> findByFileHash(String fileHash);
}
