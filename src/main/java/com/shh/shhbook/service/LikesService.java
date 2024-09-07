package com.shh.shhbook.service;

import com.shh.shhbook.model.Likes;
import com.shh.shhbook.model.Posts;
import com.shh.shhbook.model.Users;
import com.shh.shhbook.repository.LikesRepository;
import com.shh.shhbook.repository.PostsRepository;
import com.shh.shhbook.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class LikesService {

    @Autowired
    private LikesRepository likeRepository;

    @Autowired
    private UsersRepository userRepository;

    @Autowired
    private PostsRepository postRepository;

    @Transactional
    public boolean toggleLike(String username, Long postId) {
        Users user = userRepository.findByUsername(username);
        Posts post = postRepository.findById(postId);
        Optional<Likes> existingLike = likeRepository.findByUsernameAndPostId(user, post);

        if (existingLike.isPresent()) {
            likeRepository.deleteByUsernameAndPostId(user, post);
            post.setLike_count(getLikeCount(postId));
            return false;
        } else {
            Likes like = new Likes();
            like.setUsername(user);
            like.setPostId(new Posts(postId));
            likeRepository.save(like);
            post.setLike_count(getLikeCount(postId));
            return true;
        }
    }

    public int getLikeCount(Long postId) {
        return likeRepository.countByPostId(postRepository.findById(postId));
    }
}
