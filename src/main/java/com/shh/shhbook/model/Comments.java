package com.shh.shhbook.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Getter
@Entity
@NoArgsConstructor
public class Comments {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long comment_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @JsonBackReference
    private Posts post;
    private String comment_content;
    private String user;
    private Timestamp created_at;

    public Comments(Posts post, String comment_content, Timestamp created_at, String user) {
        this.post = post;
        this.comment_content = comment_content;
        this.created_at = created_at;
        this.user = user;
    }
}
