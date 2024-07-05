package com.shh.shhbook.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class PostFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String fileType;

    @Lob
    private byte[] data;

    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false)
    private Posts post;

    public PostFile(String fileName, String fileType, byte[] data, Posts post) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.data = data;
        this.post = post;
    }
}
