package com.urbix.service;

public interface PdfService {
    byte[] generatePdf(String spotId,String platform,String city);
}
