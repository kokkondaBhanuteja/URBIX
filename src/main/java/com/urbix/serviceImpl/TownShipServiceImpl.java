package com.urbix.serviceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urbix.dto.TownshipDTO;
import com.urbix.entity.Township;
import com.urbix.repository.TownShipRepository;
import com.urbix.service.TownShipService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TownShipServiceImpl implements TownShipService {

    private final TownShipRepository townshipRepository;

    public TownShipServiceImpl(TownShipRepository townshipRepository) {
        this.townshipRepository = townshipRepository;
    }
    @Override
    public List<String> getAvailableCities() {
        // You can pull distinct city names from your data source
        return Arrays.asList("hyderabad", "mumbai", "bangalore", "delhi","warangal","kolkata");
    }

    private void process99AcresData(String city, ObjectMapper objectMapper,
                                    List<String> locations, List<Double> prices, List<Double> areas) {
        try {
            File file = new File("src/main/resources/static/json/99acres/land/" + city + ".json");
            if (!file.exists()) {
                System.out.println("99acres data not found for: " + city);
                return;
            }

            JsonNode rootNode = objectMapper.readTree(file);
            JsonNode properties = rootNode.get("properties");

            if (properties != null && properties.isArray()) {
                for (JsonNode property : properties) {
                    String locality = property.get("LOCALITY_WO_CITY").asText("");
                    double price = property.path("MIN_PRICE").asDouble(0);
                    double area = property.path("SUPER_SQFT").asDouble(0);

                    locations.add(locality);
                    prices.add(price);
                    areas.add(area);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading 99acres data for city " + city + ": " + e.getMessage());
        }
    }
    private void processOlxData(String city, ObjectMapper objectMapper,
                                List<String> locations, List<Double> prices, List<Double> areas) {
        try {
            File file = new File("src/main/resources/static/json/olx/land/" + city + ".json");
            if (!file.exists()) {
                System.out.println("OLX data not found for: " + city);
                return;
            }

            JsonNode rootNode = objectMapper.readTree(file);
            JsonNode data = rootNode.get("data");

            if (data != null && data.isArray()) {
                for (JsonNode property : data) {
                    String locality = property.path("locations_resolved")
                            .path("SUBLOCALITY_LEVEL_1_name")
                            .asText("");
                    double price = property.path("price")
                            .path("value")
                            .path("raw")
                            .asDouble(0);

                    double area = 0;
                    JsonNode parameters = property.get("parameters");
                    if (parameters != null && parameters.isArray()) {
                        for (JsonNode param : parameters) {
                            if ("yd".equals(param.path("key").asText(""))) {
                                area = param.path("value").asDouble(0) * 9; // convert sq yards to sq feet
                                break;
                            }
                        }
                    }

                    locations.add(locality);
                    prices.add(price);
                    areas.add(area);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading OLX data for city " + city + ": " + e.getMessage());
        }
    }


    @Override
    public Map<String, Object> processData(String city) {
        List<String> locations = new ArrayList<>();
        List<Double> prices = new ArrayList<>();
        List<Double> areas = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        // Process 99acres and OLX separately
        process99AcresData(city, objectMapper, locations, prices, areas);
        processOlxData(city, objectMapper, locations, prices, areas);

        Map<String, Object> data = new HashMap<>();
        data.put("locations", locations);
        data.put("prices", prices);
        data.put("areas", areas);
        return data;
    }
    /*
    @Override
    public Map<String, Object> processData(String city) {
        List<String> locations = new ArrayList<>();
        List<Double> prices = new ArrayList<>();
        List<Double> areas = new ArrayList<>();


        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // Reading 99acres JSON data
            JsonNode rootNode99 = objectMapper.readTree(new File("src/main/resources/static/json/99acres/land/"+city+".json"));
            JsonNode properties99 = rootNode99.get("properties");
            if (properties99 != null && properties99.isArray()) {
                for (JsonNode property : properties99) {
                    String locality = property.get("LOCALITY_WO_CITY").asText();
                    double price = property.get("MIN_PRICE").asDouble();
                    double area = property.get("SUPER_SQFT").asDouble();

                    locations.add(locality);
                    prices.add(price);
                    areas.add(area);
                }
            }

            // Reading OLX JSON data
            JsonNode rootNodeOlx = objectMapper.readTree(new File("src/main/resources/static/json/olx/land/"+city+".json"));
            JsonNode dataOlx = rootNodeOlx.get("data");

            if (dataOlx != null && dataOlx.isArray()) {
                for (JsonNode property : dataOlx) {
                    String locality = property.get("locations_resolved").get("SUBLOCALITY_LEVEL_1_name").asText();
                    double price = property.get("price").get("value").get("raw").asDouble();

                    // Extract plot area in square yards and convert to square feet
                    double area = 0;
                    JsonNode parameters = property.get("parameters");
                    if (parameters != null && parameters.isArray()) {
                        for (JsonNode param : parameters) {
                            if ("yd".equals(param.get("key").asText())) {
                                area = param.get("value").asDouble() * 9; // Convert sq yards to sq feet
                                break;
                            }
                        }
                    }

                    locations.add(locality);
                    prices.add(price);
                    areas.add(area);
                }
            }

        }  catch (Exception e) {
            e.printStackTrace();
        }
            Map<String, Object> data = new HashMap<>();
            data.put("locations",locations);
            data.put("prices", prices);
            data.put("areas", areas);
            return data;
    }*/
    @Override
    public List<TownshipDTO> getAllTownships() {
        List<Township> townships = townshipRepository.findAll();

        return townships.stream().map(t -> {
            String base64Image = (t.getImage() != null) ? Base64.getEncoder().encodeToString(t.getImage()) : "";

            System.out.println("Encoded Image for " + t.getName() + ": " + base64Image.substring(0, Math.min(50, base64Image.length())) + "...");

            return new TownshipDTO(
                    t.getId(),
                    t.getName(),
                    t.getState(),
                    t.getDescription(),
                    base64Image,
                    t.getCreatedAt(),
                    t.getUpdatedAt()
            );
        }).collect(Collectors.toList());
    }
    @Override
    public Township getTownshipById(Long id) {
        return townshipRepository.findById(id).orElse(null);
    }

    @Override
    public void deleteTownshipById(Long id) {
        townshipRepository.deleteById(id);
    }

    @Override
    public Township updateTownship(Township township, MultipartFile imageFile) throws IOException, IOException {
        Township existingTownship = townshipRepository.findById(township.getId()).orElse(null);

        if (existingTownship == null) {
            return null;
        }

        existingTownship.setName(township.getName());
        existingTownship.setState(township.getState());
        existingTownship.setDescription(township.getDescription());
        existingTownship.setUpdatedAt(LocalDateTime.now());
        // Update image only if a new one is provided
        if (imageFile != null && !imageFile.isEmpty()) {
            existingTownship.setImage(imageFile.getBytes());
        }

        return townshipRepository.save(existingTownship);
    }

    @Override
    public List<TownshipDTO> getTownshipsByUserId(Long userId) {
        List<Township> townships = townshipRepository.findByUserId(userId);

        return townships.stream().map(t -> {
            String base64Image = (t.getImage() != null) ? Base64.getEncoder().encodeToString(t.getImage()) : "";

            return new TownshipDTO(
                    t.getId(),
                    t.getName(),
                    t.getState(),
                    t.getDescription(),
                    base64Image,
                    t.getCreatedAt(),
                    t.getUpdatedAt()
            );
        }).collect(Collectors.toList());
    }
}
