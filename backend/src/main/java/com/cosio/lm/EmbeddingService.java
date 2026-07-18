package com.cosio.lm;

import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class EmbeddingService {
    
    /** client used to call microservice on localhost port 8000 */
    private final WebClient client;
    /** holds chunks of text, tied to a document and a guest */
    private final ChunkRepository repo;


    /**
     * Constructor, autoinjection
     * @param client used to communicate with micro-service endpoints
     */
    public EmbeddingService(WebClient client, ChunkRepository repo) {
        this.client = client;
        this.repo = repo;
    }
    
    /**
     * Embeds a user's prompt to be used in a similarity search.
     * 
     * @param prompt to be embededded 
     * @return a PGvector of the embedded promt
     */
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
    public void embedDocument(MultipartFile file, Account account) {

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
            chunks.add(new Chunk(e.content(), e.embedding(), documentID, account));
        }
        repo.saveAll(chunks);

        return;
    }

    /**
     * Perform a similaity search on a guest's documents.
     * 
     * @param promptVector is the guest's embedded prompt
     * @param guest is the guest for whose documents should be searched through
     * @return a list of the top 3 most similar chunks of text
     */
    public List<String> similaritySearch(float[] promptVector, Account account) {
        
        List<Chunk> chunks = repo.findSimilarChunks(account.getID(), promptVector, 3);
        List<String> results = new ArrayList<String>();
        for (Chunk c : chunks) {
            results.add(c.getContent());
        }

        return results;
    }

    private record EmbeddedChunk(String content, float[] embedding) {}
}
