package com.cinebook.movie.dto;

public class TranslationDto {
    private String languageCode;
    private String languageName;
    private String title;
    private String overview;

    public TranslationDto() {}

    public TranslationDto(String languageCode, String languageName, String title, String overview) {
        this.languageCode = languageCode;
        this.languageName = languageName;
        this.title = title;
        this.overview = overview;
    }

    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }

    public String getLanguageName() { return languageName; }
    public void setLanguageName(String languageName) { this.languageName = languageName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }
}

