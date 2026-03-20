<template>
  <div class="login-container">
    <!-- 装饰光球 -->
    <div class="orb orb--1"></div>
    <div class="orb orb--2"></div>
    <div class="orb orb--3"></div>

    <!-- 登录卡片 -->
    <div class="login-card">
      <div class="login-header">
        <div class="brand-icon">
          <thunderbolt-outlined class="brand-icon-svg" />
        </div>
        <h1 class="brand-title">{{ systemName }}</h1>
        <p class="brand-subtitle">Lead Declaration System</p>
      </div>
      
      <a-form
        :model="loginForm"
        :rules="rules"
        ref="formRef"
        @finish="handleLogin"
        class="login-form"
        layout="vertical"
      >
        <a-form-item name="username" label="用户名">
          <a-input
            v-model:value="loginForm.username"
            placeholder="请输入用户名"
            size="large"
            @press-enter="handleLogin"
          >
            <template #prefix>
              <user-outlined style="color: #94a3b8" />
            </template>
          </a-input>
        </a-form-item>
        
        <a-form-item name="password" label="密码">
          <a-input-password
            v-model:value="loginForm.password"
            placeholder="请输入密码"
            size="large"
            @press-enter="handleLogin"
          >
            <template #prefix>
              <lock-outlined style="color: #94a3b8" />
            </template>
          </a-input-password>
        </a-form-item>

        <div class="login-options">
          <a-checkbox v-model:checked="rememberMe">记住我</a-checkbox>
        </div>
        
        <a-form-item style="margin-top: 8px; margin-bottom: 0;">
          <a-button
            type="primary"
            html-type="submit"
            size="large"
            block
            :loading="loading"
            class="login-btn"
          >
            <span v-if="!loading">登 录</span>
          </a-button>
        </a-form-item>
      </a-form>
      
      <div class="login-footer">
        <p>© 2026 {{ systemName }}. All rights reserved.</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import { getSystemParam } from '@/utils/system-params'

import type { Rule } from 'ant-design-vue/es/form'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const rememberMe = ref(true)

const systemName = computed(() => getSystemParam('system.name', '线索申报系统'))

const loginForm = reactive({
  username: 'admin',
  password: 'admin'
})

const rules: Record<string, Rule[]> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  try {
    loading.value = true
    await userStore.login(loginForm)
    
    // 登录成功后立即获取用户信息和权限
    await userStore.getUserInfo()
    await userStore.generateRoutes()
    
    message.success('登录成功')
    
    const redirect = route.query.redirect as string || '/'
    router.push(redirect)
  } catch (error: any) {
    message.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #020617 0%, #0F172A 40%, #1E40AF 100%);
  position: relative;
  overflow: hidden;
}

/* 浮动光球 */
.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.35;
  pointer-events: none;
}

.orb--1 {
  width: 500px;
  height: 500px;
  background: #1E40AF;
  top: -150px;
  right: -120px;
  animation: float1 10s ease-in-out infinite;
}

.orb--2 {
  width: 400px;
  height: 400px;
  background: #3B82F6;
  bottom: -100px;
  left: -80px;
  animation: float2 12s ease-in-out infinite;
}

.orb--3 {
  width: 250px;
  height: 250px;
  background: #0EA5E9;
  top: 40%;
  left: 15%;
  animation: float3 14s ease-in-out infinite;
}

@keyframes float1 {
  0%, 100% { transform: translate(0, 0); }
  50%      { transform: translate(-40px, 30px); }
}

@keyframes float2 {
  0%, 100% { transform: translate(0, 0); }
  50%      { transform: translate(30px, -40px); }
}

@keyframes float3 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33%      { transform: translate(25px, -15px) scale(1.05); }
  66%      { transform: translate(-15px, 10px) scale(0.97); }
}

/* 登录卡片 — 白底 */
.login-card {
  width: 420px;
  padding: 44px 40px 32px;
  background: #ffffff;
  border-radius: 24px;
  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.3), 0 0 0 1px rgba(255, 255, 255, 0.05);
  position: relative;
  z-index: 1;
  animation: cardEnter 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes cardEnter {
  from { opacity: 0; transform: translateY(20px) scale(0.97); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

/* 品牌 */
.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.brand-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 16px;
  border-radius: 16px;
  background: linear-gradient(135deg, #1E40AF, #3B82F6);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 16px rgba(30, 64, 175, 0.35);
}

.brand-icon-svg {
  color: white;
  font-size: 26px;
}

.brand-title {
  margin: 0 0 4px;
  font-size: 24px;
  font-weight: 700;
  color: #0F172A;
  letter-spacing: 0.5px;
}

.brand-subtitle {
  margin: 0;
  color: #94A3B8;
  font-size: 13px;
  letter-spacing: 2px;
  text-transform: uppercase;
}

/* 表单 */
.login-form {
  margin-top: 8px;
}

/* label 样式 */
:deep(.ant-form-item-label > label) {
  color: #475569 !important;
  font-weight: 500 !important;
  font-size: 13px !important;
}

/* 输入框 — 确保深色文字可见并富有质感 */
:deep(.ant-input-affix-wrapper) {
  background: #ffffff !important;
  border: 1px solid #d1d5db !important;
  border-radius: 12px !important;
  padding: 0 16px !important;
  height: 52px !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05) !important;
}

:deep(.ant-input-affix-wrapper .ant-input) {
  background: transparent !important;
  color: #111827 !important;
  font-size: 16px !important;
  height: 48px !important;
}

:deep(.ant-input::placeholder) {
  color: #9ca3af !important;
}

:deep(.ant-input-affix-wrapper:hover) {
  border-color: #3B82F6 !important;
}

:deep(.ant-input-affix-wrapper-focused),
:deep(.ant-input-affix-wrapper:focus-within) {
  border-color: #2563EB !important;
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.12) !important;
  transform: translateY(-1px);
}

:deep(.ant-input-password-icon) {
  color: #94A3B8 !important;
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

:deep(.login-options .ant-checkbox-wrapper) {
  color: #64748B;
  font-size: 13px;
}

/* 登录按钮 */
:deep(.login-btn) {
  height: 48px !important;
  border-radius: 12px !important;
  background: linear-gradient(135deg, #1E40AF, #3B82F6) !important;
  border: none !important;
  font-size: 16px !important;
  font-weight: 600 !important;
  letter-spacing: 4px !important;
  box-shadow: 0 4px 16px rgba(30, 64, 175, 0.3) !important;
  transition: all 0.25s !important;
}

:deep(.login-btn:hover) {
  box-shadow: 0 8px 28px rgba(30, 64, 175, 0.45) !important;
  transform: translateY(-2px) !important;
}

:deep(.login-btn:active) {
  transform: translateY(0) !important;
}

/* 表单校验 */
:deep(.ant-form-item-explain-error) {
  font-size: 12px !important;
}

/* 底部 */
.login-footer {
  text-align: center;
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid #F1F5F9;
}

.login-footer p {
  margin: 0;
  color: #CBD5E1;
  font-size: 12px;
  letter-spacing: 0.5px;
}
</style>