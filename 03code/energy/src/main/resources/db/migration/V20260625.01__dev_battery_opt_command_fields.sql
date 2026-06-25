-- Add missing dev_battery_opt column for battery test plan scheduling.
ALTER TABLE dev_battery_opt ADD COLUMN model_num INTEGER;