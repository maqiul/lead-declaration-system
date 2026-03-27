-- 为银行账户配置表添加银行账号+银行名称+币种的复合唯一约束
-- 首先删除可能存在的重复数据
DELETE t1 FROM bank_account_config t1
INNER JOIN bank_account_config t2 
WHERE 
    t1.id > t2.id 
    AND t1.account_number = t2.account_number 
    AND t1.bank_name = t2.bank_name 
    AND t1.currency = t2.currency
    AND t1.status = 1;  -- 只保留启用状态的记录

-- 添加复合唯一索引
ALTER TABLE `bank_account_config`
ADD UNIQUE INDEX `uk_account_bank_currency` (`account_number`, `bank_name`, `currency`);