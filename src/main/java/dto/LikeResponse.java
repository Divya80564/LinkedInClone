package com.divya.linkedinclone.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LikeResponse {
    private Long postId;
    private Long userId;
    private Long likeCount;

    public LikeResponse(Long postId, Long userId, Long likeCount) {
        this.postId = postId;
        this.userId = userId;
        this.likeCount = likeCount;
    }
}