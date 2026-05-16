<template>
  <el-container class="app-container">
    <el-header class="header">
      <h1>学习计划监督系统</h1>
      <div class="header-right">
        <el-badge :value="unreadCount" class="reminder-badge" hidden>
          <el-button type="primary" @click="showReminders = true">通知</el-button>
        </el-badge>
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px" class="aside">
        <el-menu :default-active="activeMenu" router class="menu">
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/import">导入计划</el-menu-item>
          <el-menu-item index="/plans">计划列表</el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <el-dialog v-model="showReminders" title="通知中心" width="700px">
    <template #header>
      <div class="dialog-header">
        <span>通知中心</span>
        <el-tag type="warning" size="small">未读: {{ unreadCount }}</el-tag>
      </div>
    </template>
    
    <div v-if="reminders.length > 0" class="reminder-container">
      <div 
        v-for="reminder in reminders" 
        :key="reminder.id" 
        class="reminder-item"
        :class="{ 'unread': !reminder.isRead }"
      >
        <div class="reminder-header">
          <el-tag :type="getReminderType(reminder.type)" size="small">
            {{ getReminderTypeName(reminder.type) }}
          </el-tag>
          <span class="reminder-time">{{ formatTime(reminder.createdAt) }}</span>
        </div>
        <div class="reminder-content">{{ reminder.message }}</div>
        <div class="reminder-actions">
          <el-button type="primary" size="small" link @click="markAsRead(reminder.id)">
            标记已读
          </el-button>
        </div>
      </div>
    </div>
    
    <el-empty v-else description="暂无通知" />
    
    <template #footer>
      <el-button @click="markAllAsRead" v-if="reminders.length > 0">全部标记已读</el-button>
      <el-button @click="showReminders = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from './api'

const route = useRoute()
const showReminders = ref(false)
const reminders = ref([])

const activeMenu = computed(() => route.path)
const unreadCount = computed(() => reminders.value.length)

const getReminderType = (type) => {
  const typeMap = {
    INCOMPLETE_WARNING: 'warning',
    TASK_BLOCKED: 'danger',
    MAKEUP_REQUIRED: 'warning',
    GOAL_ADJUSTED: 'info',
    REWARD_EARNED: 'success',
    DIFFICULTY_INCREASE: 'info'
  }
  return typeMap[type] || 'info'
}

const getReminderTypeName = (type) => {
  const nameMap = {
    INCOMPLETE_WARNING: '未完成警告',
    TASK_BLOCKED: '任务阻塞',
    MAKEUP_REQUIRED: '需要补学',
    GOAL_ADJUSTED: '目标调整',
    REWARD_EARNED: '获得奖励',
    DIFFICULTY_INCREASE: '难度建议'
  }
  return nameMap[type] || '其他通知'
}

const formatTime = (time) => {
  return new Date(time).toLocaleString('zh-CN')
}

const loadReminders = async () => {
  try {
    const res = await api.getUnreadReminders()
    reminders.value = res.data
  } catch (e) {
    console.error('加载通知失败', e)
  }
}

const markAsRead = async (reminderId) => {
  try {
    await api.markReminderAsRead(reminderId)
    reminders.value = reminders.value.filter(r => r.id !== reminderId)
  } catch (e) {
    console.error('标记已读失败', e)
  }
}

const markAllAsRead = async () => {
  try {
    for (const reminder of reminders.value) {
      await api.markReminderAsRead(reminder.id)
    }
    reminders.value = []
  } catch (e) {
    console.error('标记全部已读失败', e)
  }
}

onMounted(() => {
  loadReminders()
  setInterval(loadReminders, 30000)
})
</script>

<style scoped>
.app-container {
  height: 100vh;
}

.header {
  background-color: #409eff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.header h1 {
  margin: 0;
  font-size: 20px;
}

.aside {
  background-color: #f5f7fa;
}

.menu {
  border-right: none;
}

.main {
  background-color: #fff;
  padding: 20px;
}

.dialog-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.reminder-container {
  max-height: 500px;
  overflow-y: auto;
}

.reminder-item {
  padding: 15px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  margin-bottom: 12px;
  background: #fff;
  transition: all 0.3s;
}

.reminder-item.unread {
  border-left: 4px solid #409eff;
  background: #f0f9ff;
}

.reminder-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.reminder-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 10px;
}

.reminder-actions {
  text-align: right;
}

.reminder-time {
  color: #999;
  font-size: 12px;
}
</style>
