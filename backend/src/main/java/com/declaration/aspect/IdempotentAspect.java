package com.declaration.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.declaration.annotation.Idempotent;
import com.declaration.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

/**
 * 分布式防重复提交切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private final RedissonClient redissonClient;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }
        HttpServletRequest request = attributes.getRequest();
            
        // 构造全局唯一的防重Key：前缀 + 用户ID + 接口URI
        String userId = "anonymous";
        try {
            if (StpUtil.isLogin()) {
                userId = StpUtil.getLoginIdAsString();
            }
        } catch (Exception e) {
            // 忽略未登录异常
        }
            
        String key = idempotent.prefix() + userId + ":" + request.getRequestURI();
            
        RLock lock = redissonClient.getLock(key);
        boolean isLocked = false;
        try {
            // tryLock 策略：0秒等待（拿不到锁立刻返回），锁定保护窗口为 expireTime 秒
            isLocked = lock.tryLock(0, idempotent.expireTime(), TimeUnit.SECONDS);
            if (!isLocked) {
                log.warn("触发防重复提交保护，短时间内重复的高并发写请求被精准拦截: {}", key);
                return Result.fail(idempotent.message());
            }
                
            // 执行目标业务代码
            Object result = joinPoint.proceed();
                
            // 业务执行完毕后释放锁，避免锁残留导致用户无法重试
            // 防重复点击的保护已在 tryLock 阶段完成（并发请求拿不到锁会立刻返回）
            unlockSafely(lock, key);
                
            return result;
                
        } catch (Throwable ex) {
            // 业务异常时也释放锁
            unlockSafely(lock, key);
            throw ex;
        }
    }
    
    private void unlockSafely(RLock lock, String key) {
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (Exception e) {
            log.debug("释放防重锁失败（可能已过期）: {}", key);
        }
    }
}
