<template>
  <div class="plan-list">
    <h2>学习计划列表</h2>
    <el-empty v-if="plans.length === 0" description="暂无学习计划，请先导入" />
    <el-row :gutter="20" v-else>
      <el-col :span="8" v-for="plan in plans" :key="plan.id">
        <el-card class="plan-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="plan-name">{{ plan.name }}</span>
              <div class="card-actions">
                <el-button type="warning" size="small" @click="openAdjustDialog(plan)">调整目标</el-button>
                <el-button type="primary" size="small" @click="viewDetail(plan.id)">查看</el-button>
              </div>
            </div>
          </template>
          <div class="plan-info">
            <p><strong>描述：</strong>{{ plan.description || '-' }}</p>
            <p><strong>开始日期：</strong>{{ plan.startDate }}</p>
            <p><strong>预计完成：</strong>{{ plan.expectedEndDate }}</p>
            <p><strong>每日任务量：</strong>{{ plan.dailyTaskAmount }}</p>
            <p><strong>总任务数：</strong>{{ plan.totalTasks }}</p>
            <p><strong>已完成：</strong>{{ plan.completedTasks }}</p>
            <p><strong>剩余任务：</strong>{{ (plan.totalTasks || 0) - (plan.completedTasks || 0) }} 个</p>
            <el-progress
              :percentage="plan.completionRate || 0"
              :format="(val) => val.toFixed(1) + '%'"
            />
            <p class="estimate-info">
              <el-tag type="info">预计还需 {{ Math.ceil(((plan.totalTasks || 0) - (plan.completedTasks || 0)) / plan.dailyTaskAmount) }} 天</el-tag>
            </p>
            <p class="streak-info" v-if="plan.consecutiveCompletedDays > 0">
              <el-tag type="success">连续完成 {{ plan.consecutiveCompletedDays }} 天</el-tag>
            </p>
            <p class="streak-info" v-if="plan.consecutiveIncompleteDays > 0">
              <el-tag type="warning">连续未完成 {{ plan.consecutiveIncompleteDays }} 天</el-tag>
            </p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="adjustDialogVisible" title="调整学习目标" width="500px">
      <el-form :model="adjustForm" label-width="120px">
        <el-form-item label="当前计划">
          <span>{{ currentPlan?.name }}</span>
        </el-form-item>
        <el-form-item label="预计完成日期">
          <el-date-picker
            v-model="adjustForm.newExpectedEndDate"
            type="date"
            placeholder="选择新的预计完成日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="每日任务量">
          <el-input-number v-model="adjustForm.newDailyTaskAmount" :min="1" :max="20" />
          <span style="margin-left: 10px; color: #999;">个/天</span>
        </el-form-item>
        <el-form-item label="预计变化">
          <div v-if="calculateEstimate()" class="estimate-preview">
            <p>当前：{{ Math.ceil(((currentPlan?.totalTasks || 0) - (currentPlan?.completedTasks || 0)) / (currentPlan?.dailyTaskAmount || 1)) }} 天完成</p>
            <p class="change-positive">调整后：{{ calculateEstimate() }} 天完成</p>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdjustGoal" :loading="adjustLoading">应用调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()
const plans = ref([])
const adjustDialogVisible = ref(false)
const adjustLoading = ref(false)
const currentPlan = ref(null)
const adjustForm = ref({
  planId: '',
  newExpectedEndDate: null,
  newDailyTaskAmount: null
})

const loadPlans = async () => {
  try {
    const res = await api.getAllPlans()
    plans.value = res.data
  } catch (e) {
    console.error('加载计划失败', e)
  }
}

const viewDetail = (planId) => {
  router.push(`/plans/${planId}`)
}

const openAdjustDialog = (plan) => {
  currentPlan.value = plan
  adjustForm.value = {
    planId: plan.id,
    newExpectedEndDate: plan.expectedEndDate ? new Date(plan.expectedEndDate) : null,
    newDailyTaskAmount: plan.dailyTaskAmount
  }
  adjustDialogVisible.value = true
}

const calculateEstimate = () => {
  if (!currentPlan.value || !adjustForm.value.newDailyTaskAmount) {
    return null
  }
  const remainingTasks = (currentPlan.value.totalTasks || 0) - (currentPlan.value.completedTasks || 0)
  return Math.ceil(remainingTasks / adjustForm.value.newDailyTaskAmount)
}

const submitAdjustGoal = async () => {
  adjustLoading.value = true
  try {
    await api.adjustGoal(adjustForm.value)
    ElMessage.success('目标已调整！预计完成日期已重新计算')
    adjustDialogVisible.value = false
    loadPlans()
  } catch (e) {
    ElMessage.error('调整失败')
  } finally {
    adjustLoading.value = false
  }
}

onMounted(() => {
  loadPlans()
})
</script>

<style scoped>
.plan-list {
  padding: 20px;
}

.plan-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.plan-name {
  font-weight: bold;
  font-size: 16px;
}

.card-actions {
  display: flex;
  gap: 8px;
}

.plan-info p {
  margin: 8px 0;
  font-size: 14px;
}

.estimate-info {
  margin-top: 10px;
}

.streak-info {
  margin-top: 15px !important;
}

.estimate-preview p {
  margin: 5px 0;
  font-size: 14px;
}

.change-positive {
  color: #67c23a;
  font-weight: bold;
}
</style>
