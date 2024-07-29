package com.shh.shhbook.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.List;

@Getter
@Entity
@Setter
@NoArgsConstructor
public class Posts {
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String title;
    private String description;
    private Boolean show_desc;
    private String gallery_link;
    private String files_path;
    private String thumbnail_url;
    private Timestamp created_at;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JacksonXmlElementWrapper(localName = "comments")
    @JacksonXmlProperty(localName = "comment")
    private List<Comments> comments;

    public Posts(String title, String description) {
        this.title = title;
        this.description = description;
    }
}
