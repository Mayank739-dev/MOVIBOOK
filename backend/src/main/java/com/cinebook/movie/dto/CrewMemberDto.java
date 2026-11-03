package com.cinebook.movie.dto;

public class CrewMemberDto {
    private Long id;
    private String name;
    private String job;
    private String department;
    private String profileUrl;

    public CrewMemberDto() {}

    public CrewMemberDto(Long id, String name, String job, String department, String profileUrl) {
        this.id = id;
        this.name = name;
        this.job = job;
        this.department = department;
        this.profileUrl = profileUrl;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getJob() { return job; }
    public void setJob(String job) { this.job = job; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getProfileUrl() { return profileUrl; }
    public void setProfileUrl(String profileUrl) { this.profileUrl = profileUrl; }
}

