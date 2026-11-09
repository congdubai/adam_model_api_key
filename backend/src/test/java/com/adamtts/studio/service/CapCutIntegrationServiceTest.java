package com.adamtts.studio.service;

import com.adamtts.studio.dto.CapCutApplyRequest;
import com.adamtts.studio.dto.CapCutApplyResponse;
import com.adamtts.studio.model.JobEntity;
import com.adamtts.studio.model.SegmentEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CapCutIntegrationServiceTest {

    private ObjectMapper objectMapper;
    private JobEntity job;
    private CapCutIntegrationService capCutService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        job = new JobEntity();
        job.setId("job-123");
        SegmentEntity seg = new SegmentEntity();
        seg.setIndex(0);
        seg.setText("phấn nền son chỉ một lần lướt là đi hết luôn hơ");
        seg.setCleanText("phấn nền son chỉ một lần lướt là đi hết luôn hơ");
        seg.setStartTimeSeconds(0.0);
        seg.setDurationSeconds(2.5);
        seg.setEndTimeSeconds(2.5);
        job.setSegments(List.of(seg));

        JobManagerService stubJobManager = new JobManagerService(null, null, null, null, null, null) {
            @Override
            public JobEntity getActiveJob(String jobId) {
                return job;
            }
        };

        capCutService = new CapCutIntegrationService(objectMapper, stubJobManager);
    }

    @Test
    void testApplyToDraftWithTikTokSubtitles(@TempDir Path tempDir) throws IOException {
        File draftFile = tempDir.resolve("draft_content.json").toFile();
        String initialJson = """
        {
          "id": "TEST-DRAFT",
          "canvas_config": {
            "width": 1080,
            "height": 1920
          },
          "tracks": [
            { "id": "t1", "type": "video", "segments": [] }
          ],
          "materials": {
            "texts": [],
            "audios": [],
            "speeds": []
          }
        }
        """;
        Files.writeString(draftFile.toPath(), initialJson);

        CapCutApplyRequest req = new CapCutApplyRequest();
        req.setJobId("job-123");
        req.setProjectFolder(tempDir.toFile().getAbsolutePath());
        req.setSyncVideoClips(false);
        req.setAddAudioTrack(false);
        req.setAddSubtitleTrack(true);
        req.setSubtitleStyle("tiktok_black_bar");
        req.setSubtitleAlignment(0); // Left align

        CapCutApplyResponse resp = capCutService.applyToDraft(req);
        assertTrue(resp.isSuccess());
        assertEquals(1, resp.getAddedSubtitles());

        // Parse result json
        JsonNode resultJson = objectMapper.readTree(draftFile);
        JsonNode texts = resultJson.path("materials").path("texts");
        assertEquals(1, texts.size());

        JsonNode textMat = texts.get(0);
        assertEquals("#FFFFFF", textMat.path("text_color").asText());
        assertEquals(1, textMat.path("background_style").asInt());
        assertEquals("#000000", textMat.path("background_color").asText());
        assertEquals(0.70, textMat.path("background_alpha").asDouble(), 0.01);
        assertEquals(0.15, textMat.path("background_round_radius").asDouble(), 0.01);
        assertEquals(0, textMat.path("alignment").asInt());
        assertFalse(textMat.path("border_enabled").asBoolean());

        // Check text wrapping: "phấn nền son chỉ một lần lướt là đi hết\nluôn hơ"
        String baseContent = textMat.path("base_content").asText();
        assertTrue(baseContent.contains("\n"), "Should be wrapped into two lines");
        assertTrue(baseContent.startsWith("phấn nền son"));
        assertTrue(baseContent.endsWith("luôn hơ"));

        // Check track segments position
        JsonNode tracks = resultJson.path("tracks");
        JsonNode textTrack = null;
        for (JsonNode t : tracks) {
            if ("text".equals(t.path("type").asText())) {
                textTrack = t;
                break;
            }
        }
        assertNotNull(textTrack);
        JsonNode segNode = textTrack.path("segments").get(0);
        double y = segNode.path("clip").path("transform").path("y").asDouble();
        assertEquals(-0.58, y, 0.01, "Y position should be optimized for 9:16 vertical video");
    }
}
