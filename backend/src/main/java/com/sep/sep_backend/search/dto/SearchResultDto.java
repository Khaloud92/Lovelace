package com.sep.sep_backend.search.dto;

/**
 * A single global search hit with the route the frontend should navigate to.
 */
public class SearchResultDto {

    private String id;
    private String title;
    private String subtitle;
    private String type;
    private String route;

    public SearchResultDto() {
    }

    public SearchResultDto(String id, String title, String subtitle, String type, String route) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.type = type;
        this.route = route;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }
}