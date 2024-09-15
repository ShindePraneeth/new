package com.indium.iplassignment.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Entity
@Table(name = "players")
@Data
public class Player implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long playerId;

    @Column(name = "player_name", nullable = false)
    private String playerName;

    @ManyToOne
    @JoinColumn(name = "team_id", referencedColumnName = "team_id")  // Reference to team_id in Team entity
    @JsonBackReference  // To avoid circular reference issues during JSON serialization
    private Team team;
}
