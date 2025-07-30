package app.web;

import app.config.Env;
import app.model.ApplyLog;
import app.model.ApplySummary;
import app.model.EvaluationResult;
import app.model.UserProfile;
import app.service.ApplyService;
import app.service.ResumeParserService;
import app.service.VacancyEvaluationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class DashboardController {
    
    private final ApplyService applyService;
    private final VacancyEvaluationService evaluationService;
    private final ResumeParserService resumeParserService;
    private final Env env;
    private final ObjectMapper objectMapper;
    
    public DashboardController(ApplyService applyService, VacancyEvaluationService evaluationService, 
                             ResumeParserService resumeParserService, Env env) {
        this.applyService = applyService;
        this.evaluationService = evaluationService;
        this.resumeParserService = resumeParserService;
        this.env = env;
        this.objectMapper = new ObjectMapper();
    }
    
    @GetMapping("/")
    public String dashboard(Model model) {
        // Get recent logs
        List<ApplyLog> recentLogs = applyService.getRecentLogs(20);
        
        // Get total applied count
        int totalApplied = applyService.getAppliedCount();
        
        model.addAttribute("recentLogs", recentLogs);
        model.addAttribute("totalApplied", totalApplied);
        model.addAttribute("isDryRun", env.isApplyDryRun());
        
        return "dashboard";
    }
    
    @PostMapping("/run")
    public String runApplyProcess(RedirectAttributes redirectAttributes) {
        try {
            ApplySummary summary = applyService.runApplyProcess();
            
            String message = String.format(
                "Обработка завершена! Найдено: %d, новых: %d, откликнуто: %d, ошибок: %d%s",
                summary.totalFound(),
                summary.newVacancies(),
                summary.applied(),
                summary.errors(),
                summary.dryRun() ? " (РЕЖИМ ТЕСТИРОВАНИЯ)" : ""
            );
            
            redirectAttributes.addFlashAttribute("message", message);
            redirectAttributes.addFlashAttribute("messageType", summary.errors() > 0 ? "warning" : "success");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Ошибка при запуске: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        
        return "redirect:/";
    }
    
    @PostMapping("/evaluate")
    public String evaluateVacancy(@RequestParam String vacancyId, 
                                 @RequestParam String userProfileJson,
                                 RedirectAttributes redirectAttributes) {
        try {
            // Парсим профиль пользователя из JSON
            UserProfile userProfile = objectMapper.readValue(userProfileJson, UserProfile.class);
            
            // Оцениваем вакансию
            EvaluationResult result = evaluationService.evaluateVacancy(vacancyId, userProfile);
            
            // Сохраняем результат в атрибутах для отображения
            redirectAttributes.addFlashAttribute("evaluationResult", result);
            redirectAttributes.addFlashAttribute("vacancyId", vacancyId);
            redirectAttributes.addFlashAttribute("userProfile", userProfile);
            redirectAttributes.addFlashAttribute("message", "Оценка завершена успешно!");
            redirectAttributes.addFlashAttribute("messageType", "success");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Ошибка при оценке: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        
        return "redirect:/";
    }
    
    @PostMapping("/evaluate-with-resume")
    public String evaluateWithResume(@RequestParam String vacancyId, 
                                   @RequestParam("resumeFile") MultipartFile resumeFile,
                                   RedirectAttributes redirectAttributes) {
        try {
            // Проверяем тип файла
            if (!resumeFile.getContentType().equals("application/pdf")) {
                redirectAttributes.addFlashAttribute("message", "Поддерживаются только PDF файлы!");
                redirectAttributes.addFlashAttribute("messageType", "error");
                return "redirect:/";
            }
            
            // Парсим резюме
            UserProfile userProfile = resumeParserService.parseResume(resumeFile);
            
            // Оцениваем вакансию
            EvaluationResult result = evaluationService.evaluateVacancy(vacancyId, userProfile);
            
            // Сохраняем результат в атрибутах для отображения
            redirectAttributes.addFlashAttribute("evaluationResult", result);
            redirectAttributes.addFlashAttribute("vacancyId", vacancyId);
            redirectAttributes.addFlashAttribute("userProfile", userProfile);
            redirectAttributes.addFlashAttribute("message", "Резюме успешно обработано и оценка завершена!");
            redirectAttributes.addFlashAttribute("messageType", "success");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Ошибка при обработке резюме: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        
        return "redirect:/";
    }
} 