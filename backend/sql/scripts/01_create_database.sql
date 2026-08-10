-- 01 建库（单独执行，防止整个 init.sql 单包过大触发 max_allowed_packet 报错）
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS worklog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE worklog;
