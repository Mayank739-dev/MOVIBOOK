package com.cinebook.movie;

import com.cinebook.movie.dto.*;
import com.cinebook.show.Show;
import com.cinebook.show.ShowRepository;
import com.cinebook.theater.TheaterRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/movies")
public class MovieController {
    private final MovieRepository movieRepo;
    private final TheaterRepository theaterRepo;
    private final ShowRepository showRepo;
    private final TmdbService tmdbService;

    public MovieController(MovieRepository movieRepo, TheaterRepository theaterRepo, ShowRepository showRepo, TmdbService tmdbService) {
        this.movieRepo = movieRepo;
        this.theaterRepo = theaterRepo;
        this.showRepo = showRepo;
        this.tmdbService = tmdbService;
    }

    @GetMapping("/config")
    public Map<String, Object> configStatus() {
        return Map.of("configured", tmdbService.isConfigured());
    }

    @GetMapping("/cities")
    public List<String> cities() {
        List<String> list = theaterRepo.findDistinctCities();
        return (list != null && !list.isEmpty()) ? list : List.of("Chandigarh", "Delhi", "Mumbai");
    }

    /**
     * Requirement 1 & 4: NOW SHOWING
     * Strictly driven by database shows in the selected city.
     * No show in the selected city = No movie in Now Showing.
     */
    @GetMapping("/now-showing")
    public List<MovieDto> nowShowing(@RequestParam(defaultValue = "Chandigarh") String city) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<Movie> movies = movieRepo.findNowShowingByCity(city, today, now);
        return movies.stream().map(this::convertToDto).toList();
    }

    /**
     * Requirement 2 & 6: COMING SOON
     * Strictly release_date > CURRENT_DATE.
     * No old movies from previous years.
     */
    @GetMapping("/coming-soon")
    public List<MovieDto> comingSoon() {
        LocalDate today = LocalDate.now();
        List<Movie> dbComingSoon = movieRepo.findComingSoon(today);
        List<MovieDto> results = new ArrayList<>(dbComingSoon.stream().map(this::convertToDto).toList());

        // Also fetch from external API upcoming if configured, but strictly filter release_date > today
        PagedResponse<MovieDto> apiUpcoming = tmdbService.getUpcoming(1);
        if (apiUpcoming != null && apiUpcoming.getResults() != null) {
            for (MovieDto m : apiUpcoming.getResults()) {
                if (m.getReleaseDate() != null && !m.getReleaseDate().isBlank()) {
                    try {
                        LocalDate relDate = LocalDate.parse(m.getReleaseDate());
                        if (relDate.isAfter(today)) {
                            // Ensure no duplicate title
                            boolean exists = results.stream().anyMatch(x -> x.getTitle().equalsIgnoreCase(m.getTitle()));
                            if (!exists) {
                                results.add(m);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        results.sort(Comparator.comparing(MovieDto::getReleaseDate));
        return results;
    }

    /**
     * Requirement 5.4: FEATURED MOVIES
     * Only movies with active shows in the selected city.
     */
    @GetMapping("/featured")
    public List<MovieDto> featured(@RequestParam(defaultValue = "Chandigarh") String city) {
        return nowShowing(city);
    }

    @GetMapping("/genres")
    public List<GenreDto> genres() {
        return tmdbService.getGenres();
    }

    @GetMapping("/trending")
    public PagedResponse<MovieDto> trending(
            @RequestParam(defaultValue = "day") String timeWindow,
            @RequestParam(defaultValue = "1") int page) {
        return tmdbService.getTrending(timeWindow, page);
    }

    @GetMapping("/popular")
    public PagedResponse<MovieDto> popular(@RequestParam(defaultValue = "1") int page) {
        return tmdbService.getPopular(page);
    }

    @GetMapping("/top-rated")
    public PagedResponse<MovieDto> topRated(@RequestParam(defaultValue = "1") int page) {
        return tmdbService.getTopRated(page);
    }

    @GetMapping("/upcoming")
    public PagedResponse<MovieDto> upcoming(@RequestParam(defaultValue = "1") int page) {
        return tmdbService.getUpcoming(page);
    }

    @GetMapping("/search")
    public PagedResponse<MovieDto> search(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "1") int page) {
        return tmdbService.search(query, page);
    }

    @GetMapping("/discover")
    public PagedResponse<MovieDto> discover(
            @RequestParam(required = false) String genreId,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "popularity.desc") String sortBy,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) String language,
            @RequestParam(defaultValue = "1") int page) {
        return tmdbService.discover(genreId, year, sortBy, minRating, language, page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> one(@PathVariable String id, @RequestParam(defaultValue = "Chandigarh") String city) {
        try {
            UUID uuid = UUID.fromString(id);
            Optional<Movie> localMovie = movieRepo.findById(uuid);
            if (localMovie.isPresent()) {
                MovieDetailDto detail = convertToDetailDto(localMovie.get());
                boolean hasShows = showRepo.hasActiveShowsInCity(uuid, city, LocalDate.now(), LocalTime.now());
                detail.setBookingAvailable(hasShows);
                return ResponseEntity.ok(detail);
            }
        } catch (IllegalArgumentException ignored) {
            // Not a UUID, handle TMDB numeric ID
        }

        MovieDetailDto detail = tmdbService.getMovieDetail(id);
        if (detail != null) {
            detail.setBookingAvailable(false);
            return ResponseEntity.ok(detail);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/bookable")
    public Map<String, Object> isBookable(@PathVariable String id, @RequestParam(defaultValue = "Chandigarh") String city) {
        try {
            UUID uuid = UUID.fromString(id);
            boolean hasShows = showRepo.hasActiveShowsInCity(uuid, city, LocalDate.now(), LocalTime.now());
            return Map.of("bookable", hasShows, "city", city);
        } catch (IllegalArgumentException e) {
            return Map.of("bookable", false, "city", city);
        }
    }

    @GetMapping("/{id}/credits")
    public Map<String, Object> credits(@PathVariable String id) {
        MovieDetailDto detail = tmdbService.getMovieDetail(id);
        if (detail == null) {
            return Map.of("cast", Collections.emptyList(), "crew", Collections.emptyList());
        }
        return Map.of("cast", detail.getCast(), "crew", detail.getCrew());
    }

    @GetMapping("/{id}/videos")
    public List<VideoDto> videos(@PathVariable String id) {
        return tmdbService.getVideos(id);
    }

    @GetMapping("/{id}/reviews")
    public List<ReviewDto> reviews(@PathVariable String id) {
        return tmdbService.getReviews(id);
    }

    @GetMapping("/{id}/similar")
    public List<MovieDto> similar(@PathVariable String id) {
        return tmdbService.getSimilar(id);
    }

    @GetMapping("/{id}/recommendations")
    public List<MovieDto> recommendations(@PathVariable String id) {
        return tmdbService.getRecommendations(id);
    }

    @GetMapping("/{id}/keywords")
    public List<String> keywords(@PathVariable String id) {
        return tmdbService.getKeywords(id);
    }

    @GetMapping
    public List<MovieDto> all(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "Chandigarh") String city) {
        if (search != null && !search.isBlank()) {
            return tmdbService.search(search, 1).getResults();
        }
        return nowShowing(city);
    }

    private MovieDto convertToDto(Movie m) {
        MovieDto dto = new MovieDto();
        dto.setId(m.getId().toString());
        dto.setTitle(m.getTitle());
        dto.setOverview(m.getDescription());
        dto.setPosterUrl(m.getPosterUrl());
        dto.setBackdropUrl(m.getBackdropUrl());
        dto.setDurationMinutes(m.getDurationMinutes());
        dto.setLanguage(m.getLanguage());
        dto.setRating(m.getRating());
        dto.setReleaseDate(m.getReleaseDate() != null ? m.getReleaseDate().toString() : "");
        dto.setTrailerUrl(m.getTrailerUrl());
        dto.setStatus(m.getStatus());
        if (m.getGenre() != null) {
            dto.setGenres(Arrays.stream(m.getGenre().split("•|,")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        }
        return dto;
    }

    private MovieDetailDto convertToDetailDto(Movie m) {
        MovieDto base = convertToDto(m);
        MovieDetailDto detail = new MovieDetailDto();
        detail.setId(base.getId());
        detail.setTitle(base.getTitle());
        detail.setOverview(base.getOverview());
        detail.setPosterUrl(base.getPosterUrl());
        detail.setBackdropUrl(base.getBackdropUrl());
        detail.setDurationMinutes(base.getDurationMinutes());
        detail.setLanguage(base.getLanguage());
        detail.setRating(base.getRating());
        detail.setReleaseDate(base.getReleaseDate());
        detail.setTrailerUrl(base.getTrailerUrl());
        detail.setStatus(base.getStatus());
        detail.setGenres(base.getGenres());

        // Attach trailers if present
        if (m.getTrailerUrl() != null && !m.getTrailerUrl().isBlank()) {
            String key = m.getTrailerUrl().contains("v=") ? m.getTrailerUrl().split("v=")[1] : "uYPbbksJxIg";
            VideoDto trailer = new VideoDto("v1", "Official Trailer", key, "YouTube", "Trailer", true, m.getTrailerUrl());
            detail.setVideos(List.of(trailer));
            detail.setPrimaryTrailer(trailer);
        }
        return detail;
    }
}
