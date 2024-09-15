package com.indium.iplassignment.repository;


import com.indium.iplassignment.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    @Query("SELECT p FROM Player p " +
            "JOIN p.team t " +
            "JOIN Inning i ON t.teamId = i.team.teamId " +
            "JOIN i.match m " +
            "WHERE t.teamName = :teamName AND m.matchNumber = :matchNumber")
    List<Player> findPlayersByTeamAndMatch(@Param("teamName") String teamName, @Param("matchNumber") int matchNumber);

}