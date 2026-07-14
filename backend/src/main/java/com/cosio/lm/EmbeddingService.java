package com.cosio.lm;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.id.UUIDGenerator;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.pgvector.PGvector;

@Service
public class EmbeddingService {
    
    /** client used to call microservice on localhost port 8000 */
    private final WebClient client;
    private final ChunkRepository repo;


    /**
     * Constructor, autoinjection
     * @param client used to communicate with micro-service endpoints
     */
    public EmbeddingService(WebClient client, ChunkRepository repo) {
        this.client = client;
        this.repo = repo;
    }
    

    public float[] embedPrompt(String prompt) {

        float[] embedding = client.post()
            .uri("/embedPrompt")
            .bodyValue(Map.of("role", "user", "content", prompt))
            .retrieve()
            .bodyToMono(float[].class)
            .block();

        return embedding;
    }

    /**
     * Embed a document and save it as chunks
     * 
     * @param file
     * @return
     */
    public void embedDocument(MultipartFile file) {

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", file.getResource());

        List<EmbeddedChunk> embeddings = client.post()
            .uri("/embedDocument")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .bodyValue(builder.build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<EmbeddedChunk>>() {})
            .block();
        

        // package chunks and save to persistence
        UUID documentID = UUID.randomUUID();
        List<Chunk> chunks = new ArrayList<Chunk>();
        for(EmbeddedChunk e : embeddings) {
            chunks.add(new Chunk(e.chunk, new PGvector(e.embedding()), documentID));
        }
        repo.saveAll(chunks);

        return;
    }

    // public float similaritySearch(String prompt) {
    //     float[] embedding = embedPrompt(prompt);
    //     PGvector queryVector = new PGvector(embedding);

    //     List<Chunk> results = repo.findSimilarChunks(queryVector, 5);

    //     return 0;
    // }

    private record EmbeddedChunk(String chunk, float[] embedding) {}

}
