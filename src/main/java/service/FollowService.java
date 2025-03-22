package com.divya.linkedinclone.service;

import com.divya.linkedinclone.entity.Follow;
import com.divya.linkedinclone.entity.User;
import com.divya.linkedinclone.exception.UserNotFoundException;
import com.divya.linkedinclone.repository.FollowRepository;
import com.divya.linkedinclone.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FollowService {

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private UserRepository userRepository;

    public String followUser(Long followerId, Long followingId) {
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new UserNotFoundException("Follower user not found with id: " + followerId));
        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new UserNotFoundException("Following user not found with id: " + followingId));

        Optional<Follow> existingFollow = followRepository.findByFollowerAndFollowing(follower, following);
        if (existingFollow.isPresent()) {
            return "You are already following this user";
        }

        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);
        followRepository.save(follow);

        return "You are now following user " + followingId;
    }

    public String unfollowUser(Long followerId, Long followingId) {
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new UserNotFoundException("Follower user not found with id: " + followerId));
        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new UserNotFoundException("Following user not found with id: " + followingId));

        Optional<Follow> existingFollow = followRepository.findByFollowerAndFollowing(follower, following);
        if (existingFollow.isEmpty()) {
            return "You are not following this user";
        }

        followRepository.delete(existingFollow.get());
        return "You have unfollowed user " + followingId;
    }

    public List<Long> getFollowers(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        return followRepository.findByFollowing(user).stream()
                .map(follow -> follow.getFollower().getId())
                .collect(Collectors.toList());
    }

    public List<Long> getFollowing(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        return followRepository.findByFollower(user).stream()
                .map(follow -> follow.getFollowing().getId())
                .collect(Collectors.toList());
    }
}