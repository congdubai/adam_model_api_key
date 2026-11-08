package com.adamtts.studio.controller;

import com.adamtts.studio.dto.CapCutApplyRequest;
import com.adamtts.studio.dto.CapCutApplyResponse;
import com.adamtts.studio.dto.CapCutProjectDto;
import com.adamtts.studio.service.CapCutIntegrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.Desktop;
import java.io.File;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/capcut")
public class CapCutController {

    private final CapCutIntegrationService capCutService;

    public CapCutController(CapCutIntegrationService capCutService) {
        this.capCutService = capCutService;
    }

    @GetMapping("/detect")
    public ResponseEntity<Map<String, Object>> detectCapCut() {
        String dir = capCutService.getDefaultCapCutDir();
        boolean exists = new File(dir).exists();
        boolean isRunning = capCutService.isCapCutRunning();
        return ResponseEntity.ok(Map.of(
                "available", exists,
                "capcutDir", dir,
                "isRunning", isRunning
        ));
    }

    @GetMapping("/projects")
    public ResponseEntity<List<CapCutProjectDto>> listProjects(@RequestParam(value = "dir", required = false) String customDir) {
        List<CapCutProjectDto> projects = capCutService.listProjects(customDir);
        return ResponseEntity.ok(projects);
    }

    @PostMapping("/apply")
    public ResponseEntity<CapCutApplyResponse> applyToCapCut(@RequestBody CapCutApplyRequest req) {
        CapCutApplyResponse resp = capCutService.applyToDraft(req);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/open-folder")
    public ResponseEntity<Map<String, Object>> openFolder(@RequestBody Map<String, String> body) {
        String path = body.get("path");
        if (path == null || path.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Thiếu đường dẫn"));
        }
        File folder = new File(path);
        if (!folder.exists()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Thư mục không tồn tại"));
        }

        try {
            if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                new ProcessBuilder("cmd.exe", "/c", "start", "", folder.getAbsolutePath()).start();
            } else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(folder);
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}

