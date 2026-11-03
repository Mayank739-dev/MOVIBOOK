package com.cinebook.movie.dto;

import java.util.ArrayList;
import java.util.List;

public class MovieDetailDto extends MovieDto {
    private String imdbId;
    private List<CastMemberDto> cast = new ArrayList<>();
    private List<CrewMemberDto> crew = new ArrayList<>();
    private List<VideoDto> videos = new ArrayList<>();
    private VideoDto primaryTrailer;
    private List<ReviewDto> reviews = new ArrayList<>();
    private List<String> keywords = new ArrayList<>();
    private List<ProductionCompanyDto> productionCompanies = new ArrayList<>();
    private List<ProductionCountryDto> productionCountries = new ArrayList<>();
    private List<MovieDto> similarMovies = new ArrayList<>();
    private List<MovieDto> recommendedMovies = new ArrayList<>();
    private List<TranslationDto> translations = new ArrayList<>();
    private boolean bookingAvailable = true;

    public MovieDetailDto() {
        super();
    }

    public String getImdbId() { return imdbId; }
    public void setImdbId(String imdbId) { this.imdbId = imdbId; }

    public List<CastMemberDto> getCast() { return cast; }
    public void setCast(List<CastMemberDto> cast) { this.cast = cast; }

    public List<CrewMemberDto> getCrew() { return crew; }
    public void setCrew(List<CrewMemberDto> crew) { this.crew = crew; }

    public List<VideoDto> getVideos() { return videos; }
    public void setVideos(List<VideoDto> videos) { this.videos = videos; }

    public VideoDto getPrimaryTrailer() { return primaryTrailer; }
    public void setPrimaryTrailer(VideoDto primaryTrailer) { this.primaryTrailer = primaryTrailer; }

    public List<ReviewDto> getReviews() { return reviews; }
    public void setReviews(List<ReviewDto> reviews) { this.reviews = reviews; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public List<ProductionCompanyDto> getProductionCompanies() { return productionCompanies; }
    public void setProductionCompanies(List<ProductionCompanyDto> productionCompanies) { this.productionCompanies = productionCompanies; }

    public List<ProductionCountryDto> getProductionCountries() { return productionCountries; }
    public void setProductionCountries(List<ProductionCountryDto> productionCountries) { this.productionCountries = productionCountries; }

    public List<MovieDto> getSimilarMovies() { return similarMovies; }
    public void setSimilarMovies(List<MovieDto> similarMovies) { this.similarMovies = similarMovies; }

    public List<MovieDto> getRecommendedMovies() { return recommendedMovies; }
    public void setRecommendedMovies(List<MovieDto> recommendedMovies) { this.recommendedMovies = recommendedMovies; }

    public List<TranslationDto> getTranslations() { return translations; }
    public void setTranslations(List<TranslationDto> translations) { this.translations = translations; }

    public boolean isBookingAvailable() { return bookingAvailable; }
    public void setBookingAvailable(boolean bookingAvailable) { this.bookingAvailable = bookingAvailable; }
}
