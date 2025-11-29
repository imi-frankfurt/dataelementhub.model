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
import java.util.stream.Collectors;

@Service
public class LoincService {

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
    public LoincService(RestTemplate restTemplate, TranslationService translationService) {
        this.restTemplate = restTemplate;
        this.translationService = translationService;
    }
    /**
     * Create Basic Auth headers with encoded username and password.
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
     * Search LOINC concepts via the Regenstrief API using free text.
     *
     * @param query Text to search for
     * @return List of LoincConcept objects
     */
    public List<LoincConcept> searchViaRegenstriefApi(String query) {
        String url = "https://loinc.regenstrief.org/searchapi/loincs";

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

            String finalUrl = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("query", translatedQuery + " Status:(Active OR Trial OR Discouraged)").build(false).toUriString();

            System.out.println("Übersetzter Begriff plus url: " + finalUrl);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    finalUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {} //ParameterisedTypeReference preserves the exact structure of the response and Spring can deserialise the response correctly.
            );
            System.out.println("Übersetzter Begriff plus url: " + response);
            Map<String, Object> body = response.getBody();

            if (body != null) {
                String version = (String) body.get("ResponseSummary.LoincVersion");

                List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("Results");

                if (results != null) {
                    return results.stream()
                            .map(item -> new LoincConcept(
                                    (String) item.get("LOINC_NUM"),
                                    (String) item.get("LONG_COMMON_NAME"),
                                    "https://loinc.org",
                                    version != null ? version : "2.80"
                            ))
                            .collect(Collectors.toList());
                }
            }

        } catch (Exception e) {
            System.err.println("Error during Regenstrief API request: " + e.getMessage());
            e.printStackTrace();
        }

        return Collections.emptyList();
    }
    /**
     * Search for a specific LOINC code using the Regenstrief API.
     *
     * @param code LOINC code to search for (e.g., "2345-7")
     * @return Optional containing LoincConcept if found, otherwise empty
     */
    public Optional<LoincConcept> searchByCodeViaRegenstrief(String code) {
        String url = "https://loinc.regenstrief.org/searchapi/loincs";

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("query", code);

        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();

            if (body != null) {
                String version = (String) body.get("ResponseSummary.LoincVersion");

                List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("Results");

                if (results != null) {
                    return results.stream()
//                            // remove deprecated code
//                            .filter(item -> {
//                                String status = (String) item.get("STATUS");
//                                return !"DEPRECATED".equalsIgnoreCase(status);
//                            })
                            .filter(item -> code.equals(item.get("code")))
                            .map(item -> new LoincConcept(
                                    (String) item.get("LOINC_NUM"),
                                    (String) item.get("LONG_COMMON_NAME"),
                                    "https://loinc.org",
                                    version != null ? version : "2.80"
                            ))
                            .findFirst();
                }
            }

        } catch (Exception e) {
            System.err.println("Error during Regenstrief API request: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }
    /**
     * Inner class representing a LOINC concept with code and display name.
     */
    @Data
    public class LoincConcept {
        private String term;
        private String text;
        private String system;
        private String version;

        public LoincConcept(String term, String text, String system, String version) {
            this.term = term;
            this.text = text;
            this.system = system;
            this.version = version;
        }
    }

    /**
     * Lookup a specific LOINC code via FHIR server.
     *
     * @param code LOINC code to search for
     * @return Optional containing LoincConcept if found, otherwise empty
     */
    public Optional<LoincConcept> lookupCode(String code) {
        String url = "https://fhir.loinc.org/CodeSystem/$lookup";

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("system", "http://loinc.org")
                .queryParam("code", code);

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(uriBuilder.toUriString(), Map.class);
            Map<String, Object> body = response.getBody();

            if (body != null && body.containsKey("parameter")) {
                return Optional.of(extractConcept(body));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return Optional.empty();
    }

    /**
     * Extracts LoincConcept from the response map for lookupCode method.
     */
    private LoincConcept extractConcept(Map<String, Object> response) {
        List<Map<String, Object>> params = (List<Map<String, Object>>) response.get("parameter");
        String term = "";
        String text = "";
        String system = "";
        String version = "";

        for (Map<String, Object> param : params) {
            String paramName = (String) param.get("name");
            String paramValue = (String) param.get("valueString");

            if ("text".equals(paramName)) {
                text = paramValue;
            }
            if ("term".equals(paramName)) {
                term = paramValue;
            }
            if ("system".equals(paramName)) {
                system = paramValue;
            }
            if ("version".equals(paramName)) {
                version = paramValue;
            }
        }

        return new LoincConcept(term, text, system,version);
    }
//
//    /**
//     * Search LOINC concepts by free text using the FHIR endpoint.
//     *
//     * @param searchText Text to search for
//     * @return List of LoincConcept objects
//     */
//    public List<LoincConcept> searchByText(String searchText) {
//        String url = "https://fhir.loinc.org/ValueSet/$expand";
//
//        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
//                .queryParam("url", "http://loinc.org/vs")
//                .queryParam("filter", searchText);
//
//        try {
//            ResponseEntity<Map> response = restTemplate.getForEntity(builder.toUriString(), Map.class);
//            Map<String, Object> body = response.getBody();
//
//            if (body != null && body.containsKey("expansion")) {
//                Map<String, Object> expansion = (Map<String, Object>) body.get("expansion");
//                List<Map<String, Object>> contains = (List<Map<String, Object>>) expansion.get("contains");
//
//                List<LoincConcept> results = new ArrayList<>();
//                for (Map<String, Object> item : contains) {
//                    String code = (String) item.get("code");
//                    String display = (String) item.get("display");
//                    results.add(new LoincConcept(code, display));
//                }
//                return results;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return Collections.emptyList();
//    }
//
}