package com.shh.shhbook.repository;

import com.shh.shhbook.model.Comments;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentsRepository extends JpaRepository<Comments, String> {
    List<Comments> findByPost_idContaining(Long post_id);
}
