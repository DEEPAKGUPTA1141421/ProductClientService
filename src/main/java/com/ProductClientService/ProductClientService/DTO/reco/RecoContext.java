package com.ProductClientService.ProductClientService.DTO.reco;

/** Surface the recommendation was requested for — changes ranking & blend. */
public enum RecoContext {
    HOME, PDP, CART;

    public static RecoContext parse(String s) {
        if (s == null || s.isBlank()) return HOME;
        try { return RecoContext.valueOf(s.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return HOME; }
    }
}
