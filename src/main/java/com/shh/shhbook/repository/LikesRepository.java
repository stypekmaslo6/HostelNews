package com.shh.shhbook.repository;

import com.shh.shhbook.model.Likes;
import com.shh.shhbook.model.Posts;
import com.shh.shhbook.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface LikesRepository extends JpaRepository<Likes, Long> {
    Optional<Likes> findByUsernameAndPostId(Users username, Posts postId);
    void deleteByUsernameAndPostId(Users username, Posts postId);
    int countByPostId(Posts postId);

    @Query("SELECT l.postId.id FROM Likes l WHERE l.username = :username")
    List<Long> findPostIdsByUsername(@Param("username") Users username);
}
