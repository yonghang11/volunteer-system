import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { HOME_BY_ROLE } from '@/utils/menu'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: 'student/home',
        component: () => import('@/views/student/Home.vue'),
        meta: { title: '学生首页', roles: ['STUDENT'] }
      },
      {
        path: 'student/intent',
        component: () => import('@/views/student/Intent.vue'),
        meta: { title: '意向填报', roles: ['STUDENT'] }
      },
      {
        path: 'student/interview',
        component: () => import('@/views/student/Interview.vue'),
        meta: { title: '面试安排', roles: ['STUDENT'] }
      },
      {
        path: 'student/result',
        component: () => import('@/views/student/Result.vue'),
        meta: { title: '录取结果', roles: ['STUDENT'] }
      },
      {
        path: 'mentor/review',
        component: () => import('@/views/mentor/Review.vue'),
        meta: { title: '面试评审', roles: ['MENTOR'] }
      },
      {
        path: 'lab/manage',
        component: () => import('@/views/lab/Manage.vue'),
        meta: { title: '实训室管理', roles: ['LAB_ADMIN'] }
      },
      {
        path: 'admin',
        component: () => import('@/views/admin/Admin.vue'),
        meta: { title: '学院管理', roles: ['COLLEGE_ADMIN'] }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.path === '/login') {
    if (userStore.token) {
      return next(HOME_BY_ROLE[userStore.role] || '/login')
    }
    return next()
  }

  if (!userStore.token) {
    return next('/login')
  }

  if (to.path === '/') {
    return next(HOME_BY_ROLE[userStore.role] || '/login')
  }

  if (to.meta && to.meta.roles && !to.meta.roles.includes(userStore.role)) {
    return next(HOME_BY_ROLE[userStore.role] || '/login')
  }

  return next()
})

export default router
