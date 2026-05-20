package com.ProductClientService.ProductClientService.DTO.network;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Mirror of DeliveryInventoryService's DeliveryEstimateResponse.
 * Used by DeliveryInventoryClient (OpenFeign) in ProductClientService.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeliveryEstimateDto {
    private double seg1Km;
    private double seg2Km;
    private double seg3Km;
    private double totalKm;
    private int etaMinutes;
    private String etaLabel;
    private String srcWarehouseCity;
    private String destWarehouseCity;
    private boolean sameCityDelivery;
}
