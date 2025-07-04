package com.urbix.service;

import java.util.List;
import java.util.Map;

public interface ScrapeService {
    List<Map<String, Object>> scrapeOlxLands(String city, Integer minPrice, Integer maxPrice, Integer minSqft, Integer maxSqft);
    List<Map<String, Object>> scrape99AcresLands(String city, Integer minPrice, Integer maxPrice, Integer minSqft, Integer maxSqft);
    List<String> getLocationSuggestions(String query, String platform);
}
