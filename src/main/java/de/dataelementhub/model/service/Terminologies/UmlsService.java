package de.dataelementhub.model.service.Terminologies;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    //search in all Sources
    public List<UmlsConcept> searchTerm(String searchTerm) throws IOException {
        try {
            // Step 1: fetch current version dynamically
            String version = "version at:" + LocalDate.now();
            String searchUrl = BASE_URL + "/search/current/";

            List<UmlsConcept> results = new ArrayList<>();
            int page = 0;
            boolean morePages = true;
            String translatedQuery = null;

            translatedQuery = translationService.translateText(searchTerm, "en");

            System.out.println("Translated term: " + translatedQuery);
            while (morePages) {
                page++;
                URI uri = UriComponentsBuilder.fromHttpUrl(searchUrl)
                        .queryParam("string", translatedQuery)
                        .queryParam("apiKey", apiKey)
                        .queryParam("pageNumber", page)
                        .build().encode(StandardCharsets.UTF_8).toUri();

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
            throw new RuntimeException("Error during UMLS search: " + e.getMessage(), e);
        }
    }
    //search in a specific Sources
    public List<UmlsConcept> searchTermInOntology(String searchTerm, String sabs) throws IOException {
        try {
            String version = "version at:" + LocalDate.now();
            String searchUrl = BASE_URL + "/search/current/";

            String translated = translationService.translateText(searchTerm, "en");

            List<UmlsConcept> results = new ArrayList<>();
            for (int page = 1; ; page++) {
                URI uri = UriComponentsBuilder.fromHttpUrl(searchUrl)
                        .queryParam("string", translated)
                        .queryParam("apiKey", apiKey)
                        .queryParam("pageNumber", page)
                        .queryParam("sabs", sabs)          //Ontology
                        .build()
                        .encode(StandardCharsets.UTF_8)
                        .toUri();

                ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
                JsonNode items = mapper.readTree(response.getBody()).at("/result/results");
                if (!items.isArray() || items.isEmpty()) break;

                for (JsonNode item : items) {
                    String ui = item.path("ui").asText();
                    String name = item.path("name").asText();
                    String root = item.path("rootSource").asText();
                    if (!"NONE".equals(ui) && root != null && !root.isEmpty() && root.equalsIgnoreCase(sabs)) {
                        results.add(new UmlsConcept(ui, name, root, version));
                    }
                }
            }
            return results;

        } catch (Exception e) {
            throw new RuntimeException("Error during UMLS search: " + e.getMessage(), e);
        }
    }
    // Get the list of all Sources
    public List<String> getAllOntologyIds() {
        String url = "https://uts-ws.nlm.nih.gov/rest/metadata/current/sources";
        UmlsSourcesResponse response = restTemplate.getForObject(url, UmlsSourcesResponse.class);

        if (response == null || response.getResult() == null) {
            return Collections.emptyList();
        }

        return response.getResult().stream()
                .map(UmlsSourceItem::getAbbreviation)
                .filter(Objects::nonNull)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
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
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UmlsSourcesResponse {
        private List<UmlsSourceItem> result;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UmlsSourceItem {
        private String abbreviation;  //ontology ID
        private String name;
        private String release;
        private String family;
    }
}
