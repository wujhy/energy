-- 业务运行日志增加最后进展时间，watchdog 超时按无进展时长判断。
ALTER TABLE dev_opt_log ADD COLUMN last_progress_at TEXT;

-- 存量运行中日志回填，避免迁移后立即被视为无进展。
UPDATE dev_opt_log
SET last_progress_at = COALESCE(update_time, create_time)
WHERE result IS NULL AND type != 99;
