package com.declaration.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.common.PageParam;
import com.declaration.dao.UserDao;
import com.declaration.entity.User;
import com.declaration.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserDao, User> implements UserService {

    @Override
    public IPage<User> getUserPage(PageParam pageParam, User user) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (user != null) {
            if (StrUtil.isNotBlank(user.getUsername())) {
                queryWrapper.like(User::getUsername, user.getUsername());
            }
            if (StrUtil.isNotBlank(user.getNickname())) {
                queryWrapper.like(User::getNickname, user.getNickname());
            }
            if (StrUtil.isNotBlank(user.getPhone())) {
                queryWrapper.like(User::getPhone, user.getPhone());
            }
            if (user.getStatus() != null) {
                queryWrapper.eq(User::getStatus, user.getStatus());
            }
        }
        
        // 排序
        queryWrapper.orderByDesc(User::getCreateTime);
        
        // 分页查询
        Page<User> page = new Page<>(pageParam.getCurrent(), pageParam.getSize());
        return this.page(page, queryWrapper);
    }

    @Override
    public User getByUsername(String username) {
        return this.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    public String login(String username, String password) {
        // 查询用户
        User user = this.getByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        if (user.getStatus() == 0) {
            throw new RuntimeException("用户已被禁用");
        }
        
        // 验证密码（这里应该使用加密后的密码比较）
        String encryptedPassword = DigestUtil.md5Hex(password);
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        
        // 登录并返回token
        StpUtil.login(user.getId());
        return StpUtil.getTokenValue();
    }

    @Override
    public boolean register(User user) {
        // 检查用户名是否已存在
        if (this.getByUsername(user.getUsername()) != null) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 密码加密
        user.setPassword(DigestUtil.md5Hex(user.getPassword()));
        user.setStatus(1); // 默认启用
        
        return this.save(user);
    }
}