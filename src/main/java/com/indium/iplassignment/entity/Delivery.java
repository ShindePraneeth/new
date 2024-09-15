package com.indium.iplassignment.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Entity
@Table(name = "deliveries")
@Data
public class Delivery implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deliveryId;

    @ManyToOne
    @JoinColumn(name = "inning_id")
    @JsonBackReference

    private Inning inning;

    private int overNumber;
    private int ballNumber;
    private String batter;
    private String bowler;
    private String nonStriker;
    private int runsBatter;
    private int runsExtras;
    private int runsTotal;

    private boolean wicket;          // Whether the delivery resulted in a wicket
    private String dismissalKind;    // Type of dismissal (e.g., bowled, caught)
    private String playerOut;        // Name of the player who got out
    private String fielders;         // Names of fielders involved in the dismissal (if applicable)
}
