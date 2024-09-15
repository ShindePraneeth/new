package com.indium.iplassignment.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "matches")
@Data
@ToString(exclude = "innings") // Exclude innings to avoid circular references
public class Match implements Serializable {

    @Id
    @Column(name = "match_number") // Explicitly map to match_number column
    private Long matchNumber; // This is the primary key, mapped to match_number in DB

    private String matchType;
    private String city;
    private String venue;
    private String eventName;
    private String winner;
    private String tossWinner;
    private String tossDecision;
    private String playerOfMatch;

    @Column(name = "match_date")
    private LocalDate matchDate; // Match date from the JSON

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Inning> innings;
}
