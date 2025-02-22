<template>
  <div class="home">
    <h1>欢迎使用 MindCare</h1>
    <div class="features">
      <div class="feature-card">
        <h2>表情分析</h2>
        <p>通过定期捕捉面部表情，分析您的情绪变化，为您提供及时的情绪反馈。</p>
      </div>
      <div class="feature-card">
        <h2>心理咨询</h2>
        <p>为您推荐附近的心理咨询资源，帮助您找到专业的心理咨询师。</p>
      </div>
      <div class="feature-card">
        <h2>心灵社区</h2>
        <p>加入我们的社区，与志同道合的朋友交流分享，互相支持和鼓励。</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.home {
  text-align: center;
}

.features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 2rem;
  margin-top: 2rem;
}

.feature-card {
  padding: 1.5rem;
  border-radius: 8px;
  background: #f8f9fa;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

h1 {
  color: #2c3e50;
  margin-bottom: 2rem;
}

h2 {
  color: #42b983;
  margin-bottom: 1rem;
}
</style> 