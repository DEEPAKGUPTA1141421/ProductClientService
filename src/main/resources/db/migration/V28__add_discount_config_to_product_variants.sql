-- Seller-configurable, per-variant discount (percentage off MRP or flat amount
-- off MRP), toggleable and optionally schedulable, independent of price/stock.
-- discount_price / discount_percentage already existed as the derived/effective
-- values that buyer-facing search (SearchResultsRepository / ProductSearchRepository)
-- reads — this migration adds the raw seller-entered config columns that drive them.
ALTER TABLE product_variants
    ADD COLUMN IF NOT EXISTS discount_type varchar(20),
    ADD COLUMN IF NOT EXISTS discount_value varchar,
    ADD COLUMN IF NOT EXISTS discount_active boolean DEFAULT false,
    ADD COLUMN IF NOT EXISTS discount_starts_at timestamptz,
    ADD COLUMN IF NOT EXISTS discount_ends_at timestamptz;

-- Supports the scheduled sweep (WHERE discount_active = true AND (discount_starts_at
-- IS NOT NULL OR discount_ends_at IS NOT NULL)) run by DiscountScheduleService.
CREATE INDEX IF NOT EXISTS idx_product_variants_discount_schedule
    ON product_variants (discount_active)
    WHERE discount_active = true AND (discount_starts_at IS NOT NULL OR discount_ends_at IS NOT NULL);
