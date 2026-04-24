# Reviews & Ratings API — Frontend Integration Guide

Base URL: `http://localhost:8081`

---

## Table of Contents

1. [Authentication](#1-authentication)
2. [Product Detail (updated)](#2-product-detail-updated)
3. [Submit / Update a Review](#3-submit--update-a-review)
4. [Get Paginated Reviews](#4-get-paginated-reviews)
5. [Rating Summary (Histogram)](#5-rating-summary-histogram)
6. [Toggle Helpful Vote](#6-toggle-helpful-vote)
7. [Delete a Review](#7-delete-a-review)
8. [Error Response Shape](#8-error-response-shape)
9. [UI Rendering Guide](#9-ui-rendering-guide)

---

## 1. Authentication

Protected endpoints require a JWT token in the `Authorization` header.

```
Authorization: Bearer <token>
```

| Endpoint | Auth required |
|----------|--------------|
| GET product detail | No |
| GET reviews | No |
| GET rating summary | No |
| POST submit review | **Yes** |
| POST helpful | **Yes** |
| DELETE review | **Yes** |

---

## 2. Product Detail (updated)

The product detail endpoint now fetches data from **Elasticsearch** (no DB join) and automatically attaches a **rating summary** to every response.

### Request

```
GET /api/v1/product/{productId}
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `productId` | UUID (path) | Yes | Product UUID |

### Response

```json
{
  "success": true,
  "message": "Get Product Detail",
  "statusCode": 200,
  "data": {
    "product": {
      "product_id": "ad4f03c9-1b2e-420f-bbf6-e848771d86a1",
      "name": "Cotton Oversized T-Shirt",
      "description": "...",
      "brand_name": "H&M",
      "category_name": "Men > Tops > T-Shirts",
      "min_price_paise": 79900,
      "original_price_paise": 129900,
      "discount_percent": 38,
      "avg_rating": 3.8,
      "review_count": 13728,
      "in_stock": true,
      "images": [
        "https://res.cloudinary.com/demo/image/upload/v1/product1.jpg"
      ],
      "attributes": [
        { "name": "Fabric", "value": "100% Cotton" },
        { "name": "Fit", "value": "Oversized" }
      ],
      "created_at": "2024-11-01T10:00:00Z"
    },
    "ratingSummary": {
      "averageRating": 3.8,
      "totalRatings": 13728,
      "distribution": {
        "5": 5785,
        "4": 3253,
        "3": 2251,
        "2": 984,
        "1": 1455
      },
      "verifiedCount": 0,
      "withImagesCount": 0
    }
  }
}
```

> **Note on prices:** All prices are in **paise** (1 INR = 100 paise). Divide by 100 to display in rupees.

---

## 3. Submit / Update a Review

One review per user per product. Calling this endpoint again overwrites the existing review.

### Request

```
POST /api/v1/reviews/{productId}
Content-Type: multipart/form-data
Authorization: Bearer <token>
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `rating` | integer (form) | Yes | 1–5 |
| `title` | string (form) | No | max 100 chars |
| `review` | string (form) | No | max 2000 chars |
| `images` | file[] (multipart) | No | max 5 files, any image format |

### Example — `fetch`

```js
const formData = new FormData();
formData.append('rating', '4');
formData.append('title', 'Great quality!');
formData.append('review', 'Fits perfectly and fabric is very soft.');
imageFiles.forEach(file => formData.append('images', file));

const res = await fetch(`/api/v1/reviews/${productId}`, {
  method: 'POST',
  headers: { Authorization: `Bearer ${token}` },
  body: formData,
});
const data = await res.json();
```

### Example — `axios`

```js
const formData = new FormData();
formData.append('rating', rating);
formData.append('title', title);
formData.append('review', reviewText);
imageFiles.forEach(file => formData.append('images', file));

const { data } = await axios.post(`/api/v1/reviews/${productId}`, formData, {
  headers: {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'multipart/form-data',
  },
});
```

### Response (201)

```json
{
  "success": true,
  "message": "Review submitted successfully",
  "statusCode": 201,
  "data": {
    "id": "b2f1c3d4-...",
    "productId": "ad4f03c9-...",
    "rating": 4,
    "title": "Great quality!",
    "review": "Fits perfectly and fabric is very soft.",
    "reviewImages": [
      "https://res.cloudinary.com/demo/image/upload/v1/review1.jpg"
    ],
    "helpfulCount": 0,
    "verifiedPurchase": false,
    "createdAt": "2026-04-21T10:30:00+05:30",
    "user": {
      "id": "usr-uuid",
      "name": "Rahul Sharma",
      "image": "https://..."
    }
  }
}
```

> On **update**, `message` becomes `"Review updated successfully"`.

---

## 4. Get Paginated Reviews

### Request

```
GET /api/v1/reviews/{productId}?page=0&size=10&sort=newest
```

| Query param | Default | Options |
|-------------|---------|---------|
| `page` | `0` | 0-indexed |
| `size` | `10` | max 50 |
| `sort` | `newest` | `newest`, `helpful`, `highest`, `lowest` |

### Response (200)

```json
{
  "success": true,
  "message": "Reviews fetched",
  "statusCode": 200,
  "data": {
    "reviews": [
      {
        "id": "b2f1c3d4-...",
        "productId": "ad4f03c9-...",
        "rating": 5,
        "title": "Best in the market!",
        "review": "Omg I'm in love with this top, this looks so good",
        "reviewImages": [],
        "helpfulCount": 23,
        "verifiedPurchase": true,
        "createdAt": "2024-04-10T14:22:00+05:30",
        "user": {
          "id": "usr-uuid",
          "name": "Juhi Bhattacharya",
          "image": "https://..."
        }
      }
    ],
    "totalElements": 13728,
    "totalPages": 1373,
    "page": 0,
    "hasMore": true
  }
}
```

---

## 5. Rating Summary (Histogram)

Use this on the **dedicated reviews page** or to render the ratings histogram widget without re-fetching the full product.

### Request

```
GET /api/v1/reviews/{productId}/summary
```

### Response (200)

```json
{
  "success": true,
  "message": "Rating summary fetched",
  "statusCode": 200,
  "data": {
    "averageRating": 3.8,
    "totalRatings": 13728,
    "distribution": {
      "5": 5785,
      "4": 3253,
      "3": 2251,
      "2": 984,
      "1": 1455
    },
    "verifiedCount": 0,
    "withImagesCount": 0
  }
}
```

### Rendering the bar chart

```js
// percentage width for each star bar
const barWidth = (star) =>
  summary.totalRatings > 0
    ? (summary.distribution[star] / summary.totalRatings) * 100
    : 0;

// e.g. barWidth(5) → 42.1  → use as CSS width %
```

---

## 6. Toggle Helpful Vote

Calling this endpoint on a review the user **has not voted** → adds vote.
Calling it again (already voted) → removes vote. Fully idempotent toggle.

### Request

```
POST /api/v1/reviews/{reviewId}/helpful
Authorization: Bearer <token>
```

### Response (200)

```json
{
  "success": true,
  "message": "Marked as helpful",
  "statusCode": 200,
  "data": { "action": "ADDED" }
}
```

```json
{
  "success": true,
  "message": "Removed helpful vote",
  "statusCode": 200,
  "data": { "action": "REMOVED" }
}
```

> Update `helpfulCount` on the review card optimistically by +1 / -1 based on `action`.

---

## 7. Delete a Review

Only the review's author can delete it. Returns 403 if another user attempts.

### Request

```
DELETE /api/v1/reviews/{reviewId}
Authorization: Bearer <token>
```

### Response (200)

```json
{
  "success": true,
  "message": "Review deleted",
  "statusCode": 200,
  "data": null
}
```

---

## 8. Error Response Shape

All errors follow the same envelope:

```json
{
  "success": false,
  "message": "Product not found",
  "statusCode": 404,
  "data": null
}
```

| Status | Meaning |
|--------|---------|
| 400 | Validation error (e.g. rating out of 1-5 range) |
| 401 | Missing or expired JWT token |
| 403 | Trying to delete someone else's review |
| 404 | Product or review not found |
| 201 | Review created/updated |
| 200 | Successful read / helpful toggle / delete |

---

## 9. UI Rendering Guide

### Product Detail Page

```
┌─────────────────────────────────┐
│  product.name                   │
│  ★ avg_rating  (total_ratings)  │  ← from data.ratingSummary
│                                 │
│  5★ ████████████████░░  42%     │
│  4★ ████████████░░░░░░  24%     │  ← distribution bars
│  3★ ████████░░░░░░░░░░  16%     │
│  2★ ████░░░░░░░░░░░░░░   7%     │
│  1★ █████░░░░░░░░░░░░░  11%     │
└─────────────────────────────────┘
```

### Review Card

```
┌───────────────────────────────────────────┐
│ [Avatar] Juhi Bhattacharya  ✓ Verified    │
│          ★★★★★  Best in the market!       │
│          2 years ago                      │
│                                           │
│  Omg I'm in love with this top...         │
│                                           │
│  [img1] [img2]                            │
│                                           │
│  👍 Helpful (23)   🗑 Delete (own only)   │
└───────────────────────────────────────────┘
```

### Sort Tabs (Dedicated Reviews Page)

```jsx
const SORT_OPTIONS = [
  { label: 'Most Recent',  value: 'newest'  },
  { label: 'Most Helpful', value: 'helpful' },
  { label: 'Highest First',value: 'highest' },
  { label: 'Lowest First', value: 'lowest'  },
];
```

### Submit Review Form

```
Rating     ★ ★ ★ ★ ★  (tap to select)
Title      [ Great quality!              ]
Review     [ Fits perfectly...           ]
Photos     [ + Add photos ] (max 5)
           [     Submit Review           ]
```

> Use `Content-Type: multipart/form-data` — **not** `application/json` — because of image files.
> If the user has no images to upload, still send the form without the `images` field (do not send an empty array).

---

### Checking if current user already reviewed

When the review list loads, check if any review's `user.id` matches the logged-in user's ID. If yes:
- Show **Edit** (re-submit to the same POST endpoint — it will overwrite)
- Show **Delete** button
- Hide the "Write a Review" CTA
