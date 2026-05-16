import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8004/api',
  timeout: 10000
})

export default {
  importPlan(data) {
    return api.post('/plans/import', data)
  },
  getAllPlans() {
    return api.get('/plans')
  },
  getPlanById(planId) {
    return api.get(`/plans/${planId}`)
  },
  getTasksByPlanId(planId) {
    return api.get(`/plans/${planId}/tasks`)
  },
  updateTaskStatus(data) {
    return api.put('/tasks/status', data)
  },
  adjustGoal(data) {
    return api.post('/plans/adjust', data)
  },
  getDailySummary() {
    return api.get('/daily-summary')
  },
  getUnreadReminders() {
    return api.get('/reminders/unread')
  },
  markReminderAsRead(reminderId) {
    return api.put(`/reminders/${reminderId}/read`)
  }
}
