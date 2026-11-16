<template>
  <div class="layout">
    <header class="layout-header">
      <div class="brand">
        <!-- 侧边栏折叠开关 -->
        <button
          type="button"
          class="sys-collapse"
          :aria-label="collapsed ? '展开侧边栏' : '收起侧边栏'"
          @click="toggleCollapse"
        >
          <el-icon :size="18">
            <Expand v-if="collapsed" />
            <Fold v-else />
          </el-icon>
        </button>

        <div class="brand-logo">
          <el-icon :size="20"><School /></el-icon>
        </div>
        <div class="brand-text">
          <div class="brand-title">学生实训室选组选拔系统</div>
          <div class="brand-sub">Student Lab Selection &amp; Recruitment</div>
        </div>

        <!-- 面包屑：首页 / 当前页面 -->
        <nav class="sys-crumb" aria-label="面包屑">
          <span class="sys-crumb-item">首页</span>
          <el-icon class="sys-crumb-sep" :size="12"><ArrowRight /></el-icon>
          <span class="sys-crumb-item is-current">{{ currentLabel }}</span>
        </nav>
      </div>

      <div class="header-right">
        <div class="stage-pill">
          <template v-if="stage.current">
            <span class="stage-text">
              第 {{ stage.current.no }} 环节 · {{ stage.current.name }}
            </span>
            <el-tag size="small" :type="phaseInfo.type" effect="light">
              {{ phaseInfo.text }}
            </el-tag>
            <span v-if="stage.current.phase === 'OPEN'" class="stage-countdown">
              剩余 {{ formatRemain(stage.remainingMs) }}
            </span>
          </template>
          <span v-else class="stage-text muted">环节信息加载中</span>
        </div>

        <el-divider direction="vertical" />

        <el-dropdown trigger="click" @command="handleCommand">
          <span class="user-entry">
            <!-- 头像：取姓名首字，避免额外图片资源 -->
            <span class="sys-avatar">{{ avatarText }}</span>
            <span class="user-name">{{ userStore.displayName }}</span>
            <el-tag size="small" effect="plain" class="sys-role-tag">{{ roleText }}</el-tag>
            <el-icon class="user-arrow"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>
                账号：{{ userStore.user?.username }}
              </el-dropdown-item>
              <el-dropdown-item disabled>角色：{{ roleText }}</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div class="layout-body">
      <aside class="layout-aside" :class="{ 'is-collapsed': collapsed }">
        <el-menu
          :default-active="route.path"
          :collapse="collapsed"
          :collapse-transition="false"
          router
          class="layout-menu"
        >
          <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
            <el-icon><component :is="iconMap[item.icon]" /></el-icon>
            <template #title>{{ item.label }}</template>
          </el-menu-item>
        </el-menu>
        <div class="aside-foot">
          <div v-show="!collapsed">演示环境</div>
          <div v-show="!collapsed">v1.0 · 学术管理后台</div>
        </div>
      </aside>

      <main class="layout-main">
        <!-- 路由切换：淡入 + 上移 400ms -->
        <router-view v-slot="{ Component }">
          <transition name="sys-page" mode="out-in">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  School,
  UserFilled,
  ArrowDown,
  ArrowRight,
  Fold,
  Expand,
  HomeFilled,
  EditPen,
  Calendar,
  Trophy,
  DocumentChecked,
  OfficeBuilding,
  Setting
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useStageStore } from '@/stores/stage'
import { MENU_BY_ROLE, ROLE_MAP } from '@/utils/menu'
import { PHASE_MAP, formatRemain, tagOf } from '@/utils/format'

// 菜单图标按需引入，避免全量注册图标组件
const iconMap = {
  HomeFilled,
  EditPen,
  Calendar,
  Trophy,
  DocumentChecked,
  OfficeBuilding,
  Setting
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const stage = useStageStore()

const menus = computed(() => MENU_BY_ROLE[userStore.role] || [])
const roleText = computed(() => ROLE_MAP[userStore.role] || '未登录')
const phaseInfo = computed(() =>
  tagOf(PHASE_MAP, stage.current ? stage.current.phase : '')
)

// 当前页面名称用于面包屑
const currentLabel = computed(() => {
  const hit = menus.value.find((item) => item.path === route.path)
  return hit ? hit.label : '工作台'
})

// 头像文字：取显示名首字
const avatarText = computed(() => {
  const name = userStore.displayName || userStore.user?.username || '用'
  return name.replace(/[（(].*?[）)]/g, '').slice(0, 1)
})

/* ---------------- 侧边栏折叠（记忆用户偏好） ---------------- */
const COLLAPSE_KEY = 'sys_sidebar_collapsed'
const collapsed = ref(localStorage.getItem(COLLAPSE_KEY) === '1')

function toggleCollapse() {
  collapsed.value = !collapsed.value
  localStorage.setItem(COLLAPSE_KEY, collapsed.value ? '1' : '0')
}

onMounted(() => stage.start())
onUnmounted(() => stage.stop())

function handleCommand(command) {
  if (command !== 'logout') return
  ElMessageBox.confirm('确认退出当前账号吗？', '退出登录', {
    confirmButtonText: '退出',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      userStore.logout()
      ElMessage.success('已退出登录')
      router.replace('/login')
    })
    .catch(() => {})
}
</script>

<style scoped>
.layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: transparent;
}

/* ---------- 顶部导航 ---------- */
.layout-header {
  flex: 0 0 64px;
  height: 64px;
  padding: 0 20px 0 16px;
  background: #ffffff;
  border-bottom: 1px solid var(--sys-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  z-index: 20;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

/* 折叠按钮：hover 发光 */
.sys-collapse {
  flex: 0 0 auto;
  width: 34px;
  height: 34px;
  border-radius: var(--sys-radius-sm);
  border: 1px solid var(--sys-border);
  background: #f9fafb;
  color: var(--sys-text-sub);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease,
    background 0.2s ease;
}

.sys-collapse:hover {
  color: var(--sys-cyan);
  border-color: rgba(8, 145, 178, 0.4);
  background: rgba(8, 145, 178, 0.08);
  box-shadow: 0 0 12px rgba(8, 145, 178, 0.2);
}

.brand-logo {
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  border-radius: var(--sys-radius-sm);
  background: linear-gradient(135deg, var(--sys-primary) 0%, var(--sys-cyan) 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 22px rgba(59, 130, 246, 0.35);
}

.brand-text {
  min-width: 0;
}

.brand-title {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: var(--sys-text);
  white-space: nowrap;
}

.brand-sub {
  font-size: 11px;
  color: var(--sys-text-muted);
  letter-spacing: 0.4px;
  white-space: nowrap;
}

/* 面包屑 */
.sys-crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 16px;
  padding-left: 16px;
  border-left: 1px solid var(--sys-border);
  font-size: 13px;
}

.sys-crumb-item {
  color: var(--sys-text-muted);
  white-space: nowrap;
}

.sys-crumb-item.is-current {
  color: var(--sys-text);
  font-weight: 600;
}

.sys-crumb-sep {
  color: var(--sys-text-muted);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 0 0 auto;
}

.stage-pill {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 38px;
  padding: 0 14px;
  background: #f9fafb;
  border: 1px solid var(--sys-border);
  border-radius: 999px;
}

.stage-text {
  font-size: 13px;
  font-weight: 600;
  color: var(--sys-text);
  white-space: nowrap;
}

.stage-countdown {
  font-size: 13px;
  font-weight: 600;
  color: var(--sys-cyan);
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border-radius: 999px;
  border: 1px solid transparent;
  cursor: pointer;
  outline: none;
  color: var(--sys-text);
  transition: background 0.2s ease, border-color 0.2s ease;
}

.user-entry:hover {
  background: #f3f4f6;
  border-color: var(--sys-border);
}

/* 头像：渐变底 + 首字 */
.sys-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, var(--sys-primary) 0%, var(--sys-cyan) 100%);
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.22);
}

.user-name {
  font-size: 13px;
  font-weight: 500;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sys-role-tag {
  border-color: rgba(6, 182, 212, 0.4);
  color: var(--sys-cyan);
}

.user-arrow {
  color: var(--sys-text-muted);
}

/* ---------- 主体 ---------- */
.layout-body {
  flex: 1;
  min-height: 0;
  display: flex;
}

/* 侧边栏：宽度过渡 */
.layout-aside {
  width: 216px;
  flex: 0 0 216px;
  align-self: stretch;
  background: #ffffff;
  border-right: 1px solid var(--sys-border);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
  transition: width 320ms var(--sys-ease), flex-basis 320ms var(--sys-ease);
}

.layout-aside.is-collapsed {
  width: 76px;
  flex-basis: 76px;
}

.layout-menu {
  border-right: none;
  background: transparent;
  padding: 12px 10px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
}

.layout-menu:not(.el-menu--collapse) {
  width: 100%;
}

/* 菜单项：图标 + 文字；文字随折叠渐显 */
.layout-menu :deep(.el-menu-item) {
  position: relative;
  height: 46px;
  line-height: 46px;
  border-radius: var(--sys-radius-sm);
  margin-bottom: 6px;
  color: var(--sys-text-sub);
  font-size: 14px;
  transition: color 0.2s ease, background 0.24s ease;
}

.layout-menu :deep(.el-menu-item .el-icon) {
  transition: color 0.2s ease, transform 0.24s var(--sys-ease);
}

.layout-menu :deep(.el-menu-item:hover) {
  background: #f3f4f6;
  color: var(--sys-text);
}

.layout-menu :deep(.el-menu-item:hover .el-icon) {
  color: var(--sys-cyan);
}

/* 选中项：渐变底 + 左侧发光竖条 */
.layout-menu :deep(.el-menu-item.is-active) {
  color: #fff;
  font-weight: 600;
  background: linear-gradient(
    90deg,
    rgba(59, 130, 246, 0.9) 0%,
    rgba(8, 145, 178, 0.85) 100%
  );
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.25);
}

.layout-menu :deep(.el-menu-item.is-active)::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  width: 3px;
  height: 22px;
  border-radius: 0 3px 3px 0;
  background: linear-gradient(180deg, var(--sys-primary), var(--sys-cyan));
  box-shadow: 0 0 12px rgba(6, 182, 212, 0.8);
  transform: translateY(-50%);
}

.layout-menu :deep(.el-menu-item.is-active .el-icon) {
  color: var(--sys-cyan);
}

/* 折叠态：只留图标并居中 */
.layout-menu.el-menu--collapse :deep(.el-menu-item) {
  justify-content: center;
  padding: 0 !important;
}

.aside-foot {
  padding: 14px 20px 18px;
  font-size: 11px;
  color: var(--sys-text-muted);
  line-height: 1.8;
  border-top: 1px solid var(--sys-border);
  white-space: nowrap;
}

.layout-main {
  flex: 1;
  min-width: 0;
  height: 100%;
  padding: 24px;
  overflow-y: auto;
  overflow-x: hidden;
  scroll-behavior: smooth;
}

/* 窄屏：自动收起侧边栏，避免挤压内容 */
@media (max-width: 1180px) {
  .sys-crumb {
    display: none;
  }

  .brand-sub {
    display: none;
  }
}

@media (max-width: 900px) {
  .stage-pill {
    display: none;
  }

  .brand-text {
    display: none;
  }

  .layout-main {
    padding: 16px;
  }
}
</style>
