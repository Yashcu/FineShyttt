-- Add timestamp columns to cart_items table
ALTER TABLE cart_items
    ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add index for common queries
CREATE INDEX idx_cart_items_created_at ON cart_items(created_at);
