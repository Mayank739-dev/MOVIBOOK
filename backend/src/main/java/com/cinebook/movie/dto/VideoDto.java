package com.cinebook.movie.dto;

public class VideoDto {
    private String id;
    private String name;
    private String key;
    private String site;
    private String type;
    private Boolean official;
    private String videoUrl;

    public VideoDto() {}

    public VideoDto(String id, String name, String key, String site, String type, Boolean official, String videoUrl) {
        this.id = id;
        this.name = name;
        this.key = key;
        this.site = site;
        this.type = type;
        this.official = official;
        this.videoUrl = videoUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Boolean getOfficial() { return official; }
    public void setOfficial(Boolean official) { this.official = official; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
}

