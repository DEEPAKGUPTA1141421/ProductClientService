# Product Addition Flow — Frontend Guide

## Overview

The product creation flow has **6 steps**. A seller can close the app at any step and resume later. The backend enforces that a seller can only have **one active (non-live) product** at a time.

---

## Step Map

| Step # | Step Name            | Enum Value          | Endpoint called                           |
|--------|----------------------|---------------------|-------------------------------------------|
| 1      | Basic Info           | `PRODUCT_NAME`      | `POST /create`                            |
| 2      | Attributes           | `PRODUCT_ATTRIBUTE` | `POST /create-product-attribute`          |
| 3      | Variants             | `PRODUCT_VARIANT`   | `POST /add-variants`                      |
| 4      | Images & Media       | `PRODUCT_IMAGE`     | `POST /upload-images`                     |
| 5      | Tags & Brand         | *PRODUCT_BRAND_AND_TAGS*  | `POST /add-tag` · `POST /attach-brand`    |
| 6      | Go Live              | `LIVE`              | `GET /make-product-live/{productId}`      |

---

## App Open — Check for Draft First

**Every time the seller opens the Add Product screen, call this first:**

```
GET /api/v1/seller/product/draft-product/full
Authorization: Bearer <token>
```

### Response — No Draft

```json
{
  "success": true,
  "message": "No draft product found",
  "data": null,
  "statusCode": 200
}
```

→ Allow the seller to start a new product.

### Response — Draft Exists

```json
{
  "success": true,
  "message": "Draft product data fetched",
  "data": {
    "productId": "94bab102-45c6-4db0-99f4-4fab766d2ebd",
    "currentStep": "PRODUCT_IMAGE",
    "basicInfo": {
      "name": "Men's Solid T-Shirt",
      "description": "100% cotton comfortable tee",
      "categoryId": "c1234...",
      "categoryName": "T-Shirts"
    },
    "attributes": [
      {
        "id": "6b01f022-...",
        "categoryAttributeId": "33e1e2c4-...",
        "name": "Color",
        "value": "Red",
        "isImageAttribute": true,
        "isVariantAttribute": false,
        "images": ["https://res.cloudinary.com/.../red1.png"]
      },
      {
        "id": "9b915199-...",
        "categoryAttributeId": "05d394b2-...",
        "name": "Size",
        "value": "XXS",
        "isImageAttribute": false,
        "isVariantAttribute": true,
        "images": []
      }
    ],
    "variants": [
      {
        "id": "abc...",
        "sku": "PRD-XXS-001",
        "label": "XXS",
        "price": 100.0,
        "mrp": 150.0,
        "stock": 10,
        "combination": { "Size": "XXS" }
      }
    ],
    "media": {
      "coverImageUrl": "https://res.cloudinary.com/.../cover.png",
      "attributeMedia": {
        "Red": ["https://res.cloudinary.com/.../red1.png"]
      }
    },
    "tags": [
      { "id": "tag-uuid", "name": "casual" }
    ],
    "brand": {
      "id": "brand-uuid",
      "name": "Nike"
    }
  },
  "statusCode": 200
}
```

→ Show a dialog: **"You have an unfinished product. Continue or Discard?"**
- **Continue** → navigate the seller to the screen matching `currentStep` (see routing table below)
- **Discard** → call `DELETE /api/v1/seller/product/discard-draft-product`, then allow new product creation

### Step → Screen Routing

```js
const stepRoute = {
  "PRODUCT_NAME":      "/seller/add-product/step-1",
  "PRODUCT_ATTRIBUTE": "/seller/add-product/step-2",
  "PRODUCT_VARIANT":   "/seller/add-product/step-3",
  "PRODUCT_IMAGE":     "/seller/add-product/step-4",
  "CATALOG_SELECTED":  "/seller/add-product/step-3",  // skip to variants
};
// If currentStep is beyond a step, pre-fill that step's UI with data from the response
```

---

## Step 1 — Basic Info

```
POST /api/v1/seller/product/create
Content-Type: multipart/form-data

name        = "Men's Solid T-Shirt"
description = "100% cotton"
category    = "c1234-..."
step        = "PRODUCT_NAME"
```

**Success →** store `productId` from response in local state.

**If you get `409` here**, the seller already has a draft — show the resume/discard dialog.

---

## Step 2 — Attributes

```
POST /api/v1/seller/product/create-product-attribute
Content-Type: application/json

{
  "productId": "<from step 1>",
  "attributes": [
    { "categoryAttributeId": "33e1e2c4-...", "value": "Red" },
    { "categoryAttributeId": "05d394b2-...", "value": "XXS" }
  ]
}
```

**On resume:** `data.attributes` from the full draft response already contains all saved attribute rows. Pre-fill the form with those values.

---

## Step 3 — Variants

```
POST /api/v1/seller/product/add-variants
Content-Type: application/json

{
  "productId": "<productId>",
  "variants": [
    {
      "combination": { "Size": "XXS" },
      "label": "XXS",
      "price": 100,
      "mrp": 150,
      "stock": 10,
      "sku": "PRD-XXS-001"
    }
  ]
}
```

**On resume:** `data.variants` contains all saved variants. Pre-fill the table.

---

## Step 4 — Images & Media

```
POST /api/v1/seller/product/upload-images
Content-Type: multipart/form-data

productId             = "<productId>"
images                = <cover image file>           (single file — the main listing image)
attributeImageKeys    = "33e1e2c4-...::Red"          (repeated, one per attributeImages file)
attributeImages       = <binary file>                (repeated, same order as attributeImageKeys)
```

### Rules
- `images` = the cover/primary image shown in listings (1 file)
- `attributeImages[i]` maps to `attributeImageKeys[i]`
- Key format: `{categoryAttributeId}::{attributeValue}`
- Calling this endpoint again with the same `productId` **replaces** all media (idempotent)
- Attribute images only accepted for attributes where `isImageAttribute = true`

**On resume:** `data.media.coverImageUrl` and `data.media.attributeMedia` contain all uploaded URLs. Show existing images with a delete button.

**Success response:**
```json
{
  "success": true,
  "data": {
    "productId": "...",
    "coverImageUrl": "https://cdn.../cover.png",
    "attributeMedia": {
      "Red": ["https://cdn.../red1.png", "https://cdn.../red2.png"],
      "Blue": ["https://cdn.../blue1.png"]
    }
  }
}
```

### Remove a specific media file

```
DELETE /api/v1/seller/product/media/{mediaId}
```

### Change the cover image

```
PATCH /api/v1/seller/product/media/{mediaId}/set-cover
```

---

## Step 5 — Tags & Brand

These can be done in any order. Both are optional but recommended.

**Add tags:**
```
POST /api/v1/seller/product/add-tag
Content-Type: application/json

{ "productId": "<productId>", "tags": ["casual", "summer"] }
```

**Attach brand:**
```
POST /api/v1/seller/product/attach-brand?productId=<productId>&brandId=<brandId>
```

**On resume:** `data.tags` and `data.brand` are pre-populated.

---

## Step 6 — Go Live

```
GET /api/v1/seller/product/make-product-live/{productId}
```

After a successful response the product is live and the seller can create a new product.

---

## Error Handling Cheatsheet

| HTTP / field             | Meaning                                 | Action                                 |
|--------------------------|-----------------------------------------|----------------------------------------|
| `success: false, 409`    | Draft already exists                    | Show resume/discard dialog             |
| `success: false, 400`    | Validation error (file type, count etc) | Show `message` to user                 |
| `success: false, 500`    | Server error                            | Show generic error, allow retry        |
| `data: null` on draft    | No draft exists                         | Allow new product creation             |

---

## Discard Draft

```
DELETE /api/v1/seller/product/discard-draft-product
Authorization: Bearer <token>
```

Permanently deletes the draft product and all associated data (attributes, variants, media). The seller can then start fresh.

---

## Full API Base URL

```
http://localhost:8081/api/v1/seller/product
```

All endpoints require `Authorization: Bearer <seller-jwt-token>` and the seller must have role `SELLER`.
