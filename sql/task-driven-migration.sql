-- =====================================================
-- 任务驱动迁移脚本
-- 将申报系统从"状态驱动"改为"Flowable任务驱动"
-- =====================================================

-- 背景说明：
-- 并行阶段主状态始终为2（处理中），状态3/4/5被移除
-- 具体进度由Flowable活跃任务决定，而非数据库status字段

-- 将已有的 status 3/4/5 数据统一回退为 2（处理中）
UPDATE declaration_form SET status = 2 WHERE status IN (3, 4, 5);

-- 验证迁移结果（可选执行）
-- SELECT status, COUNT(*) as count FROM declaration_form GROUP BY status ORDER BY status;
