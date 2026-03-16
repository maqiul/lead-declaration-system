package com.declaration.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.common.PageParam;
import com.declaration.entity.User;

/**
 * 用户服务接口
 *
 * @author Administrator
 * @since 2026-03-13
 */
public interface UserService extends IService<User> {

    /**
     * 分页查询用户列表
     *
     * @param pageParam 分页参数
     * @param user 查询条件
     * @return 用户分页数据
     */
    IPage<User> getUserPage(PageParam pageParam, User user);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    User getByUsername(String username);

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return token
     */
    String login(String username, String password);

    /**
     * 用户注册
     *
     * @param user 用户信息
     * @return 是否成功
     */
    boolean register(User user);
}