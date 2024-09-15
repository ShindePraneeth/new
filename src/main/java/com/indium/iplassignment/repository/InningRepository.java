package com.indium.iplassignment.repository;
import com.indium.iplassignment.entity.Inning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InningRepository extends JpaRepository<Inning, Long> {
}
