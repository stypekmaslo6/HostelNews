package com.shh.shhbook.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Getter
@Entity
@NoArgsConstructor
public class Comments {
    @Id
    private Long comment_id;
    private Long post_id;
    private String comment_content;
    private Timestamp created_at;
    public Comments(Long comment_id, Long post_id, String comment_content, Timestamp created_at) {
        this.comment_id = comment_id;
        this.post_id = post_id;
        this.comment_content = comment_content;
        this.created_at = created_at;
    }

    public void setComment_id(Long comment_id) {
        this.comment_id = comment_id;
    }

    public void setPost_id(Long post_id) {
        this.post_id = post_id;
    }

    public void setComment_content(String comment_content) {
        this.comment_content = comment_content;
    }
}
