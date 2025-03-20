package com.divya.linkedinclone.service;

import com.divya.linkedinclone.entity.Like;
import com.divya.linkedinclone.entity.Post;
import com.divya.linkedinclone.entity.User;
import com.divya.linkedinclone.exception.PostNotFoundException;
import com.divya.linkedinclone.exception.UserNotFoundException;
import com.divya.linkedinclone.repository.LikeRepository;
import com.divya.linkedinclone.repository.PostRepository;
import com.divya.linkedinclone.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.divya.linkedinclone.dto.LikeResponse;
import java.util.Optional;

@Service
public class LikeService {

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    public LikeResponse toggleLike(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with id: " + postId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        Optional<Like> existingLike = likeRepository.findByPostIdAndUserId(postId, userId);

        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
        } else {
            Like like = new Like();
            like.setPost(post);
            like.setUser(user);
            likeRepository.save(like);
        }

        Long likeCount = likeRepository.countByPostId(postId);
        return new LikeResponse(postId, userId, likeCount);
    }

    public Long getLikeCount(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException("Post not found with id: " + postId);
        }
        return likeRepository.countByPostId(postId);
    }
}