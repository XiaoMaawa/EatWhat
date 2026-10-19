package com.eatwhat.modules.review.controller;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.common.result.R;
import com.eatwhat.common.utils.UserContext;
import com.eatwhat.modules.review.dto.CreateReviewRequest;
import com.eatwhat.modules.review.entity.Review;
import com.eatwhat.modules.review.service.ReviewService;
import com.eatwhat.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 评价接口。
 */
@Tag(name = "评价")
@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "提交评价")
    @RequireRole("CONSUMER")
    @PostMapping
    public R<Review> create(@Valid @RequestBody CreateReviewRequest request) {
        return R.ok(reviewService.create(UserContext.userId(), request));
    }

    @Operation(summary = "我的评价")
    @RequireRole("CONSUMER")
    @GetMapping("/mine")
    public R<PageResult<Review>> mine(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        return R.ok(reviewService.pageMyReviews(UserContext.userId(), page, size));
    }

    @Operation(summary = "商家回复评价")
    @RequireRole({"MERCHANT", "ADMIN"})
    @PostMapping("/{id}/reply")
    public R<Void> reply(@PathVariable Long id, @RequestBody Map<String, String> body) {
        reviewService.reply(UserContext.userId(), id, body == null ? null : body.get("reply"));
        return R.ok();
    }
}
