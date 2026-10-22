/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 */
package hku.ec.service;

import hku.ec.web.Dtos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 评价照片的四条规则：
 *   1. 只能往自己写的评价上加照片；
 *   2. 只收 JPEG / PNG / WebP（按文件头判断，不看浏览器声明的类型）；
 *   3. 单张不超过 3 MB；
 *   4. 一条评价最多 3 张；删评价时照片一起清掉。
 * 测试用临时目录，避免往仓库目录里写文件。
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "app.upload-dir=${java.io.tmpdir}/ec-test-photos")
class ReviewPhotoRulesTest {

    @Autowired private ReviewService reviews;
    @Autowired private ReviewPhotoService photos;

    @Test
    @DisplayName("1. 只能给自⼰的评价加照片")
    void onlyTheAuthorCanAddPhotos() {
        Dtos.ReviewView mine = reviews.upsert("CPD-LG.01", 5, "unit test photo mine", "studentA");
        Dtos.ReviewView theirs = reviews.upsert("CPD-LG.01", 4, "unit test photo theirs", "studentB");

        assertThrows(IllegalArgumentException.class,
                () -> photos.add(theirs.id(), "studentA", png(64), false),
                "别人不能往我的评价里塞照片");

        assertDoesNotThrow(() -> photos.add(mine.id(), "studentA", png(64), false));
    }

    @Test
    @DisplayName("2. 不是图片就拒收（即使文件名以 .png 结尾）")
    void rejectsNonImages() {
        Dtos.ReviewView r = reviews.upsert("CPD-LG.01", 5, "unit test non image", "studentC");

        MockMultipartFile fake = new MockMultipartFile(
                "file", "looks-like.png", "image/png",
                "this is plain text, not a PNG".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalArgumentException.class,
                () -> photos.add(r.id(), "studentC", fake, false),
                "按文件头判断，文本文件必须被拒");
    }

    @Test
    @DisplayName("3. 单张超过 3 MB 拒收")
    void rejectsOversizedPhotos() {
        Dtos.ReviewView r = reviews.upsert("CPD-LG.01", 5, "unit test too big", "studentD");

        assertThrows(IllegalArgumentException.class,
                () -> photos.add(r.id(), "studentD", png(ReviewPhotoService.MAX_BYTES + 1), false),
                "超过 3 MB 必须被拒");
    }

    @Test
    @DisplayName("4. 一条评价最多 3 张，删评价时照片一起清掉")
    void capsAtThreeAndCleansUp() {
        Dtos.ReviewView r = reviews.upsert("CPD-LG.01", 5, "unit test three photos", "studentE");

        for (int i = 0; i < ReviewPhotoService.MAX_PER_REVIEW; i++) {
            photos.add(r.id(), "studentE", png(32), false);
        }
        assertThrows(IllegalArgumentException.class,
                () -> photos.add(r.id(), "studentE", png(32), false),
                "第 4 张必须被拒");

        Dtos.ReviewList before = reviews.list("CPD-LG.01", "time", "studentE");
        int attached = before.items().stream()
                .filter(v -> v.id().equals(r.id()))
                .findFirst().orElseThrow().photos().size();
        assertEquals(ReviewPhotoService.MAX_PER_REVIEW, attached, "三条照片应该都挂在这条评价上");

        reviews.delete(r.id(), "studentE", false);

        Dtos.ReviewList after = reviews.list("CPD-LG.01", "time", "studentE");
        boolean stillThere = after.items().stream().anyMatch(v -> v.id().equals(r.id()));
        assertEquals(false, stillThere, "评价删掉后不该还在列表里");
    }

    /** 造一张「文件头正确」的假 PNG：只用于测规则，不要求真的能解码 */
    private static MockMultipartFile png(int totalBytes) {
        byte[] head = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        byte[] body = new byte[Math.max(totalBytes, head.length)];
        System.arraycopy(head, 0, body, 0, head.length);
        return new MockMultipartFile("file", "photo.png", "image/png", body);
    }
}
