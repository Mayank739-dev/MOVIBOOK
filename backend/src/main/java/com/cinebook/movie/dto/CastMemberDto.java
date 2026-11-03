package com.cinebook.movie.dto;

public class CastMemberDto {
    private Long id;
    private String name;
    private String character;
    private String profileUrl;
    private Integer order;

    public CastMemberDto() {}

    public CastMemberDto(Long id, String name, String character, String profileUrl, Integer order) {
        this.id = id;
        this.name = name;
        this.character = character;
        this.profileUrl = profileUrl;
        this.order = order;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCharacter() { return character; }
    public void setCharacter(String character) { this.character = character; }

    public String getProfileUrl() { return profileUrl; }
    public void setProfileUrl(String profileUrl) { this.profileUrl = profileUrl; }

    public Integer getOrder() { return order; }
    public void setOrder(Integer order) { this.order = order; }
}

