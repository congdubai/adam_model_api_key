package com.adamtts.studio.service;

import com.adamtts.studio.dto.CapCutApplyRequest;
import com.adamtts.studio.dto.CapCutApplyResponse;
import com.adamtts.studio.dto.CapCutProjectDto;
import com.adamtts.studio.model.JobEntity;
import com.adamtts.studio.model.SegmentEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
public class CapCutIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(CapCutIntegrationService.class);
    private static final double MIN_SPEED = 0.4;
    private static final double MAX_SPEED = 2.0;

    private final ObjectMapper objectMapper;
    private final JobManagerService jobManagerService;

    public CapCutIntegrationService(ObjectMapper objectMapper, JobManagerService jobManagerService) {
        this.objectMapper = objectMapper;
        this.jobManagerService = jobManagerService;
    }

    public String getDefaultCapCutDir() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData == null || localAppData.isBlank()) {
            localAppData = System.getProperty("user.home") + "\\AppData\\Local";
        }
        File winDraft = new File(localAppData, "CapCut\\User Data\\Projects\\com.lveditor.draft");
        if (winDraft.exists() && winDraft.isDirectory()) {
            return winDraft.getAbsolutePath();
        }
        File macDraft = new File(System.getProperty("user.home"), "Movies/CapCut/User Data/Projects/com.lveditor.draft");
        if (macDraft.exists() && macDraft.isDirectory()) {
            return macDraft.getAbsolutePath();
        }
        return winDraft.getAbsolutePath();
    }

    public boolean isCapCutRunning() {
        try {
            return ProcessHandle.allProcesses().anyMatch(p ->
                    p.info().command()
                            .map(cmd -> cmd.toLowerCase().contains("capcut"))
                            .orElse(false)
            );
        } catch (Exception e) {
            return false;
        }
    }

    public List<CapCutProjectDto> listProjects(String customDir) {
        String draftDirPath = (customDir != null && !customDir.isBlank()) ? customDir : getDefaultCapCutDir();
        File draftDir = new File(draftDirPath);
        List<CapCutProjectDto> projects = new ArrayList<>();
        Set<String> seenFolders = new HashSet<>();

        if (!draftDir.exists() || !draftDir.isDirectory()) {
            return projects;
        }

        // 1. Try reading root_meta_info.json
        File rootMetaFile = new File(draftDir, "root_meta_info.json");
        if (rootMetaFile.exists()) {
            try {
                JsonNode rootNode = objectMapper.readTree(rootMetaFile);
                JsonNode drafts = rootNode.get("all_draft_store");
                if (drafts != null && drafts.isArray()) {
                    for (JsonNode d : drafts) {
                        String name = d.path("draft_name").asText("Project");
                        String path = d.path("draft_fold_path").asText("");
                        long modTime = d.path("tm_draft_modified").asLong(0);
                        double durationSec = d.path("tm_duration").asLong(0) / 1_000_000.0;
                        if (!path.isBlank()) {
                            File folder = new File(path);
                            if (folder.exists() && seenFolders.add(folder.getAbsolutePath())) {
                                CapCutProjectDto dto = inspectProject(name, folder, modTime, durationSec);
                                projects.add(dto);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to read root_meta_info.json: {}", e.getMessage());
            }
        }

        // 2. Scan subdirectories directly for any other unlisted projects
        File[] subdirs = draftDir.listFiles(File::isDirectory);
        if (subdirs != null) {
            for (File folder : subdirs) {
                if (seenFolders.add(folder.getAbsolutePath())) {
                    File draftContent = new File(folder, "draft_content.json");
                    if (draftContent.exists()) {
                        String name = folder.getName();
                        File metaFile = new File(folder, "draft_meta_info.json");
                        if (metaFile.exists()) {
                            try {
                                JsonNode meta = objectMapper.readTree(metaFile);
                                name = meta.path("draft_name").asText(name);
                            } catch (Exception ignored) {
                            }
                        }
                        CapCutProjectDto dto = inspectProject(name, folder, folder.lastModified(), 0);
                        projects.add(dto);
                    }
                }
            }
        }

        // Sort by last modified descending
        projects.sort((a, b) -> Long.compare(b.getModifiedTime(), a.getModifiedTime()));
        return projects;
    }

    private CapCutProjectDto inspectProject(String name, File folder, long modTime, double durationSec) {
        File draftContent = new File(folder, "draft_content.json");
        int clipCount = 0;
        boolean readable = false;

        if (draftContent.exists()) {
            try (FileInputStream fis = new FileInputStream(draftContent)) {
                byte[] head = fis.readNBytes(4);
                if (head.length > 0 && (head[0] == '{' || head[0] == '[')) {
                    readable = true;
                    JsonNode node = objectMapper.readTree(draftContent);
                    JsonNode tracks = node.get("tracks");
                    if (tracks != null && tracks.isArray()) {
                        for (JsonNode t : tracks) {
                            if ("video".equalsIgnoreCase(t.path("type").asText())) {
                                JsonNode segs = t.get("segments");
                                if (segs != null && segs.isArray()) {
                                    clipCount = segs.size();
                                }
                                break;
                            }
                        }
                    }
                    if (durationSec <= 0) {
                        durationSec = node.path("duration").asLong(0) / 1_000_000.0;
                    }
                }
            } catch (Exception e) {
                log.debug("Project {} cannot be fully inspected: {}", name, e.getMessage());
            }
        }

        return new CapCutProjectDto(
                folder.getName(),
                name,
                folder.getAbsolutePath(),
                clipCount,
                readable,
                modTime > 0 ? modTime : folder.lastModified(),
                durationSec
        );
    }

    public synchronized CapCutApplyResponse applyToDraft(CapCutApplyRequest req) {
        CapCutApplyResponse resp = new CapCutApplyResponse();
        resp.setProjectFolder(req.getProjectFolder());

        JobEntity job = jobManagerService.getActiveJob(req.getJobId());
        if (job == null) {
            resp.setSuccess(false);
            resp.setMessage("Không tìm thấy Job TTS ID: " + req.getJobId());
            return resp;
        }

        File projectFolder = new File(req.getProjectFolder());
        if (!projectFolder.exists() || !projectFolder.isDirectory()) {
            resp.setSuccess(false);
            resp.setMessage("Thư mục project CapCut không tồn tại: " + req.getProjectFolder());
            return resp;
        }

        File draftPath = new File(projectFolder, "draft_content.json");
        if (!draftPath.exists()) {
            resp.setSuccess(false);
            resp.setMessage("Không tìm thấy file draft_content.json trong project CapCut!");
            return resp;
        }

        boolean capcutRunning = isCapCutRunning();
        resp.setCapcutWasRunning(capcutRunning);
        if (capcutRunning) {
            try {
                if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                    new ProcessBuilder("taskkill", "/F", "/IM", "CapCut.exe").start().waitFor();
                    Thread.sleep(800);
                }
            } catch (Exception e) {
                log.warn("Could not terminate CapCut: {}", e.getMessage());
            }
        }

        try {
            // Remove .locked file if present
            File lockedFile = new File(projectFolder, ".locked");
            if (lockedFile.exists()) {
                lockedFile.delete();
            }

            // Backup draft_content.json -> draft_content.json.bak
            File backupFile = new File(projectFolder, "draft_content.json.bak");
            Files.copy(draftPath.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Clear cache if requested
            if (req.isClearCache()) {
                clearCapCutCache(projectFolder);
            }

            // Read root draft json
            ObjectNode draftNode = (ObjectNode) objectMapper.readTree(draftPath);

            ArrayNode tracks = (ArrayNode) draftNode.withArray("tracks");
            ObjectNode materials = draftNode.withObject("materials");

            List<SegmentEntity> jobSegments = job.getSegments();
            if (jobSegments == null || jobSegments.isEmpty()) {
                resp.setSuccess(false);
                resp.setMessage("Job không có đoạn âm thanh nào để đồng bộ.");
                return resp;
            }

            int syncedClips = 0;

            // 1. Synchronize Video Clips if requested
            if (req.isSyncVideoClips()) {
                ObjectNode videoTrack = null;
                for (JsonNode t : tracks) {
                    if ("video".equalsIgnoreCase(t.path("type").asText())) {
                        videoTrack = (ObjectNode) t;
                        break;
                    }
                }

                if (videoTrack != null) {
                    ArrayNode videoSegs = (ArrayNode) videoTrack.withArray("segments");
                    ArrayNode speedsMat = materials.withArray("speeds");
                    Map<String, ObjectNode> speedById = new HashMap<>();
                    for (JsonNode sNode : speedsMat) {
                        if (sNode.isObject()) {
                            speedById.put(sNode.path("id").asText(), (ObjectNode) sNode);
                        }
                    }

                    int count = Math.min(videoSegs.size(), jobSegments.size());
                    for (int i = 0; i < count; i++) {
                        ObjectNode seg = (ObjectNode) videoSegs.get(i);
                        SegmentEntity jobSeg = jobSegments.get(i);

                        long startUs = Math.round(jobSeg.getStartTimeSeconds() * 1_000_000);
                        long durUs;
                        if (i + 1 < jobSegments.size()) {
                            durUs = Math.round((jobSegments.get(i + 1).getStartTimeSeconds() - jobSeg.getStartTimeSeconds()) * 1_000_000);
                        } else {
                            durUs = Math.round(jobSeg.getDurationSeconds() * 1_000_000);
                        }

                        if (durUs <= 0) durUs = 1_000_000;

                        ObjectNode srcNode = seg.withObject("source_timerange");
                        long sourceDuration = srcNode.path("duration").asLong(durUs);

                        double rawSpeed = (double) sourceDuration / (double) durUs;
                        double speed = Math.max(MIN_SPEED, Math.min(MAX_SPEED, rawSpeed));

                        if (Math.abs(speed - rawSpeed) > 0.001) {
                            srcNode.put("duration", Math.round(durUs * speed));
                        }

                        ObjectNode targetNode = seg.withObject("target_timerange");
                        targetNode.put("start", startUs);
                        targetNode.put("duration", durUs);
                        seg.put("speed", speed);

                        ArrayNode refs = seg.withArray("extra_material_refs");
                        String existingSpeedRef = null;
                        for (JsonNode r : refs) {
                            if (speedById.containsKey(r.asText())) {
                                existingSpeedRef = r.asText();
                                break;
                            }
                        }

                        if (existingSpeedRef != null) {
                            speedById.get(existingSpeedRef).put("speed", speed);
                        } else {
                            String sid = genId();
                            ObjectNode m = objectMapper.createObjectNode();
                            m.putNull("curve_speed");
                            m.put("id", sid);
                            m.put("mode", 0);
                            m.put("speed", speed);
                            m.put("type", "speed");
                            speedsMat.add(m);
                            speedById.put(sid, m);
                            refs.add(sid);
                        }
                        syncedClips++;
                    }
                }
            }
            resp.setSyncedClips(syncedClips);

            // 2. Add Audio Track with SEPARATE individual clips for each timeline scene!
            boolean audioAdded = false;
            if (req.isAddAudioTrack()) {
                // Remove any previous audio track created by Adam TTS
                for (int i = tracks.size() - 1; i >= 0; i--) {
                    if ("audio".equalsIgnoreCase(tracks.get(i).path("type").asText()) &&
                            tracks.get(i).path("name").asText("").contains("Adam TTS")) {
                        tracks.remove(i);
                    }
                }

                File resourcesDir = new File(projectFolder, "Resources");
                if (!resourcesDir.exists()) {
                    resourcesDir.mkdirs();
                }

                ArrayNode audiosMat = materials.withArray("audios");
                ArrayNode speedsMat = materials.withArray("speeds");
                ArrayNode placeholderMat = materials.withArray("placeholder_infos");
                ArrayNode beatsMat = materials.withArray("beats");
                ArrayNode channelMat = materials.withArray("sound_channel_mappings");
                ArrayNode vocalMat = materials.withArray("vocal_separations");

                ObjectNode newAudioTrack = objectMapper.createObjectNode();
                newAudioTrack.put("id", genId());
                newAudioTrack.put("type", "audio");
                newAudioTrack.put("name", "Adam TTS Voice");
                newAudioTrack.put("attribute", 0);
                newAudioTrack.put("flag", 0);
                newAudioTrack.put("is_default_name", false);
                ArrayNode audioSegs = newAudioTrack.putArray("segments");

                for (int i = 0; i < jobSegments.size(); i++) {
                    SegmentEntity seg = jobSegments.get(i);
                    File segSrc = jobManagerService.getSegmentFile(job.getId(), seg.getIndex());
                    if (segSrc == null || !segSrc.exists()) {
                        continue;
                    }

                    String ext = segSrc.getName().endsWith(".wav") ? ".wav" : ".mp3";
                    File destSeg = new File(resourcesDir, String.format("Doan_%02d_tu_%.1fs%s", i + 1, seg.getStartTimeSeconds(), ext));
                    Files.copy(segSrc.toPath(), destSeg.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    long startUs = Math.round(seg.getStartTimeSeconds() * 1_000_000);
                    long durUs = Math.round(seg.getDurationSeconds() * 1_000_000);
                    if (durUs <= 0) durUs = 1_000_000;

                    String audioMatId = genId();
                    String sid = genId();
                    String pid = genId();
                    String bid = genId();
                    String smid = genId();
                    String vid = genId();

                    // Audio Material
                    ObjectNode audioMat = objectMapper.createObjectNode();
                    audioMat.put("id", audioMatId);
                    audioMat.put("type", "extract_music");
                    audioMat.put("name", destSeg.getName());
                    audioMat.put("duration", durUs);
                    audioMat.put("path", destSeg.getAbsolutePath().replace("\\", "/"));
                    audioMat.put("category_name", "local");
                    audioMat.putArray("wave_points");
                    audioMat.put("check_flag", 1);
                    audiosMat.add(audioMat);

                    // Speed Material
                    ObjectNode sObj = objectMapper.createObjectNode();
                    sObj.putNull("curve_speed");
                    sObj.put("id", sid);
                    sObj.put("mode", 0);
                    sObj.put("speed", 1.0);
                    sObj.put("type", "speed");
                    speedsMat.add(sObj);

                    // Placeholder Info
                    ObjectNode pObj = objectMapper.createObjectNode();
                    pObj.put("id", pid);
                    pObj.put("type", "placeholder_info");
                    pObj.put("meta_type", "none");
                    pObj.put("res_path", "");
                    pObj.put("res_text", "");
                    pObj.put("error_path", "");
                    pObj.put("error_text", "");
                    placeholderMat.add(pObj);

                    // Beats Material
                    ObjectNode bObj = objectMapper.createObjectNode();
                    bObj.put("id", bid);
                    bObj.put("type", "beats");
                    bObj.put("enable_ai_beats", false);
                    bObj.put("gear", 404);
                    bObj.put("gear_count", 0);
                    bObj.put("mode", 404);
                    bObj.putArray("user_beats");
                    bObj.putNull("user_delete_ai_beats");
                    ObjectNode aiBeats = bObj.putObject("ai_beats");
                    aiBeats.put("melody_url", "");
                    aiBeats.put("melody_path", "");
                    aiBeats.put("beats_url", "");
                    aiBeats.put("beats_path", "");
                    aiBeats.putArray("melody_percents").add(0.0);
                    aiBeats.putArray("beat_speed_infos");
                    beatsMat.add(bObj);

                    // Sound Channel Mapping
                    ObjectNode smObj = objectMapper.createObjectNode();
                    smObj.put("id", smid);
                    smObj.put("type", "");
                    smObj.put("audio_channel_mapping", 0);
                    smObj.put("is_config_open", false);
                    channelMat.add(smObj);

                    // Vocal Separation
                    ObjectNode vObj = objectMapper.createObjectNode();
                    vObj.put("id", vid);
                    vObj.put("type", "vocal_separation");
                    vObj.put("choice", 0);
                    vObj.putArray("removed_sounds");
                    vObj.putNull("time_range");
                    vObj.put("production_path", "");
                    vObj.put("final_algorithm", "");
                    vObj.put("enter_from", "");
                    vocalMat.add(vObj);

                    // Audio Segment
                    ObjectNode audioSeg = objectMapper.createObjectNode();
                    audioSeg.put("id", genId());
                    audioSeg.put("material_id", audioMatId);
                    audioSeg.put("render_index", 0);
                    audioSeg.put("speed", 1.0);
                    audioSeg.put("volume", 1.0);
                    audioSeg.put("last_nonzero_volume", 1.0);
                    audioSeg.put("visible", true);
                    audioSeg.put("track_attribute", 0);
                    audioSeg.put("track_render_index", 1);

                    ArrayNode refs = audioSeg.putArray("extra_material_refs");
                    refs.add(sid);
                    refs.add(pid);
                    refs.add(bid);
                    refs.add(smid);
                    refs.add(vid);

                    ObjectNode aSource = audioSeg.putObject("source_timerange");
                    aSource.put("start", 0);
                    aSource.put("duration", durUs);

                    ObjectNode aTarget = audioSeg.putObject("target_timerange");
                    aTarget.put("start", startUs);
                    aTarget.put("duration", durUs);

                    audioSegs.add(audioSeg);
                }

                if (audioSegs.size() > 0) {
                    tracks.add(newAudioTrack);
                    audioAdded = true;
                }
            }
            resp.setAudioAdded(audioAdded);

            // 3. Add Subtitle Track if requested
            int addedSubtitles = 0;
            if (req.isAddSubtitleTrack()) {
                // Remove existing text tracks
                for (int i = tracks.size() - 1; i >= 0; i--) {
                    if ("text".equalsIgnoreCase(tracks.get(i).path("type").asText())) {
                        tracks.remove(i);
                    }
                }

                ArrayNode textsMat = materials.withArray("texts");
                ArrayNode textSegs = objectMapper.createArrayNode();

                for (SegmentEntity seg : jobSegments) {
                    String spoken = seg.getCleanText();
                    if (spoken == null || spoken.isBlank()) {
                        spoken = seg.getText();
                    }
                    if (spoken == null || spoken.isBlank()) {
                        continue;
                    }

                    long startUs = Math.round(seg.getStartTimeSeconds() * 1_000_000);
                    long durUs = Math.round(seg.getDurationSeconds() * 1_000_000);
                    if (durUs <= 0) durUs = 1_000_000;

                    String textMatId = genId();
                    String textSegId = genId();

                    // Subtitle Material
                    ObjectNode contentObj = objectMapper.createObjectNode();
                    contentObj.put("text", spoken);
                    ArrayNode styles = contentObj.putArray("styles");
                    ObjectNode styleObj = objectMapper.createObjectNode();
                    styleObj.put("size", 8.0);
                    ArrayNode rng = styleObj.putArray("range");
                    rng.add(0);
                    rng.add(spoken.length());
                    ObjectNode font = styleObj.putObject("font");
                    font.put("id", "");
                    font.put("title", "Default");
                    ObjectNode fill = styleObj.putObject("fill");
                    fill.put("alpha", 1);
                    ObjectNode fillContent = fill.putObject("content");
                    fillContent.put("start_color", "#FFFFFF");
                    fillContent.put("end_color", "#FFFFFF");
                    styles.add(styleObj);

                    ObjectNode tMat = objectMapper.createObjectNode();
                    tMat.put("id", textMatId);
                    tMat.put("type", "text");
                    tMat.put("text_color", "#FFFFFF");
                    tMat.put("text_size", 30);
                    tMat.put("alignment", 1); // Centered
                    tMat.put("border_enabled", true);
                    tMat.put("border_color", "#000000");
                    tMat.put("border_width", 0.08);
                    tMat.put("border_alpha", 1.0);
                    tMat.put("has_shadow", true);
                    tMat.put("shadow_color", "#000000");
                    tMat.put("shadow_alpha", 0.9);
                    tMat.put("shadow_blur", 0.45);
                    tMat.put("shadow_distance", 8.0);
                    tMat.put("content", objectMapper.writeValueAsString(contentObj));
                    textsMat.add(tMat);

                    // Subtitle Segment
                    ObjectNode tSeg = objectMapper.createObjectNode();
                    tSeg.put("id", textSegId);
                    tSeg.put("material_id", textMatId);
                    tSeg.put("render_index", 11000);
                    tSeg.put("speed", 1.0);
                    tSeg.put("visible", true);
                    tSeg.put("volume", 1.0);

                    ObjectNode clip = tSeg.putObject("clip");
                    clip.put("alpha", 1.0);
                    ObjectNode transform = clip.putObject("transform");
                    transform.put("x", 0.0);
                    transform.put("y", -0.78); // Bottom subtitle position

                    ObjectNode tSrc = tSeg.putObject("source_timerange");
                    tSrc.put("start", 0);
                    tSrc.put("duration", durUs);

                    ObjectNode tTarget = tSeg.putObject("target_timerange");
                    tTarget.put("start", startUs);
                    tTarget.put("duration", durUs);

                    textSegs.add(tSeg);
                    addedSubtitles++;
                }

                ObjectNode newTextTrack = objectMapper.createObjectNode();
                newTextTrack.put("id", genId());
                newTextTrack.put("type", "text");
                newTextTrack.put("name", "Phụ đề Adam TTS");
                newTextTrack.put("attribute", 0);
                newTextTrack.put("flag", 0);
                newTextTrack.put("is_default_name", false);
                newTextTrack.set("segments", textSegs);
                tracks.add(newTextTrack);
            }
            resp.setAddedSubtitles(addedSubtitles);

            // 4. Update overall draft duration
            long totalUs = Math.round(jobSegments.get(jobSegments.size() - 1).getEndTimeSeconds() * 1_000_000);
            if (totalUs > 0) {
                draftNode.put("duration", totalUs);
            }

            // 5. Write back draft_content.json and template-2.tmp
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(draftPath, draftNode);
            File tmpFile = new File(projectFolder, "template-2.tmp");
            try {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmpFile, draftNode);
            } catch (Exception e) {
                log.warn("Could not write template-2.tmp: {}", e.getMessage());
            }

            // 6. Update metadata root_meta_info.json / draft_info.json
            updateCapCutMetadata(projectFolder, totalUs);

            resp.setSuccess(true);
            resp.setMessage(String.format("Đã đồng bộ thành công vào CapCut! (%d clip video, %d câu phụ đề, %s)",
                    syncedClips, addedSubtitles, audioAdded ? "có âm thanh lồng tiếng" : "không kèm audio"));

        } catch (Exception e) {
            log.error("Failed to apply to CapCut draft: {}", e.getMessage(), e);
            resp.setSuccess(false);
            resp.setMessage("Lỗi đồng bộ vào CapCut: " + e.getMessage());
        }

        return resp;
    }

    private void updateCapCutMetadata(File projectFolder, long durationUs) {
        File parentDir = projectFolder.getParentFile();
        if (parentDir == null || !parentDir.exists()) return;

        long nowUs = System.currentTimeMillis() * 1000;
        File rootMetaFile = new File(parentDir, "root_meta_info.json");
        if (rootMetaFile.exists()) {
            try {
                JsonNode root = objectMapper.readTree(rootMetaFile);
                if (root.isObject()) {
                    JsonNode list = root.get("all_draft_store");
                    if (list != null && list.isArray()) {
                        String targetNorm = projectFolder.getCanonicalPath();
                        for (JsonNode item : list) {
                            String itemPath = item.path("draft_fold_path").asText("");
                            if (!itemPath.isBlank() && new File(itemPath).getCanonicalPath().equalsIgnoreCase(targetNorm)) {
                                if (item.isObject()) {
                                    ObjectNode itemObj = (ObjectNode) item;
                                    itemObj.put("tm_draft_modified", nowUs);
                                    if (durationUs > 0) {
                                        itemObj.put("tm_duration", durationUs);
                                    }
                                }
                                break;
                            }
                        }
                        objectMapper.writeValue(rootMetaFile, root);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to update root_meta_info.json: {}", e.getMessage());
            }
        }
    }

    private void clearCapCutCache(File projectFolder) {
        try {
            File parent = projectFolder.getParentFile();
            if (parent != null && parent.getParentFile() != null) {
                File userData = parent.getParentFile();
                String[] cacheNames = {"Cache", "Code Cache", "GPUCache", "DawnCache", "GrShaderCache", "ShaderCache"};
                for (String cName : cacheNames) {
                    File cDir = new File(userData, cName);
                    if (cDir.exists() && cDir.isDirectory()) {
                        deleteDir(cDir);
                    }
                }
            }
            // Project sub cache
            for (String cn : new String[]{"Cache", "cache", "temp", "Temp"}) {
                File pCache = new File(projectFolder, cn);
                if (pCache.exists() && pCache.isDirectory()) {
                    deleteDir(pCache);
                }
            }
        } catch (Exception e) {
            log.warn("CapCut cache clear error: {}", e.getMessage());
        }
    }

    private void deleteDir(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDir(f);
                } else {
                    f.delete();
                }
            }
        }
        dir.delete();
    }

    private String genId() {
        return UUID.randomUUID().toString().toUpperCase();
    }
}

