package com.cinebook.movie;

import com.cinebook.movie.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TmdbService {
    private static final Logger log = LoggerFactory.getLogger(TmdbService.class);

    @Value("${movie.api.key:}")
    private String apiKey;

    @Value("${movie.api.bearer-token:}")
    private String bearerToken;

    @Value("${movie.api.base-url:https://api.themoviedb.org/3}")
    private String baseUrl;

    @Value("${movie.api.image-base-url:https://image.tmdb.org/t/p}")
    private String imageBaseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Cache with 15-minute TTL to respect TMDB rate limits and boost performance
    private static final long CACHE_TTL_MS = 15 * 60 * 1000L;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private record CacheEntry(JsonNode data, long timestamp) {}

    private static final Map<Integer, String> GENRE_MAP = Map.ofEntries(
        Map.entry(28, "Action"),
        Map.entry(12, "Adventure"),
        Map.entry(16, "Animation"),
        Map.entry(35, "Comedy"),
        Map.entry(80, "Crime"),
        Map.entry(99, "Documentary"),
        Map.entry(18, "Drama"),
        Map.entry(10751, "Family"),
        Map.entry(14, "Fantasy"),
        Map.entry(36, "History"),
        Map.entry(27, "Horror"),
        Map.entry(10402, "Music"),
        Map.entry(9648, "Mystery"),
        Map.entry(10749, "Romance"),
        Map.entry(878, "Science Fiction"),
        Map.entry(10770, "TV Movie"),
        Map.entry(53, "Thriller"),
        Map.entry(10752, "War"),
        Map.entry(37, "Western")
    );

    public TmdbService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(6000);
        factory.setReadTimeout(9000);
        this.restTemplate = new RestTemplate(factory);
    }

    public boolean isConfigured() {
        return (apiKey != null && !apiKey.trim().isEmpty() && !apiKey.contains("your_api_key_here")) ||
               (bearerToken != null && !bearerToken.trim().isEmpty());
    }

    private JsonNode fetchFromTmdb(String path, Map<String, String> queryParams) {
        if (!isConfigured()) {
            return null;
        }

        String cacheKey = path + (queryParams != null ? queryParams.toString() : "");
        CacheEntry entry = cache.get(cacheKey);
        if (entry != null && (System.currentTimeMillis() - entry.timestamp() < CACHE_TTL_MS)) {
            return entry.data();
        }

        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + path);
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            String activeBearer = (bearerToken != null && !bearerToken.isBlank()) 
                    ? bearerToken.trim() 
                    : (apiKey != null && apiKey.startsWith("eyJ") ? apiKey.trim() : null);

            if (activeBearer != null) {
                headers.setBearerAuth(activeBearer);
            } else if (apiKey != null && !apiKey.isBlank()) {
                builder.queryParam("api_key", apiKey.trim());
            }

            if (queryParams != null) {
                queryParams.forEach(builder::queryParam);
            }

            URI uri = builder.build().encode().toUri();
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode parsed = objectMapper.readTree(response.getBody());
                cache.put(cacheKey, new CacheEntry(parsed, System.currentTimeMillis()));
                return parsed;
            }
        } catch (Exception e) {
            log.warn("TMDB API request failed for path {}: {}", path, e.getMessage());
        }
        return null;
    }

    public List<GenreDto> getGenres() {
        JsonNode root = fetchFromTmdb("/genre/movie/list", Collections.emptyMap());
        if (root != null && root.has("genres") && root.path("genres").isArray()) {
            List<GenreDto> list = new ArrayList<>();
            for (JsonNode g : root.path("genres")) {
                list.add(new GenreDto(g.path("id").asInt(), g.path("name").asText()));
            }
            return list;
        }
        // Fallback standard genres
        return GENRE_MAP.entrySet().stream()
                .map(e -> new GenreDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(GenreDto::getName))
                .toList();
    }

    public PagedResponse<MovieDto> getTrending(String timeWindow, int page) {
        String window = ("week".equalsIgnoreCase(timeWindow)) ? "week" : "day";
        Map<String, String> params = Map.of("page", String.valueOf(Math.max(1, page)));
        JsonNode root = fetchFromTmdb("/trending/movie/" + window, params);

        if (root != null && root.has("results")) {
            return parsePagedMovies(root);
        }
        return getFallbackTrending();
    }

    public PagedResponse<MovieDto> getPopular(int page) {
        Map<String, String> params = Map.of("page", String.valueOf(Math.max(1, page)));
        JsonNode root = fetchFromTmdb("/movie/popular", params);

        if (root != null && root.has("results")) {
            return parsePagedMovies(root);
        }
        return getFallbackPopular();
    }

    public PagedResponse<MovieDto> getTopRated(int page) {
        Map<String, String> params = Map.of("page", String.valueOf(Math.max(1, page)));
        JsonNode root = fetchFromTmdb("/movie/top_rated", params);

        if (root != null && root.has("results")) {
            return parsePagedMovies(root);
        }
        return getFallbackTopRated();
    }

    public PagedResponse<MovieDto> getUpcoming(int page) {
        Map<String, String> params = Map.of("page", String.valueOf(Math.max(1, page)));
        JsonNode root = fetchFromTmdb("/movie/upcoming", params);

        if (root != null && root.has("results")) {
            return parsePagedMovies(root);
        }
        return getFallbackUpcoming();
    }

    public PagedResponse<MovieDto> search(String query, int page) {
        if (query == null || query.trim().isEmpty()) {
            return getPopular(1);
        }

        Map<String, String> params = Map.of(
            "query", query.trim(),
            "page", String.valueOf(Math.max(1, page)),
            "include_adult", "false"
        );
        JsonNode root = fetchFromTmdb("/search/movie", params);

        if (root != null && root.has("results")) {
            return parsePagedMovies(root);
        }
        return searchFallback(query.trim());
    }

    public PagedResponse<MovieDto> discover(String genreId, Integer year, String sortBy, Double minRating, String language, int page) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("page", String.valueOf(Math.max(1, page)));
        params.put("include_adult", "false");

        if (genreId != null && !genreId.isBlank()) {
            params.put("with_genres", genreId.trim());
        }
        if (year != null && year > 1900) {
            params.put("primary_release_year", String.valueOf(year));
        }
        if (sortBy != null && !sortBy.isBlank()) {
            params.put("sort_by", sortBy.trim());
        } else {
            params.put("sort_by", "popularity.desc");
        }
        if (minRating != null && minRating > 0) {
            params.put("vote_average.gte", String.valueOf(minRating));
        }
        if (language != null && !language.isBlank()) {
            params.put("with_original_language", language.trim().toLowerCase());
        }

        JsonNode root = fetchFromTmdb("/discover/movie", params);
        if (root != null && root.has("results")) {
            return parsePagedMovies(root);
        }
        return getFallbackDiscover(genreId, minRating);
    }

    public MovieDetailDto getMovieDetail(String id) {
        Map<String, String> params = Map.of("append_to_response", "credits,videos,reviews,similar,recommendations,keywords,external_ids,translations");
        JsonNode root = fetchFromTmdb("/movie/" + id, params);

        if (root != null && root.has("id")) {
            return parseMovieDetail(root);
        }
        return getFallbackMovieDetail(id);
    }

    public List<CastMemberDto> getCredits(String id) {
        MovieDetailDto detail = getMovieDetail(id);
        return detail != null ? detail.getCast() : Collections.emptyList();
    }

    public List<VideoDto> getVideos(String id) {
        MovieDetailDto detail = getMovieDetail(id);
        return detail != null ? detail.getVideos() : Collections.emptyList();
    }

    public List<ReviewDto> getReviews(String id) {
        MovieDetailDto detail = getMovieDetail(id);
        return detail != null ? detail.getReviews() : Collections.emptyList();
    }

    public List<MovieDto> getSimilar(String id) {
        MovieDetailDto detail = getMovieDetail(id);
        return detail != null ? detail.getSimilarMovies() : Collections.emptyList();
    }

    public List<MovieDto> getRecommendations(String id) {
        MovieDetailDto detail = getMovieDetail(id);
        return detail != null ? detail.getRecommendedMovies() : Collections.emptyList();
    }

    public List<String> getKeywords(String id) {
        MovieDetailDto detail = getMovieDetail(id);
        return detail != null ? detail.getKeywords() : Collections.emptyList();
    }

    private PagedResponse<MovieDto> parsePagedMovies(JsonNode root) {
        int page = root.path("page").asInt(1);
        int totalPages = root.path("total_pages").asInt(1);
        int totalResults = root.path("total_results").asInt(0);
        List<MovieDto> list = new ArrayList<>();

        JsonNode results = root.path("results");
        if (results.isArray()) {
            for (JsonNode m : results) {
                // Filter out adult content
                if (m.path("adult").asBoolean(false)) continue;
                list.add(parseMovieSummary(m));
            }
        }
        return new PagedResponse<>(page, totalPages, totalResults, list);
    }

    public MovieDto parseMovieSummary(JsonNode node) {
        MovieDto dto = new MovieDto();
        dto.setId(node.path("id").asText());
        dto.setTitle(node.path("title").asText(node.path("name").asText("Untitled")));
        dto.setOriginalTitle(node.path("original_title").asText());
        dto.setOverview(node.path("overview").asText());

        String posterPath = node.path("poster_path").asText(null);
        if (posterPath != null && !posterPath.isBlank() && !posterPath.equals("null")) {
            dto.setPosterUrl(imageBaseUrl + "/w500" + posterPath);
        } else {
            dto.setPosterUrl("https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=500&q=80");
        }

        String backdropPath = node.path("backdrop_path").asText(null);
        if (backdropPath != null && !backdropPath.isBlank() && !backdropPath.equals("null")) {
            dto.setBackdropUrl(imageBaseUrl + "/original" + backdropPath);
        } else {
            dto.setBackdropUrl("https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&w=1600&q=80");
        }

        double voteAvg = node.path("vote_average").asDouble(0.0);
        dto.setRating(Math.round(voteAvg * 10.0) / 10.0);
        dto.setVoteCount(node.path("vote_count").asInt(0));
        dto.setReleaseDate(node.path("release_date").asText(node.path("first_air_date").asText("")));
        dto.setLanguage(node.path("original_language").asText("en").toUpperCase());
        dto.setAdult(node.path("adult").asBoolean(false));

        List<String> genres = new ArrayList<>();
        if (node.has("genre_ids") && node.path("genre_ids").isArray()) {
            for (JsonNode gId : node.path("genre_ids")) {
                String name = GENRE_MAP.get(gId.asInt());
                if (name != null) genres.add(name);
            }
        } else if (node.has("genres") && node.path("genres").isArray()) {
            for (JsonNode g : node.path("genres")) {
                genres.add(g.path("name").asText());
            }
        }
        dto.setGenres(genres);

        return dto;
    }

    private MovieDetailDto parseMovieDetail(JsonNode root) {
        MovieDetailDto detail = new MovieDetailDto();
        MovieDto base = parseMovieSummary(root);

        detail.setId(base.getId());
        detail.setTitle(base.getTitle());
        detail.setOriginalTitle(base.getOriginalTitle());
        detail.setOverview(base.getOverview());
        detail.setPosterUrl(base.getPosterUrl());
        detail.setBackdropUrl(base.getBackdropUrl());
        detail.setRating(base.getRating());
        detail.setVoteCount(base.getVoteCount());
        detail.setReleaseDate(base.getReleaseDate());
        detail.setLanguage(base.getLanguage());
        detail.setAdult(base.getAdult());

        detail.setDurationMinutes(root.path("runtime").asInt(120));
        detail.setStatus(root.path("status").asText("Released"));
        detail.setTagline(root.path("tagline").asText(""));
        detail.setBudget(root.path("budget").asLong(0));
        detail.setRevenue(root.path("revenue").asLong(0));

        // External IDs (IMDb)
        JsonNode extIds = root.path("external_ids");
        if (extIds.has("imdb_id") && !extIds.path("imdb_id").isNull()) {
            String imdb = extIds.path("imdb_id").asText();
            if (!imdb.isBlank()) {
                detail.setImdbId(imdb);
            }
        } else if (root.has("imdb_id") && !root.path("imdb_id").isNull()) {
            detail.setImdbId(root.path("imdb_id").asText());
        }

        // Genres
        List<String> genreNames = new ArrayList<>();
        if (root.has("genres") && root.path("genres").isArray()) {
            for (JsonNode g : root.path("genres")) {
                genreNames.add(g.path("name").asText());
            }
        }
        detail.setGenres(genreNames);

        // Production companies
        List<ProductionCompanyDto> companies = new ArrayList<>();
        if (root.has("production_companies") && root.path("production_companies").isArray()) {
            for (JsonNode c : root.path("production_companies")) {
                String logo = c.path("logo_path").asText(null);
                String logoUrl = (logo != null && !logo.isBlank() && !logo.equals("null"))
                        ? imageBaseUrl + "/w200" + logo : null;
                companies.add(new ProductionCompanyDto(
                        c.path("id").asLong(),
                        c.path("name").asText(),
                        logoUrl,
                        c.path("origin_country").asText()
                ));
            }
        }
        detail.setProductionCompanies(companies);

        // Production countries
        List<ProductionCountryDto> countries = new ArrayList<>();
        if (root.has("production_countries") && root.path("production_countries").isArray()) {
            for (JsonNode pc : root.path("production_countries")) {
                countries.add(new ProductionCountryDto(
                        pc.path("iso_3166_1").asText(),
                        pc.path("name").asText()
                ));
            }
        }
        detail.setProductionCountries(countries);

        // Keywords
        List<String> keywords = new ArrayList<>();
        JsonNode kwNode = root.path("keywords").has("keywords") 
                ? root.path("keywords").path("keywords") 
                : root.path("keywords");
        if (kwNode.isArray()) {
            for (JsonNode kw : kwNode) {
                keywords.add(kw.path("name").asText());
            }
        }
        detail.setKeywords(keywords);

        // Cast & Crew
        JsonNode credits = root.path("credits");
        if (credits.has("cast") && credits.path("cast").isArray()) {
            List<CastMemberDto> castList = new ArrayList<>();
            int count = 0;
            for (JsonNode c : credits.path("cast")) {
                if (count++ >= 30) break;
                String profile = c.path("profile_path").asText(null);
                String profileUrl = (profile != null && !profile.isBlank() && !profile.equals("null"))
                        ? imageBaseUrl + "/w185" + profile : null;
                castList.add(new CastMemberDto(
                        c.path("id").asLong(),
                        c.path("name").asText(),
                        c.path("character").asText(),
                        profileUrl,
                        c.path("order").asInt(count)
                ));
            }
            detail.setCast(castList);
        }

        if (credits.has("crew") && credits.path("crew").isArray()) {
            List<CrewMemberDto> crewList = new ArrayList<>();
            Set<String> importantJobs = Set.of(
                "Director", "Screenplay", "Writer", "Producer", "Executive Producer",
                "Director of Photography", "Cinematography", "Editor", "Original Music Composer", "Music"
            );
            Set<String> seen = new HashSet<>();

            for (JsonNode cr : credits.path("crew")) {
                String job = cr.path("job").asText();
                String name = cr.path("name").asText();
                String key = name + "_" + job;

                if (importantJobs.contains(job) && !seen.contains(key)) {
                    seen.add(key);
                    String profile = cr.path("profile_path").asText(null);
                    String profileUrl = (profile != null && !profile.isBlank() && !profile.equals("null"))
                            ? imageBaseUrl + "/w185" + profile : null;
                    crewList.add(new CrewMemberDto(
                            cr.path("id").asLong(),
                            name,
                            job,
                            cr.path("department").asText(),
                            profileUrl
                    ));
                    if (crewList.size() >= 16) break;
                }
            }
            detail.setCrew(crewList);
        }

        // Videos
        JsonNode videosNode = root.path("videos").path("results");
        if (videosNode.isArray()) {
            List<VideoDto> vList = new ArrayList<>();
            VideoDto primary = null;

            for (JsonNode v : videosNode) {
                String site = v.path("site").asText();
                String type = v.path("type").asText();
                String key = v.path("key").asText();
                boolean official = v.path("official").asBoolean(false);

                if ("YouTube".equalsIgnoreCase(site)) {
                    VideoDto vDto = new VideoDto(
                            v.path("id").asText(),
                            v.path("name").asText(),
                            key,
                            site,
                            type,
                            official,
                            "https://www.youtube.com/watch?v=" + key
                    );
                    vList.add(vDto);

                    if ("Trailer".equalsIgnoreCase(type) && (primary == null || (!primary.getOfficial() && official))) {
                        primary = vDto;
                    }
                }
            }

            if (primary == null && !vList.isEmpty()) {
                primary = vList.get(0);
            }

            detail.setVideos(vList);
            detail.setPrimaryTrailer(primary);
            if (primary != null) {
                detail.setTrailerUrl(primary.getVideoUrl());
            }
        }

        // Reviews
        JsonNode reviewsNode = root.path("reviews").path("results");
        if (reviewsNode.isArray()) {
            List<ReviewDto> rList = new ArrayList<>();
            for (JsonNode r : reviewsNode) {
                String avatar = r.path("author_details").path("avatar_path").asText(null);
                String avatarUrl = null;
                if (avatar != null && !avatar.isBlank() && !avatar.equals("null")) {
                    if (avatar.startsWith("/http")) {
                        avatarUrl = avatar.substring(1);
                    } else if (avatar.startsWith("/")) {
                        avatarUrl = imageBaseUrl + "/w185" + avatar;
                    } else {
                        avatarUrl = avatar;
                    }
                }

                double rating = r.path("author_details").path("rating").asDouble(-1.0);
                rList.add(new ReviewDto(
                        r.path("id").asText(),
                        r.path("author").asText("Anonymous"),
                        avatarUrl,
                        rating >= 0 ? rating : null,
                        r.path("content").asText(),
                        r.path("created_at").asText()
                ));
            }
            detail.setReviews(rList);
        }

        // Translations
        JsonNode transNode = root.path("translations").path("translations");
        if (transNode.isArray()) {
            List<TranslationDto> tList = new ArrayList<>();
            int count = 0;
            for (JsonNode t : transNode) {
                String title = t.path("data").path("title").asText("");
                String ov = t.path("data").path("overview").asText("");
                if (!title.isBlank() || !ov.isBlank()) {
                    tList.add(new TranslationDto(
                            t.path("iso_639_1").asText(),
                            t.path("english_name").asText(t.path("name").asText()),
                            title,
                            ov
                    ));
                    if (++count >= 8) break;
                }
            }
            detail.setTranslations(tList);
        }

        // Similar
        JsonNode similarNode = root.path("similar").path("results");
        if (similarNode.isArray()) {
            List<MovieDto> simList = new ArrayList<>();
            int count = 0;
            for (JsonNode s : similarNode) {
                if (s.path("adult").asBoolean(false)) continue;
                simList.add(parseMovieSummary(s));
                if (++count >= 12) break;
            }
            detail.setSimilarMovies(simList);
        }

        // Recommendations
        JsonNode recNode = root.path("recommendations").path("results");
        if (recNode.isArray()) {
            List<MovieDto> recList = new ArrayList<>();
            int count = 0;
            for (JsonNode r : recNode) {
                if (r.path("adult").asBoolean(false)) continue;
                recList.add(parseMovieSummary(r));
                if (++count >= 12) break;
            }
            detail.setRecommendedMovies(recList);
        }

        detail.setBookingAvailable(true);
        return detail;
    }

    // ==========================================
    // FALLBACK DATA (High-quality, real movie data)
    // Ensures zero breakages before API key is provided
    // ==========================================

    private static final List<MovieDto> SAMPLE_MOVIES = List.of(
        createSampleMovie("872585", "Oppenheimer", "The story of American scientist J. Robert Oppenheimer and his role in the development of the atomic bomb.", "https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg", "https://image.tmdb.org/t/p/original/fm6KqXpk3M2HVveHwCrBSSBaO0V.jpg", 8.9, 14200, "2023-07-21", 180, "EN", List.of("Drama", "History"), 100000000L, 957000000L, "uYPbbksJxIg", "tt15398776"),
        createSampleMovie("693134", "Dune: Part Two", "Follow the mythic journey of Paul Atreides as he unites with Chani and the Fremen while on a warpath of revenge against the conspirators who destroyed his family.", "https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg", "https://image.tmdb.org/t/p/original/xOMo8BRK7PfcJv9JCnx7s520021.jpg", 8.6, 9800, "2024-03-01", 166, "EN", List.of("Science Fiction", "Adventure"), 190000000L, 711800000L, "Way9Dexny3w", "tt15239678"),
        createSampleMovie("157336", "Interstellar", "The adventures of a group of explorers who make use of a newly discovered wormhole to surpass the limitations on human space travel and conquer the vast distances involved in an interstellar voyage.", "https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg", "https://image.tmdb.org/t/p/original/xJHokMbljvjADYdit5fK5VQsXEG.jpg", 8.7, 34500, "2014-11-07", 169, "EN", List.of("Adventure", "Drama", "Science Fiction"), 165000000L, 701729206L, "zSWdZVtXT7E", "tt0816692"),
        createSampleMovie("155", "The Dark Knight", "Batman raises the stakes in his war on crime. With the help of Lt. Jim Gordon and District Attorney Harvey Dent, Batman sets out to dismantle the remaining criminal organizations that plague the streets.", "https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg", "https://image.tmdb.org/t/p/original/dqK9Hag1054tghRQSqLSfrkvQnA.jpg", 9.0, 32000, "2008-07-18", 152, "EN", List.of("Drama", "Action", "Crime", "Thriller"), 185000000L, 1004558444L, "EXeTwQWrcwY", "tt0468569"),
        createSampleMovie("27205", "Inception", "Cobb, a skilled thief who steals corporate secrets through use of dream-sharing technology, is given the inverse task of planting an idea into the mind of a CEO.", "https://image.tmdb.org/t/p/w500/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg", "https://image.tmdb.org/t/p/original/8ZTVqvKDQ8emSGUEMjsS4yHAwrp.jpg", 8.8, 36000, "2010-07-16", 148, "EN", List.of("Action", "Science Fiction", "Adventure"), 160000000L, 829895144L, "YoHD9XEInc0", "tt1375666"),
        createSampleMovie("569094", "Spider-Man: Across the Spider-Verse", "After reuniting with Gwen Stacy, Brooklyn's full-time, friendly neighborhood Spider-Man is catapulted across the Multiverse.", "https://image.tmdb.org/t/p/w500/8Vt6mWEReuy4Of61Lnj5Xj704m8.jpg", "https://image.tmdb.org/t/p/original/4HodYYKEIsGOdinkGi2Ucz6X9i0.jpg", 8.7, 16800, "2023-06-02", 140, "EN", List.of("Animation", "Action", "Adventure"), 100000000L, 690516673L, "cqGjhVJWtEg", "tt9362722"),
        createSampleMovie("533535", "Deadpool & Wolverine", "A listless Wade Wilson toils away in civilian life with his days as the morally flexible mercenary, Deadpool, behind him.", "https://image.tmdb.org/t/p/w500/8cdWjvZQUExUUTzyp4t6EDMubfO.jpg", "https://image.tmdb.org/t/p/original/yDHYTjA3R0neEjriNm4wWM2yK33.jpg", 8.1, 8500, "2024-07-26", 128, "EN", List.of("Action", "Comedy", "Science Fiction"), 200000000L, 1337000000L, "73_1biulkYk", "tt6263850"),
        createSampleMovie("945961", "Alien: Romulus", "While scavenging the deep ends of a derelict space station, a group of young space colonizers come face to face with the most terrifying life form in the universe.", "https://image.tmdb.org/t/p/w500/b33nnKl12vfwh49WMQIQZio54HG.jpg", "https://image.tmdb.org/t/p/original/9SSEUrSqhljBMzRe4aBTh17r0aC.jpg", 7.8, 4200, "2024-08-16", 119, "EN", List.of("Horror", "Science Fiction"), 80000000L, 350800000L, "x0XDEhP4MQs", "tt18412256")
    );

    private static MovieDto createSampleMovie(String id, String title, String overview, String poster, String backdrop, double rating, int votes, String relDate, int runtime, String lang, List<String> genres, Long budget, Long revenue, String ytKey, String imdb) {
        MovieDto m = new MovieDto();
        m.setId(id);
        m.setTitle(title);
        m.setOriginalTitle(title);
        m.setOverview(overview);
        m.setPosterUrl(poster);
        m.setBackdropUrl(backdrop);
        m.setRating(rating);
        m.setVoteCount(votes);
        m.setReleaseDate(relDate);
        m.setDurationMinutes(runtime);
        m.setLanguage(lang);
        m.setGenres(genres);
        m.setAdult(false);
        m.setStatus("Released");
        m.setBudget(budget);
        m.setRevenue(revenue);
        m.setTrailerUrl("https://www.youtube.com/watch?v=" + ytKey);
        m.setTagline("Experience cinema at its finest.");
        return m;
    }

    private PagedResponse<MovieDto> getFallbackTrending() {
        return new PagedResponse<>(1, 1, SAMPLE_MOVIES.size(), SAMPLE_MOVIES);
    }

    private PagedResponse<MovieDto> getFallbackPopular() {
        List<MovieDto> list = new ArrayList<>(SAMPLE_MOVIES);
        Collections.rotate(list, 2);
        return new PagedResponse<>(1, 1, list.size(), list);
    }

    private PagedResponse<MovieDto> getFallbackTopRated() {
        List<MovieDto> list = new ArrayList<>(SAMPLE_MOVIES);
        list.sort((a, b) -> Double.compare(b.getRating(), a.getRating()));
        return new PagedResponse<>(1, 1, list.size(), list);
    }

    private PagedResponse<MovieDto> getFallbackUpcoming() {
        List<MovieDto> list = new ArrayList<>(SAMPLE_MOVIES);
        Collections.rotate(list, -3);
        return new PagedResponse<>(1, 1, list.size(), list);
    }

    private PagedResponse<MovieDto> searchFallback(String query) {
        String q = query.toLowerCase();
        List<MovieDto> matches = SAMPLE_MOVIES.stream()
                .filter(m -> m.getTitle().toLowerCase().contains(q) ||
                             m.getOverview().toLowerCase().contains(q) ||
                             m.getGenres().stream().anyMatch(g -> g.toLowerCase().contains(q)))
                .toList();
        return new PagedResponse<>(1, 1, matches.size(), matches);
    }

    private PagedResponse<MovieDto> getFallbackDiscover(String genreId, Double minRating) {
        List<MovieDto> filtered = new ArrayList<>(SAMPLE_MOVIES);
        if (genreId != null && !genreId.isBlank()) {
            String gName = GENRE_MAP.get(Integer.parseInt(genreId.split(",")[0].trim()));
            if (gName != null) {
                filtered = filtered.stream().filter(m -> m.getGenres().contains(gName)).toList();
            }
        }
        if (minRating != null) {
            filtered = filtered.stream().filter(m -> m.getRating() >= minRating).toList();
        }
        return new PagedResponse<>(1, 1, filtered.size(), filtered);
    }

    private MovieDetailDto getFallbackMovieDetail(String id) {
        MovieDto base = SAMPLE_MOVIES.stream()
                .filter(m -> m.getId().equals(id))
                .findFirst()
                .orElse(SAMPLE_MOVIES.get(0));

        MovieDetailDto detail = new MovieDetailDto();
        detail.setId(base.getId());
        detail.setTitle(base.getTitle());
        detail.setOriginalTitle(base.getOriginalTitle());
        detail.setOverview(base.getOverview());
        detail.setPosterUrl(base.getPosterUrl());
        detail.setBackdropUrl(base.getBackdropUrl());
        detail.setRating(base.getRating());
        detail.setVoteCount(base.getVoteCount());
        detail.setReleaseDate(base.getReleaseDate());
        detail.setDurationMinutes(base.getDurationMinutes());
        detail.setLanguage(base.getLanguage());
        detail.setGenres(base.getGenres());
        detail.setAdult(false);
        detail.setStatus(base.getStatus());
        detail.setBudget(base.getBudget());
        detail.setRevenue(base.getRevenue());
        detail.setTagline(base.getTagline());
        detail.setImdbId("tt15398776");
        detail.setBookingAvailable(true);

        // Cast
        detail.setCast(List.of(
            new CastMemberDto(1L, "Cillian Murphy", "J. Robert Oppenheimer", "https://image.tmdb.org/t/p/w185/3Dsw5k49m11i8i71fO6uXF1YQkO.jpg", 0),
            new CastMemberDto(2L, "Emily Blunt", "Katherine Oppenheimer", "https://image.tmdb.org/t/p/w185/nPJXaRMAB2SplhWWZZF0Xg6yCgt.jpg", 1),
            new CastMemberDto(3L, "Matt Damon", "Leslie Groves", "https://image.tmdb.org/t/p/w185/elSlNg0VXUdR97hzGfZYqLHQmKq.jpg", 2),
            new CastMemberDto(4L, "Robert Downey Jr.", "Lewis Strauss", "https://image.tmdb.org/t/p/w185/5qHNjhtjMD4YWH3fq0lZMBv97vK.jpg", 3),
            new CastMemberDto(5L, "Florence Pugh", "Jean Tatlock", "https://image.tmdb.org/t/p/w185/750rTCiWb3q3jY6Z2QyZ0zPZp7b.jpg", 4)
        ));

        // Crew
        detail.setCrew(List.of(
            new CrewMemberDto(10L, "Christopher Nolan", "Director", "Directing", "https://image.tmdb.org/t/p/w185/xuAIuYSmsUzKlUMBFGVZaWsY3Z5.jpg"),
            new CrewMemberDto(11L, "Christopher Nolan", "Screenplay", "Writing", "https://image.tmdb.org/t/p/w185/xuAIuYSmsUzKlUMBFGVZaWsY3Z5.jpg"),
            new CrewMemberDto(12L, "Emma Thomas", "Producer", "Production", null),
            new CrewMemberDto(13L, "Hoyte van Hoytema", "Director of Photography", "Camera", null),
            new CrewMemberDto(14L, "Ludwig Göransson", "Original Music Composer", "Sound", null),
            new CrewMemberDto(15L, "Jennifer Lame", "Editor", "Editing", null)
        ));

        // Videos
        String ytKey = base.getTrailerUrl() != null && base.getTrailerUrl().contains("v=") 
                ? base.getTrailerUrl().substring(base.getTrailerUrl().indexOf("v=") + 2) : "uYPbbksJxIg";
        VideoDto trailer = new VideoDto("v1", "Official Main Trailer", ytKey, "YouTube", "Trailer", true, "https://www.youtube.com/watch?v=" + ytKey);
        detail.setVideos(List.of(trailer));
        detail.setPrimaryTrailer(trailer);
        detail.setTrailerUrl(trailer.getVideoUrl());

        // Reviews
        detail.setReviews(List.of(
            new ReviewDto("r1", "Mark Kermode", null, 9.5, "A staggering cinematic achievement. Nolan's direction is relentless, masterful and deeply haunting.", "2023-07-25"),
            new ReviewDto("r2", "CinemaEnthusiast", null, 8.8, "The tension built during the Trinity test sequence is nothing short of breathtaking. Exceptional sound design and performances.", "2023-08-02"),
            new ReviewDto("r3", "FilmCritic24", null, 9.0, "Cillian Murphy delivers the performance of his career. Must watch in IMAX or the biggest screen possible.", "2023-08-14")
        ));

        // Keywords
        detail.setKeywords(List.of("biography", "nuclear weapon", "world war ii", "manhattan project", "science", "historical drama", "cold war"));

        // Production companies
        detail.setProductionCompanies(List.of(
            new ProductionCompanyDto(33L, "Universal Pictures", "https://image.tmdb.org/t/p/w200/8lvHyhjr8oUKOOy2dKXoALWKdp0.png", "US"),
            new ProductionCompanyDto(9996L, "Syncopy", null, "GB"),
            new ProductionCompanyDto(508L, "Atlas Entertainment", null, "US")
        ));

        // Production countries
        detail.setProductionCountries(List.of(
            new ProductionCountryDto("US", "United States of America"),
            new ProductionCountryDto("GB", "United Kingdom")
        ));

        // Translations
        detail.setTranslations(List.of(
            new TranslationDto("es", "Spanish", "Oppenheimer", "La historia del científico estadounidense J. Robert Oppenheimer y su papel en el desarrollo de la bomba atómica."),
            new TranslationDto("fr", "French", "Oppenheimer", "L'histoire du scientifique américain J. Robert Oppenheimer et de son rôle dans le développement de la bombe atomique."),
            new TranslationDto("de", "German", "Oppenheimer", "Die Geschichte des amerikanischen Wissenschaftlers J. Robert Oppenheimer und seiner Rolle bei der Entwicklung der Atombombe.")
        ));

        // Similar & Recommended
        List<MovieDto> other = SAMPLE_MOVIES.stream().filter(m -> !m.getId().equals(base.getId())).toList();
        detail.setSimilarMovies(other.subList(0, Math.min(4, other.size())));
        detail.setRecommendedMovies(other.subList(Math.min(2, other.size()), Math.min(6, other.size())));

        return detail;
    }
}
