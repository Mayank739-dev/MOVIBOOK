package com.cinebook.config;

import com.cinebook.booking.Booking;
import com.cinebook.booking.BookingRepository;
import com.cinebook.movie.Movie;
import com.cinebook.movie.MovieRepository;
import com.cinebook.show.Show;
import com.cinebook.show.ShowRepository;
import com.cinebook.theater.Screen;
import com.cinebook.theater.ScreenRepository;
import com.cinebook.theater.Theater;
import com.cinebook.theater.TheaterRepository;
import com.cinebook.user.User;
import com.cinebook.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final MovieRepository movieRepo;
    private final TheaterRepository theaterRepo;
    private final ScreenRepository screenRepo;
    private final ShowRepository showRepo;
    private final UserRepository userRepo;
    private final BookingRepository bookingRepo;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(MovieRepository movieRepo, TheaterRepository theaterRepo, ScreenRepository screenRepo,
                           ShowRepository showRepo, UserRepository userRepo, BookingRepository bookingRepo,
                           PasswordEncoder passwordEncoder) {
        this.movieRepo = movieRepo;
        this.theaterRepo = theaterRepo;
        this.screenRepo = screenRepo;
        this.showRepo = showRepo;
        this.userRepo = userRepo;
        this.bookingRepo = bookingRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        try {
            log.info("Initializing database records for cities, theaters, movies, and active shows...");

            // 1. Seed Theaters
            Theater inoxChd = getOrCreateTheater("INOX • Elante Mall", "Chandigarh", "Industrial Area Phase I, Chandigarh");
            Theater pvrChd = getOrCreateTheater("PVR Cinemas • Nexus Mall", "Chandigarh", "Nexus Mall, Chandigarh");
            Theater pvrDel = getOrCreateTheater("PVR Director's Cut • Vasant Kunj", "Delhi", "Ambience Mall, Vasant Kunj, New Delhi");
            Theater cinepolisDel = getOrCreateTheater("Cinepolis • DLF Avenue", "Delhi", "Saket District Centre, New Delhi");
            Theater pvrMum = getOrCreateTheater("PVR ICON • Phoenix Palladium", "Mumbai", "Lower Parel, Mumbai");

            // 2. Seed Screens
            Screen s1InoxChd = getOrCreateScreen(inoxChd, "Audi 1 (Dolby Atmos)");
            Screen s2InoxChd = getOrCreateScreen(inoxChd, "Audi 2 (IMAX 3D)");
            Screen s1PvrDel = getOrCreateScreen(pvrDel, "Screen 1 (Laser 4K)");
            Screen s1PvrMum = getOrCreateScreen(pvrMum, "IMAX Grand Screen");

            // 3. Seed Movies
            LocalDate today = LocalDate.now();

            // Active Movies (Now Showing)
            Movie midnight = getOrCreateMovie(
                "Midnight Protocol",
                "A cyber-thriller about an elite team racing against a city-wide blackout.",
                "https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&w=600&h=900&q=80",
                "https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&w=1600&q=80",
                128, "English", "Action • Thriller", 8.7, today.minusDays(1),
                "https://www.youtube.com/watch?v=uYPbbksJxIg", "NOW_SHOWING"
            );

            Movie neon = getOrCreateMovie(
                "Neon Dreams",
                "A romantic sci-fi journey through a future city where memories can be replayed.",
                "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&h=900&q=80",
                "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?auto=format&fit=crop&w=1600&q=80",
                116, "Hindi", "Sci-Fi • Romance", 8.2, today.minusDays(1),
                "https://www.youtube.com/watch?v=YoHD9XEInc0", "NOW_SHOWING"
            );

            // Special Screening Exception (Requirement 7): Older movie screened again in 2026
            Movie inception = getOrCreateMovie(
                "Inception (Special 2026 IMAX Screening)",
                "Christopher Nolan's mind-bending masterpiece returns to IMAX screens for a strictly limited exclusive run.",
                "https://image.tmdb.org/t/p/w500/9gk7adHYeDvHkCSEqAvQNLV5Uge.jpg",
                "https://image.tmdb.org/t/p/original/8ZTVqvKDQ8emSGUEMjsS4yHAwrp.jpg",
                148, "English", "Action • Sci-Fi", 8.8, LocalDate.of(2010, 7, 16),
                "https://www.youtube.com/watch?v=YoHD9XEInc0", "NOW_SHOWING"
            );
            // Ensure poster URL is updated if existing record had horizontal backdrop
            if (inception.getPosterUrl() != null && inception.getPosterUrl().contains("edv5CZvWj09upOsy2Y6IwDhK8bt")) {
                inception.setPosterUrl("https://image.tmdb.org/t/p/w500/9gk7adHYeDvHkCSEqAvQNLV5Uge.jpg");
                movieRepo.save(inception);
            }

            Movie dune2 = getOrCreateMovie(
                "Dune: Part Two",
                "Paul Atreides unites with Chani and the Fremen on a warpath of revenge against the conspirators who destroyed his family.",
                "https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg",
                "https://image.tmdb.org/t/p/original/xOMo8BRK7PfcJv9JCnx7s520021.jpg",
                166, "English", "Sci-Fi • Adventure", 8.6, LocalDate.of(2024, 3, 1),
                "https://www.youtube.com/watch?v=Way9Dexny3w", "NOW_SHOWING"
            );

            Movie oppenheimer = getOrCreateMovie(
                "Oppenheimer (70mm Special Screening)",
                "The story of J. Robert Oppenheimer and his role in the development of the atomic bomb.",
                "https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg",
                "https://image.tmdb.org/t/p/original/fm6KqXpk3M2HVveHwCrBSSBaO0V.jpg",
                180, "English", "Drama • History", 8.9, LocalDate.of(2023, 7, 21),
                "https://www.youtube.com/watch?v=uYPbbksJxIg", "NOW_SHOWING"
            );

            // Coming Soon Movies (strictly future release dates: release_date > today)
            getOrCreateMovie(
                "The Last Kingdom: Dawn of Valhalla",
                "A warrior returns home to defend his people from an invading army in an epic struggle for freedom.",
                "https://images.unsplash.com/photo-1579532537598-459ecdaf39cc?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1579532537598-459ecdaf39cc?auto=format&fit=crop&w=1600&q=80",
                142, "English", "Drama • Action", 9.0, today.plusDays(25), // Nov 28, 2026
                "https://www.youtube.com/watch?v=EXeTwQWrcwY", "COMING_SOON"
            );

            getOrCreateMovie(
                "Avatar: Fire and Ash",
                "James Cameron takes the journey on Pandora to the terrifying volcanic regions of the Ash People.",
                "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=1600&q=80",
                190, "English", "Sci-Fi • Adventure", 8.9, today.plusDays(46), // Dec 19, 2026
                "https://www.youtube.com/watch?v=d9MyW72ELq0", "COMING_SOON"
            );

            getOrCreateMovie(
                "Blade Runner 2099",
                "A neo-noir cyberpunk continuation of the Blade Runner saga across futuristic mega-cities.",
                "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?auto=format&fit=crop&w=1600&q=80",
                135, "English", "Sci-Fi • Mystery", 8.4, today.plusDays(52), // Dec 25, 2026
                "https://www.youtube.com/watch?v=gCcx85zbxz4", "COMING_SOON"
            );

            getOrCreateMovie(
                "Avengers: Secret Wars",
                "The multiverse collapses into Battleworld where heroic forces make a final stand.",
                "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=1600&q=80",
                175, "English", "Action • Adventure", 9.2, today.plusDays(185), // May 2027
                "https://www.youtube.com/watch?v=cqGjhVJWtEg", "COMING_SOON"
            );

            // 4. Seed Shows by City if not already created
            if (showRepo.count() == 0) {
                // CHANDIGARH shows: Midnight Protocol, Neon Dreams, Inception
                createShow(midnight, s1InoxChd, today, LocalTime.of(22, 30), "2D Dolby Atmos", BigDecimal.valueOf(250));
                createShow(midnight, s1InoxChd, today.plusDays(1), LocalTime.of(10, 0), "2D Dolby Atmos", BigDecimal.valueOf(250));
                createShow(midnight, s1InoxChd, today.plusDays(1), LocalTime.of(16, 30), "2D Dolby Atmos", BigDecimal.valueOf(250));
                createShow(midnight, s1InoxChd, today.plusDays(1), LocalTime.of(20, 0), "2D Dolby Atmos", BigDecimal.valueOf(300));

                createShow(neon, s1InoxChd, today, LocalTime.of(23, 0), "2D", BigDecimal.valueOf(220));
                createShow(neon, s1InoxChd, today.plusDays(1), LocalTime.of(13, 15), "2D", BigDecimal.valueOf(220));
                createShow(neon, s1InoxChd, today.plusDays(1), LocalTime.of(18, 0), "2D", BigDecimal.valueOf(250));

                createShow(inception, s2InoxChd, today.plusDays(1), LocalTime.of(14, 0), "IMAX", BigDecimal.valueOf(450));
                createShow(inception, s2InoxChd, today.plusDays(1), LocalTime.of(19, 30), "IMAX", BigDecimal.valueOf(500));

                // DELHI shows: Midnight Protocol, Dune: Part Two (Neon Dreams has NO shows in Delhi)
                createShow(midnight, s1PvrDel, today.plusDays(1), LocalTime.of(11, 0), "Laser 4K", BigDecimal.valueOf(350));
                createShow(midnight, s1PvrDel, today.plusDays(1), LocalTime.of(17, 30), "Laser 4K", BigDecimal.valueOf(380));

                createShow(dune2, s1PvrDel, today.plusDays(1), LocalTime.of(14, 0), "Laser 4K", BigDecimal.valueOf(400));
                createShow(dune2, s1PvrDel, today.plusDays(1), LocalTime.of(20, 45), "Laser 4K", BigDecimal.valueOf(450));

                // MUMBAI shows: Oppenheimer, Neon Dreams (Midnight Protocol has NO shows in Mumbai)
                createShow(oppenheimer, s1PvrMum, today.plusDays(1), LocalTime.of(15, 0), "IMAX 70mm", BigDecimal.valueOf(600));
                createShow(oppenheimer, s1PvrMum, today.plusDays(1), LocalTime.of(20, 0), "IMAX 70mm", BigDecimal.valueOf(700));

                createShow(neon, s1PvrMum, today.plusDays(1), LocalTime.of(18, 30), "2D", BigDecimal.valueOf(320));
            }

            // 5. Seed Demo Users for Login System
            User alex = getOrCreateUser("Alex Johnson", "alex@cinebook.com", "password123");
            User sarah = getOrCreateUser("Sarah Miller", "sarah@cinebook.com", "password123");
            User rahul = getOrCreateUser("Rahul Sharma", "rahul@cinebook.com", "password123");

            // 6. Seed Distinct Bookings per User if empty
            if (bookingRepo.count() == 0) {
                List<Show> allShows = showRepo.findAll();
                Show showInception = allShows.stream().filter(s -> s.getMovie().getTitle().contains("Inception")).findFirst().orElse(allShows.isEmpty() ? null : allShows.get(0));
                Show showNeon = allShows.stream().filter(s -> s.getMovie().getTitle().contains("Neon")).findFirst().orElse(showInception);
                Show showDune = allShows.stream().filter(s -> s.getMovie().getTitle().contains("Dune")).findFirst().orElse(showInception);
                Show showOpp = allShows.stream().filter(s -> s.getMovie().getTitle().contains("Oppenheimer")).findFirst().orElse(showInception);

                if (showInception != null) {
                    // Alex's Bookings
                    Booking bAlex1 = new Booking(showInception, alex, "CBK-892104", alex.getEmail(), alex.getName(),
                            "Inception (Special 2026 IMAX Screening)", "INOX • Elante Mall", "Tomorrow • 07:30 PM", "C4, C5", BigDecimal.valueOf(1050));
                    bookingRepo.save(bAlex1);

                    Booking bAlex2 = new Booking(showNeon != null ? showNeon : showInception, alex, "CBK-341902", alex.getEmail(), alex.getName(),
                            "Neon Dreams", "INOX • Elante Mall", "Tomorrow • 01:15 PM", "E2", BigDecimal.valueOf(250));
                    bookingRepo.save(bAlex2);

                    // Sarah's Booking
                    Booking bSarah = new Booking(showDune != null ? showDune : showInception, sarah, "CBK-551293", sarah.getEmail(), sarah.getName(),
                            "Dune: Part Two", "PVR Director's Cut • Vasant Kunj", "Tomorrow • 08:45 PM", "D7, D8", BigDecimal.valueOf(945));
                    bookingRepo.save(bSarah);

                    // Rahul's Cancelled Booking
                    Booking bRahul = new Booking(showOpp != null ? showOpp : showInception, rahul, "CBK-109482", rahul.getEmail(), rahul.getName(),
                            "Oppenheimer (70mm Special Screening)", "PVR ICON • Phoenix Palladium", "Tomorrow • 08:00 PM", "F5, F6", BigDecimal.valueOf(1470));
                    bRahul.setStatus("CANCELLED");
                    bRahul.setCancelledAt("Nov 02, 2026, 04:15 PM");
                    bRahul.setRefundAmount(BigDecimal.valueOf(1470));
                    bRahul.setRefundStatus("REFUND_INITIATED");
                    bookingRepo.save(bRahul);
                }
            }

            log.info("Database initialization completed successfully!");
        } catch (Exception e) {
            log.error("DataInitializer error: {}", e.getMessage(), e);
        }
    }

    private Theater getOrCreateTheater(String name, String city, String address) {
        List<Theater> theaters = theaterRepo.findByCityIgnoreCase(city);
        for (Theater t : theaters) {
            if (t.getName().equalsIgnoreCase(name)) return t;
        }
        return theaterRepo.save(new Theater(name, city, address));
    }

    private Screen getOrCreateScreen(Theater theater, String name) {
        List<Screen> screens = screenRepo.findByTheaterId(theater.getId());
        for (Screen s : screens) {
            if (s.getName().equalsIgnoreCase(name)) return s;
        }
        return screenRepo.save(new Screen(theater, name, 6, 10));
    }

    private Movie getOrCreateMovie(String title, String desc, String poster, String backdrop,
                                   int duration, String lang, String genre, double rating,
                                   LocalDate relDate, String trailer, String status) {
        Optional<Movie> opt = movieRepo.findByTitleIgnoreCase(title);
        if (opt.isPresent()) {
            Movie m = opt.get();
            m.setDescription(desc);
            m.setPosterUrl(poster);
            m.setBackdropUrl(backdrop);
            m.setDurationMinutes(duration);
            m.setLanguage(lang);
            m.setGenre(genre);
            m.setRating(rating);
            m.setReleaseDate(relDate);
            m.setTrailerUrl(trailer);
            m.setStatus(status);
            m.setIsActive(true);
            return movieRepo.save(m);
        }
        Movie m = new Movie(title, desc, poster, backdrop, duration, lang, genre, rating, relDate, trailer, status);
        return movieRepo.save(m);
    }

    private void createShow(Movie movie, Screen screen, LocalDate date, LocalTime time, String format, BigDecimal price) {
        showRepo.save(new Show(movie, screen, date, time, format, price));
    }

    private User getOrCreateUser(String name, String email, String rawPassword) {
        Optional<User> opt = userRepo.findByEmailIgnoreCase(email);
        if (opt.isPresent()) {
            return opt.get();
        }
        User u = new User(name, email, passwordEncoder.encode(rawPassword));
        return userRepo.save(u);
    }
}
