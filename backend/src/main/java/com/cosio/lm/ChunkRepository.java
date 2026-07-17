package com.cosio.lm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
// import com.pgvector.PGvector;
import java.util.List;
import java.util.UUID;

public interface ChunkRepository extends JpaRepository<Chunk, UUID>{
    // @Query (value = "SELECT * FROM Chunk WHERE guestID = :g ORDER BY embedding <-> CAST(:queryVector AS vector) LIMIT :k", nativeQuery = true)
    // List<Chunk> findSimilarChunks(@Param("g") UUID g, @Param("queryVector") PGvector queryVector, @Param("k") int k);

    @Query(value = "SELECT * FROM chunk WHERE guestID = :guestID ORDER BY embedding <-> CAST(:embedding AS vector) LIMIT :limit", nativeQuery = true)
List<Chunk> findSimilarChunks(
    @Param("guestID") UUID guestID,
    @Param("embedding") String embedding,
    @Param("limit") int limit
);
}
