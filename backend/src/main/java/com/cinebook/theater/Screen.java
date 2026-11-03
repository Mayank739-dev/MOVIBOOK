package com.cinebook.theater;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "screen")
public class Screen {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theater_id", nullable = false)
    private Theater theater;

    @Column(nullable = false)
    private String name;

    @Column(name = "rows_count", nullable = false)
    private Integer rowsCount = 6;

    @Column(name = "seats_per_row", nullable = false)
    private Integer seatsPerRow = 10;

    public Screen() {}

    public Screen(Theater theater, String name, Integer rowsCount, Integer seatsPerRow) {
        this.theater = theater;
        this.name = name;
        this.rowsCount = rowsCount;
        this.seatsPerRow = seatsPerRow;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Theater getTheater() { return theater; }
    public void setTheater(Theater theater) { this.theater = theater; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getRowsCount() { return rowsCount; }
    public void setRowsCount(Integer rowsCount) { this.rowsCount = rowsCount; }

    public Integer getSeatsPerRow() { return seatsPerRow; }
    public void setSeatsPerRow(Integer seatsPerRow) { this.seatsPerRow = seatsPerRow; }
}

