package app.web;

import app.config.Env;
import app.model.ApplyLog;
import app.model.ApplySummary;
import app.service.ApplyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class DashboardController {
    
    private final ApplyService applyService;
    private final Env env;
    
    public DashboardController(ApplyService applyService, Env env) {
        this.applyService = applyService;
        this.env = env;
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
} 