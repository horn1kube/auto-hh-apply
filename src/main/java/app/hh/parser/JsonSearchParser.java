package app.hh.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class JsonSearchParser {
    private static final Logger log = LoggerFactory.getLogger(JsonSearchParser.class);
    private final ObjectMapper objectMapper;
    
    public JsonSearchParser() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Extracts vacancy IDs from JSON search response
     * @param json JSON content of the search response
     * @return Set of unique vacancy IDs
     */
    public Set<String> extractVacancyIds(String json) {
        Set<String> vacancyIds = new HashSet<>();
        
        try {
            JsonNode rootNode = objectMapper.readTree(json);
            
            // Navigate to vacancySearchResult.vacancies
            JsonNode vacancySearchResult = rootNode.get("vacancySearchResult");
            if (vacancySearchResult != null) {
                JsonNode vacancies = vacancySearchResult.get("vacancies");
                if (vacancies != null && vacancies.isArray()) {
                    for (JsonNode vacancy : vacancies) {
                        JsonNode idNode = vacancy.get("vacancyId");
                        if (idNode != null) {
                            String vacancyId = idNode.asText();
                            vacancyIds.add(vacancyId);
                            log.debug("Found vacancy ID: {}", vacancyId);
                        }
                    }
                }
            }
            
            log.info("Extracted {} unique vacancy IDs from JSON response", vacancyIds.size());
            
        } catch (Exception e) {
            log.error("Failed to parse JSON search response", e);
            log.debug("JSON content: {}", json.substring(0, Math.min(500, json.length())));
        }
        
        return vacancyIds;
    }
} 