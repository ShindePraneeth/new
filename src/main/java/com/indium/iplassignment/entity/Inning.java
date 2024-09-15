package com.indium.iplassignment.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "innings")
@Data
@ToString(exclude = "match") // Exclude match to avoid circular references
public class Inning implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inningId;

    // Mapping to the Match entity using match_number as the foreign key
    @ManyToOne
    @JoinColumn(name = "match_number", referencedColumnName = "match_number")
    @JsonBackReference// Reference match_number column in the Match entity
    private Match match;

    // Mapping to the Team entity using team_id as the foreign key
    @ManyToOne
    @JoinColumn(name = "team_id", referencedColumnName = "team_id")
    private Team team;

    @OneToMany(mappedBy = "inning", cascade = CascadeType.ALL)
    private List<Delivery> deliveries;
}
