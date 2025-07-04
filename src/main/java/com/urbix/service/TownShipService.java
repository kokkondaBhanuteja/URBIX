package com.urbix.service;

import com.urbix.dto.TownshipDTO;
import com.urbix.entity.Township;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface TownShipService {
    List<String> getAvailableCities();
    Map<String, Object> processData(String city);
    List<TownshipDTO> getAllTownships();
    Township getTownshipById(Long id);
    void deleteTownshipById(Long id);
    Township updateTownship(Township township, MultipartFile imageFile) throws IOException;
    List<TownshipDTO> getTownshipsByUserId(Long userId);
}