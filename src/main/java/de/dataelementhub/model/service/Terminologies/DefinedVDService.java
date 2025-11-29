package de.dataelementhub.model.service.Terminologies;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.*;
@Service
public class DefinedVDService {
    private final RestTemplate restTemplate;
    private final TranslationService translationService;
    @Value("${loinc.regentief-api.username}")
    private String username;

    @Value("${loinc.regentief-api.password}")
    private String password;
    /**
     * Constructor for injecting RestTemplate.
     */
    @Autowired
    public DefinedVDService(RestTemplate restTemplate, TranslationService translationService) {
        this.restTemplate = restTemplate;
        this.translationService = translationService;
    }
    /**
     * Create Basic Auth headers with encoded username and password for LOINC.
     */
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);
        return headers;
    }
    /**
     * Search LOINC concepts FHIR ValueSet API using free text.
     *
     * @param query Text to search for
     * @return List of LoincConcept objects
     */
    public List<ValueSetResponse> loincValueSet(String query) {
        if (query == null || query.trim().isEmpty()) {
            System.err.println("Query parameter is empty or null.");
            return Collections.emptyList();
        }

        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            System.out.println("Begriff: " + query);
            String translatedQuery = translationService.translateText(query, "en");
            System.out.println("Übersetzter Begriff: " + translatedQuery);

            String finalUrl = UriComponentsBuilder
                    .fromHttpUrl("https://fhir.loinc.org/ValueSet")
                    .queryParam("name:in", translatedQuery)
                    .build()
                    .toUriString();

            System.out.println("Final URL: " + finalUrl);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    finalUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("entry")) return Collections.emptyList();

            List<Map<String, Object>> entries = (List<Map<String, Object>>) body.get("entry");

            // Map<Name, Resource with the highest version>
            Map<String, Map<String, Object>> latestPerName = new HashMap<>();

            for (Map<String, Object> entry : entries) {
                Map<String, Object> resource = (Map<String, Object>) entry.get("resource");
                if (resource == null) continue;

                String name = (String) resource.get("name");
                String version = (String) resource.get("version");

                if (name == null || version == null) continue;

                Map<String, Object> existing = latestPerName.get(name);
                if (existing == null || compareVersionStrings(version, (String) existing.get("version")) > 0) {
                    latestPerName.put(name, resource);
                }
            }

            // Jetzt bauen wir ValueSetResponse pro Name
            List<ValueSetResponse> responses = new ArrayList<>();

            for (Map.Entry<String, Map<String, Object>> entry : latestPerName.entrySet()) {
                Map<String, Object> resource = entry.getValue();
                String version = (String) resource.get("version");
                String subsetUri = (String) resource.get("url");
                String id = (String) resource.get("id");

                List<ValueSet> items = new ArrayList<>();
                Map<String, Object> compose = (Map<String, Object>) resource.get("compose");
                if (compose != null) {
                    List<Map<String, Object>> includes = (List<Map<String, Object>>) compose.get("include");
                    if (includes != null) {
                        for (Map<String, Object> include : includes) {
                            List<Map<String, Object>> concepts = (List<Map<String, Object>>) include.get("concept");
                            if (concepts != null) {
                                for (Map<String, Object> concept : concepts) {
                                    String code = (String) concept.get("code");
                                    String display = (String) concept.get("display");
                                    items.add(new ValueSet(code, display));
                                }
                            }
                        }
                    }
                }

                responses.add(new ValueSetResponse(entry.getKey(), version, id, subsetUri, items));
            }

            return responses;

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
    /**
     * Search Snomed concepts FHIR ValueSet API using free text.
     *
     * @param query Text to search for
     * @return List of Concept objects
     */
    public List<ValueSetResponse> snomedValueSet(String query) {
        if (query == null || query.trim().isEmpty()) {
            System.err.println("Query parameter is empty or null.");
            return Collections.emptyList();
        }

        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String translatedQuery = translationService.translateText(query, "en");
            System.out.println("translated query: " + translatedQuery);

            String finalUrl = UriComponentsBuilder
                    .fromHttpUrl("https://snowstorm-fhir.snomedtools.org/fhir/ValueSet/$expand")
                    .queryParam("url", "http://snomed.info/sct?fhir_vs")
                    .queryParam("filter", translatedQuery)
                    .build(true)
                    .toUriString();

            System.out.println("Final URL: " + finalUrl);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    finalUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body == null || !"ValueSet".equals(body.get("resourceType"))) {
                return Collections.emptyList();
            }

            String name   = (String) body.get("name");
            String url    = (String) body.get("url");
            String id     = (String) body.get("id");
            String ver    = (String) body.get("version");

            Map<String, Object> expansion = (Map<String, Object>) body.get("expansion");
            if (expansion == null) return Collections.emptyList();

            List<Map<String, Object>> contains = (List<Map<String, Object>>) expansion.get("contains");
            if (contains == null) return Collections.emptyList();

            List<ValueSet> items = new ArrayList<>();
            for (Map<String, Object> c : contains) {
                String code    = (String) c.get("code");
                String display = (String) c.get("display");
                items.add(new ValueSet(code, display));
            }

            ValueSetResponse vsr = new ValueSetResponse(
                    name != null ? name : "SNOMED implicit VS",
                    ver,
                    id,
                    url,
                    items
            );
            return Arrays.asList(vsr);

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
    /**
     * Search concepts FHIR TS ValueSet API using free text.
     *
     * @param query Text to search for
     * @return List of Concept objects
     */
    public List<ValueSetResponse> fhirValueSet(String query) {
        if (query == null || query.trim().isEmpty()) {
            System.err.println("Query parameter is empty or null.");
            return Collections.emptyList();
        }

        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            System.out.println("Begriff: " + query);
            String translatedQuery = translationService.translateText(query, "en");
            System.out.println("Übersetzter Begriff: " + translatedQuery);

            String finalUrl = UriComponentsBuilder
                    .fromHttpUrl("https://tx.fhir.org/r4/ValueSet")
                    .queryParam("name:in", translatedQuery)
                    .build()
                    .toUriString();

            System.out.println("Final URL: " + finalUrl);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    finalUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("entry")) return Collections.emptyList();

            List<Map<String, Object>> entries = (List<Map<String, Object>>) body.get("entry");

            // Map<Name, Resource with the highest version>
            Map<String, Map<String, Object>> latestPerName = new HashMap<>();

            for (Map<String, Object> entry : entries) {
                Map<String, Object> resource = (Map<String, Object>) entry.get("resource");
                if (resource == null) continue;

                String name = (String) resource.get("name");
                String version = (String) resource.get("version");

                if (name == null || version == null) continue;

                Map<String, Object> existing = latestPerName.get(name);
                if (existing == null || compareVersionStrings(version, (String) existing.get("version")) > 0) {
                    latestPerName.put(name, resource);
                }
            }

            // Jetzt bauen wir ValueSetResponse pro Name
            List<ValueSetResponse> responses = new ArrayList<>();

            for (Map.Entry<String, Map<String, Object>> entry : latestPerName.entrySet()) {
                Map<String, Object> resource = entry.getValue();
                String version = (String) resource.get("version");
                String subsetUri = (String) resource.get("url");
                String id = (String) resource.get("id");

                List<ValueSet> items = new ArrayList<>();
                Map<String, Object> compose = (Map<String, Object>) resource.get("compose");
                if (compose != null) {
                    List<Map<String, Object>> includes = (List<Map<String, Object>>) compose.get("include");
                    if (includes != null) {
                        for (Map<String, Object> include : includes) {
                            List<Map<String, Object>> concepts = (List<Map<String, Object>>) include.get("concept");
                            if (concepts != null) {
                                for (Map<String, Object> concept : concepts) {
                                    String code = (String) concept.get("code");
                                    String display = (String) concept.get("display");
                                    items.add(new ValueSet(code, display));
                                }
                            }
                        }
                    }
                }

                responses.add(new ValueSetResponse(entry.getKey(), version, id, subsetUri, items));
            }

            return responses;

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
    /**
     * Comparison of version strings such as ‘Loinc_2.77-2.77’ or ‘2.77’
     */
    private int compareVersionStrings(String v1, String v2) {
        if (v1 == null) return -1;
        if (v2 == null) return 1;

        String n1 = v1.replaceAll("[^0-9.]", "");
        String n2 = v2.replaceAll("[^0-9.]", "");

        try {
            return Double.compare(Double.parseDouble(n1), Double.parseDouble(n2));
        } catch (NumberFormatException e) {
            return v1.compareTo(v2);
        }
    }

    /**
     * Inner class representing a LOINC concept with code and display name.
     */
    @Data
    public class ValueSet {
        private String code;
        private String display;


        public ValueSet(String code, String display) {
            this.code = code;
            this.display = display;
        }
    }
    @Data
    public class ValueSetResponse {
        private String name;
        private String version;
        private String id;
        private String subsetUri;
        private List<ValueSet> items;

        public ValueSetResponse(String name, String version, String id, String subsetUri, List<ValueSet> items) {
            this.name = name;
            this.version = version;
            this.id = id;
            this.subsetUri = subsetUri;
            this.items = items;
        }
    }
}
