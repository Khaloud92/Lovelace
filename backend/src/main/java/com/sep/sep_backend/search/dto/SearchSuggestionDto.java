package com.sep.sep_backend.search.dto;

/**
 * A previously used search text offered as a suggestion.
 */
public class SearchSuggestionDto {

    private String queryText;
    private long count;

    public SearchSuggestionDto() {
    }

    public SearchSuggestionDto(String queryText, long count) {
        this.queryText = queryText;
        this.count = count;
    }

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}