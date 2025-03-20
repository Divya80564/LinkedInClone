package com.divya.linkedinclone.service;

import com.divya.linkedinclone.entity.Post;
import com.divya.linkedinclone.entity.User;
import com.divya.linkedinclone.repository.PostRepository;
import com.divya.linkedinclone.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;


    @Value("${upload.directory}") // Define the upload directory in application.properties
    private String uploadDirectory;

    public Post createPostWithImage(Long userId, String content, MultipartFile image) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Save the image file
        String imagePath = saveImage(image);

        // Create and save the post
        Post post = new Post();
        post.setContent(content);
        post.setImagePath(imagePath);
        post.setCreatedAt(LocalDateTime.now());
        post.setUser(user);

        return postRepository.save(post);
    }

    private String saveImage(MultipartFile image) throws IOException {
        if (image.isEmpty()) {
            throw new RuntimeException("Image file is empty");
        }

        // Generate a unique file name
        String fileName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();

        // Create the upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Save the file to the upload directory
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(image.getInputStream(), filePath);

        return filePath.toString();
    }


    // Create a new post
    public Post createPost(Long userId, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Post post = new Post();
        post.setContent(content);
        post.setCreatedAt(LocalDateTime.now());
        post.setUser(user);

        return postRepository.save(post);
    }


    // Get all posts by a user
    public List<Post> getPostsByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found");
        }
        return postRepository.findByUserId(userId);
    }

    // Get a specific post by ID
    public Post getPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
    }
    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }
    // Update a post
    public Post updatePost(Long postId, String newContent) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        post.setContent(newContent); // Update the content
        return postRepository.save(post); // Save the updated post
    }
    // Delete a post by ID
    public void deletePost(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new RuntimeException("Post not found");
        }
        postRepository.deleteById(postId);
    }
}