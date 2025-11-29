package de.dataelementhub.model.service.Terminologies;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class FhirTxService {
    private final RestTemplate restTemplate;
    private final TranslationService translationService;

    /**
     * Constructor for injecting RestTemplate.
     */
    @Autowired
    public FhirTxService(RestTemplate restTemplate, TranslationService translationService) {
        this.restTemplate = restTemplate;
        this.translationService = translationService;
    }

    /**
     * Search FHIR concepts via the FHIR-TX API using free text.
     *
     * @param query Text to search for
     * @return List of FHIRConcept objects
     */
    public List<Concept> searchFhir(String query) {
        if (query == null || query.trim().isEmpty()) {
            System.err.println("Query parameter is empty or null.");
            return Collections.emptyList();
        }
        try {
            System.out.println("Begriff: " + query);
            String translatedQuery = translationService.translateText(query, "en");
            System.out.println("Übersetzter Begriff: " + translatedQuery);

            String urlVsSearch = UriComponentsBuilder
                    .fromHttpUrl("https://tx.fhir.org/r4/ValueSet")
                    .queryParam("name:in", translatedQuery.trim())
                    .encode().toUriString();

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    urlVsSearch, HttpMethod.GET, null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> bundle = response.getBody();
            if (bundle == null || !"Bundle".equals(bundle.get("resourceType"))) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> entries =
                    (List<Map<String, Object>>) bundle.getOrDefault("entry", Arrays.asList());
            if (entries.isEmpty()) return Collections.emptyList();

            List<Concept> all = new ArrayList<>();

            for (Map<String, Object> e : entries) {
                Map<String, Object> vs = (Map<String, Object>) e.get("resource");
                if (vs == null || !"ValueSet".equals(vs.get("resourceType"))) continue;

                String canonical = safeStr(vs.get("url"));
                String id        = safeStr(vs.get("id"));

                String expandUrl;
                if (!canonical.isEmpty()) {
                    expandUrl = UriComponentsBuilder
                            .fromHttpUrl("https://tx.fhir.org/r4/ValueSet/$expand")
                            .queryParam("url", canonical)
                            .queryParam("filter", translatedQuery.trim())
                            .queryParam("count", 50)
                            .encode().toUriString();
                } else if (!id.isEmpty()) {
                    expandUrl = UriComponentsBuilder
                            .fromHttpUrl("https://tx.fhir.org/r4/ValueSet/" + id + "/$expand")
                            .queryParam("filter", translatedQuery.trim())
                            .queryParam("count", 50)
                            .encode().toUriString();
                } else {
                    continue;
                }

                try {
                    ResponseEntity<Map<String, Object>> exp = restTemplate.exchange(
                            expandUrl, HttpMethod.GET, null,
                            new ParameterizedTypeReference<Map<String, Object>>() {}
                    );

                    Map<String, Object> vsExpanded = exp.getBody();
                    if (vsExpanded == null || !"ValueSet".equals(vsExpanded.get("resourceType"))) continue;

                    Map<String, Object> expansion = (Map<String, Object>) vsExpanded.get("expansion");
                    if (expansion == null) continue;

                    List<Map<String, Object>> contains =
                            (List<Map<String, Object>>) expansion.get("contains");
                    if (contains == null) continue;

                    for (Map<String, Object> item : contains) {
                        all.add(new Concept(
                                safeStr(item.get("code")),
                                safeStr(item.get("display")),
                                safeStr(item.get("system")),
                                extractVersion(safeStr(item.get("version")))
                        ));
                    }
                } catch (HttpClientErrorException e422) {
                    System.err.println("Skipping ValueSet due to expand error: "
                            + e422.getStatusCode());
                    System.err.println("Expand URL: " + expandUrl);
                }
            }

            return all.stream()
                    .filter(c -> !c.getTerm().isEmpty() && !c.getSystem().isEmpty())
                    .collect(Collectors.collectingAndThen(
                            Collectors.toMap(
                                    c -> c.getSystem() + "|" + c.getTerm(),
                                    c -> c, (a, b) -> a, LinkedHashMap::new
                            ),
                            m -> new ArrayList<>(m.values())
                    ));

        } catch (Exception e) {
            System.err.println("Error during FHIR TX request: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

private static String safeStr(Object code) {
        return code == null ? "" : String.valueOf(code);
        }
    private static String extractVersion(String versionUrl) {
        if (versionUrl == null) return "";
        Matcher m = Pattern.compile(".*/version/(\\w+)$").matcher(versionUrl);
        return m.find() ? m.group(1) : versionUrl;
    }
}
