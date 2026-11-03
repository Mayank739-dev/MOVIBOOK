package com.cinebook.show;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface ShowRepository extends JpaRepository<Show, UUID> {

    @Query("""
        SELECT s FROM Show s
        JOIN FETCH s.screen sc
        JOIN FETCH sc.theater t
        JOIN FETCH s.movie m
        WHERE s.movie.id = :movieId
        AND LOWER(t.city) = LOWER(:city)
        AND (s.status = 'ACTIVE' OR s.status IS NULL)
        AND (s.bookingOpen = true OR s.bookingOpen IS NULL)
        AND (s.showDate > :currentDate OR (s.showDate = :currentDate AND s.startTime >= :currentTime))
        ORDER BY s.showDate ASC, s.startTime ASC
    """)
    List<Show> findActiveShowsByMovieAndCity(
            @Param("movieId") UUID movieId,
            @Param("city") String city,
            @Param("currentDate") LocalDate currentDate,
            @Param("currentTime") LocalTime currentTime
    );

    @Query("""
        SELECT COUNT(s) > 0 FROM Show s
        JOIN s.screen sc
        JOIN sc.theater t
        WHERE s.movie.id = :movieId
        AND LOWER(t.city) = LOWER(:city)
        AND (s.status = 'ACTIVE' OR s.status IS NULL)
        AND (s.bookingOpen = true OR s.bookingOpen IS NULL)
        AND (s.showDate > :currentDate OR (s.showDate = :currentDate AND s.startTime >= :currentTime))
    """)
    boolean hasActiveShowsInCity(
            @Param("movieId") UUID movieId,
            @Param("city") String city,
            @Param("currentDate") LocalDate currentDate,
            @Param("currentTime") LocalTime currentTime
    );
}

