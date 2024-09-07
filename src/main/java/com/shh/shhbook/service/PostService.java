package com.shh.shhbook.service;

import com.shh.shhbook.model.Posts;
import com.shh.shhbook.repository.PostsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PostService {

    @Autowired
    private PostsRepository postRepository;

    @Transactional(readOnly = true)
    public List<Posts> getAllPosts() {
        return postRepository.findAll();
    }

    public void deletePostById(Long id) {
        postRepository.deleteById(id);
    }

    public Posts updatePost(Long id, Posts updatedPost) {
        Posts existingPost = postRepository.findById(String.valueOf(id)).orElseThrow(() -> new RuntimeException("Post not found"));

        if (!updatedPost.getTitle().isEmpty()) {
            existingPost.setTitle(updatedPost.getTitle());
        }
        if (!updatedPost.getDescription().isEmpty()) {
            existingPost.setDescription(updatedPost.getDescription());
            existingPost.setShow_desc(updatedPost.getShow_desc());
        }
        if (!updatedPost.getGallery_link().isEmpty()) {
            existingPost.setGallery_link(updatedPost.getGallery_link());
        }

        return postRepository.save(existingPost);
    }
}
