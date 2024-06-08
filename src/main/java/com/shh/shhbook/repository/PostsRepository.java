package com.shh.shhbook.repository;

import com.shh.shhbook.model.Posts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostsRepository extends JpaRepository<Posts, String> {
    List<Posts> findByDescriptionContaining(String description);
}
