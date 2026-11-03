package com.cinebook.movie;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovieRepository extends JpaRepository<Movie, UUID> {
    List<Movie> findByTitleContainingIgnoreCase(String title);
    Optional<Movie> findByTitleIgnoreCase(String title);

    @Query(value = """
        SELECT DISTINCT m.*
        FROM movie m
        JOIN "show" s ON s.movie_id = m.id
        JOIN screen sc ON s.screen_id = sc.id
        JOIN theater t ON sc.theater_id = t.id
        WHERE LOWER(t.city) = LOWER(:city)
        AND (s.status = 'ACTIVE' OR s.status IS NULL)
        AND (s.booking_open = TRUE OR s.booking_open IS NULL)
        AND (s.show_date > :currentDate OR (s.show_date = :currentDate AND s.start_time >= :currentTime))
        AND (m.is_active = TRUE OR m.is_active IS NULL)
        ORDER BY m.rating DESC
        """, nativeQuery = true)
    List<Movie> findNowShowingByCity(
            @Param("city") String city,
            @Param("currentDate") LocalDate currentDate,
            @Param("currentTime") LocalTime currentTime
    );

    @Query(value = """
        SELECT *
        FROM movie
        WHERE release_date > :currentDate
        AND (is_active = TRUE OR is_active IS NULL)
        ORDER BY release_date ASC
        """, nativeQuery = true)
    List<Movie> findComingSoon(@Param("currentDate") LocalDate currentDate);
}
