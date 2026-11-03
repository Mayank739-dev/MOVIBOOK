package com.cinebook.movie.dto;

public class ProductionCompanyDto {
    private Long id;
    private String name;
    private String logoUrl;
    private String originCountry;

    public ProductionCompanyDto() {}

    public ProductionCompanyDto(Long id, String name, String logoUrl, String originCountry) {
        this.id = id;
        this.name = name;
        this.logoUrl = logoUrl;
        this.originCountry = originCountry;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getOriginCountry() { return originCountry; }
    public void setOriginCountry(String originCountry) { this.originCountry = originCountry; }
}

