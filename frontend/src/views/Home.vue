<template>
  <div class="home">
    <h2>今日概览</h2>
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-title">今日任务</div>
          <div class="stat-value">{{ summary.todayTaskCount || 0 }}</div>
          <div class="stat-sub">已完成: {{ summary.todayCompletedCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card expired">
          <div class="stat-title">过期任务</div>
          <div class="stat-value">{{ summary.expiredTaskCount || 0 }}</div>
          <div class="stat-sub">需要补学</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card completed">
          <div class="stat-title">总完成率</div>
          <div class="stat-value">{{ summary.completionRate || 0 }}%</div>
          <div class="stat-sub">总任务: {{ summary.totalTasks || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card plans">
          <div class="stat-title">进行中计划</div>
          <div class="stat-value">{{ summary.plans?.length || 0 }}</div>
          <div class="stat-sub">继续加油！</div>
        </el-card>
      </el-col>
    </el-row>

    <el-alert
      v-if="showProgressChange"
      :title="'进度更新成功！'"
      type="success"
      :closable="true"
      @close="showProgressChange = false"
      class="progress-alert"
    >
      <template #default>
        <div class="progress-details">
          <p><strong>完成率变化：</strong>{{ progressChange.beforeRate }}% → {{ progressChange.afterRate }}% 
            <span class="change-positive">(+{{ (progressChange.afterRate - progressChange.beforeRate).toFixed(2) }}%)</span>
          </p>
          <p><strong>剩余任务：</strong>{{ progressChange.remainingTasks }} 个</p>
          <p v-if="progressChange.planAdjustments.length > 0">
            <strong>受影响的计划：</strong>
            <el-tag v-for="(adjust, idx) in progressChange.planAdjustments" :key="idx" size="small" style="margin-left: 5px;">
              {{ adjust.planName }}: 预计完成 {{ adjust.expectedEndDate }}
            </el-tag>
          </p>
        </div>
      </template>
    </el-alert>

    <h3>学习计划状态监控</h3>
    <el-table :data="summary.plans || []" border class="status-table">
      <el-table-column prop="name" label="计划名称" width="200" />
      <el-table-column label="连续完成天数" width="150">
        <template #default="{ row }">
          <el-tag v-if="row.consecutiveCompletedDays > 0" type="success">
            {{ row.consecutiveCompletedDays }} 天
          </el-tag>
          <span v-else class="no-data">-</span>
        </template>
      </el-table-column>
      <el-table-column label="连续未完成天数" width="150">
        <template #default="{ row }">
          <el-tag v-if="row.consecutiveIncompleteDays > 0" type="warning">
            {{ row.consecutiveIncompleteDays }} 天
          </el-tag>
          <span v-else class="no-data">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="dailyTaskAmount" label="每日任务量" width="120">
        <template #default="{ row }">
          <span :class="{ 'task-amount-reduced': row.consecutiveIncompleteDays >= 2 }">
            {{ row.dailyTaskAmount }} 个/天
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="completionRate" label="完成率" width="120">
        <template #default="{ row }">
          <el-progress :percentage="row.completionRate || 0" :stroke-width="12" />
        </template>
      </el-table-column>
      <el-table-column label="获得奖励" width="150">
        <template #default="{ row }">
          <div v-if="row.rewards && row.rewards.length > 0">
            <el-tag v-for="(reward, idx) in row.rewards.slice(0, 2)" :key="idx" size="small" type="success" class="reward-tag">
              {{ reward }}
            </el-tag>
            <el-tag v-if="row.rewards.length > 2" size="small">+{{ row.rewards.length - 2 }}</el-tag>
          </div>
          <span v-else class="no-data">-</span>
        </template>
      </el-table-column>
    </el-table>

    <h3>今日任务</h3>
    <el-table :data="summary.todayTasks || []" border class="task-table">
      <el-table-column prop="name" label="任务名称" />
      <el-table-column prop="planId" label="所属计划" :formatter="getPlanName" />
      <el-table-column prop="level" label="难度">
        <template #default="{ row }">
          <el-tag :type="getLevelType(row.level)">{{ getLevelName(row.level) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ getStatusName(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="前置依赖" width="250">
        <template #default="{ row }">
          <div v-if="row.prerequisiteTaskIds && row.prerequisiteTaskIds.length > 0">
            <el-tag
              v-for="id in row.prerequisiteTaskIds"
              :key="id"
              size="small"
              :type="getPrerequisiteStatus(id)"
              class="prereq-tag"
            >
              {{ getTaskNameById(id) }}
            </el-tag>
          </div>
          <span v-else class="no-data">无</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <div v-if="row.status === 'BLOCKED'">
            <el-button type="danger" size="small" disabled>被阻塞</el-button>
            <el-tooltip placement="top">
              <template #content>
                <div class="block-reason">
                  <p><strong>任务被阻塞</strong></p>
                  <p>请先完成以下基础任务：</p>
                  <div v-for="id in row.prerequisiteTaskIds" :key="id" style="color: #f56c6c;">
                    • {{ getTaskNameById(id) }} ({{ getStatusName(getTaskById(id)?.status) }})
                  </div>
                </div>
              </template>
              <el-button type="info" size="small" link>查看原因</el-button>
            </el-tooltip>
          </div>
          <el-button
            v-else-if="row.status !== 'COMPLETED'"
            type="primary"
            size="small"
            @click="completeTask(row.id)"
          >
            完成
          </el-button>
          <el-tag v-else type="success">已完成</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <h3 v-if="summary.expiredTasks?.length > 0" class="expired-title">过期任务（需要补学）</h3>
    <el-table v-if="summary.expiredTasks?.length > 0" :data="summary.expiredTasks || []" border class="task-table">
      <el-table-column prop="name" label="任务名称" />
      <el-table-column prop="scheduledDate" label="原定日期" />
      <el-table-column prop="level" label="难度">
        <template #default="{ row }">
          <el-tag :type="getLevelType(row.level)">{{ row.level }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button
            type="primary"
            size="small"
            @click="completeTask(row.id)"
          >
            完成
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const summary = ref({})
const showProgressChange = ref(false)
const progressChange = ref({
  beforeRate: 0,
  afterRate: 0,
  remainingTasks: 0,
  planAdjustments: []
})

const loadSummary = async () => {
  try {
    const res = await api.getDailySummary()
    summary.value = res.data
  } catch (e) {
    console.error('加载摘要失败', e)
  }
}

const completeTask = async (taskId) => {
  const beforeRate = summary.value.completionRate || 0
  const beforePlans = JSON.parse(JSON.stringify(summary.value.plans || []))

  try {
    await api.updateTaskStatus({ taskId, completed: true })
    await loadSummary()

    const afterRate = summary.value.completionRate || 0
    const remainingTasks = (summary.value.totalTasks || 0) - (summary.value.completedTasks || 0)

    const planAdjustments = []
    const afterPlans = summary.value.plans || []
    afterPlans.forEach((afterPlan, idx) => {
      const beforePlan = beforePlans[idx]
      if (beforePlan && afterPlan) {
        planAdjustments.push({
          planName: afterPlan.name,
          expectedEndDate: afterPlan.expectedEndDate || '待定',
          consecutiveDays: afterPlan.consecutiveCompletedDays || 0
        })
      }
    })

    progressChange.value = {
      beforeRate: beforeRate.toFixed(2),
      afterRate: afterRate.toFixed(2),
      remainingTasks,
      planAdjustments
    }

    showProgressChange.value = true
    ElMessage.success('任务已完成！进度已更新')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const getPlanName = (row) => {
  const plan = summary.value.plans?.find(p => p.id === row.planId)
  return plan?.name || '-'
}

const getLevelType = (level) => {
  const map = { BASIC: 'success', INTERMEDIATE: 'warning', ADVANCED: 'danger' }
  return map[level] || 'info'
}

const getStatusType = (status) => {
  const map = { PENDING: 'info', IN_PROGRESS: 'warning', COMPLETED: 'success', EXPIRED: 'danger', BLOCKED: 'danger' }
  return map[status] || 'info'
}

const getLevelName = (level) => {
  const map = { BASIC: '基础', INTERMEDIATE: '中级', ADVANCED: '高级' }
  return map[level] || level
}

const getStatusName = (status) => {
  const map = { PENDING: '待完成', IN_PROGRESS: '进行中', COMPLETED: '已完成', EXPIRED: '已过期', BLOCKED: '被阻塞' }
  return map[status] || status
}

const getAllTasks = () => {
  const allTasks = []
  const plans = summary.value.plans || []
  plans.forEach(plan => {
    if (plan.tasks) {
      allTasks.push(...plan.tasks)
    }
  })
  return allTasks
}

const getTaskById = (taskId) => {
  const allTasks = getAllTasks()
  return allTasks.find(t => t.id === taskId)
}

const getTaskNameById = (taskId) => {
  const task = getTaskById(taskId)
  return task?.name || taskId?.substring(0, 8) || '-'
}

const getPrerequisiteStatus = (taskId) => {
  const task = getTaskById(taskId)
  if (!task) return 'info'
  if (task.status === 'COMPLETED') return 'success'
  return 'warning'
}

onMounted(() => {
  loadSummary()
})
</script>

<style scoped>
.home {
  padding: 20px;
}

.stats-row {
  margin-bottom: 30px;
}

.stat-card {
  text-align: center;
}

.stat-card .stat-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 10px;
}

.stat-card .stat-value {
  font-size: 36px;
  font-weight: bold;
  color: #409eff;
}

.stat-card.expired .stat-value {
  color: #f56c6c;
}

.stat-card.completed .stat-value {
  color: #67c23a;
}

.stat-card.plans .stat-value {
  color: #e6a23c;
}

.stat-card .stat-sub {
  font-size: 12px;
  color: #999;
  margin-top: 10px;
}

.progress-alert {
  margin-bottom: 30px;
}

.progress-details {
  padding: 10px 0;
}

.progress-details p {
  margin: 8px 0;
  line-height: 1.6;
}

.change-positive {
  color: #67c23a;
  font-weight: bold;
}

.status-table {
  margin-bottom: 30px;
}

.task-amount-reduced {
  color: #f56c6c;
  font-weight: bold;
}

.no-data {
  color: #c0c4cc;
}

.reward-tag {
  margin: 2px;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.prereq-tag {
  margin: 2px;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.block-reason {
  padding: 5px;
}

.block-reason p {
  margin: 8px 0;
}

.block-reason ul {
  margin: 10px 0;
  padding-left: 20px;
}

.block-reason li {
  margin: 5px 0;
  color: #f56c6c;
}

.task-table {
  margin-bottom: 30px;
}

.expired-title {
  color: #f56c6c;
}
</style>
