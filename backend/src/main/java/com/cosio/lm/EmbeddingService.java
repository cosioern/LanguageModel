package com.cosio.lm;

import java.util.Map;
import java.util.UUID;
// import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.transaction.Transactional;

@Service
public class EmbeddingService {
    
    /** client used to call microservice on localhost port 8000 */
    private final WebClient client;
    /** holds chunks of text, tied to a document and a guest */
    private final ChunkRepository chunkRepo;
    /** query documents by account, getting id and */
    private final DocumentRepository docRepo;

    /**
     * Constructor, autoinjection
     * @param client used to communicate with micro-service endpoints
     */
    public EmbeddingService(WebClient client, ChunkRepository chunkRepo, DocumentRepository docRepo) {
        this.client = client;
        this.chunkRepo = chunkRepo;
        this.docRepo = docRepo;
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
        // UUID documentID = UUID.randomUUID();
        Document doc = new Document(file.getOriginalFilename(), account);
        docRepo.save(doc);
        List<Chunk> chunks = new ArrayList<Chunk>();
        for(EmbeddedChunk e : embeddings) {
            // chunks.add(new Chunk(e.content(), e.embedding(), documentID, account));
            chunks.add(new Chunk(e.content, e.embedding(), doc.getID(), account));
        }
        chunkRepo.saveAll(chunks);
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
        
        List<Chunk> chunks = chunkRepo.findSimilarChunks(account.getID(), promptVector, 3);
        List<String> results = new ArrayList<String>();
        for (Chunk c : chunks) {
            results.add(c.getContent());
        }

        return results;
    }

    /**
     * Produce list of details for each document
     * that a user has uploaded to persistence.
     * 
     * @param account   of the user looking up their documents
     * @return          a list of FileRecords (accountID, filename)
     */
    public List<FileRecord> getDocumentNames(Account account) {
        List<FileRecord> fileRecords = new ArrayList<>();
        for (Document doc : docRepo.findByAccount(account)) {
            fileRecords.add(new FileRecord(doc.getID(), doc.getFilename()));
        }
        
        return fileRecords;
    }

    /**
     * Remove a document and its associated chunks from persistence.
     * 
     * @param account       the document should belong to
     * @param documentID    identifies the chunks and document for deletion
     * @return              true if successful, false otherwise
     */
    @Transactional
    public boolean deleteDocument(Account account, UUID documentID) {
        Document doc = docRepo.findById(documentID).orElse(null);
        if ((doc == null) || !(doc.getAccount().getID().equals(account.getID()))) 
            return false;

        chunkRepo.deleteAllByDocumentID(documentID);
        docRepo.delete(doc);
        
        return true;
    }

    private record EmbeddedChunk(String content, float[] embedding) {}
}