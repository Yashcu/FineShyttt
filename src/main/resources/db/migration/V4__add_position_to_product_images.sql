-- Add position column to product_images table
ALTER TABLE product_images
    ADD COLUMN IF NOT EXISTS position INTEGER NOT NULL DEFAULT 0;

-- Update existing rows to have sequential positions
UPDATE product_images
SET position = 0
WHERE position IS NULL;
