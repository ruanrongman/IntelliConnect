<template>  
  <div class="forget-password-container">  
    <div class="forget-password-box">  
      <div class="forget-password-left">  
        <div class="platform-title">  
          <div class="title-line">创万联</div>  
          <div class="title-line">InteliConnect</div>  
          <div class="title-line">物联网平台</div>  
        </div>  
      </div>  
        
      <div class="forget-password-form">  
        <h2 class="welcome-text">忘记密码</h2>  
        <p class="sub-title">重置您的密码</p>  
  
        <a-form   
          :model="forgetPasswordForm"   
          @finish="handleSubmit"   
          class="forget-password-form-content"   
        >  
          <a-form-item  
            name="username"   
            :rules="[   
              { required: true, message: '请输入用户名' },   
              { min: 3, message: '用户名至少3个字符' }   
            ]"   
          >  
            <a-input   
              v-model:value="forgetPasswordForm.username"   
              size="large"   
              placeholder="用户名"   
            >  
              <template #prefix>  
                <UserOutlined class="site-form-item-icon" />  
              </template>  
            </a-input>  
          </a-form-item>  
  
          <a-form-item  
            name="email"   
            :rules="[   
              { required: true, message: '请输入邮箱' },   
              { type: 'email', message: '请输入正确的邮箱格式' }   
            ]"   
          >  
            <a-input   
              v-model:value="forgetPasswordForm.email"   
              size="large"   
              placeholder="邮箱"   
            >  
              <template #prefix>  
                <MailOutlined class="site-form-item-icon" />  
              </template>  
            </a-input>  
          </a-form-item>  
  
          <a-form-item  
            name="password"   
            :rules="[   
              { required: true, message: '请输入新密码' },   
              { min: 6, message: '密码至少6个字符' }   
            ]"   
          >  
            <a-input-password   
              v-model:value="forgetPasswordForm.password"   
              size="large"   
              placeholder="新密码"   
            >  
              <template #prefix>  
                <LockOutlined class="site-form-item-icon" />  
              </template>  
            </a-input-password>  
          </a-form-item>  

          <!-- 新增确认密码字段 -->
          <a-form-item  
            name="confirmPassword"   
            :rules="[   
              { required: true, message: '请再次输入密码' },
              { validator: validateConfirmPassword }
            ]"   
          >  
            <a-input-password   
              v-model:value="forgetPasswordForm.confirmPassword"   
              size="large"   
              placeholder="确认新密码"   
            >  
              <template #prefix>  
                <LockOutlined class="site-form-item-icon" />  
              </template>  
            </a-input-password>  
          </a-form-item>  
  
          <a-form-item  
            name="userCode"   
            :rules="[{ required: true, message: '请输入验证码' }]"   
          >  
            <a-space>  
              <a-input   
                v-model:value="forgetPasswordForm.userCode"   
                size="large"   
                placeholder="验证码"   
                style="width: 200px"   
              >  
                <template #prefix>  
                  <SafetyCertificateOutlined class="site-form-item-icon" />  
                </template>  
              </a-input>  
              <a-button   
                :disabled="!!countdown || loading"   
                @click="handleSendCode"   
                size="large"   
                class="code-button"   
              >  
                {{ countdown ? `${countdown}s后重试` : '获取验证码' }}   
              </a-button>  
            </a-space>  
          </a-form-item>  
  
          <a-form-item>  
            <a-button   
              type="primary"   
              html-type="submit"   
              size="large"   
              :loading="loading"   
              class="forget-password-button"   
              block  
            >  
              重置密码  
            </a-button>  
          </a-form-item>  
  
          <div class="login-link">  
            记起密码了? <router-link to="/login">立即登录</router-link>  
          </div>  
        </a-form>  
      </div>  
    </div>  
  </div>  
</template>  
  
<script setup>  
import { ref, reactive, onUnmounted } from 'vue'   
import {   
  UserOutlined,   
  MailOutlined,   
  LockOutlined,   
  SafetyCertificateOutlined   
} from '@ant-design/icons-vue'   
import { forgetPassword, getEmailCode } from '@/api/user'   
import { useRouter } from 'vue-router'   
import { message } from 'ant-design-vue'   
  
const router = useRouter()   
const loading = ref(false)   
const countdown = ref(0)   
let timer = null  
  
const forgetPasswordForm = reactive({   
  username: '', // 用户名  
  email: '', // 邮箱  
  password: '',   
  confirmPassword: '', // 新增确认密码字段
  userCode: '' // 验证码  
})   

// 密码确认验证器
const validateConfirmPassword = (rule, value) => {
  if (!value) {
    return Promise.reject(new Error('请再次输入密码'))
  }
  if (value !== forgetPasswordForm.password) {
    return Promise.reject(new Error('两次输入的密码不一致'))
  }
  return Promise.resolve()
}
  
// 发送验证码  
const handleSendCode = async () => {   
  // 验证邮箱  
  if(!forgetPasswordForm.email) {   
    message.error('请输入邮箱')   
    return  
  }   
  if(!/^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/.test(forgetPasswordForm.email)) {   
    message.error('请输入正确的邮箱格式')   
    return  
  } 
    
  try {   
    loading.value = true  
    const res = await getEmailCode({   
      email: forgetPasswordForm.email,   
      username: forgetPasswordForm.username  
    })   
      
    if(res.data.errorCode === 200) {   
      message.success('验证码已发送')   
      countdown.value = 60  
      timer = setInterval(() => {   
        if(countdown.value > 0) {   
          countdown.value--   
        } else {   
          clearInterval(timer)   
        }   
      }, 1000)   
    } else {   
      message.error(res.data.errorMsg || '验证码发送失败')   
    }   
  } catch(err) {   
    message.error('验证码发送失败')   
    console.error(err)   
  } finally {   
    loading.value = false  
  }   
}   
  
const handleSubmit = async (values) => {   
  // 前端安全验证
  if (!values.password || !values.confirmPassword) {
    message.error('请输入新密码')
    return
  }
  
  if (values.password !== values.confirmPassword) {
    message.error('两次输入的密码不一致')
    return
  }

  if (values.password.length < 6) {
    message.error('密码至少6个字符')
    return
  }

  try {   
    loading.value = true  
    const res = await forgetPassword({   
      username: values.username, // 用户名    
      password: values.password,     
      userCode: values.userCode, // 验证码    
    })   
      
    if (res.data.errorCode === 200) {   
      message.success('密码重置成功!')   
      router.push('/login')   
    } else {   
      message.error(res.data.errorMsg || '密码重置失败')   
    }   
  } catch (err) {   
    message.error('密码重置失败,请稍后重试')   
    console.error(err)   
  } finally {   
    loading.value = false  
  }   
}
  
// 组件销毁时清除定时器  
onUnmounted(() => {   
  if(timer) {   
    clearInterval(timer)   
  }   
})   
</script>  
  
<style lang="less" scoped>  
.forget-password-container {   
  display: flex;   
  justify-content: center;   
  align-items: center;   
  min-height: 100vh;   
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);   
  padding: 20px;   
}   
  
.forget-password-box {   
  display: flex;   
  background: white;   
  border-radius: 20px;   
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.1);   
  width: 1000px;   
  max-width: 100%;   
  min-height: 600px;   
  overflow: hidden;   
}   
  
.forget-password-left {   
  position: relative;   
  flex: 1;   
  background: #f5f7ff;   
  padding: 40px;   
  display: flex;   
  align-items: center;   
  justify-content: center;   
  
  .platform-title {   
    position: absolute;   
    top: 80px;   
    left: 50px;   
    z-index: 2;   
  
    .title-line {   
      font-size: 36px;   
      font-weight: bold;   
      color: #667eea;   
      line-height: 1.5;   
      text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);   
  
      &:nth-child(2) {   
        font-size: 42px;   
        background: linear-gradient(90deg, #667eea, #764ba2);   
        -webkit-background-clip: text;   
        -webkit-text-fill-color: transparent;   
      }   
  
      &:nth-child(3) {   
        font-size: 32px;   
      }   
    }   
  }   
  
  @media (max-width: 768px) {   
    display: none;   
  }   
}   
  
.forget-password-form {   
  flex: 1;   
  padding: 50px;   
  display: flex;   
  flex-direction: column;   
  justify-content: center;   
}   
  
.welcome-text {   
  font-size: 32px;   
  font-weight: 600;   
  color: #1a1a1a;   
  margin-bottom: 8px;   
}   
  
.sub-title {   
  color: #666;   
  font-size: 16px;   
  margin-bottom: 40px;   
}   
  
.forget-password-form-content {   
  max-width: 380px;   
}   
  
.site-form-item-icon {   
  color: #bfbfbf;   
}   
  
:deep(.ant-input-affix-wrapper) {   
  border-radius: 6px;   
  padding: 8px 11px;   
    
  &:hover, &:focus {   
    border-color: #667eea;   
  }   
}   
  
.code-button {   
  height: 40px;   
  border-radius: 6px;   
  background: #f5f7ff;   
  border-color: #667eea;   
  color: #667eea;   
    
  &:hover:not(:disabled) {   
    background: #667eea;   
    border-color: #667eea;   
    color: white;   
  }   
    
  &:disabled {   
    color: #999;   
    border-color: #d9d9d9;   
    background: #f5f5f5;   
  }   
}   
  
.forget-password-button {   
  height: 45px;   
  border-radius: 6px;   
  font-size: 16px;   
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);   
  border: none;   
  
  &:hover {   
    background: linear-gradient(90deg, #764ba2 0%, #667eea 100%);   
  }   
}   
  
.login-link {   
  text-align: center;   
  margin-top: 16px;   
  color: #666;   
  
  a {   
    color: #667eea;   
    text-decoration: none;   
      
    &:hover {   
      color: #764ba2;   
      text-decoration: underline;   
    }   
  }   
}   
  
.ant-space {   
  display: flex;   
  gap: 8px !important;   
}   
</style>