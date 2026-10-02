package hku.ec.service;

import hku.ec.web.Dtos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 评价的删除权限：任何人都只能删自己的；只有超级管理员能删别人的。
 * （普通用户和管理员都不行。）
 */
@SpringBootTest
@Transactional
class ReviewRulesTest {

    @Autowired private ReviewService reviews;

    @Test
    @DisplayName("1. 评价：只能删自己的，删别人的必须超级管理员")
    void onlyOwnerOrSuperCanDelete() {
        Dtos.ReviewView mine = reviews.upsert("CPD-LG.01", 5, "unit test mine", "studentA");
        Dtos.ReviewView theirs = reviews.upsert("CPD-LG.01", 4, "unit test theirs", "studentB");

        assertThrows(IllegalArgumentException.class,
                () -> reviews.delete(theirs.id(), "studentA", false),
                "普通用户不能删别人的评价");
        assertThrows(IllegalArgumentException.class,
                () -> reviews.delete(theirs.id(), "admin1", false),
                "管理员也只能删自己的评价");

        reviews.delete(mine.id(), "studentA", false);     // 自己的：可以
        reviews.delete(theirs.id(), "super1", true);      // 超级管理员：可以
    }
}
