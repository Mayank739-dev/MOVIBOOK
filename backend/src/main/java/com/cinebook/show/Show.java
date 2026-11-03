package com.cinebook.show;

import com.cinebook.movie.Movie;
import com.cinebook.theater.Screen;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "\"show\"")
public class Show {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @Column(name = "show_date", nullable = false)
    private LocalDate showDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(length = 20, nullable = false)
    private String format = "2D";

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice;

    @Column(length = 20)
    private String status = "ACTIVE";

    @Column(name = "booking_open")
    private Boolean bookingOpen = true;

    public Show() {}

    public Show(Movie movie, Screen screen, LocalDate showDate, LocalTime startTime, String format, BigDecimal basePrice) {
        this.movie = movie;
        this.screen = screen;
        this.showDate = showDate;
        this.startTime = startTime;
        this.format = format;
        this.basePrice = basePrice;
        this.status = "ACTIVE";
        this.bookingOpen = true;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Movie getMovie() { return movie; }
    public void setMovie(Movie movie) { this.movie = movie; }

    public Screen getScreen() { return screen; }
    public void setScreen(Screen screen) { this.screen = screen; }

    public LocalDate getShowDate() { return showDate; }
    public void setShowDate(LocalDate showDate) { this.showDate = showDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getBookingOpen() { return bookingOpen != null ? bookingOpen : true; }
    public void setBookingOpen(Boolean bookingOpen) { this.bookingOpen = bookingOpen; }
}

