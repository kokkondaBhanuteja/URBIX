package com.urbix.serviceImpl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urbix.service.ScrapeService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ScraperServiceImpl implements ScrapeService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<Map<String, Object>> scrapeOlxLands(String city, Integer minPrice, Integer maxPrice, Integer minSqft, Integer maxSqft) {
        return getFilteredProperties("src/main/resources/static/json/olx/land/", city, "data", minPrice, maxPrice, minSqft, maxSqft);
    }

    @Override
    public List<Map<String, Object>> scrape99AcresLands(String city, Integer minPrice, Integer maxPrice, Integer minSqft, Integer maxSqft) {
        return getFilteredProperties("src/main/resources/static/json/99acres/land/", city, "properties", minPrice, maxPrice, minSqft, maxSqft);
    }
    @Override
    public List<String> getLocationSuggestions(String query, String platform) {
        String jsonFile = platform.equalsIgnoreCase("olx") ? "src/main/resources/static/json/olx/land/" :
                "src/main/resources/static/json/99acres/land/";

        File[] files = new File(jsonFile).listFiles();
        List<String> matchingLocations = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName().replace(".json", "");
                if (fileName.toLowerCase().contains(query.toLowerCase())) {
                    matchingLocations.add(fileName);
                }
            }
        }
        return matchingLocations;
    }

    private List<Map<String, Object>> getFilteredProperties(String directory, String city, String dataKey,
                                                            Integer minPrice, Integer maxPrice, Integer minSqft, Integer maxSqft) {
        List<Map<String, Object>> filteredListings = new ArrayList<>();
        File targetFile = new File(directory, city.toLowerCase() + ".json");

        if (targetFile.exists()) {
            try {
                JsonNode rootNode = objectMapper.readTree(targetFile);
                JsonNode dataArray = rootNode.get(dataKey);

                if (dataArray != null && dataArray.isArray()) {
                    List<Map<String, Object>> allListings = objectMapper.convertValue(dataArray, new TypeReference<>() {});

                    for (Map<String, Object> property : allListings) {
                        int price = getPropertyPrice(property);
                        int sqft = getPropertySquareFeet(property);

                        boolean matchesPrice = (minPrice == null || price >= minPrice) && (maxPrice == null || price <= maxPrice);
                        boolean matchesSqft = (minSqft == null || sqft >= minSqft) && (maxSqft == null || sqft <= maxSqft);

                        if (matchesPrice && matchesSqft) {
                            filteredListings.add(property);
                        }
                    }
                    return filteredListings.isEmpty() ? allListings : filteredListings; // Return all if no filters applied
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return filteredListings;
    }

    private int getPropertyPrice(Map<String, Object> property) {
        try {
            Map<String, Object> priceNode = (Map<String, Object>) property.get("price");
            Map<String, Object> valueNode = (Map<String, Object>) priceNode.get("value");
            return (int) valueNode.get("raw");
        } catch (Exception e) {
            return 0; // Default if no price is found
        }
    }

    private int getPropertySquareFeet(Map<String, Object> property) {
        try {
            List<Map<String, Object>> parameters = (List<Map<String, Object>>) property.get("parameters");
            for (Map<String, Object> param : parameters) {
                if ("yd".equals(param.get("key"))) { // "yd" key represents plot area in square yards
                    return Integer.parseInt(param.get("value").toString());
                }
            }
        } catch (Exception e) {
            return 0; // Default if no area is found
        }
        return 0;
    }
}
