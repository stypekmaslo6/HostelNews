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
}
