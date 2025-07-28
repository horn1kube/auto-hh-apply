package app.service;

import app.config.Env;
import app.hh.HhClient;
import app.hh.parser.JsonSearchParser;
import app.model.ApplyLog;
import app.model.ApplyResult;
import app.model.ApplySummary;
import app.store.SqliteStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Service
public class ApplyService {
    private static final Logger log = LoggerFactory.getLogger(ApplyService.class);
    
    private final Env env;
    private final HhClient hhClient;
    private final JsonSearchParser jsonSearchParser;
    private final SqliteStore store;
    private final Random random = new Random();
    
    public ApplyService(Env env, HhClient hhClient, JsonSearchParser jsonSearchParser, SqliteStore store) {
        this.env = env;
        this.hhClient = hhClient;
        this.jsonSearchParser = jsonSearchParser;
        this.store = store;
    }
    
    /**
     * Runs the complete vacancy application process
     * @return Summary of the operation
     */
    public ApplySummary runApplyProcess() {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("Starting vacancy application process");
        
        int totalFound = 0;
        int newVacancies = 0;
        int applied = 0;
        int errors = 0;
        
        try {
            // Fetch search JSON
            String searchJson = hhClient.fetchSearchJson();
            
            // Parse vacancy IDs from JSON
            Set<String> vacancyIds = jsonSearchParser.extractVacancyIds(searchJson);
            totalFound = vacancyIds.size();
            
            log.info("Found {} vacancies to process", totalFound);
            
            // Process each vacancy
            for (String vacancyId : vacancyIds) {
                try {
                    // Log found vacancy
                    store.addLog(ApplyLog.found(vacancyId));
                    
                    // Check if already applied
                    if (store.isApplied(vacancyId)) {
                        store.addLog(ApplyLog.skip(vacancyId, "Already applied"));
                        continue;
                    }
                    
                    newVacancies++;
                    
                    // Optional: Register interaction
                    hhClient.registerInteraction(vacancyId);
                    
                    // Apply to vacancy
                    ApplyResult result = hhClient.applyMultipart(vacancyId);
                    
                    if (result.success()) {
                        if (!env.isApplyDryRun()) {
                            store.markApplied(vacancyId);
                        }
                        store.addLog(ApplyLog.applyOk(vacancyId));
                        applied++;
                        log.info("APPLY OK vacancyId={}", vacancyId);
                    } else {
                        store.addLog(ApplyLog.applyFail(vacancyId, "Status: " + result.statusCode()));
                        errors++;
                        log.error("APPLY FAIL vacancyId={} code={} message={}", 
                                 vacancyId, result.statusCode(), result.message());
                    }
                    
                    // Random delay between requests
                    if (vacancyIds.size() > 1) {
                        int delay = random.nextInt(
                            env.getRateLimitMaxDelayMs() - env.getRateLimitMinDelayMs()
                        ) + env.getRateLimitMinDelayMs();
                        
                        Thread.sleep(delay);
                    }
                    
                } catch (Exception e) {
                    errors++;
                    store.addLog(ApplyLog.applyFail(vacancyId, e.getMessage()));
                    log.error("Error processing vacancy {}", vacancyId, e);
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to run apply process", e);
            errors++;
        }
        
        ApplySummary summary = ApplySummary.create(startTime, totalFound, newVacancies, applied, errors, env.isApplyDryRun());
        
        log.info("Apply process completed: found={}, new={}, applied={}, errors={}, dryRun={}", 
                totalFound, newVacancies, applied, errors, env.isApplyDryRun());
        
        return summary;
    }
    
    /**
     * Gets recent logs
     * @param limit Maximum number of logs to return
     * @return List of recent logs
     */
    public List<ApplyLog> getRecentLogs(int limit) {
        return store.getRecentLogs(limit);
    }
    
    /**
     * Gets total applied count
     * @return Number of applied vacancies
     */
    public int getAppliedCount() {
        return store.getAppliedCount();
    }
} 