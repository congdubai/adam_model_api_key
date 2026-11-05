package com.adamtts.studio.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextSplittingServiceTest {

    private TextSplittingService service;

    @BeforeEach
    void setUp() {
        service = new TextSplittingService();
    }

    @Test
    @DisplayName("Tách văn bản theo từng dòng (LINE mode)")
    void testSplitByLine() {
        String input = "Dòng 1: Xin chào.\r\nDòng 2: Đây là Adam TTS Studio.\n\nDòng 3: Dòng cuối cùng.";
        List<String> segments = service.splitText(input, "LINE", "eleven_v3");

        assertEquals(3, segments.size());
        assertEquals("Dòng 1: Xin chào.", segments.get(0));
        assertEquals("Dòng 2: Đây là Adam TTS Studio.", segments.get(1));
        assertEquals("Dòng 3: Dòng cuối cùng.", segments.get(2));
    }

    @Test
    @DisplayName("Tách văn bản theo đoạn cách nhau bằng dòng trống (PARAGRAPH mode)")
    void testSplitByParagraph() {
        String input = "Đoạn 1 câu 1. Đoạn 1 câu 2.\n\nĐoạn 2 câu 1.\nĐoạn 2 câu 2.\n\n\nĐoạn 3 kết thúc.";
        List<String> segments = service.splitText(input, "PARAGRAPH", "eleven_v3");

        assertEquals(3, segments.size());
        assertTrue(segments.get(0).contains("Đoạn 1 câu 1. Đoạn 1 câu 2."));
        assertTrue(segments.get(1).contains("Đoạn 2 câu 1.\nĐoạn 2 câu 2."));
        assertEquals("Đoạn 3 kết thúc.", segments.get(2));
    }

    @Test
    @DisplayName("Tự động cắt đoạn vượt quá 4500 ký tự (model eleven_v3) theo ranh giới câu")
    void testSplitSentenceBoundaryForLongTextV3() {
        // Build a chunk > 4500 chars
        StringBuilder sentence = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sentence.append("Đây là câu số ").append(i).append(" để kiểm tra khả năng tách câu chính xác. ");
        }
        String longText = sentence.toString(); // Around ~3200 chars
        // Repeat to make it > 4500
        String doubleLong = longText + "Câu phân cách ở giữa! " + longText;
        assertTrue(doubleLong.length() > 4500);

        List<String> segments = service.splitText(doubleLong, "PARAGRAPH", "eleven_v3");
        assertTrue(segments.size() >= 2);

        for (String seg : segments) {
            assertTrue(seg.length() <= 4500, "Đoạn không được vượt quá 4500 ký tự với eleven_v3");
        }
    }

    @Test
    @DisplayName("Model khác cho phép đến 9000 ký tự")
    void testMaxCharsForOtherModels() {
        assertEquals(4500, service.getMaxCharsForModel("eleven_v3"));
        assertEquals(9000, service.getMaxCharsForModel("eleven_multilingual_v2"));
        assertEquals(9000, service.getMaxCharsForModel("eleven_flash_v2_5"));
    }

    @Test
    @DisplayName("Bỏ toàn bộ thẻ cảm xúc [..] khỏi phụ đề")
    void testStripEmotionTags() {
        String raw = "[excited] Xin chào quý vị! [whispers] Hãy lắng nghe kỹ điều này. [laughs] Thật buồn cười [short pause] đúng không? [long pause] Cảm ơn [sighs] bạn.";
        String clean = service.stripEmotionTags(raw);

        assertEquals("Xin chào quý vị! Hãy lắng nghe kỹ điều này. Thật buồn cười đúng không? Cảm ơn bạn.", clean);
        assertFalse(clean.contains("["));
        assertFalse(clean.contains("]"));
    }

    @Test
    @DisplayName("Tách mốc thời gian [start, end] và giữ nguyên thẻ cảm xúc trong spoken text")
    void testParseSegmentTiming() {
        TextSplittingService.ParsedSegment p1 = service.parseSegmentTiming("[0, 20] [excited] Đoạn mở đầu từ 0 đến 20s");
        assertEquals(0.0, p1.targetStartTime());
        assertEquals(20.0, p1.targetEndTime());
        assertEquals("[excited] Đoạn mở đầu từ 0 đến 20s", p1.spokenText());

        TextSplittingService.ParsedSegment p2 = service.parseSegmentTiming("[45, 60] Đoạn tiếp theo ở giây 45");
        assertEquals(45.0, p2.targetStartTime());
        assertEquals(60.0, p2.targetEndTime());
        assertEquals("Đoạn tiếp theo ở giây 45", p2.spokenText());

        TextSplittingService.ParsedSegment p3 = service.parseSegmentTiming("[01:15, 01:45] Mốc thời gian dạng MM:SS");
        assertEquals(75.0, p3.targetStartTime());
        assertEquals(105.0, p3.targetEndTime());
        assertEquals("Mốc thời gian dạng MM:SS", p3.spokenText());

        TextSplittingService.ParsedSegment p4 = service.parseSegmentTiming("[30] Chỉ có mốc bắt đầu");
        assertEquals(30.0, p4.targetStartTime());
        assertNull(p4.targetEndTime());
        assertEquals("Chỉ có mốc bắt đầu", p4.spokenText());

        TextSplittingService.ParsedSegment p5 = service.parseSegmentTiming("Không có mốc thời gian");
        assertNull(p5.targetStartTime());
        assertNull(p5.targetEndTime());
        assertEquals("Không có mốc thời gian", p5.spokenText());
    }
}

