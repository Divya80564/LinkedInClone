package com.divya.linkedinclone.controller;

import com.divya.linkedinclone.dto.CreatePostRequest;
import com.divya.linkedinclone.dto.PostResponse;
import com.divya.linkedinclone.entity.Post;
import com.divya.linkedinclone.service.PostService;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostService postService;

    // Create a new post
    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody CreatePostRequest createPostRequest) {
        try {
            Post createdPost = postService.createPost(createPostRequest.getUserId(), createPostRequest.getContent());
            return ResponseEntity.status(HttpStatus.CREATED).body(new PostResponse(createdPost));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // Create a post with an image
    @PostMapping("/image")
    public ResponseEntity<?> createPostWithImage(
            @RequestParam("userId") Long userId,
            @RequestParam("content") String content,
            @RequestParam("image") MultipartFile image) {
        try {
            Post createdPost = postService.createPostWithImage(userId, content, image);
            return ResponseEntity.status(HttpStatus.CREATED).body(new PostResponse(createdPost));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to upload image"));
        }
    }

    // Get all posts by a user
    @GetMapping("/users/{userId}/posts")
    public ResponseEntity<?> getPostsByUserId(@PathVariable Long userId) {
        try {
            List<Post> posts = postService.getPostsByUserId(userId);
            List<PostResponse> postResponses = posts.stream()
                    .map(PostResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(postResponses);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // Get a specific post by ID
    @GetMapping("/{postId}")
    public ResponseEntity<?> getPostById(@PathVariable Long postId) {
        try {
            Post post = postService.getPostById(postId);
            return ResponseEntity.ok(new PostResponse(post));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // Get all posts
    @GetMapping
    public ResponseEntity<List<PostResponse>> getAllPosts() {
        List<Post> posts = postService.getAllPosts();
        List<PostResponse> postResponses = posts.stream()
                .map(PostResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(postResponses);
    }

    // Update a post
    @PutMapping("/{postId}")
    public ResponseEntity<?> updatePost(@PathVariable Long postId, @RequestBody Map<String, String> request) {
        try {
            String newContent = request.get("content"); // Get the new content from the request body
            Post updatedPost = postService.updatePost(postId, newContent);
            return ResponseEntity.ok(new PostResponse(updatedPost));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // Delete a post by ID
    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Long postId) {
        try {
            postService.deletePost(postId);
            return ResponseEntity.ok(Map.of("message", "Post deleted successfully!"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}