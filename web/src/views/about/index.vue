<template>
  <div class="about-container">
    <div class="content-wrapper">
      <div class="beta-notice">
        <div class="beta-badge">
          <span class="beta-text">BETA</span>
        </div>
        <h2 class="title">项目内测中</h2>
        <p class="description">
          本项目目前处于内测状态，我们正在努力完善功能和修复问题。
          <br>
          如遇到 bug，敬请原谅并欢迎反馈！
        </p>
        <div class="decorative-line"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
let name = ref('about')
</script>

<style scoped>
.about-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.content-wrapper {
  max-width: 500px;
  width: 100%;
}

.beta-notice {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  padding: 40px 30px;
  text-align: center;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  position: relative;
  overflow: hidden;
}

.beta-notice::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea, #764ba2, #667eea);
  animation: gradient-move 3s ease-in-out infinite;
}

.beta-badge {
  display: inline-block;
  background: linear-gradient(45deg, #ff6b6b, #ff8e53);
  padding: 8px 16px;
  border-radius: 20px;
  margin-bottom: 20px;
  box-shadow: 0 4px 15px rgba(255, 107, 107, 0.3);
  transform: rotate(-2deg);
}

.beta-text {
  color: white;
  font-weight: bold;
  font-size: 12px;
  letter-spacing: 1px;
}

.title {
  color: #333;
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 15px;
  background: linear-gradient(45deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.description {
  color: #666;
  font-size: 16px;
  line-height: 1.6;
  margin-bottom: 25px;
}

.decorative-line {
  width: 60px;
  height: 3px;
  background: linear-gradient(90deg, #667eea, #764ba2);
  margin: 0 auto;
  border-radius: 2px;
  opacity: 0.7;
}

@keyframes gradient-move {
  0%, 100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .about-container {
    padding: 15px;
  }
  
  .beta-notice {
    padding: 30px 20px;
  }
  
  .title {
    font-size: 24px;
  }
  
  .description {
    font-size: 14px;
  }
}

/* 悬浮动画效果 */
.beta-notice {
  transition: transform 0.3s ease;
}

.beta-notice:hover {
  transform: translateY(-5px);
}
</style>