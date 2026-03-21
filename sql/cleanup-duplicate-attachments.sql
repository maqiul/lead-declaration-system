-- 清理重复的提货单附件记录
-- 保留file_type='PickupList'的记录，删除file_type='Common'的重复记录

DELETE da1 FROM declaration_attachment da1
INNER JOIN declaration_attachment da2 
WHERE da1.id > da2.id 
AND da1.file_url = da2.file_url 
AND da1.form_id = da2.form_id
AND da1.file_type = 'Common'
AND da2.file_type = 'PickupList';

-- 验证清理结果
SELECT * FROM declaration_attachment 
WHERE file_type IN ('Common', 'PickupList')
ORDER BY form_id, file_url, id;