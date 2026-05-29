-- Add export_file_url column to declaration_form table
ALTER TABLE declaration_form ADD COLUMN export_file_url VARCHAR(500) DEFAULT NULL COMMENT '自动生成的导出文件路径';
