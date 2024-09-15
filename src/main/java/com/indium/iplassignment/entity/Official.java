package com.indium.iplassignment.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Entity
@Table(name = "officials")
@Data
public class Official  implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long officialId;

    private String officialName;
    private String role;

    @ManyToOne
    @JoinColumn(name = "match_number")
    private Match match;
}