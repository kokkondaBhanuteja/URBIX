package com.urbix.controller;

import com.urbix.service.ScrapeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Controller
@RequestMapping("/api/scraper")
public class ScraperController {

    @Autowired
    private ScrapeService scrapeService;
    @GetMapping("/searchProperty")
    public String showSearchProperty(Model model) {
        List<Map<String, Object>> listings = new ArrayList<>();  // Fetch actual listings here
        model.addAttribute("listings", listings);
        model.addAttribute("location", "");
        model.addAttribute("platform","");// Optional: Pass default location
        return "searchProperty";
    }
    @GetMapping("/suggest-locations")
    public ResponseEntity<List<String>> getLocationSuggestions(@RequestParam String query, @RequestParam String platform) {
        System.out.println("From the Suggestion Controller");
        List<String> suggestions = scrapeService.getLocationSuggestions(query, platform);
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/property-list")
    public String getProperties(@RequestParam("location") String location,
                                @RequestParam("platform") String platform,
                                @RequestParam(required = false) Integer minPrice,
                                @RequestParam(required = false) Integer maxPrice,
                                @RequestParam(required = false) Integer minSqft,
                                @RequestParam(required = false) Integer maxSqft,
                                Model model) {
        List<Map<String, Object>> properties;
        if ("olx".equalsIgnoreCase(platform)) {
            properties = scrapeService.scrapeOlxLands(location,minPrice,maxPrice,minSqft,maxSqft);
        } else {
            properties = scrapeService.scrape99AcresLands(location,minPrice,maxPrice,minSqft,maxSqft);
        }
        model.addAttribute("listings", properties);
        model.addAttribute("location", location);
        model.addAttribute("platform", platform);
        model.addAttribute("selectedMinPrice", minPrice);
        model.addAttribute("selectedMaxPrice", maxPrice);
        model.addAttribute("selectedMinSqft", minSqft);
        model.addAttribute("selectedMaxSqft", maxSqft);
        // ✅ Ensure Thymeleaf template is returned directly
        return "searchProperty";
    }
}

