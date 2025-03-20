package com.divya.linkedinclone.dto;

import com.divya.linkedinclone.entity.Post;
import com.divya.linkedinclone.entity.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PostResponse {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String imagePath;
    private UserResponse user; // Include user details

    @Getter
    @Setter
    public static class UserResponse {
        private Long id;
        private String name;
        private String email;

        public UserResponse(User user) {
            this.id = user.getId();
            this.name = user.getName();
            this.email = user.getEmail();
        }
    }

    public PostResponse(Post post) {
        this.id = post.getId();
        this.content = post.getContent();
        this.createdAt = post.getCreatedAt();
        this.updatedAt = post.getUpdatedAt();
        this.imagePath = post.getImagePath();
        this.user = new UserResponse(post.getUser()); // Include user details
    }
}