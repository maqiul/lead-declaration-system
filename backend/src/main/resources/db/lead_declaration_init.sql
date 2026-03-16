/*
 线索申报系统完整数据库初始化脚本
 包含：基础用户、权限管理、组织机构、工作流模块
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Database: lead_declaration
-- ----------------------------
CREATE DATABASE IF NOT EXISTS `lead_declaration` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `lead_declaration`;

-- ====================================================================
-- 基础用户表
-- ====================================================================

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '昵称',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像',
  `org_id` bigint NULL DEFAULT NULL COMMENT '所属组织ID',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标识 0-未删除 1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE COMMENT '用户名唯一索引',
  INDEX `idx_org_id`(`org_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '21232f297a57a5a743894a0e4a801fc3', '管理员', '13800138000', 'admin@example.com', NULL, 1, 1, 0, '2026-03-13 10:00:00', '2026-03-13 10:00:00', NULL, NULL);

-- ====================================================================
-- 权限管理系统
-- ====================================================================

-- ----------------------------
-- Table structure for sys_org 组织机构表
-- ----------------------------
DROP TABLE IF EXISTS `sys_org`;
CREATE TABLE `sys_org`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `org_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '组织名称',
  `org_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '组织编码',
  `parent_id` bigint NULL DEFAULT NULL COMMENT '父级ID',
  `level` int NOT NULL DEFAULT 1 COMMENT '组织层级',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `leader` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '负责人',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '邮箱',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标识 0-未删除 1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_org_code`(`org_code` ASC) USING BTREE COMMENT '组织编码唯一索引',
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '组织机构表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_org
-- ----------------------------
INSERT INTO `sys_org` VALUES (1, '总公司', 'COMPANY', 0, 1, 1, '张三', '13800138000', 'company@example.com', 1, 0, NOW(), NOW(), NULL, NULL);
INSERT INTO `sys_org` VALUES (2, '技术部', 'TECH', 1, 2, 1, '李四', '13800138001', 'tech@example.com', 1, 0, NOW(), NOW(), NULL, NULL);
INSERT INTO `sys_org` VALUES (3, '销售部', 'SALES', 1, 2, 2, '王五', '13800138002', 'sales@example.com', 1, 0, NOW(), NOW(), NULL, NULL);

-- ----------------------------
-- Table structure for sys_role 角色表
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色编码',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '角色描述',
  `data_scope` tinyint NOT NULL DEFAULT 1 COMMENT '数据权限范围 1-全部 2-本级 3-本级及下级 4-自定义',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标识 0-未删除 1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_code`(`role_code` ASC) USING BTREE COMMENT '角色编码唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '超级管理员', 'ADMIN', '系统超级管理员，拥有所有权限', 1, 1, 0, NOW(), NOW(), NULL, NULL);
INSERT INTO `sys_role` VALUES (2, '普通用户', 'USER', '普通用户角色', 3, 1, 0, NOW(), NOW(), NULL, NULL);
INSERT INTO `sys_role` VALUES (3, '部门管理员', 'DEPT_ADMIN', '部门管理员角色', 3, 1, 0, NOW(), NOW(), NULL, NULL);

-- ----------------------------
-- Table structure for sys_menu 菜单权限表
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `menu_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜单名称',
  `menu_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '菜单编码',
  `parent_id` bigint NULL DEFAULT NULL COMMENT '父级ID',
  `menu_type` tinyint NOT NULL DEFAULT 1 COMMENT '菜单类型 1-目录 2-菜单 3-按钮',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '路由地址',
  `component` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '组件路径',
  `permission` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '图标',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `is_external` tinyint NOT NULL DEFAULT 0 COMMENT '是否外链 0-否 1-是',
  `is_cache` tinyint NOT NULL DEFAULT 0 COMMENT '是否缓存 0-否 1-是',
  `is_show` tinyint NOT NULL DEFAULT 1 COMMENT '是否显示 0-隐藏 1-显示',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标识 0-未删除 1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_permission`(`permission` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜单权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
-- 系统管理目录
INSERT INTO `sys_menu` VALUES (1, '系统管理', 'system', 0, 1, '/system', 'Layout', NULL, 'system', 1, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL);
-- 用户管理菜单
INSERT INTO `sys_menu` VALUES (2, '用户管理', 'system:user', 1, 2, 'user', 'system/user/index', NULL, 'user', 1, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL);
INSERT INTO `sys_menu` VALUES (3, '用户查询', 'system:user:query', 2, 3, NULL, NULL, 'user:query', NULL, 1, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL);
INSERT INTO `sys_menu` VALUES (4, '用户新增', 'system:user:add', 2, 3, NULL, NULL, 'user:add', NULL, 2, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL);
INSERT INTO `sys_menu` VALUES (5, '用户修改', 'system:user:update', 2, 3, NULL, NULL, 'user:update', NULL, 3, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL);
INSERT INTO `sys_menu` VALUES (6, '用户删除', 'system:user:delete', 2, 3, NULL, NULL, 'user:delete', NULL, 4, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL);
-- 角色管理菜单
INSERT INTO `sys_menu` VALUES (7, '角色管理', 'system:role', 1, 2, 'role', 'system/role/index', NULL, 'peoples', 2, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL);
INSERT INTO `sys_menu` VALUES (8, '角色查询', 'system:role:query', 7, 3, NULL, NULL, 'role:query', NULL, 1, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL);
INSERT INTO `sys_menu` VALUES (9, '角色新增', 'system:role:add', 7, 3, NULL, NULL, 'role:add', NULL, 2, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL);
INSERT INTO `sys_menu` VALUES (10, '角色修改', 'system:role:update', 7, 3, NULL, NULL, 'role:update', NULL, 3, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL);
INSERT INTO `sys_menu` VALUES (11, '角色删除', 'system:role:delete', 7, 3, NULL, NULL, 'role:delete', NULL, 4, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL);
-- 组织管理菜单
INSERT INTO `sys_menu` VALUES (12, '组织管理', 'system:org', 1, 2, 'org', 'system/org/index', NULL, 'tree', 3, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL);
INSERT INTO `sys_menu` VALUES (13, '组织查询', 'system:org:query', 12, 3, NULL, NULL, 'org:query', NULL, 1, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL);
INSERT INTO `sys_menu` VALUES (14, '组织新增', 'system:org:add', 12, 3, NULL, NULL, 'org:add', NULL, 2, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL);
INSERT INTO `sys_menu` VALUES (15, '组织修改', 'system:org:update', 12, 3, NULL, NULL, 'org:update', NULL, 3, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL);
INSERT INTO `sys_menu` VALUES (16, '组织删除', 'system:org:delete', 12, 3, NULL, NULL, 'org:delete', NULL, 4, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL);
-- 菜单管理菜单
INSERT INTO `sys_menu` VALUES (17, '菜单管理', 'system:menu', 1, 2, 'menu', 'system/menu/index', NULL, 'tree-table', 4, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL);
INSERT INTO `sys_menu` VALUES (18, '菜单查询', 'system:menu:query', 17, 3, NULL, NULL, 'menu:query', NULL, 1, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL);
INSERT INTO `sys_menu` VALUES (19, '菜单新增', 'system:menu:add', 17, 3, NULL, NULL, 'menu:add', NULL, 2, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL);
INSERT INTO `sys_menu` VALUES (20, '菜单修改', 'system:menu:update', 17, 3, NULL, NULL, 'menu:update', NULL, 3, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL);
INSERT INTO `sys_menu` VALUES (21, '菜单删除', 'system:menu:delete', 17, 3, NULL, NULL, 'menu:delete', NULL, 4, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL);

-- ----------------------------
-- Table structure for sys_user_role 用户角色关联表
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_role`(`user_id` ASC, `role_id` ASC) USING BTREE COMMENT '用户角色唯一索引',
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (1, 1, 1, NOW(), NULL); -- admin用户分配超级管理员角色

-- ----------------------------
-- Table structure for sys_role_menu 角色菜单关联表
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_menu`(`role_id` ASC, `menu_id` ASC) USING BTREE COMMENT '角色菜单唯一索引',
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE,
  INDEX `idx_menu_id`(`menu_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色菜单关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
-- 超级管理员拥有所有菜单权限
INSERT INTO `sys_role_menu` VALUES (1, 1, 1, NOW(), NULL);
INSERT INTO `sys_role_menu` VALUES (2, 1, 2, NOW(), NULL);
INSERT INTO `sys_role_menu` VALUES (3, 1, 3, NOW(), NULL);
INSERT INTO `sys_role_menu` VALUES (4, 1, 4, NOW(), NULL);
INSERT INTO `sys_role_menu` VALUES (5, 1, 5, NOW(), NULL);
INSERT INTO `sys_role_menu` VALUES (6, 1, 6, NOW(), NULL);
INSERT INTO `sys_role_menu` VALUES (7, 1, 7, NOW(), NULL);
INSERT INTO `sys_role_menu` VALUES (8, 1, 8, NOW(), NULL);
INSERT INTO `sys_role_menu` VALUES (9, 1, 9, NOW(), NULL);
INSERT INTO `sys_role_menu` VALUES (10, 1, 10, NOW(), NULL);
INSERT INTO `sys_role_menu` VALUES (11, 1, 11, NOW(), NULL);
INSERT INTO `sys_role_menu` VALUES (12, 1, 12, NOW(), NULL);
INSERT INTO `sys_role_menu` VALUES (13, 1, 13, NOW(), NULL);
INSERT INTO `sys_role_menu` VALUES (14, 1, 14, NOW(), NULL);
INSERT INTO `sys_role_menu` VALUES (15, 1, 15, NOW(), NULL);
INSERT INTO `sys_role_menu` VALUES (16, 1, 16, NOW(), NULL);
INSERT INTO `sys_role_menu` VALUES (17, 1, 17, NOW(), NULL);
INSERT INTO `sys_role_menu` VALUES (18, 1, 18, NOW(), NULL);
INSERT INTO `sys_role_menu` VALUES (19, 1, 19, NOW(), NULL);
INSERT INTO `sys_role_menu` VALUES (20, 1, 20, NOW(), NULL);
INSERT INTO `sys_role_menu` VALUES (21, 1, 21, NOW(), NULL);

-- ----------------------------
-- Table structure for sys_user_org 用户组织关联表
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_org`;
CREATE TABLE `sys_user_org`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `org_id` bigint NOT NULL COMMENT '组织ID',
  `is_main` tinyint NOT NULL DEFAULT 0 COMMENT '是否主组织 0-否 1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_org`(`user_id` ASC, `org_id` ASC) USING BTREE COMMENT '用户组织唯一索引',
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_org_id`(`org_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户组织关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_org
-- ----------------------------
INSERT INTO `sys_user_org` VALUES (1, 1, 1, 1, NOW(), NULL); -- admin用户属于总公司

-- ====================================================================
-- 工作流模块
-- ====================================================================

-- ----------------------------
-- Table structure for wf_process_definition 流程定义表
-- ----------------------------
DROP TABLE IF EXISTS `wf_process_definition`;
CREATE TABLE `wf_process_definition`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `process_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '流程定义KEY',
  `process_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '流程名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '流程描述',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '流程分类',
  `version` int NOT NULL DEFAULT 1 COMMENT '版本号',
  `bpmn_xml` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '流程XML内容',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 0-停用 1-启用',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标识 0-未删除 1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_process_key_version`(`process_key` ASC, `version` ASC) USING BTREE COMMENT '流程KEY和版本唯一索引',
  INDEX `idx_process_key`(`process_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '流程定义表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wf_process_definition
-- ----------------------------
INSERT INTO `wf_process_definition` VALUES (1, 'leave_process', '请假审批流程', '员工请假审批流程', 'HR', 1, '<?xml version="1.0" encoding="UTF-8"?>
<bpmn2:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xsi:schemaLocation="http://www.omg.org/spec/BPMN/20100524/MODEL BPMN20.xsd" id="sample-diagram" targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn2:process id="leave_process" isExecutable="true">
    <bpmn2:startEvent id="StartEvent_1" name="开始" />
    <bpmn2:userTask id="UserTask_1" name="部门经理审批" />
    <bpmn2:userTask id="UserTask_2" name="人事审批" />
    <bpmn2:endEvent id="EndEvent_1" name="结束" />
    <bpmn2:sequenceFlow id="SequenceFlow_1" sourceRef="StartEvent_1" targetRef="UserTask_1" />
    <bpmn2:sequenceFlow id="SequenceFlow_2" sourceRef="UserTask_1" targetRef="UserTask_2" />
    <bpmn2:sequenceFlow id="SequenceFlow_3" sourceRef="UserTask_2" targetRef="EndEvent_1" />
  </bpmn2:process>
</bpmn2:definitions>', 1, 0, NOW(), NOW(), NULL, NULL);

-- ----------------------------
-- Table structure for wf_process_instance 流程实例表
-- ----------------------------
DROP TABLE IF EXISTS `wf_process_instance`;
CREATE TABLE `wf_process_instance`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '流程实例ID',
  `definition_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '流程定义ID',
  `process_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '流程定义KEY',
  `process_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '流程名称',
  `business_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '业务KEY',
  `starter_id` bigint NULL DEFAULT NULL COMMENT '发起人ID',
  `starter_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '发起人姓名',
  `current_activity_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '当前节点ID',
  `current_activity_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '当前节点名称',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '流程状态 0-运行中 1-已完成 2-已终止 3-已挂起',
  `start_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标识 0-未删除 1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_instance_id`(`instance_id` ASC) USING BTREE COMMENT '流程实例ID唯一索引',
  INDEX `idx_definition_id`(`definition_id` ASC) USING BTREE,
  INDEX `idx_process_key`(`process_key` ASC) USING BTREE,
  INDEX `idx_business_key`(`business_key` ASC) USING BTREE,
  INDEX `idx_starter_id`(`starter_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '流程实例表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wf_process_instance
-- ----------------------------

-- ----------------------------
-- Table structure for wf_task_instance 任务实例表
-- ----------------------------
DROP TABLE IF EXISTS `wf_task_instance`;
CREATE TABLE `wf_task_instance`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务ID',
  `task_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '任务描述',
  `instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '流程实例ID',
  `definition_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '流程定义ID',
  `activity_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '节点ID',
  `activity_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '节点名称',
  `assignee_id` bigint NULL DEFAULT NULL COMMENT '办理人ID',
  `assignee_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '办理人姓名',
  `candidate_ids` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '候选人IDs',
  `candidate_group_ids` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '候选组IDs',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '任务状态 0-待办 1-已办 2-已撤回 3-已终止',
  `priority` int NOT NULL DEFAULT 50 COMMENT '优先级',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `claim_time` datetime NULL DEFAULT NULL COMMENT '签收时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '完成时间',
  `due_time` datetime NULL DEFAULT NULL COMMENT '到期时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标识 0-未删除 1-已删除',
  `create_time_db` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据库创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_task_id`(`task_id` ASC) USING BTREE COMMENT '任务ID唯一索引',
  INDEX `idx_instance_id`(`instance_id` ASC) USING BTREE,
  INDEX `idx_definition_id`(`definition_id` ASC) USING BTREE,
  INDEX `idx_activity_id`(`activity_id` ASC) USING BTREE,
  INDEX `idx_assignee_id`(`assignee_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '任务实例表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wf_task_instance
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;