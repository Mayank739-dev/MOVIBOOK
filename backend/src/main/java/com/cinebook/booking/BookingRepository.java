package com.cinebook.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findByUserEmailIgnoreCaseOrderByBookingDateDesc(String userEmail);
    Optional<Booking> findByBookingCode(String bookingCode);
}

