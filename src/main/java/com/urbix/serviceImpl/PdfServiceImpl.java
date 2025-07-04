package com.urbix.serviceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.urbix.service.PdfService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PdfServiceImpl implements PdfService {

    // Define file paths for OLX and 99acres
    private static final String OLX_JSON_PATH = "src/main/resources/static/json/olx/land/";
    private static final String NINETY_NINE_ACRES_JSON_PATH = "src/main/resources/static/json/99acres/land/";

    @Override
    public byte[] generatePdf(String spotId, String platform,String city) {
        try {
            // Get property details from respective platform
            Map<String, Object> plotDetails = getPlotDetails(spotId, platform,city);
            if (plotDetails.isEmpty()) {
                return null;
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);

            // Title with color
            Paragraph title = new Paragraph("Property Details Report")
                    .setFontSize(20)
                    .setFontColor(ColorConstants.BLUE)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            // Add extracted details
            document.add(new Paragraph("Spot ID: " + spotId));
            document.add(new Paragraph("Platform: " + platform));
            document.add(new Paragraph("Owner Name: " + plotDetails.getOrDefault("ownerName", "N/A")));
            document.add(new Paragraph("Location: " + plotDetails.getOrDefault("location", "N/A")));
            document.add(new Paragraph("Project Name: " + plotDetails.getOrDefault("project", "N/A")));
            document.add(new Paragraph("Property Type: " + plotDetails.getOrDefault("propertyType", "N/A")));
            document.add(new Paragraph("Price: " + plotDetails.getOrDefault("price", "N/A")));
            document.add(new Paragraph("Description: \n" + plotDetails.getOrDefault("description", "N/A")));
            document.add(new Paragraph("\n"));

            // Add Images
            List<String> imageUrls = (List<String>) plotDetails.get("imageUrls");
            if (imageUrls != null && !imageUrls.isEmpty()) {
                document.add(new Paragraph("Property Images:"));

                // Table layout for images (2 images per row)
                Table table = new Table(1);
                int imageCount = 0;
                for (int i = 0; i < imageUrls.size(); i++) {
                    try {
                        ImageData imageData = ImageDataFactory.create(imageUrls.get(i));
                        Image image = new Image(imageData).scaleToFit(350, 300);
                        table.addCell(image);
                        imageCount++;
                    } catch (Exception e) {
                        table.addCell(new Paragraph("Image " + (i + 1) + " not available"));
                        imageCount++;
                    }

                }
                // If an incomplete row exists, fill the remaining cell
                if (imageCount == 1) {
                    table.addCell(new Paragraph("")); // Empty cell to balance the last row
                    document.add(table);
                }

                // Add remaining images if not added
                if (!table.isEmpty()) {
                    document.add(table);
                }
            }
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Extract property details from JSON based on platform (OLX or 99acres)
    private Map<String, Object> getPlotDetails(String spotId, String platform, String city) {
        Map<String, Object> details = new HashMap<>();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String filePath = platform.equalsIgnoreCase("OLX") ? OLX_JSON_PATH+city+".json" : NINETY_NINE_ACRES_JSON_PATH+city+".json";
            JsonNode rootNode = objectMapper.readTree(new File(filePath));

            if (platform.equalsIgnoreCase("OLX")) {
                if (rootNode.has("data")) {
                    for (JsonNode item : rootNode.get("data")) {
                        if (item.has("id") && item.get("id").asText().equals(spotId)) {
                            details.put("ownerName", item.has("user_type") ? item.get("user_type").asText() : "N/A");
                            details.put("location", item.has("locations_resolved") ?
                                    item.get("locations_resolved").get("ADMIN_LEVEL_3_name").asText() : "N/A");
                            details.put("description", item.has("description") ? item.get("description").asText() : "N/A");

                            List<String> images = new ArrayList<>();
                            if (item.has("images")) {
                                for (JsonNode img : item.get("images")) {
                                    images.add(img.get("url").asText());
                                }
                            }
                            details.put("imageUrls", images);

                            if (item.has("parameters")) {
                                for (JsonNode param : item.get("parameters")) {
                                    if (param.has("key") && "projects".equals(param.get("key").asText())) {
                                        details.put("project", param.get("value_name").asText());
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            } else if (platform.equalsIgnoreCase("99acres")) {
                if (rootNode.has("properties")) {
                    for (JsonNode item : rootNode.get("properties")) {
                        if (item.has("SPID") && item.get("SPID").asText().equals(spotId)) {
                            details.put("ownerName", item.has("CONTACT_NAME") ? item.get("CONTACT_NAME").asText() : "N/A");
                            details.put("location", item.has("LOCALITY") ? item.get("LOCALITY").asText() : "N/A");
                            details.put("project", item.has("PROP_NAME") ? item.get("PROP_NAME").asText() : "N/A");
                            details.put("propertyType", item.has("PROPERTY_TYPE") ? item.get("PROPERTY_TYPE").asText() : "N/A");
                            details.put("price", item.has("PRICE") ? item.get("PRICE").asText() : "N/A");
                            details.put("description", item.has("DESCRIPTION") ? item.get("DESCRIPTION").asText() : "N/A");

                            // Collect all available images
                            List<String> images = new ArrayList<>();
                            if (item.has("PHOTO_URL")) images.add(item.get("PHOTO_URL").asText());
                            if (item.has("MEDIUM_PHOTO_URL")) images.add(item.get("MEDIUM_PHOTO_URL").asText());
                            if (item.has("PROPERTY_IMAGES")) {
                                for (JsonNode img : item.get("PROPERTY_IMAGES")) {
                                    images.add(img.asText());
                                }
                            }
                            if (item.has("THUMBNAIL_IMAGES")) {
                                for (JsonNode img : item.get("THUMBNAIL_IMAGES")) {
                                    images.add(img.asText());
                                }
                            }
                            details.put("imageUrls", images);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return details;
    }
}
