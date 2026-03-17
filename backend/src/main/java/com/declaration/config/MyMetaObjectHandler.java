package com.declaration.config;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充配置
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("开始插入填充...");
        
        // 创建时间
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        // 更新时间
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        
        // 创建人和更新人（如果有登录用户）
        try {
            if (StpUtil.isLogin()) {
                Long userId = StpUtil.getLoginIdAsLong();
                this.strictInsertFill(metaObject, "createBy", Long.class, userId);
                this.strictInsertFill(metaObject, "updateBy", Long.class, userId);
                
                // 填充所属组织ID
                // 注意：这里需要从缓存或数据库获取用户的 orgId
                // 简化起见，假设 StpUtil 扩展了获取 orgId 的方法或者从 session 获取
                Object orgId = StpUtil.getSession().get("orgId");
                if (orgId != null) {
                    this.strictInsertFill(metaObject, "orgId", Long.class, Long.valueOf(orgId.toString()));
                }
            }
        } catch (Exception e) {
            log.debug("获取登录用户信息失败: {}", e.getMessage());
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("开始更新填充...");
        
        // 更新时间
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        
        // 更新人（如果有登录用户）
        try {
            if (StpUtil.isLogin()) {
                Long userId = StpUtil.getLoginIdAsLong();
                this.strictUpdateFill(metaObject, "updateBy", Long.class, userId);
            }
        } catch (Exception e) {
            log.debug("获取登录用户信息失败: {}", e.getMessage());
        }
    }
}