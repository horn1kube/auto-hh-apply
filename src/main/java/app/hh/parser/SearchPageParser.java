package app.hh.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SearchPageParser {
    private static final Logger log = LoggerFactory.getLogger(SearchPageParser.class);
    private static final Pattern VACANCY_ID_PATTERN = Pattern.compile("(?<=/vacancy/)\\d+");
    
    /**
     * Extracts vacancy IDs from HTML search page
     * @param html HTML content of the search page
     * @return Set of unique vacancy IDs
     */
    public Set<String> extractVacancyIds(String html) {
        Set<String> vacancyIds = new HashSet<>();
        
        try {
            Document doc = Jsoup.parse(html, "UTF-8");
            Elements vacancyLinks = doc.select("a[href*=\"/vacancy/\"]");
            
            for (Element link : vacancyLinks) {
                String href = link.attr("href");
                Matcher matcher = VACANCY_ID_PATTERN.matcher(href);
                
                if (matcher.find()) {
                    String vacancyId = matcher.group();
                    vacancyIds.add(vacancyId);
                    log.debug("Found vacancy ID: {}", vacancyId);
                }
            }
            
            log.info("Extracted {} unique vacancy IDs from search page", vacancyIds.size());
            
        } catch (Exception e) {
            log.error("Failed to parse search page HTML", e);
        }
        
        return vacancyIds;
    }
} 