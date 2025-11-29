package de.dataelementhub.model.service.Terminologies;

import lombok.Data;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class SnomedService {

    private final RestTemplate restTemplate;

    @Autowired
    public SnomedService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Search for SNOMED concepts by term using the Snowstorm API.
     * @param term The search term (e.g., "diabetes")
     * @param offset The pagination offset
     * @param limit The number of results to return
     * @return A list of SnomedConcept objects
     */
    public List<SnomedConcept> searchByTerm(String term, int offset, int limit) {
        String url = "https://snowstorm.snomedtools.org/snowstorm/snomed-ct/multisearch/descriptions";

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("term", term)
                .queryParam("contentScope", "ALL_PUBLISHED_CONTENT")
                .queryParam("offset", offset)
                .queryParam("limit", limit);

        String requestUrl = builder.toUriString();
        System.out.println("SNOMED Request URL: " + requestUrl);


        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(requestUrl, Map.class);
            Map<String, Object> body = response.getBody();

            System.out.println("Response Body: " + body);

            if (body != null && body.containsKey("items")) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");

                List<SnomedConcept> results = new ArrayList<>();
                for (Map<String, Object> item : items) {
                    String branch = (String) item.get("branchPath");
                    Map<String, Object> concept = (Map<String, Object>) item.get("concept");

                    if (concept != null) {
                        term = (String) concept.get("conceptId");
                        Map<String, Object> fsn = (Map<String, Object>) concept.get("fsn");
                       // Map<String, Object> pt = (Map<String, Object>) concept.get("pt");
                        String text = fsn != null ? (String) fsn.get("term") : null;
                        String system = "http://snomed.info/sct/" + branch;
                        String version = fetchVersion();


                        if (term != null && text != null) {
                            results.add(new SnomedConcept(term, text, system, version));
                        }
                    }
                }

                return results;
            }

        } catch (Exception e) {
            System.err.println("Error during SNOMED API request: " + e.getMessage());
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    public static String fetchVersion() throws Exception {
        URL url = new URL("https://snowstorm.ihtsdotools.org/snowstorm/snomed-ct/version");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        JSONObject json = new JSONObject(response.toString());
        return json.getString("version");
    }
    @Data
    public static class SnomedConcept {
        private String term;
        private String text;
        private String system;
        private String version;

        public SnomedConcept(String term, String text, String system, String version) {
            this.term = term;
            this.text = text;
            this.system = system;
            this.version = version;
        }
    }
}