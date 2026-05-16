<template>
  <div class="plan-detail">
    <el-button @click="$router.back()" style="margin-bottom: 20px">返回</el-button>
    
    <el-card v-if="plan" class="info-card">
      <template #header>
        <h2>{{ plan.name }}</h2>
      </template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="描述">{{ plan.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ plan.startDate }}</el-descriptions-item>
        <el-descriptions-item label="预计完成">{{ plan.expectedEndDate }}</el-descriptions-item>
        <el-descriptions-item label="每日任务量">{{ plan.dailyTaskAmount }}</el-descriptions-item>
        <el-descriptions-item label="总任务数">{{ plan.totalTasks }}</el-descriptions-item>
        <el-descriptions-item label="已完成">{{ plan.completedTasks }}</el-descriptions-item>
        <el-descriptions-item label="完成率" :span="3">
          <el-progress
            :percentage="plan.completionRate || 0"
            :format="(val) => val.toFixed(1) + '%'"
          />
        </el-descriptions-item>
      </el-descriptions>

      <div class="adjust-section">
        <h3>调整目标</h3>
        <el-form :model="adjustForm" label-width="120px">
          <el-form-item label="预计完成日期">
            <el-date-picker
              v-model="adjustForm.newExpectedEndDate"
              type="date"
              placeholder="选择新的预计完成日期"
              style="width: 250px"
            />
            <span v-if="plan.expectedEndDate" class="current-value">
              当前：{{ plan.expectedEndDate }}
            </span>
          </el-form-item>
          <el-form-item label="每日任务量">
            <el-input-number v-model="adjustForm.newDailyTaskAmount" :min="1" :max="20" />
            <span class="current-value">
              当前：{{ plan.dailyTaskAmount }} 个/天
            </span>
          </el-form-item>
          <el-form-item label="预计完成天数">
            <div class="estimate-preview">
              <span class="estimate-current">当前：{{ Math.ceil(((plan.totalTasks || 0) - (plan.completedTasks || 0)) / (plan.dailyTaskAmount || 1)) }} 天</span>
              <el-icon class="estimate-arrow"><ArrowRight /></el-icon>
              <span class="estimate-new">调整后：{{ calculateNewEstimate() }} 天</span>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="adjustGoal" :loading="adjustLoading">应用调整</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div v-if="plan.rewards?.length > 0" class="rewards-section">
        <h3>获得的奖励</h3>
        <el-tag v-for="(reward, idx) in plan.rewards" :key="idx" type="success" style="margin: 5px">
          {{ reward }}
        </el-tag>
      </div>
    </el-card>

    <h3 style="margin: 20px 0">任务列表</h3>
    <el-table :data="tasks" border class="task-table">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="name" label="任务名称" width="250" />
      <el-table-column prop="description" label="描述" />
      <el-table-column prop="level" label="难度" width="100">
        <template #default="{ row }">
          <el-tag :type="getLevelType(row.level)">{{ row.level }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="scheduledDate" label="计划日期" width="120" />
      <el-table-column prop="completedDate" label="完成日期" width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="补学" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.isMakeup" type="warning">是</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="前置依赖" width="200">
        <template #default="{ row }">
          <div v-if="row.prerequisiteTaskIds?.length > 0">
            <el-tag v-for="id in row.prerequisiteTaskIds" :key="id" size="small" style="margin: 2px">
              {{ getTaskName(id) }}
            </el-tag>
          </div>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button
            v-if="row.status !== 'COMPLETED' && row.status !== 'BLOCKED'"
            type="primary"
            size="small"
            @click="completeTask(row.id)"
          >
            完成
          </el-button>
          <el-tag v-else-if="row.status === 'COMPLETED'" type="success">已完成</el-tag>
          <el-tag v-else type="danger">已阻塞</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import api from '../api'

const route = useRoute()
const planId = computed(() => route.params.id)

const plan = ref(null)
const tasks = ref([])
const adjustLoading = ref(false)
const adjustForm = ref({
  planId: '',
  newExpectedEndDate: null,
  newDailyTaskAmount: null
})

const loadPlan = async () => {
  try {
    const res = await api.getPlanById(planId.value)
    plan.value = res.data
    adjustForm.value.planId = planId.value
  } catch (e) {
    console.error('加载计划失败', e)
  }
}

const loadTasks = async () => {
  try {
    const res = await api.getTasksByPlanId(planId.value)
    tasks.value = res.data
  } catch (e) {
    console.error('加载任务失败', e)
  }
}

const completeTask = async (taskId) => {
  try {
    await api.updateTaskStatus({ taskId, completed: true })
    ElMessage.success('任务已完成！')
    loadPlan()
    loadTasks()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const calculateNewEstimate = () => {
  if (!plan.value || !adjustForm.value.newDailyTaskAmount) {
    return Math.ceil(((plan.value?.totalTasks || 0) - (plan.value?.completedTasks || 0)) / (plan.value?.dailyTaskAmount || 1))
  }
  const remainingTasks = (plan.value.totalTasks || 0) - (plan.value.completedTasks || 0)
  return Math.ceil(remainingTasks / adjustForm.value.newDailyTaskAmount)
}

const adjustGoal = async () => {
  adjustLoading.value = true
  try {
    await api.adjustGoal(adjustForm.value)
    ElMessage.success('目标已调整！预计完成日期已重新计算')
    loadPlan()
  } catch (e) {
    ElMessage.error('调整失败')
  } finally {
    adjustLoading.value = false
  }
}

const getLevelType = (level) => {
  const map = { BASIC: 'success', INTERMEDIATE: 'warning', ADVANCED: 'danger' }
  return map[level] || 'info'
}

const getStatusType = (status) => {
  const map = { PENDING: 'info', IN_PROGRESS: 'warning', COMPLETED: 'success', EXPIRED: 'danger', BLOCKED: 'danger' }
  return map[status] || 'info'
}

const getTaskName = (taskId) => {
  const task = tasks.value.find(t => t.id === taskId)
  return task?.name || taskId?.substring(0, 8)
}

onMounted(() => {
  loadPlan()
  loadTasks()
})
</script>

<style scoped>
.plan-detail {
  padding: 20px;
}

.info-card {
  margin-bottom: 20px;
}

.adjust-section {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.current-value {
  margin-left: 15px;
  color: #999;
  font-size: 14px;
}

.estimate-preview {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.estimate-current {
  color: #666;
}

.estimate-arrow {
  margin: 0 10px;
  color: #409eff;
}

.estimate-new {
  color: #67c23a;
  font-weight: bold;
}

.rewards-section {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.task-table {
  margin-top: 20px;
}
</style>
