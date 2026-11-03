package com.cinebook.movie.dto;

public class ReviewDto {
    private String id;
    private String author;
    private String authorAvatar;
    private Double rating;
    private String content;
    private String createdAt;

    public ReviewDto() {}

    public ReviewDto(String id, String author, String authorAvatar, Double rating, String content, String createdAt) {
        this.id = id;
        this.author = author;
        this.authorAvatar = authorAvatar;
        this.rating = rating;
        this.content = content;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getAuthorAvatar() { return authorAvatar; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}

