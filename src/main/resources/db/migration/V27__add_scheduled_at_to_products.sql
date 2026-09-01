-- Products can now be scheduled for a future auto-publish instead of being made
-- live immediately (seller "Scheduled Products" tab). scheduled_at is set once
-- the wizard is complete (step PRODUCT_BRAND_AND_TAGS / CATALOG_SELECTED) and is
-- cleared once the product actually goes LIVE (whether by the cron job or a
-- manual "Publish now").
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS scheduled_at TIMESTAMPTZ;

-- Supports the due-scheduled-products cron sweep (WHERE scheduled_at <= now AND step <> 5)
-- and the seller-scoped "Scheduled Products" tab listing (WHERE seller_id = ? AND scheduled_at IS NOT NULL).
CREATE INDEX IF NOT EXISTS idx_products_scheduled_at
    ON products (scheduled_at)
    WHERE scheduled_at IS NOT NULL;
