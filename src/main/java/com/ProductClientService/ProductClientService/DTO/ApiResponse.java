package com.ProductClientService.ProductClientService.DTO;

public record ApiResponse<T>(boolean success, String message,  T data, int statusCode) {}