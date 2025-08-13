package de.dataelementhub.model.service.Terminologies;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
@Service
public class DefinedVDFihrService {
    private final RestTemplate restTemplate;
    private final TranslationService translationService;

    @Autowired
    public DefinedVDFihrService(RestTemplate restTemplate, TranslationService translationService) {
        this.restTemplate = restTemplate;
        this.translationService = translationService;
    }

    public List<ValueSetResponse> fetchValueSet(
            String query,
            String baseUrl,
            String queryParamKey,
            boolean useTranslation,
            HttpHeaders headers
    ) {
        if (query == null || query.trim().isEmpty()) {
            System.err.println("Query parameter is empty or null.");
            return Collections.emptyList();
        }

        try {
            String searchQuery = useTranslation
                    ? translationService.translateText(query, "en")
                    : query;

            String finalUrl = UriComponentsBuilder
                    .fromHttpUrl(baseUrl)
                    .queryParam(queryParamKey, searchQuery)
                    .build()
                    .toUriString();

            System.out.println("Final URL: " + finalUrl);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    finalUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("entry")) return Collections.emptyList();

            List<Map<String, Object>> entries = (List<Map<String, Object>>) body.get("entry");
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

    private int compareVersionStrings(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int num1 = parseVersionPart(parts1, i);
            int num2 = parseVersionPart(parts2, i);

            if (num1 != num2) {
                return num1 - num2;
            }
        }

        return 0;
    }

    private int parseVersionPart(String[] parts, int index) {
        if (index >= parts.length) return 0;

        try {
            // Extract only the digits at the beginning, if possible
            String cleaned = parts[index].replaceAll("[^0-9]", "");
            return cleaned.isEmpty() ? 0 : Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ---------- DTO Klassen ----------
    @Data
    public static class ValueSet {
        private final String code;
        private final String display;
    }

    @Data
    public static class ValueSetResponse {
        private final String name;
        private final String version;
        private final String id;
        private final String subsetUri;
        private final List<ValueSet> items;
    }
}