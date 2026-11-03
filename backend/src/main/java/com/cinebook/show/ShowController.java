package com.cinebook.show;

import com.cinebook.theater.Theater;
import com.cinebook.theater.TheaterRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/shows")
public class ShowController {

    private final ShowRepository showRepo;
    private final TheaterRepository theaterRepo;

    public ShowController(ShowRepository showRepo, TheaterRepository theaterRepo) {
        this.showRepo = showRepo;
        this.theaterRepo = theaterRepo;
    }

    @GetMapping("/theaters")
    public List<Theater> getTheaters(@RequestParam(required = false) String city) {
        if (city != null && !city.isBlank()) {
            return theaterRepo.findByCityIgnoreCase(city);
        }
        return theaterRepo.findAll();
    }

    @GetMapping("/movie/{movieId}")
    public Map<String, Object> getMovieShows(
            @PathVariable String movieId,
            @RequestParam(defaultValue = "Chandigarh") String city) {
        try {
            UUID uuid = UUID.fromString(movieId);
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();

            List<Show> activeShows = showRepo.findActiveShowsByMovieAndCity(uuid, city, today, now);

            if (activeShows.isEmpty()) {
                return Map.of(
                    "movieId", movieId,
                    "city", city,
                    "bookable", false,
                    "theaters", Collections.emptyList()
                );
            }

            // Group shows by Theater
            Map<UUID, Map<String, Object>> theaterMap = new LinkedHashMap<>();
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

            for (Show s : activeShows) {
                Theater t = s.getScreen().getTheater();
                UUID tId = t.getId();

                Map<String, Object> tData = theaterMap.computeIfAbsent(tId, k -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", t.getId().toString());
                    map.put("name", t.getName());
                    map.put("city", t.getCity());
                    map.put("address", t.getAddress());
                    map.put("startingPrice", s.getBasePrice());
                    map.put("times", new ArrayList<Map<String, Object>>());
                    return map;
                });

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> timesList = (List<Map<String, Object>>) tData.get("times");
                timesList.add(Map.of(
                    "showId", s.getId().toString(),
                    "date", s.getShowDate().toString(),
                    "time", s.getStartTime().format(timeFormatter),
                    "format", s.getFormat(),
                    "price", s.getBasePrice()
                ));
            }

            return Map.of(
                "movieId", movieId,
                "city", city,
                "bookable", true,
                "theaters", new ArrayList<>(theaterMap.values())
            );

        } catch (IllegalArgumentException e) {
            // Non-UUID (external API movie with no local database shows)
            return Map.of(
                "movieId", movieId,
                "city", city,
                "bookable", false,
                "theaters", Collections.emptyList()
            );
        }
    }

    @GetMapping("/{showId}/seats")
    public List<Map<String, Object>> seats(@PathVariable String showId) {
        List<Map<String, Object>> result = new ArrayList<>();
        String[] rows = {"A", "B", "C", "D", "E", "F"};
        for (String row : rows) {
            for (int n = 1; n <= 10; n++) {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("id", row + n);
                s.put("label", row + n);
                s.put("type", row.equals("F") ? "RECLINER" : row.equals("E") ? "PLATINUM" : "GOLD");
                s.put("price", row.equals("F") ? 500 : row.equals("E") ? 350 : 250);
                s.put("status", ((n == 4 && row.equals("C")) || (n == 7 && row.equals("D")) || (n == 3 && row.equals("E"))) ? "BOOKED" : "AVAILABLE");
                result.add(s);
            }
        }
        return result;
    }
}
