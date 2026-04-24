package com.ProductClientService.ProductClientService.DTO.events;

/**
 * Event weights drive the recommendation model (LightFM loss weighting).
 * COD purchases are weighted highest — in Tier-2/3 India they are a stronger
 * intent signal than prepaid purchases because users self-select into paying
 * on delivery only when seriously committed.
 */
public enum InteractionType {
    VIEW((short) 1, 1.0),
    CLICK((short) 2, 2.0),
    WISHLIST((short) 3, 4.0),
    CART((short) 4, 6.0),
    PURCHASE_PREPAID((short) 5, 10.0),
    PURCHASE_COD((short) 6, 14.0);

    private final short code;
    private final double weight;

    InteractionType(short code, double weight) {
        this.code = code;
        this.weight = weight;
    }

    public short code() { return code; }
    public double weight() { return weight; }

    public static InteractionType fromCode(short code) {
        for (InteractionType t : values()) if (t.code == code) return t;
        throw new IllegalArgumentException("Unknown interaction code: " + code);
    }
}
