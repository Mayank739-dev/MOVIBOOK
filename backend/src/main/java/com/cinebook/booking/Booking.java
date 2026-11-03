package com.cinebook.booking;

import com.cinebook.show.Show;
import com.cinebook.user.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "booking", indexes = {
    @Index(name = "idx_booking_user_email", columnList = "user_email"),
    @Index(name = "idx_booking_code", columnList = "booking_code")
})
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    @JsonIgnore
    private Show show;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @Column(name = "booking_code", nullable = false, unique = true)
    private String bookingCode;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "movie_title", nullable = false)
    private String movieTitle;

    @Column(name = "theater_name", nullable = false)
    private String theaterName;

    @Column(name = "show_time", nullable = false)
    private String showTime;

    @Column(nullable = false)
    private String seats;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String status = "CONFIRMED"; // CONFIRMED, CANCELLED

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate = LocalDate.now();

    @Column(name = "cancelled_at")
    private String cancelledAt;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refund_status")
    private String refundStatus;

    public Booking() {}

    public Booking(Show show, User user, String bookingCode, String userEmail, String userName, String movieTitle,
                   String theaterName, String showTime, String seats, BigDecimal amount) {
        this.show = show;
        this.user = user;
        this.bookingCode = bookingCode;
        this.userEmail = userEmail;
        this.userName = userName;
        this.movieTitle = movieTitle;
        this.theaterName = theaterName;
        this.showTime = showTime;
        this.seats = seats;
        this.amount = amount;
        this.totalAmount = amount;
        this.status = "CONFIRMED";
        this.bookingDate = LocalDate.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @JsonIgnore
    public Show getShow() {
        return show;
    }

    public void setShow(Show show) {
        this.show = show;
    }

    @JsonIgnore
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public void setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getTheaterName() {
        return theaterName;
    }

    public void setTheaterName(String theaterName) {
        this.theaterName = theaterName;
    }

    public String getShowTime() {
        return showTime;
    }

    public void setShowTime(String showTime) {
        this.showTime = showTime;
    }

    public String getSeats() {
        return seats;
    }

    public void setSeats(String seats) {
        this.seats = seats;
    }

    public BigDecimal getAmount() {
        return amount != null ? amount : totalAmount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
        this.totalAmount = amount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount != null ? totalAmount : amount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
        this.amount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(String cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(String refundStatus) {
        this.refundStatus = refundStatus;
    }

    // Convenience getters for frontend compatibility
    @JsonProperty("code")
    public String getCode() {
        return bookingCode;
    }

    @JsonProperty("title")
    public String getTitle() {
        return movieTitle;
    }

    @JsonProperty("theater")
    public String getTheater() {
        return theaterName;
    }

    @JsonProperty("date")
    public String getDate() {
        return showTime;
    }

    @JsonProperty("posterUrl")
    public String getPosterUrl() {
        if (movieTitle != null) {
            String lower = movieTitle.toLowerCase();
            if (lower.contains("inception")) return "https://image.tmdb.org/t/p/w500/9gk7adHYeDvHkCSEqAvQNLV5Uge.jpg";
            if (lower.contains("dune")) return "https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg";
            if (lower.contains("oppenheimer")) return "https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg";
            if (lower.contains("midnight")) return "https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&w=600&h=900&q=80";
            if (lower.contains("neon")) return "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&h=900&q=80";
        }
        return "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=600&q=80";
    }
}
