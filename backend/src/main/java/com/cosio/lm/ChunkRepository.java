package com.cosio.lm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.pgvector.PGvector;
import java.util.List;
import java.util.UUID;

public interface ChunkRepository extends JpaRepository<Chunk, UUID>{
    
    @Query (value = "SELECT * FROM Chunk ORDER BY embedding <-> queryVector LIMIT :K", nativeQuery = true)
    List<Chunk> findSimilarChunks(@Param("queryVector") PGvector queryVector, @Param("k") int k);

}
