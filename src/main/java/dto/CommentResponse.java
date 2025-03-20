package com.divya.linkedinclone.dto;

import com.divya.linkedinclone.entity.Comment;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Getter
@Setter
public class CommentResponse {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private Long postId;
    private Long userId;

    public CommentResponse(Comment comment) {
        this.id = comment.getId();
        this.content = comment.getContent();
        this.createdAt = comment.getCreatedAt();
        this.postId = comment.getPost().getId();
        this.userId = comment.getUser().getId();
    }
}