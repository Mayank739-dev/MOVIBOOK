package com.cinebook.booking;

import com.cinebook.show.Show;
import com.cinebook.show.ShowRepository;
import com.cinebook.user.User;
import com.cinebook.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingRepository bookingRepo;
    private final UserRepository userRepo;
    private final ShowRepository showRepo;

    public BookingController(BookingRepository bookingRepo, UserRepository userRepo, ShowRepository showRepo) {
        this.bookingRepo = bookingRepo;
        this.userRepo = userRepo;
        this.showRepo = showRepo;
    }

    @GetMapping
    public List<Booking> getBookings(@RequestParam(required = false) String email) {
        if (email == null || email.isBlank()) {
            return Collections.emptyList();
        }
        return bookingRepo.findByUserEmailIgnoreCaseOrderByBookingDateDesc(email.trim());
    }

    @PostMapping
    public ResponseEntity<?> createBooking(@RequestBody Map<String, Object> body) {
        String email = (String) body.get("email");
        String name = (String) body.get("name");
        String movieTitle = (String) body.get("movieTitle");
        String theaterName = (String) body.get("theaterName");
        String showTime = (String) body.get("showTime");
        String showIdStr = (String) body.get("showId");
        Object seatsObj = body.get("seats");
        Object amountObj = body.get("amount");

        if (email == null || email.isBlank() || movieTitle == null || theaterName == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "User email, movie title, and theater are required."
            ));
        }

        String seatsStr = "";
        if (seatsObj instanceof List<?>) {
            List<?> list = (List<?>) seatsObj;
            seatsStr = String.join(", ", list.stream().map(Object::toString).toList());
        } else if (seatsObj != null) {
            seatsStr = seatsObj.toString();
        }

        BigDecimal amount = BigDecimal.valueOf(350);
        if (amountObj instanceof Number) {
            amount = BigDecimal.valueOf(((Number) amountObj).doubleValue());
        }

        // Find show
        Show show = null;
        if (showIdStr != null && !showIdStr.isBlank()) {
            try {
                show = showRepo.findById(UUID.fromString(showIdStr)).orElse(null);
            } catch (Exception ignored) {}
        }
        if (show == null) {
            List<Show> allShows = showRepo.findAll();
            if (!allShows.isEmpty()) {
                show = allShows.get(0);
            }
        }

        if (show == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Cannot find an active screening to attach this booking."
            ));
        }

        // Find or create user
        User user = userRepo.findByEmailIgnoreCase(email.trim()).orElse(null);

        // Generate unique CBK code
        String code = "CBK-" + (100000 + new Random().nextInt(900000));
        Booking booking = new Booking(show, user, code, email.trim().toLowerCase(), name, movieTitle, theaterName, showTime, seatsStr, amount);
        booking = bookingRepo.save(booking);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Booking confirmed successfully!",
            "booking", booking
        ));
    }

    @PostMapping("/{code}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable String code) {
        Optional<Booking> opt = bookingRepo.findByBookingCode(code);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Booking b = opt.get();
        if ("CANCELLED".equalsIgnoreCase(b.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "This booking is already cancelled."
            ));
        }

        b.setStatus("CANCELLED");
        b.setCancelledAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy, hh:mm a")));
        b.setRefundAmount(b.getAmount());
        b.setRefundStatus("REFUND_INITIATED");

        Booking saved = bookingRepo.save(b);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Booking cancelled. 100% refund initiated.",
            "booking", saved
        ));
    }
}
