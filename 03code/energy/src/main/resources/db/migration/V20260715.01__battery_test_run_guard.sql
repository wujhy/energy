-- Keep only the newest legacy active run per device and battery group.
UPDATE dev_opt_log AS current
SET result = 2,
    status = 'interrupted',
    ended_at = strftime('%Y-%m-%d %H:%M:%S', 'now', 'localtime'),
    update_time = strftime('%Y-%m-%d %H:%M:%S', 'now', 'localtime')
WHERE current.result IS NULL
  AND current.type != 99
  AND EXISTS (
      SELECT 1
      FROM dev_opt_log AS newer
      WHERE newer.config_id = current.config_id
        AND newer.pack_num = current.pack_num
        AND newer.result IS NULL
        AND newer.type != 99
        AND newer.id > current.id
  );

CREATE UNIQUE INDEX IF NOT EXISTS uk_dev_opt_log_active_pack
    ON dev_opt_log (config_id, pack_num)
    WHERE result IS NULL AND type != 99;
