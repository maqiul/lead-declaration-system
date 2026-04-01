package com.declaration.runner;

import com.declaration.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.Arrays;

/**
 * 缓存预热器
 * 在 Spring Boot 项目启动完成后，自动将核心字典数据加载进 Redis 缓存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CachePreloader implements ApplicationRunner {

    private final MeasurementUnitService measurementUnitService;
    private final CurrencyInfoService currencyInfoService;
    private final CountryInfoService countryInfoService;
    private final PaymentMethodService paymentMethodService;
    private final TransportModeService transportModeService;
    private final ProductTypeConfigService productTypeConfigService;
    private final BankAccountConfigService bankAccountConfigService;
    private final CacheManager cacheManager;

    @Override
    public void run(ApplicationArguments args) {
        log.info("============== 开始执行系统全局字典 Redis 缓存预热 ==============");
        refreshCache();
    }

    /**
     * 每天凌晨2点自动执行一次全量刷新，无需等待第一次点击
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledRefresh() {
        log.info("============== 定时任务触发：开始自动刷新系统全局字典 Redis 缓存 ==============");
        refreshCache();
    }

    private void refreshCache() {
        try {
            // 清除所有的原有缓存
            String[] cacheNames = {
                    "sys:dict:measurement-units",
                    "sys:dict:currencies",
                    "sys:dict:countries",
                    "sys:dict:payment-methods",
                    "sys:dict:transport-modes",
                    "sys:dict:product-types",
                    "sys:dict:bank-accounts"
            };
            for (String name : cacheNames) {
                Cache cache = cacheManager.getCache(name);
                if (cache != null) {
                    cache.clear();
                }
            }
            // 触发 AOP 代理的 @Cacheable 方法，重新将最新数据写回 Redis
            measurementUnitService.getActiveUnits();
            log.info("[1/7] 计量单位数据预热/刷新完成");

            currencyInfoService.getEnabledList();
            log.info("[2/7] 货币数据预热/刷新完成");

            countryInfoService.getEnabledList();
            log.info("[3/7] 国家数据预热/刷新完成");

            paymentMethodService.getEnabledList();
            log.info("[4/7] 支付方式预热/刷新完成");

            transportModeService.getEnabledList();
            log.info("[5/7] 运输方式预热/刷新完成");

            productTypeConfigService.getEnabledList();
            log.info("[6/7] HS商品类型预热/刷新完成");

            bankAccountConfigService.getEnabledList(null);
            log.info("[7/7] 打款账户预热/刷新完成");

            log.info("============== 字典缓存预热/定时刷新完毕！==============");
            
        } catch (Exception e) {
            log.error("缓存预热过程中发生异常: {}", e.getMessage(), e);
        }
    }
}
