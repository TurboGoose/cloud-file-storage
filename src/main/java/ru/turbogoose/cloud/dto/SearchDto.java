package ru.turbogoose.cloud.dto;

import lombok.Data;
import ru.turbogoose.cloud.services.SearchService;

@Data
public class SearchDto {
    private String query;
    private SearchService.SearchType type;
    private boolean matchCase;
}
