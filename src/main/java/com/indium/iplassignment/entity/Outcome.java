package com.indium.iplassignment.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Entity
@Table(name = "outcomes")
@Data
public class Outcome implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long outcomeId;

    @ManyToOne
    @JoinColumn(name = "match_number")
    private Match match;

    private String outcomeBy;
    private int margin;

    // New fields for the winning team
    @ManyToOne
    @JoinColumn(name = "winner_team_id")
    private Team winnerTeam;

    private String winnerTeamName;  // Winner team name directly stored
}
