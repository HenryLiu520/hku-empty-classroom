package hku.ec.web;

import hku.ec.service.AuthService;
import hku.ec.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 房间评价：任何登录用户都可以评价房间（星级 + 文字），可以改自己的、删自己的；管理员可以删任何一条。 */
@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviews;
    private final AuthService auth;

    public ReviewController(ReviewService reviews, AuthService auth) {
        this.reviews = reviews;
        this.auth = auth;
    }

    /** 某间房的评价（公开可读；带上 token 才知道哪条是你自己的） */
    @GetMapping("/rooms/{code}/reviews")
    public ResponseEntity<?> list(@PathVariable String code,
                                 @RequestParam(defaultValue = "time") String sort,
                                 @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String viewer = auth.resolve(Tokens.from(authHeader)).map(AuthService.Session::username).orElse(null);
        try {
            return ResponseEntity.ok(reviews.list(code, sort, viewer));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(new Dtos.ApiError("ROOM_NOT_FOUND", e.getMessage()));
        }
    }

    /** 提交或更新自己的评价（一个人对一间房只有一条） */
    @PostMapping("/rooms/{code}/reviews")
    public ResponseEntity<?> upsert(@PathVariable String code,
                                    @RequestBody Dtos.ReviewRequest req,
                                    @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AuthService.Session s;
        try {
            s = auth.require(Tokens.from(authHeader), null);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).body(new Dtos.ApiError("UNAUTHORIZED", "Sign in to review a room"));
        }
        try {
            return ResponseEntity.ok(reviews.upsert(code, req.rating(), req.body(), s.username()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new Dtos.ApiError("BAD_REVIEW", e.getMessage()));
        }
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AuthService.Session s;
        try {
            s = auth.require(Tokens.from(authHeader), null);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).body(new Dtos.ApiError("UNAUTHORIZED", "Sign in first"));
        }
        try {
            reviews.delete(id, s.username(), AuthService.atLeast(s.role(), "admin"));
            return ResponseEntity.ok(Map.of("deleted", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new Dtos.ApiError("BAD_REVIEW", e.getMessage()));
        }
    }

    /** 每间房的平均分与条数（房间列表上显示星级用） */
    @GetMapping("/reviews/summary")
    public ResponseEntity<?> summary() {
        return ResponseEntity.ok(reviews.summary());
    }
}
