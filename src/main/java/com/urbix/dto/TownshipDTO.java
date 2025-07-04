package com.urbix.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class TownshipDTO {
    private Long id;
    private String name;
    private String state;
    private String description;
    private String imageBase64;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor without timestamps (for backward compatibility)
    public TownshipDTO(Long id, String name, String state, String description, String imageBase64) {
        this.id = id;
        this.name = name;
        this.state = state;
        this.description = description;
        this.imageBase64 = imageBase64;
    }

    // Constructor with timestamps
    public TownshipDTO(Long id, String name, String state, String description, String imageBase64,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.state = state;
        this.description = description;
        this.imageBase64 = imageBase64;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
