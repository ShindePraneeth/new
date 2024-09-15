package com.indium.iplassignment.repository;

import com.indium.iplassignment.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    @Query("SELECT DISTINCT m.matchNumber, m.matchType, m.city, m.venue, m.eventName, m.winner, m.tossWinner, m.tossDecision, m.playerOfMatch, m.matchDate " +
            "FROM Match m " +
            "JOIN Inning i ON m.matchNumber = i.match.matchNumber " +
            "JOIN Delivery d ON i.inningId = d.inning.inningId " +
            "WHERE LOWER(d.batter) = LOWER(:playerName) OR LOWER(d.bowler) = LOWER(:playerName)")
    List<Object[]> findMatchEventsByPlayerName(@Param("playerName") String playerName);
    Match findByMatchNumber(int matchNumber);




}
