package com.declaration.controller;

import com.declaration.common.Result;
import com.declaration.entity.DeclarationForm;
import com.declaration.service.DeclarationFormService;
import com.declaration.service.TaskService;
import com.declaration.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作台/大屏统计指标控制器
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "工作台统计")
public class DashboardController {

    private final DeclarationFormService declarationFormService;
    private final TaskService taskService;
    private final UserService userService;

    @GetMapping("/stats")
    @Operation(summary = "获取工作台统计卡片数据")
    public Result<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 1. 用户总数
        long userCount = userService.count();
        stats.put("userCount", userCount);

        // 2. 流程实例数 (真实运行+历史总合)
        // 这里简化读取 declaration_form 所有走过流程的单子
        long processInstances = declarationFormService.lambdaQuery()
                .ne(DeclarationForm::getStatus, 0)
                .count();
        stats.put("processInstanceCount", processInstances);

        // 3. 待办任务数 (当前系统的运行任务)
        long pendingTasks = taskService.getRunningTasksCount();
        stats.put("pendingTaskCount", pendingTasks);

        // 4. 今日新增 (简单统计今天创建的申报单)
        long todayNew = declarationFormService.lambdaQuery()
                .ge(DeclarationForm::getCreateTime, LocalDate.now().atStartOfDay())
                .count();
        stats.put("todayNewCount", todayNew);

        return Result.success(stats);
    }
    
    @GetMapping("/charts")
    @Operation(summary = "获取工作台图表数据")
    public Result<Map<String, Object>> getDashboardCharts() {
        Map<String, Object> chartsData = new HashMap<>();
        
        // --- 流程类型统计 ---
        List<String> processNames = new ArrayList<>();
        List<Long> processCounts = new ArrayList<>();
        
        // 真实查询：我们目前只有出口退税申报一种主力流程
        long declarationCount = declarationFormService.lambdaQuery()
                .ne(DeclarationForm::getStatus, 0).count();
                
        processNames.add("出口退税审批");
        processCounts.add(declarationCount);
        
        Map<String, Object> barChart = new HashMap<>();
        barChart.put("categories", processNames);
        barChart.put("seriesData", processCounts);
        chartsData.put("processChart", barChart);
        
        
        // --- 待办任务分布 ---
        Map<String, Object> pieChart = new HashMap<>();
        // 从真实单据状态提取分布情况
        long pendingAudit = declarationFormService.lambdaQuery().in(DeclarationForm::getStatus, Arrays.asList(1, 3, 5, 7)).count();
        long processing = declarationFormService.lambdaQuery().in(DeclarationForm::getStatus, Arrays.asList(2, 4, 6)).count();
        long completed = declarationFormService.lambdaQuery().eq(DeclarationForm::getStatus, 8).count();
        
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
}
