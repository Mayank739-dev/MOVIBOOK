package com.cinebook.movie.dto;

public class ProductionCountryDto {
    private String isoCode;
    private String name;

    public ProductionCountryDto() {}

    public ProductionCountryDto(String isoCode, String name) {
        this.isoCode = isoCode;
        this.name = name;
    }

    public String getIsoCode() { return isoCode; }
    public void setIsoCode(String isoCode) { this.isoCode = isoCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

