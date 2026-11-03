package com.cinebook.movie.dto;

import java.util.List;

public class PagedResponse<T> {
    private int page;
    private int totalPages;
    private int totalResults;
    private List<T> results;

    public PagedResponse() {}

    public PagedResponse(int page, int totalPages, int totalResults, List<T> results) {
        this.page = page;
        this.totalPages = totalPages;
        this.totalResults = totalResults;
        this.results = results;
    }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public int getTotalResults() { return totalResults; }
    public void setTotalResults(int totalResults) { this.totalResults = totalResults; }

    public List<T> getResults() { return results; }
    public void setResults(List<T> results) { this.results = results; }
}

