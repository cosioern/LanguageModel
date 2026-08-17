package com.cosio.lm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface ChunkRepository extends JpaRepository<Chunk, UUID>{
    @Query(value = "SELECT * FROM chunk WHERE accountID = :accountID ORDER BY embedding <-> CAST(:queryVector AS vector) LIMIT :k", nativeQuery = true)
    List<Chunk> findSimilarChunks(@Param("accountID") UUID accountID, @Param("queryVector") float[] queryVector, @Param("k") int k);

    List<Chunk> findByAccount(Account account);
    void deleteByAccount(Account account);
    void deleteAllByDocumentID(UUID documentID);
}
