<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-left">
        <div class="platform-title">
          <div class="title-line">创万联</div>
          <div class="title-line">InteliConnect</div>
          <div class="title-line">物联网平台</div>
        </div>

      </div>
      <div class="login-form">
        <h2 class="welcome-text">欢迎回来</h2>
        <p class="sub-title">请登录您的账户</p>

        <a-form :model="loginForm" @finish="handleSubmit" class="login-form-content">
          <a-form-item name="username" :rules="[{ required: true, message: '请输入用户名' }]">
            <a-input v-model:value="loginForm.username" size="large" placeholder="用户名">
              <template #prefix>
                <UserOutlined class="site-form-item-icon" />
              </template>
            </a-input>
          </a-form-item>

          <a-form-item name="password" :rules="[{ required: true, message: '请输入密码' }]">
            <a-input-password v-model:value="loginForm.password" size="large" placeholder="密码">
              <template #prefix>
                <LockOutlined class="site-form-item-icon" />
              </template>
            </a-input-password>
          </a-form-item>

          <div class="login-options">
            <a-checkbox v-model:checked="rememberMe">记住我</a-checkbox>
            <router-link to="/forgotPassword" class="forgot-link">忘记密码？</router-link>
          </div>

          <a-form-item>
            <a-button type="primary" html-type="submit" class="login-button" size="large" :loading="loading">
              登录
            </a-button>
          </a-form-item>
        </a-form>

        <div class="register-link">
          还没有账号? <router-link to="/register">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-box {
  display: flex;
  background: white;
  border-radius: 20px;
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.1);
  width: 1000px;
  max-width: 100%;
  min-height: 600px;
  overflow: hidden;
}

.login-left {
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

  .bg-image {
    width: 100%;
    max-width: 400px;
    height: auto;
    opacity: 0.6;
  }

  @media (max-width: 768px) {
    display: none;
  }
}

.login-form {
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

.login-form-content {
  max-width: 380px;
}

.site-form-item-icon {
  color: #bfbfbf;
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.forgot-link {
  color: #1890ff;
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

.login-button {
  width: 100%;
  height: 45px;
  border-radius: 6px;
  font-size: 16px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  border: none;

  &:hover {
    background: linear-gradient(90deg, #764ba2 0%, #667eea 100%);
  }
}

:deep(.ant-input-affix-wrapper) {
  border-radius: 6px;
  padding: 8px 11px;
}

:deep(.ant-checkbox-wrapper) {
  color: #666;
}

.register-link {
  text-align: center;
  margin-top: 16px;
  color: #666;

  a {
    color: #1890ff;
    
    &:hover {
      color: #40a9ff;
    }
  }
}
</style>

<script setup>
import { ref, reactive } from 'vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import { loginIn } from '@/api/user'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { message } from 'ant-design-vue'

const router = useRouter()
const store = useStore()
const loginForm = reactive({
  username: '',
  password: '',
})

const loading = ref(false)
const rememberMe = ref(false)

const handleSubmit = (values) => {
  loginIn(values)
    .then((res) => {
      const { data, errorCode } = res.data
      console.log('auth', data)
      if (errorCode == 200) {
        store.commit('auth/GENERATE_ROUTES', data)
        store.commit('auth/SET_AUTH', data)
        console.log("__________________****")
        console.log(store.getters['auth/token'])
        router.push('/dashboard')
      } else if(errorCode == 2007){
        message.warn('账号不存在')
      }else if(errorCode == 2003){
        message.warn('密码错误')
      }else{
        message.warn('系统错误，请重新登录')
      }
    })
    .catch((err) => {
      console.log(err)
    })
}
</script>
