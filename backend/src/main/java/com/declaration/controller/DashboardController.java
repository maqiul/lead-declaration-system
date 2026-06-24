package com.declaration.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.declaration.common.Result;
import com.declaration.entity.DeclarationForm;
import com.declaration.entity.TaxRefundApplication;
import com.declaration.entity.User;
import com.declaration.service.DeclarationFormService;
import com.declaration.service.TaxRefundApplicationService;
import com.declaration.service.TaskService;
import com.declaration.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工作台/大屏统计指标控制器
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "工作台统计")
public class DashboardController {

    private final DeclarationFormService declarationFormService;
    private final TaxRefundApplicationService taxRefundApplicationService;
    private final TaskService taskService;
    private final UserService userService;

    @GetMapping("/stats")
    @Operation(summary = "获取工作台统计卡片数据")
    public Result<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 1. 用户总数（这个不需要权限过滤）
        long userCount = userService.count();
        stats.put("userCount", userCount);

        // 2. 流程实例数 - 根据权限过滤
        LambdaQueryWrapper<DeclarationForm> declarationWrapper = new LambdaQueryWrapper<>();
        declarationWrapper.ne(DeclarationForm::getStatus, 0); // 排除草稿
        applyDeclarationDataPermission(declarationWrapper);
        long processInstances = declarationFormService.count(declarationWrapper);
        stats.put("processInstanceCount", processInstances);

        // 3. 待办任务数 (当前用户的待办任务)
        long pendingTasks = taskService.getRunningTasksCount();
        stats.put("pendingTaskCount", pendingTasks);

        // 4. 今日新增 - 根据权限过滤
        LambdaQueryWrapper<DeclarationForm> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(DeclarationForm::getCreateTime, LocalDate.now().atStartOfDay());
        applyDeclarationDataPermission(todayWrapper);
        long todayNew = declarationFormService.count(todayWrapper);
        stats.put("todayNewCount", todayNew);

        return Result.success(stats);
    }
    
    @GetMapping("/charts")
    @Operation(summary = "获取工作台图表数据")
    public Result<Map<String, Object>> getDashboardCharts() {
        Map<String, Object> chartsData = new HashMap<>();

        // --- 流程类型统计 - 根据权限过滤 ---
        List<String> processNames = new ArrayList<>();
        List<Long> processCounts = new ArrayList<>();

        // 出口申报流程 - 根据权限过滤
        LambdaQueryWrapper<DeclarationForm> declarationWrapper = new LambdaQueryWrapper<>();
        declarationWrapper.ne(DeclarationForm::getStatus, 0);
        applyDeclarationDataPermission(declarationWrapper);
        long declarationCount = declarationFormService.count(declarationWrapper);
        processNames.add("出口申报");
        processCounts.add(declarationCount);

        // 退税申请流程 - 根据权限过滤
        LambdaQueryWrapper<TaxRefundApplication> taxRefundWrapper = new LambdaQueryWrapper<>();
        taxRefundWrapper.ne(TaxRefundApplication::getStatus, 0);
        applyTaxRefundDataPermission(taxRefundWrapper);
        long taxRefundCount = taxRefundApplicationService.count(taxRefundWrapper);
        processNames.add("退税申请");
        processCounts.add(taxRefundCount);

        Map<String, Object> barChart = new HashMap<>();
        barChart.put("categories", processNames);
        barChart.put("seriesData", processCounts);
        chartsData.put("processChart", barChart);


        // --- 待办任务分布 - 根据权限过滤 ---
        Map<String, Object> pieChart = new HashMap<>();

        // 待审核 - 根据权限过滤（待初审 1 + 待资料审核 3 + 退回待审 11）
        LambdaQueryWrapper<DeclarationForm> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.in(DeclarationForm::getStatus, Arrays.asList(1, 3, 11));
        applyDeclarationDataPermission(pendingWrapper);
        long pendingAudit = declarationFormService.count(pendingWrapper);
        
        LambdaQueryWrapper<TaxRefundApplication> taxPendingWrapper = new LambdaQueryWrapper<>();
        taxPendingWrapper.eq(TaxRefundApplication::getStatus, 1);
        applyTaxRefundDataPermission(taxPendingWrapper);
        pendingAudit += taxRefundApplicationService.count(taxPendingWrapper);

        // 处理中 - 根据权限过滤（待资料提交 2）
        LambdaQueryWrapper<DeclarationForm> processingWrapper = new LambdaQueryWrapper<>();
        processingWrapper.in(DeclarationForm::getStatus, Arrays.asList(2));
        applyDeclarationDataPermission(processingWrapper);
        long processing = declarationFormService.count(processingWrapper);
        
        LambdaQueryWrapper<TaxRefundApplication> taxProcessingWrapper = new LambdaQueryWrapper<>();
        taxProcessingWrapper.in(TaxRefundApplication::getStatus, Arrays.asList(2, 4, 6));
        applyTaxRefundDataPermission(taxProcessingWrapper);
        processing += taxRefundApplicationService.count(taxProcessingWrapper);

        // 已完成 - 根据权限过滤（资料审核通过 4）
        LambdaQueryWrapper<DeclarationForm> completedWrapper = new LambdaQueryWrapper<>();
        completedWrapper.eq(DeclarationForm::getStatus, 4);
        applyDeclarationDataPermission(completedWrapper);
        long completed = declarationFormService.count(completedWrapper);
        
        LambdaQueryWrapper<TaxRefundApplication> taxCompletedWrapper = new LambdaQueryWrapper<>();
        taxCompletedWrapper.in(TaxRefundApplication::getStatus, Arrays.asList(7, 8));
        applyTaxRefundDataPermission(taxCompletedWrapper);
        completed += taxRefundApplicationService.count(taxCompletedWrapper);

        List<Map<String, Object>> pieData = new ArrayList<>();
        pieData.add(createPieItem(pendingAudit, "等待审核"));
        pieData.add(createPieItem(processing, "处理中/待传单据"));
        pieData.add(createPieItem(completed, "已完成"));

        pieChart.put("seriesData", pieData);
        chartsData.put("taskPieChart", pieChart);

        return Result.success(chartsData);
    }
    
    private Map<String, Object> createPieItem(long value, String name) {
        Map<String, Object> item = new HashMap<>();
        item.put("value", value);
        item.put("name", name);
        return item;
    }

    /**
     * 为申报单查询应用数据权限过滤
     */
    private void applyDeclarationDataPermission(LambdaQueryWrapper<DeclarationForm> wrapper) {
        if (!StpUtil.isLogin()) {
            // 未登录，返回空条件（会查询不到数据）
            wrapper.eq(DeclarationForm::getId, -1);
            return;
        }

        Long userId = StpUtil.getLoginIdAsLong();
        
        // 管理员或有审批权限的用户可以查看所有数据
        boolean hasApprovePermission = StpUtil.hasPermission("business:declaration:audit");
        if (userId == 1L || hasApprovePermission) {
            // 不做过滤，查看所有数据
            return;
        }

        // 普通用户只能查看自己创建的或本组织的数据
        User currentUser = userService.getById(userId);
        if (currentUser != null && currentUser.getOrgId() != null) {
            // 查看自己创建的 或 本组织的数据
            wrapper.and(w -> w.eq(DeclarationForm::getCreateBy, userId)
                    .or().eq(DeclarationForm::getOrgId, currentUser.getOrgId()));
        } else {
            // 用户没有组织，只能看自己创建的
            wrapper.eq(DeclarationForm::getCreateBy, userId);
        }
    }

    /**
     * 为退税申请查询应用数据权限过滤
     */
    private void applyTaxRefundDataPermission(LambdaQueryWrapper<TaxRefundApplication> wrapper) {
        if (!StpUtil.isLogin()) {
            // 未登录，返回空条件
            wrapper.eq(TaxRefundApplication::getId, -1);
            return;
        }

        Long userId = StpUtil.getLoginIdAsLong();
        
        // 管理员或有审批权限的用户可以查看所有数据
        boolean hasApprovePermission = StpUtil.hasPermission("business:tax-refund:approve");
        if (userId == 1L || hasApprovePermission) {
            // 不做过滤，查看所有数据
            return;
        }

        // 普通用户只能查看自己创建的或本组织的数据
        User currentUser = userService.getById(userId);
        if (currentUser != null && currentUser.getOrgId() != null) {
            // 查看自己创建的 或 本组织的数据
            wrapper.and(w -> w.eq(TaxRefundApplication::getInitiatorId, userId)
                    .or().eq(TaxRefundApplication::getOrgId, currentUser.getOrgId()));
        } else {
            // 用户没有组织，只能看自己创建的
            wrapper.eq(TaxRefundApplication::getInitiatorId, userId);
        }
    }

    /**
     * 申报菜单统计 + 30天预警
     */
    @GetMapping("/declaration-stats")
    @Operation(summary = "获取申报菜单统计和30天预警数据")
    public Result<Map<String, Object>> getDeclarationStats() {
        Map<String, Object> result = new HashMap<>();

        // 菜单统计配置：菜单名、路由路径、状态过滤、图标
        List<List<Integer>> statusFilters = Arrays.asList(
                Arrays.asList(1, 11),    // 申报录入
                Arrays.asList(2, 3),     // 资料提交
                Arrays.asList(4, 5),     // 补充资料
                Arrays.asList(6, 7),     // 开票金额
                Arrays.asList(8, 9),     // 发票提交
                Arrays.asList(10)        // 归档查询
        );
        String[] menuNames = {"申报录入", "资料提交", "补充资料", "开票金额", "发票提交", "归档查询"};
        String[] menuPaths = {"/declaration/entry", "/declaration/material", "/declaration/supplement",
                "/declaration/invoice-amount", "/declaration/invoice", "/declaration/archive"};
        String[] menuIcons = {"EditOutlined", "UploadOutlined", "FileSearchOutlined",
                "DollarOutlined", "FileTextOutlined", "FolderOpenOutlined"};
        String[] menuThemes = {"blue", "green", "orange", "purple", "cyan", "default"};

        List<Map<String, Object>> menuStats = new ArrayList<>();
        for (int i = 0; i < menuNames.length; i++) {
            LambdaQueryWrapper<DeclarationForm> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(DeclarationForm::getStatus, statusFilters.get(i));
            applyDeclarationDataPermission(wrapper);
            long count = declarationFormService.count(wrapper);

            Map<String, Object> item = new HashMap<>();
            item.put("menuName", menuNames[i]);
            item.put("path", menuPaths[i]);
            item.put("icon", menuIcons[i]);
            item.put("theme", menuThemes[i]);
            item.put("count", count);
            menuStats.add(item);
        }
        result.put("menuStats", menuStats);

        // 30天预警：创建时间距今≥30天且未完成(status != 10)且非草稿(status != 0)的记录
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LambdaQueryWrapper<DeclarationForm> warningWrapper = new LambdaQueryWrapper<>();
        warningWrapper.lt(DeclarationForm::getCreateTime, thirtyDaysAgo);
        warningWrapper.ne(DeclarationForm::getStatus, 10);
        warningWrapper.ne(DeclarationForm::getStatus, 0);
        applyDeclarationDataPermission(warningWrapper);
        warningWrapper.orderByAsc(DeclarationForm::getCreateTime);

        List<DeclarationForm> warningList = declarationFormService.list(warningWrapper);

        // 批量查询申报人姓名
        List<Long> userIds = warningList.stream()
                .map(DeclarationForm::getCreateBy)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> userNameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userService.listByIds(userIds);
            userNameMap.putAll(users.stream().collect(Collectors.toMap(
                    User::getId,
                    u -> u.getNickname() != null ? u.getNickname() : u.getUsername(),
                    (a, b) -> a)));
        }

        List<Map<String, Object>> warningItems = warningList.stream().map(form -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", form.getId());
            item.put("formNo", form.getFormNo());
            item.put("shipperCompany", form.getShipperCompany());
            item.put("status", form.getStatus());
            item.put("createTime", form.getCreateTime());
            item.put("totalAmount", form.getTotalAmount());
            item.put("destinationCountry", form.getDestinationCountry());
            item.put("declarantName", form.getCreateBy() != null
                    ? userNameMap.getOrDefault(form.getCreateBy(), "未知用户") : "未知用户");
            return item;
        }).collect(Collectors.toList());

        result.put("warningCount", warningItems.size());
        result.put("warningList", warningItems);

        return Result.success(result);
    }
}
