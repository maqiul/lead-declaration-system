<template>
  <div class="profile-management">
    <!-- 用户信息卡片 -->
    <a-card class="info-card">
      <div class="user-header">
        <a-avatar :size="80" :src="userInfo.avatar" class="user-avatar">
          <template #icon><UserOutlined /></template>
        </a-avatar>
        <div class="user-info">
          <h2 class="user-name">{{ userInfo.nickname || userInfo.username }}</h2>
          <div class="user-tags">
            <a-tag v-for="role in userInfo.roles" :key="role" color="blue">{{ role }}</a-tag>
          </div>
          <p class="user-meta">
            <span>用户名: {{ userInfo.username }}</span>
            <a-divider type="vertical" />
            <span>创建时间: {{ userInfo.createTime || '未知' }}</span>
          </p>
        </div>
      </div>
    </a-card>

    <!-- 功能区域 -->
    <a-card class="content-card">
      <a-tabs v-model:activeKey="activeTab">
        <!-- 基本信息 -->
        <a-tab-pane key="basic" tab="基本信息">
          <a-form
            ref="basicFormRef"
            :model="basicForm"
            :rules="basicRules"
            :label-col="{ span: 4 }"
            :wrapper-col="{ span: 16 }"
            class="profile-form"
          >
            <a-form-item label="昵称" name="nickname">
              <a-input v-model:value="basicForm.nickname" placeholder="请输入昵称" />
            </a-form-item>
            <a-form-item label="手机号" name="phone">
              <a-input v-model:value="basicForm.phone" placeholder="请输入手机号" />
            </a-form-item>
            <a-form-item label="邮箱" name="email">
              <a-input v-model:value="basicForm.email" placeholder="请输入邮箱" />
            </a-form-item>
            <a-form-item label="头像" name="avatar">
              <div class="avatar-upload">
                <a-avatar :size="60" :src="basicForm.avatar">
                  <template #icon><UserOutlined /></template>
                </a-avatar>
                <a-upload
                  name="file"
                  :show-upload-list="false"
                  :customRequest="handleAvatarUpload"
                  accept="image/*"
                >
                  <a-button type="link">更换头像</a-button>
                </a-upload>
              </div>
            </a-form-item>
            <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
              <a-button type="primary" :loading="basicLoading" @click="handleBasicSubmit">
                保存修改
              </a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <!-- 修改密码 -->
        <a-tab-pane key="password" tab="修改密码">
          <a-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            :label-col="{ span: 4 }"
            :wrapper-col="{ span: 16 }"
            class="profile-form"
          >
            <a-form-item label="当前密码" name="oldPassword">
              <a-input-password
                v-model:value="passwordForm.oldPassword"
                placeholder="请输入当前密码"
              />
            </a-form-item>
            <a-form-item label="新密码" name="newPassword">
              <a-input-password
                v-model:value="passwordForm.newPassword"
                placeholder="请输入新密码"
              />
            </a-form-item>
            <a-form-item label="确认新密码" name="confirmPassword">
              <a-input-password
                v-model:value="passwordForm.confirmPassword"
                placeholder="请再次输入新密码"
              />
            </a-form-item>
            <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
              <a-button type="primary" :loading="passwordLoading" @click="handlePasswordSubmit">
                修改密码
              </a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <!-- 账号安全 -->
        <a-tab-pane key="security" tab="账号安全">
          <div class="security-list">
            <div class="security-item">
              <div class="security-left">
                <SafetyOutlined class="security-icon" />
                <div class="security-info">
                  <h4>登录密码</h4>
                  <p>定期更换密码可以提高账号安全性</p>
                </div>
              </div>
              <a-button type="link" @click="activeTab = 'password'">修改</a-button>
            </div>
            <a-divider />
            <div class="security-item">
              <div class="security-left">
                <EnvironmentOutlined class="security-icon" />
                <div class="security-info">
                  <h4>最近登录</h4>
                  <p>上次登录时间：{{ userInfo.lastLoginTime || '未知' }}</p>
                </div>
              </div>
            </div>
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { UserOutlined, SafetyOutlined, EnvironmentOutlined } from '@ant-design/icons-vue'
import type { UploadRequestOption } from 'ant-design-vue/es/vc-upload/interface'
import type { Rule } from 'ant-design-vue/es/form'
import { useUserStore } from '@/store/user'
import { getInfo } from '@/api/user'
import { updateUserProfile, changePassword, uploadAvatar } from '@/api/system'

const userStore = useUserStore()

// 用户信息
const userInfo = reactive({
  id: 0,
  username: '',
  nickname: '',
  avatar: '',
  phone: '',
  email: '',
  orgName: '',
  roles: [] as string[],
  createTime: '',
  lastLoginTime: ''
})

// 当前激活的标签页
const activeTab = ref('basic')

// 基本信息表单
const basicFormRef = ref()
const basicLoading = ref(false)
const basicForm = reactive({
  nickname: '',
  phone: '',
  email: '',
  avatar: ''
})

const basicRules: Record<string, Rule[]> = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

// 密码表单
const passwordFormRef = ref()
const passwordLoading = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = async (_rule: Rule, value: string) => {
  if (value !== passwordForm.newPassword) {
    throw new Error('两次输入的密码不一致')
  }
}

const passwordRules: Record<string, Rule[]> = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 加载用户信息
const loadUserInfo = async () => {
  try {
    const response = await getInfo()
    if (response.data?.code === 200) {
      const data = response.data.data
      Object.assign(userInfo, {
        id: data.id,
        username: data.username,
        nickname: data.nickname || data.username,
        avatar: data.avatar,
        phone: data.phone || '',
        email: data.email || '',
        orgName: data.orgName || '',
        roles: data.roles || [],
        createTime: data.createTime,
        lastLoginTime: data.lastLoginTime
      })
      // 填充表单
      basicForm.nickname = userInfo.nickname
      basicForm.phone = userInfo.phone
      basicForm.email = userInfo.email
      basicForm.avatar = userInfo.avatar
    }
  } catch (error) {
    message.error('获取用户信息失败')
  }
}

// 保存基本信息
const handleBasicSubmit = async () => {
  try {
    await basicFormRef.value?.validateFields()
    basicLoading.value = true
    
    const response = await updateUserProfile({
      nickname: basicForm.nickname,
      phone: basicForm.phone,
      email: basicForm.email
    })
    
    if (response.data?.code === 200) {
      message.success('保存成功')
      userInfo.nickname = basicForm.nickname
      userInfo.phone = basicForm.phone
      userInfo.email = basicForm.email
      userStore.name = basicForm.nickname
    } else {
      message.error(response.data?.message || '保存失败')
    }
  } catch (error) {
    message.error('保存失败')
  } finally {
    basicLoading.value = false
  }
}

// 修改密码
const handlePasswordSubmit = async () => {
  try {
    await passwordFormRef.value?.validateFields()
    passwordLoading.value = true
    
    const response = await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    
    if (response.data?.code === 200) {
      message.success('密码修改成功，请重新登录')
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
      setTimeout(() => {
        userStore.resetToken()
        window.location.href = '/login'
      }, 1500)
    } else {
      message.error(response.data?.message || '修改失败')
    }
  } catch (error) {
    message.error('修改失败')
  } finally {
    passwordLoading.value = false
  }
}

// 上传头像
const handleAvatarUpload = async (options: UploadRequestOption) => {
  const { file } = options
  const formData = new FormData()
  formData.append('file', file as File)
  
  try {
    const response = await uploadAvatar(formData)
    if (response.data?.code === 200) {
      const avatarUrl = response.data.data
      basicForm.avatar = avatarUrl
      userInfo.avatar = avatarUrl
      userStore.avatar = avatarUrl
      message.success('头像上传成功')
    } else {
      message.error(response.data?.message || '上传失败')
    }
  } catch (error) {
    message.error('上传失败')
  }
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.profile-management {
  padding: 0;
  height: 100%;
}

.info-card {
  margin-bottom: 20px;
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.user-header {
  display: flex;
  align-items: center;
  padding: 16px 0;
}

.user-avatar {
  border: 3px solid #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  flex-shrink: 0;
}

.user-info {
  margin-left: 24px;
}

.user-name {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
}

.user-tags {
  margin-bottom: 8px;
}

.user-meta {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
}

.content-card {
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.profile-form {
  max-width: 600px;
  padding: 24px 0;
}

.avatar-upload {
  display: flex;
  align-items: center;
  gap: 16px;
}

.security-list {
  padding: 16px 0;
  max-width: 600px;
}

.security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
}

.security-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.security-icon {
  font-size: 24px;
  color: #3b82f6;
}

.security-info h4 {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 500;
  color: #1f2937;
}

.security-info p {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
}

:deep(.ant-card) {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.03);
  border: 1px solid rgba(226, 232, 240, 0.6);
}

:deep(.ant-card-body) {
  padding: 24px;
}

:deep(.ant-tabs-tab) {
  font-size: 15px;
}

:deep(.ant-btn-primary) {
  background: linear-gradient(135deg, #1e40af 0%, #3b82f6 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(30, 64, 175, 0.2);
  border-radius: 8px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

:deep(.ant-btn-primary:hover) {
  background: linear-gradient(135deg, #1d4ed8 0%, #2563eb 100%);
  box-shadow: 0 4px 12px rgba(30, 64, 175, 0.3);
  transform: translateY(-1px);
}

:deep(.ant-input-affix-wrapper:focus),
:deep(.ant-input:focus) {
  box-shadow: 0 0 0 2px rgba(30, 64, 175, 0.1);
  border-color: #3b82f6;
}

@media (max-width: 768px) {
  .user-header {
    flex-direction: column;
    text-align: center;
  }
  
  .user-info {
    margin-left: 0;
    margin-top: 16px;
  }
  
  :deep(.ant-card-body) {
    padding: 16px;
  }
}
</style>
