import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import ImportPlan from '../views/ImportPlan.vue'
import PlanList from '../views/PlanList.vue'
import PlanDetail from '../views/PlanDetail.vue'

const routes = [
  { path: '/', name: 'Home', component: Home },
  { path: '/import', name: 'ImportPlan', component: ImportPlan },
  { path: '/plans', name: 'PlanList', component: PlanList },
  { path: '/plans/:id', name: 'PlanDetail', component: PlanDetail }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
