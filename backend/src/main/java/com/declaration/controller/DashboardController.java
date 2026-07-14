package com.declaration.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.declaration.common.Result;
import com.declaration.dao.RoleMenuDao;
import com.declaration.entity.CurrencyInfo;
import com.declaration.entity.DeclarationForm;
import com.declaration.entity.Menu;
import com.declaration.entity.RoleMenu;
import com.declaration.entity.TaxRefundApplication;
import com.declaration.entity.User;
import com.declaration.service.CurrencyInfoService;
import com.declaration.service.DeclarationFormService;
import com.declaration.service.MenuService;
import com.declaration.service.PermissionService;
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
import java.util.*;
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
    private final CurrencyInfoService currencyInfoService;
    private final MenuService menuService;
    private final PermissionService permissionService;
    private final RoleMenuDao roleMenuDao;

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
     * 根据用户角色配置的菜单动态显示统计项
     */
    @GetMapping("/declaration-stats")
    @Operation(summary = "获取申报菜单统计和30天预警数据")
    public Result<Map<String, Object>> getDeclarationStats() {
        Map<String, Object> result = new HashMap<>();

        // 判断用户权限：管理员看全部，view-internal/view-external 分别控制可见性
        Long userId = StpUtil.getLoginIdAsLong();
        boolean isAdmin = userId == 1L;
        boolean hasInternalPermission = StpUtil.hasPermission("business:declaration:view-internal");
        boolean hasExternalPermission = StpUtil.hasPermission("business:declaration:view-external");
        boolean canViewInternal = isAdmin || hasInternalPermission;
        boolean canViewExternal = isAdmin || hasExternalPermission;

        result.put("canViewInternal", canViewInternal);
        result.put("canViewExternal", canViewExternal);

        // ========== 动态菜单统计：根据用户角色配置的菜单显示 ==========

        // 1. 获取用户可访问的菜单ID集合
        Set<Long> accessibleMenuIds = getUserAccessibleMenuIds(userId, isAdmin);

        // 2. 查询申报子菜单（parent_id in 900, 901），只取启用+显示的
        List<Menu> allDeclMenus = menuService.list(new LambdaQueryWrapper<Menu>()
                .in(Menu::getParentId, Arrays.asList(900L, 901L))
                .eq(Menu::getMenuType, 2)
                .eq(Menu::getStatus, 1)
                .eq(Menu::getIsShow, 1)
                .orderByAsc(Menu::getSort));

        // 如果没有新菜单（迁移未执行），回退到旧菜单(200)
        if (allDeclMenus.isEmpty()) {
            allDeclMenus = menuService.list(new LambdaQueryWrapper<Menu>()
                    .eq(Menu::getParentId, 200L)
                    .eq(Menu::getMenuType, 2)
                    .eq(Menu::getStatus, 1)
                    .eq(Menu::getIsShow, 1)
                    .orderByAsc(Menu::getSort));
        }

        // 3. 按用户权限过滤菜单
        List<Menu> selfMenus = new ArrayList<>();
        List<Menu> extMenus = new ArrayList<>();
        for (Menu m : allDeclMenus) {
            if (!accessibleMenuIds.contains(m.getId())) continue;
            String code = m.getMenuCode();
            if (code == null) continue;
            if (code.startsWith("declaration-self-")) selfMenus.add(m);
            else if (code.startsWith("declaration-ext-")) extMenus.add(m);
        }

        // 4. 菜单统计配置表（key = menuCode 后缀）
        Map<String, MenuStatConfig> menuConfigMap = buildMenuConfigMap();

        // 5. 构建内部/外部统计
        result.put("internalMenuStats", buildMenuStats(selfMenus, menuConfigMap, "SELF"));
        result.put("externalMenuStats", buildMenuStats(extMenus, menuConfigMap, "EXTERNAL"));

        // 30天预警
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

        // 批量查询货币符号
        Map<String, String> currencySymbolMap = currencyInfoService.getEnabledList().stream()
                .collect(Collectors.toMap(CurrencyInfo::getCurrencyCode, CurrencyInfo::getSymbol, (a, b) -> a));

        List<Map<String, Object>> warningItems = warningList.stream().map(form -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", form.getId());
            item.put("formNo", form.getFormNo());
            item.put("shipperCompany", form.getShipperCompany());
            item.put("status", form.getStatus());
            item.put("createTime", form.getCreateTime());
            item.put("totalAmount", form.getTotalAmount());
            item.put("currency", form.getCurrency());
            item.put("currencySymbol", form.getCurrency() != null
                    ? currencySymbolMap.getOrDefault(form.getCurrency().toUpperCase(), form.getCurrency()) : "");
            item.put("destinationCountry", form.getDestinationCountry());
            item.put("declarantName", form.getCreateBy() != null
                    ? userNameMap.getOrDefault(form.getCreateBy(), "未知用户") : "未知用户");
            item.put("declarationType", form.getDeclarationType());
            return item;
        }).collect(Collectors.toList());

        result.put("warningCount", warningItems.size());
        result.put("warningList", warningItems);

        return Result.success(result);
    }

    /** 获取用户可访问的菜单ID集合 */
    private Set<Long> getUserAccessibleMenuIds(Long userId, boolean isAdmin) {
        if (isAdmin) {
            return menuService.list(new LambdaQueryWrapper<Menu>()
                    .eq(Menu::getStatus, 1)
                    .eq(Menu::getIsShow, 1))
                    .stream().map(Menu::getId).collect(Collectors.toSet());
        }
        List<Long> roleIds = permissionService.getUserRoleIds(userId);
        if (roleIds == null || roleIds.isEmpty()) return Collections.emptySet();
        return roleMenuDao.selectList(new LambdaQueryWrapper<RoleMenu>()
                .in(RoleMenu::getRoleId, roleIds))
                .stream().map(RoleMenu::getMenuId).collect(Collectors.toSet());
    }

    /** 菜单统计配置表（仅流程环节，不含财务单证/申报管理/申报统计） */
    private Map<String, MenuStatConfig> buildMenuConfigMap() {
        Map<String, MenuStatConfig> map = new LinkedHashMap<>();
        map.put("entry",          new MenuStatConfig(Arrays.asList(1, 11), "blue"));
        map.put("material",       new MenuStatConfig(Arrays.asList(2, 3), "green"));
        map.put("supplement",     new MenuStatConfig(Arrays.asList(4, 5), "orange"));
        map.put("invoice-amount", new MenuStatConfig(Arrays.asList(6, 7), "purple"));
        map.put("invoice",        new MenuStatConfig(Arrays.asList(8, 9), "cyan"));
        map.put("archive",        new MenuStatConfig(Arrays.asList(10), "default"));
        return map;
    }

    /** 根据用户可访问的菜单列表构建统计 */
    private List<Map<String, Object>> buildMenuStats(List<Menu> menus,
            Map<String, MenuStatConfig> configMap, String declarationType) {
        List<Map<String, Object>> stats = new ArrayList<>();
        for (Menu menu : menus) {
            String code = menu.getMenuCode();
            if (code == null) continue;
            // 提取后缀：declaration-self-entry → entry, declaration-ext-invoice-amount → invoice-amount
            String suffix = code.replaceFirst("^declaration-(self|ext)-", "");
            MenuStatConfig config = configMap.get(suffix);
            if (config == null) continue;

            LambdaQueryWrapper<DeclarationForm> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(DeclarationForm::getStatus, config.statusFilter);
            wrapper.eq(DeclarationForm::getDeclarationType, declarationType);
            applyDeclarationDataPermission(wrapper);
            long count = declarationFormService.count(wrapper);

            Map<String, Object> item = new HashMap<>();
            item.put("menuName", menu.getMenuName());
            item.put("path", "/declaration/" + suffix);
            item.put("icon", menu.getIcon());
            item.put("theme", config.theme);
            item.put("count", count);
            stats.add(item);
        }
        return stats;
    }

    /** 菜单统计配置（状态过滤器 + 主题色） */
    private static class MenuStatConfig {
        final List<Integer> statusFilter;
        final String theme;
        MenuStatConfig(List<Integer> statusFilter, String theme) {
            this.statusFilter = statusFilter;
            this.theme = theme;
        }
    }
}
