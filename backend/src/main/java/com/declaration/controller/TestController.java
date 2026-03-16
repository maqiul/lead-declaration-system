package com.declaration.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.declaration.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Slf4j
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Tag(name = "测试接口", description = "系统测试相关接口")
public class TestController {

    @GetMapping("/hello")
    @Operation(summary = "测试接口")
    public Result<String> hello() {
        return Result.success("Hello World! 线索申报系统运行正常！");
    }

    @GetMapping("/auth")
    @Operation(summary = "测试权限")
    public Result<Map<String, Object>> authTest() {
        Map<String, Object> data = new HashMap<>();
        data.put("isLogin", StpUtil.isLogin());
        data.put("loginId", StpUtil.getLoginIdDefaultNull());
        data.put("tokenInfo", StpUtil.getTokenInfo());
        return Result.success(data);
    }

    @GetMapping("/exception")
    @Operation(summary = "测试异常处理")
    public Result<String> exceptionTest() {
        throw new RuntimeException("测试异常处理功能");
    }
}