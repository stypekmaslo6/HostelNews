package com.shh.shhbook.repository;

import com.shh.shhbook.model.Posts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface PostsRepository extends JpaRepository<Posts, String> {
    @Query("SELECT DISTINCT p FROM Posts p WHERE p.description LIKE %:searchTerm% OR p.title LIKE %:searchTerm%")
    List<Posts> findByDescriptionOrTitleContaining(@Param("searchTerm") String searchTerm);
    Posts findById(Long postId);
    @Transactional
    void deleteById(Long id);
    Page<Posts> findByDescriptionContainingOrTitleContaining(String description, String title, Pageable pageable);
    Page<Posts> findAll(Pageable pageable);
}
