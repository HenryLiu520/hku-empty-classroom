/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 * Provenance: also exposed at GET /api/about, in the UI colophon, and in the file headers.
 */
package hku.ec.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 出处信息。项目里的一枚署名：接口本身带着作者与签名，拷走代码这个端点也会跟着走。
 */
@RestController
@RequestMapping("/api")
public class AboutController {

    static final String PROJECT = "Empty Classroom Plan";
    static final String COURSE = "HKU COMP1110";
    static final String GROUP = "Group 08";
    static final String AUTHOR = "LIU Haoran";
    static final String UID = "u3686264";
    /** 签名：课程 + 组 + 学号 + 年份，和源码文件头、界面署名一致。 */
    static final String SIGNATURE = "EC-COMP1110-G08-u3686264-2026";

    @GetMapping("/about")
    public Map<String, Object> about() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("project", PROJECT);
        out.put("course", COURSE);
        out.put("group", GROUP);
        out.put("author", AUTHOR);
        out.put("uid", UID);
        out.put("signature", SIGNATURE);
        out.put("year", 2026);
        return out;
    }
}
