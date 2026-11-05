package com.adamtts.studio.service;

import com.adamtts.studio.model.SegmentEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TimelineServiceTest {

    private TimelineService timelineService;
    private TextSplittingService textSplittingService;

    @BeforeEach
    void setUp() {
        textSplittingService = new TextSplittingService();
        timelineService = new TimelineService(new ObjectMapper(), textSplittingService);
    }

    @Test
    @DisplayName("Định dạng mốc thời gian SRT chuẩn HH:mm:ss,SSS")
    void testFormatSrtTimestamp() {
        assertEquals("00:00:00,000", timelineService.formatSrtTimestamp(0.0));
        assertEquals("00:00:03,456", timelineService.formatSrtTimestamp(3.456));
        assertEquals("00:01:05,120", timelineService.formatSrtTimestamp(65.120));
        assertEquals("01:01:01,500", timelineService.formatSrtTimestamp(3661.500));
    }

    @Test
    @DisplayName("Tính toán mốc timeline chính xác theo mẫu (sample-accurate) không bị lệch tích luỹ")
    void testCalculateTimestamps() {
        SegmentEntity s1 = new SegmentEntity(1, "Đoạn 1", "Đoạn 1", "hash1");
        s1.setDurationSeconds(2.500);

        SegmentEntity s2 = new SegmentEntity(2, "Đoạn 2", "Đoạn 2", "hash2");
        s2.setDurationSeconds(3.100);

        SegmentEntity s3 = new SegmentEntity(3, "Đoạn 3", "Đoạn 3", "hash3");
        s3.setDurationSeconds(1.800);

        List<SegmentEntity> list = List.of(s1, s2, s3);
        double gap = 0.400; // 0.4s gap

        timelineService.calculateTimestamps(list, gap);

        // Segment 1 starts at 0
        assertEquals(0.0, s1.getStartTimeSeconds(), 0.001);
        assertEquals(2.500, s1.getDurationSeconds(), 0.001);
        assertEquals(2.500, s1.getEndTimeSeconds(), 0.001);

        // Segment 2 starts after s1 + gap = 2.5 + 0.4 = 2.9
        assertEquals(2.900, s2.getStartTimeSeconds(), 0.001);
        assertEquals(6.000, s2.getEndTimeSeconds(), 0.001); // 2.9 + 3.1 = 6.0

        // Segment 3 starts after s2 + gap = 6.0 + 0.4 = 6.4
        assertEquals(6.400, s3.getStartTimeSeconds(), 0.001);
        assertEquals(8.200, s3.getEndTimeSeconds(), 0.001); // 6.4 + 1.8 = 8.2
    }

    @Test
    @DisplayName("Xuất nội dung SRT hoàn chỉnh và tự động loại bỏ thẻ cảm xúc")
    void testGenerateSrtContent() {
        SegmentEntity s1 = new SegmentEntity(1, "[excited] Chào mừng các bạn!", "Chào mừng các bạn!", "h1");
        s1.setStartTimeSeconds(0.0);
        s1.setEndTimeSeconds(2.5);

        SegmentEntity s2 = new SegmentEntity(2, "[whispers] Hãy cẩn thận [pause]", "Hãy cẩn thận", "h2");
        s2.setStartTimeSeconds(2.9);
        s2.setEndTimeSeconds(5.0);

        String srt = timelineService.generateSrtContent(List.of(s1, s2));

        assertTrue(srt.contains("1\r\n00:00:00,000 --> 00:00:02,500\r\nChào mừng các bạn!"));
        assertTrue(srt.contains("2\r\n00:00:02,900 --> 00:00:05,000\r\nHãy cẩn thận"));
        assertFalse(srt.contains("[excited]"));
        assertFalse(srt.contains("[whispers]"));
        assertFalse(srt.contains("[pause]"));
    }
}

