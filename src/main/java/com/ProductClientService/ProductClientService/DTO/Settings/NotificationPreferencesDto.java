package com.ProductClientService.ProductClientService.DTO.Settings;

public record NotificationPreferencesDto(
        boolean orderEmail, boolean orderPush, boolean orderSms,
        boolean paymentEmail, boolean paymentPush, boolean paymentSms,
        boolean stockEmail, boolean stockPush, boolean stockSms,
        boolean promoEmail, boolean promoPush, boolean promoSms,
        boolean securityEmail, boolean securityPush, boolean securitySms) {
}