package com.shh.shhbook.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "login_statistics")
@Getter
@Setter
public class LoginStatistics {
    @Id
    private Long id;
    private int year;
    private int month;
    private int loginCount;
}
