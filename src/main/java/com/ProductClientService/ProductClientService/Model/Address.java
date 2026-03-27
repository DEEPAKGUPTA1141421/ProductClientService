package com.ProductClientService.ProductClientService.Model;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.Point;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "addresses")
public class Address {
    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326); // WGS-84
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", referencedColumnName = "id", nullable = true)
    @JsonBackReference
    private Seller seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = true)
    @JsonBackReference
    private User user;

    @Column(name = "kind", nullable = false)
    @Enumerated(EnumType.STRING)
    private Kind kind = Kind.LEGAL;

    @Column(name = "address_type")
    @Enumerated(EnumType.STRING)
    private AddressType addressType = AddressType.HOME;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "line1", nullable = false)
    private String line1;

    @Column(name = "line2")
    private String line2;

    @Column(name = "landmark")
    private String landmark;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "state", nullable = false)
    private String state;

    @Column(name = "country", nullable = false)
    private String country = "IN";

    @Column(name = "pincode", nullable = false)
    private String pincode;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "adhadhar_card", nullable = true)
    private String AdhadharCard;

    @Column(name = "pan_card", nullable = true)
    private String PanCard;
    @Column(name = "location", columnDefinition = "geometry(Point,4326)")
    @JsonIgnore // expose via getLatitude() / getLongitude() instead
    private Point location;

    // ── Convenience accessors (avoids leaking JTS into callers) ──────────────

    /**
     * Returns the latitude (Y coordinate) or {@code null} if no location is set.
     */
    @Transient
    public BigDecimal getLatitude() {
        return location == null ? null : BigDecimal.valueOf(location.getY());
    }

    /**
     * Returns the longitude (X coordinate) or {@code null} if no location is set.
     */
    @Transient
    public BigDecimal getLongitude() {
        return location == null ? null : BigDecimal.valueOf(location.getX());
    }

    /**
     * Sets the geospatial point from separate lat/lng values.
     * Passing {@code null} for either clears the location.
     */
    public void setLatitude(BigDecimal latitude) {
        if (latitude == null) {
            this.location = null;
            return;
        }
        double lng = (this.location != null) ? this.location.getX() : 0.0;
        this.location = GF.createPoint(new Coordinate(lng, latitude.doubleValue()));
    }

    public void setLongitude(BigDecimal longitude) {
        if (longitude == null) {
            this.location = null;
            return;
        }
        double lat = (this.location != null) ? this.location.getY() : 0.0;
        this.location = GF.createPoint(new Coordinate(longitude.doubleValue(), lat));
    }

    /**
     * Atomically set both coordinates in one call (preferred over the two setters
     * above to avoid an intermediate state with lat=new but lng=old).
     */
    public void setCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            this.location = null;
        } else {
            this.location = GF.createPoint(
                    new Coordinate(longitude.doubleValue(), latitude.doubleValue()));
        }
    }

    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;

    private enum Kind {
        LEGAL, PICKUP, RETURN
    }

    public enum AddressType {
        HOME,
        WORK,
        OTHER
    }

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
}
// jjjjijinjkjikikjkhjijhkjhukjijijnnj