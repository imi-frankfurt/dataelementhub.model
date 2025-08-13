package de.dataelementhub.model.service.Terminologies;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class UmlsService {
    private final RestTemplate restTemplate;
    private final TranslationService translationService;
    @Value("${umls.apiKey}")
    private String apiKey;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String BASE_URL = "https://uts-ws.nlm.nih.gov/rest";
    @Autowired
    public UmlsService(RestTemplate restTemplate, TranslationService translationService) {
        this.restTemplate = restTemplate;
        this.translationService = translationService;
    }
    public List<UmlsConcept> searchTerm(String searchTerm) throws IOException {
        try {
            // Schritt 1: aktuelle Version dynamisch holen
            String version = "version at:" + LocalDate.now();
            String searchUrl = BASE_URL + "/search/current/";

            List<UmlsConcept> results = new ArrayList<>();
            int page = 0;
            boolean morePages = true;

            while (morePages) {
                page++;
                URI uri = UriComponentsBuilder.fromHttpUrl(searchUrl)
                        .queryParam("string", searchTerm)
                        .queryParam("apiKey", apiKey)
                        .queryParam("pageNumber", page)
//                        .queryParam("sabs", "SNOMEDCT_US") // optional
                        .build(true).toUri();

                ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
                JsonNode root = mapper.readTree(response.getBody());
                JsonNode items = root.at("/result/results");

                if (!items.isArray() || items.isEmpty()) break;

                for (JsonNode item : items) {
                    String term = item.path("ui").asText();
                    String text = item.path("name").asText();
                    String system = item.path("rootSource").asText();

                    if (!"NONE".equals(term) && system != null && !system.isEmpty()) {
                        results.add(new UmlsConcept(term, text, system, version));
                    }
                }
            }

            return results;

        } catch (Exception e) {
            throw new RuntimeException("Fehler bei UMLS-Suche: " + e.getMessage(), e);
        }
    }

    @Data
    public class UmlsConcept {
        private String term;
        private String text;
        private String system;
        private String version;

        public UmlsConcept(String term, String text, String system, String version) {
            this.term = term;
            this.text = text;
            this.system = system;
            this.version = version;
        }
    }
}
