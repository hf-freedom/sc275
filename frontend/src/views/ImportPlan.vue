<template>
  <div class="import-plan">
    <h2>导入学习计划</h2>
    <el-form :model="form" label-width="120px" class="plan-form">
      <el-form-item label="计划名称">
        <el-input v-model="form.name" placeholder="请输入计划名称" />
      </el-form-item>
      <el-form-item label="计划描述">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入计划描述" />
      </el-form-item>
      <el-form-item label="开始日期">
        <el-date-picker v-model="form.startDate" type="date" placeholder="选择开始日期" />
      </el-form-item>
      <el-form-item label="预计完成日期">
        <el-date-picker v-model="form.expectedEndDate" type="date" placeholder="选择完成日期" />
      </el-form-item>
      <el-form-item label="每日任务量">
        <el-input-number v-model="form.dailyTaskAmount" :min="1" :max="10" />
      </el-form-item>
    </el-form>

    <h3>任务列表</h3>
    <el-button type="primary" @click="addTask" style="margin-bottom: 20px">添加任务</el-button>
    <el-table :data="form.tasks" border class="task-table">
      <el-table-column prop="name" label="任务名称" width="200">
        <template #default="{ row }">
          <el-input v-model="row.name" size="small" />
        </template>
      </el-table-column>
      <el-table-column prop="description" label="任务描述">
        <template #default="{ row }">
          <el-input v-model="row.description" size="small" />
        </template>
      </el-table-column>
      <el-table-column prop="level" label="难度" width="120">
        <template #default="{ row }">
          <el-select v-model="row.level" size="small">
            <el-option label="基础" value="BASIC" />
            <el-option label="中级" value="INTERMEDIATE" />
            <el-option label="高级" value="ADVANCED" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column prop="estimatedDays" label="预计天数" width="120">
        <template #default="{ row }">
          <el-input-number v-model="row.estimatedDays" :min="1" :max="30" size="small" />
        </template>
      </el-table-column>
      <el-table-column prop="prerequisiteTaskNames" label="前置任务" width="200">
        <template #default="{ row }">
          <el-select v-model="row.prerequisiteTaskNames" multiple size="small" placeholder="选择前置任务">
            <el-option
              v-for="task in form.tasks.filter(t => t !== row && t.name)"
              :key="task.name"
              :label="task.name"
              :value="task.name"
            />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ $index }">
          <el-button type="danger" size="small" @click="removeTask($index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="form-actions">
      <el-button @click="resetForm">重置</el-button>
      <el-button type="primary" @click="submitForm">导入计划</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()

const form = ref({
  name: '',
  description: '',
  startDate: new Date(),
  expectedEndDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000),
  dailyTaskAmount: 3,
  tasks: [
    { name: '', description: '', level: 'BASIC', estimatedDays: 1, prerequisiteTaskNames: [] }
  ]
})

const addTask = () => {
  form.value.tasks.push({ name: '', description: '', level: 'BASIC', estimatedDays: 1, prerequisiteTaskNames: [] })
}

const removeTask = (index) => {
  if (form.value.tasks.length > 1) {
    form.value.tasks.splice(index, 1)
  } else {
    ElMessage.warning('至少保留一个任务')
  }
}

const resetForm = () => {
  form.value = {
    name: '',
    description: '',
    startDate: new Date(),
    expectedEndDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000),
    dailyTaskAmount: 3,
    tasks: [
      { name: '', description: '', level: 'BASIC', estimatedDays: 1, prerequisiteTaskNames: [] }
    ]
  }
}

const submitForm = async () => {
  if (!form.value.name) {
    ElMessage.error('请输入计划名称')
    return
  }
  if (form.value.tasks.some(t => !t.name)) {
    ElMessage.error('请填写所有任务名称')
    return
  }
  try {
    await api.importPlan(form.value)
    ElMessage.success('计划导入成功！')
    router.push('/plans')
  } catch (e) {
    ElMessage.error('导入失败：' + (e.response?.data?.message || e.message))
  }
}
</script>

<style scoped>
.import-plan {
  padding: 20px;
}

.plan-form {
  max-width: 600px;
  margin-bottom: 30px;
}

.task-table {
  margin-bottom: 20px;
}

.form-actions {
  text-align: center;
  margin-top: 30px;
}
</style>
