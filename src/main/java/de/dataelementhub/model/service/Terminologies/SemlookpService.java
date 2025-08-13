package de.dataelementhub.model.service.Terminologies;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service

public class SemlookpService {

    private final RestTemplate restTemplate;
    private final TranslationService translationService;

    @Autowired
    public SemlookpService(RestTemplate restTemplate, TranslationService translationService) {
        this.restTemplate = restTemplate;
        this.translationService = translationService;
    }

    // Suche über alle Ontologien
//    public List<Concept> searchAllOntologies(String query) {
//        List<String> ontologies = getAllOntologyIds();
//
//        List<Concept> allResults = new ArrayList<>();
//        for (String ontology : ontologies) {
//            allResults.addAll(searchInOntology(query, ontology));
//        }
//        return allResults;
//    }

    // Search in a specific ontology
    public List<Concept> searchInOntology(String query, String ontology) {

        String translatedQuery = null;
        try {
            translatedQuery = translationService.translateText(query, "en");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("Übersetzter Begriff: " + translatedQuery);

        String url = "https://semanticlookup.zbmed.de/ols/api/search?q=" + translatedQuery +
                "&ontology=" + ontology + "&obsoletes=false";

        SemanticLookupResponse response = restTemplate.getForObject(url, SemanticLookupResponse.class);
        if (response == null || response.getResponse() == null || response.getResponse().getDocs() == null) {
            return Collections.emptyList();
        }

        String version = fetchOntologyVersion(ontology);

        return response.getResponse().getDocs().stream()
                .map(doc -> {

                    String term = doc.getObo_id();
                    String termId = term != null && term.contains(":") ? term.substring(term.lastIndexOf(":") + 1) : term;

                    String system = doc.getIri();
                    String systemBase;
                    try {
                        URI uri = new URI(system);
                        systemBase = uri.getScheme() + "://" + uri.getHost();
                    } catch (Exception e) {
                        systemBase = system;
                    }
                    return new Concept(
                            termId,
                            doc.getLabel(),
                            systemBase,
                            version
                    );
                })
                .collect(Collectors.toList());
    }

    // Ontology-Version
    private String fetchOntologyVersion(String ontology) {
        String url = "https://semanticlookup.zbmed.de/ols/api/ontologies/" + ontology;
        OntologyMetadata metadata = restTemplate.getForObject(url, OntologyMetadata.class);

        if (metadata != null && metadata.getConfig() != null) {
            String version = metadata.getConfig().getVersion();
            if (version != null) {
                return version;
            }
        }
        return "version at: " + LocalDate.now();
    }

    // All Ontologies
    public List<String> getAllOntologyIds() {
        String url = "https://semanticlookup.zbmed.de/ols/api/ontologies";
        OntologyListResponse response = restTemplate.getForObject(url, OntologyListResponse.class);
        if (response == null || response.getEmbedded() == null || response.getEmbedded().getOntologies() == null) {
            return Collections.emptyList();
        }

        return response.getEmbedded().getOntologies().stream()
                .map(OntologyListItem::getOntologyId)
                .filter(Objects::nonNull)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }
    @Data
    public static class Concept {
        private String term;
        private String text;
        private String system;
        private String version;

        public Concept(String term, String text, String system, String version) {
            this.term = term;
            this.text = text;
            this.system = system;
            this.version = version;
        }
    }

    @Data
    public static class SemanticLookupResponse {
        private Response response;

        @Data
        public static class Response {
            private List<Doc> docs;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Doc {
            private String iri;
            private String label;
            private String obo_id;

            @JsonProperty("ontology_name")
            private String ontologyName;
        }
    }

    @Data
    public static class OntologyMetadata {
        private Config config;

        @Data
        public static class Config {
            private String version;
        }
    }

    @Data
    public static class OntologyListResponse {
        @JsonProperty("_embedded")
        private OntologyListEmbedded embedded;
    }
    @Data
    public static class OntologyListEmbedded {
        @JsonProperty("ontologies")
        private List<OntologyListItem> ontologies;
    }
    @Data
    public static class OntologyListItem {
        @JsonProperty("ontologyId")
        private String ontologyId;
    }
}