-- Update meta_data column to support larger JSON data
ALTER TABLE item_entity MODIFY COLUMN meta_data TEXT;