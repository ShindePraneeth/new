package com.indium.iplassignment.repository;
import com.indium.iplassignment.entity.Delivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.time.LocalDate;
import java.util.List;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    @Query("SELECT d.deliveryId, d.overNumber, d.ballNumber, d.wicket, d.batter, d.bowler, d.dismissalKind " +
            "FROM Delivery d " +
            "WHERE d.bowler = :bowlerName AND d.wicket = TRUE")
    List<Object[]> findWicketsByBowler(@Param("bowlerName") String bowlerName);

    @Query("SELECT SUM(d.runsBatter) FROM Delivery d WHERE d.batter = :batterName")
    Integer getCumulativeScoreByBatter(@Param("batterName") String batterName);

    @Query("SELECT i.match.matchNumber, i.inningId, t.teamName, SUM(d.runsTotal) " +
            "FROM Delivery d " +
            "JOIN d.inning i " +
            "JOIN i.match m " +
            "JOIN i.team t " +
            "WHERE m.matchDate = :matchDate " +
            "GROUP BY i.match.matchNumber, i.inningId, t.teamName")
    List<Object[]> findInningScoresByDate(@Param("matchDate") LocalDate matchDate);

    // Query to get total runs for each batter and order them by runs in ascending order
    @Query("SELECT d.batter, SUM(d.runsBatter) AS totalRuns " +
            "FROM Delivery d " +
            "GROUP BY d.batter " +
            "HAVING SUM(d.runsBatter) > 0 " +
            "ORDER BY totalRuns DESC")
    List<Object[]> findTopBatsmen(Pageable pageable);



    // Custom query to get the total runs and total balls faced by a batsman in a match
    @Query("SELECT CASE WHEN COALESCE(COUNT(d.deliveryId), 0) = 0 THEN 0 " +
            "ELSE (COALESCE(SUM(d.runsBatter), 0) / COUNT(d.deliveryId)) * 100 END AS strikeRate " +
            "FROM Delivery d JOIN d.inning i " +
            "WHERE LOWER(d.batter) = LOWER(:batterName) " +
            "AND i.match.matchNumber = :matchNumber")
    Double getStrikeRateByBatterAndMatch(@Param("batterName") String batterName, @Param("matchNumber") int matchNumber);

    @Query("SELECT d.bowler, COUNT(d.deliveryId) AS totalWickets " +
            "FROM Delivery d " +
            "WHERE d.wicket = TRUE " +
            "GROUP BY d.bowler " +
            "ORDER BY totalWickets ASC")
    Page<Object[]> findTopWicketTakers(Pageable pageable);
}
