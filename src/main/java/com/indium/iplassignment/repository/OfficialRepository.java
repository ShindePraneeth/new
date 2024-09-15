package com.indium.iplassignment.repository;
import com.indium.iplassignment.entity.Official;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfficialRepository extends JpaRepository<Official, Long> {
    @Query("SELECT o FROM Official o WHERE o.match.matchNumber = :matchNumber AND o.role = 'match_referees'")
    List<Official> findMatchRefereesByMatchNumber(@Param("matchNumber") Long matchNumber);
}

