-- ============================================
-- 清理重复附件记录脚本
-- 问题：同一申报单同一文件类型存在多条记录
-- 方案：每个 form_id + file_type 只保留最新一条（id最大）
-- ============================================

-- 1. 先查看重复数据情况
SELECT form_id, file_type, COUNT(*) as cnt 
FROM declaration_attachment 
WHERE deleted = 0 
GROUP BY form_id, file_type 
HAVING cnt > 1;

-- 2. 查看将被删除的记录（预览）
SELECT a.* 
FROM declaration_attachment a
INNER JOIN (
    SELECT form_id, file_type, MAX(id) as max_id
    FROM declaration_attachment
    WHERE deleted = 0
    GROUP BY form_id, file_type
    HAVING COUNT(*) > 1
) b ON a.form_id = b.form_id AND a.file_type = b.file_type AND a.id < b.max_id
WHERE a.deleted = 0;

-- 3. 执行删除（软删除）- 只保留每个 form_id + file_type 最新的一条
UPDATE declaration_attachment a
INNER JOIN (
    SELECT form_id, file_type, MAX(id) as max_id
    FROM declaration_attachment
    WHERE deleted = 0
    GROUP BY form_id, file_type
    HAVING COUNT(*) > 1
) b ON a.form_id = b.form_id AND a.file_type = b.file_type AND a.id < b.max_id
SET a.deleted = 1
WHERE a.deleted = 0;

-- 4. 如果需要物理删除（谨慎使用）
-- DELETE a FROM declaration_attachment a
-- INNER JOIN (
--     SELECT form_id, file_type, MAX(id) as max_id
--     FROM declaration_attachment
--     WHERE deleted = 0
--     GROUP BY form_id, file_type
--     HAVING COUNT(*) > 1
-- ) b ON a.form_id = b.form_id AND a.file_type = b.file_type AND a.id < b.max_id;

-- 5. 验证清理结果
SELECT form_id, file_type, COUNT(*) as cnt 
FROM declaration_attachment 
WHERE deleted = 0 
GROUP BY form_id, file_type 
HAVING cnt > 1;
