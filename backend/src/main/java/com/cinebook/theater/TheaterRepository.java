package com.cinebook.theater;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface TheaterRepository extends JpaRepository<Theater, UUID> {
    List<Theater> findByCityIgnoreCase(String city);

    @Query("SELECT DISTINCT t.city FROM Theater t ORDER BY t.city ASC")
    List<String> findDistinctCities();
}

