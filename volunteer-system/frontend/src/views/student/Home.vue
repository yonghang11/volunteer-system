<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <div>
        <div class="page-title">我的首页</div>
        <div class="page-sub">
          <template v-if="currentStage">
            当前进度：第 {{ currentStage.no }} 环节 · {{ currentStage.name }}（{{ phaseInfo.text }}）
          </template>
          <template v-else>正在获取环节信息</template>
        </div>
      </div>
      <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
    </div>

    <div class="hero-banner" v-if="currentStage">
      <div>
        <div class="hero-title">{{ currentStage.name }}</div>
        <div class="hero-sub">
          时间窗：{{ currentStage.startTime }} ~ {{ currentStage.endTime }}
          <template v-if="currentStage.phase === 'OPEN'">
            · 本环节剩余 {{ formatRemain(stage.remainingMs) }}
          </template>
        </div>
      </div>
      <el-button
        v-if="currentAction"
        type="primary"
        class="sys-btn-primary"
        @click="go(currentAction.path)"
      >
        {{ currentAction.label }}
      </el-button>
    </div>

    <div class="card sys-card--flow">
      <div class="card-title">
        <span>选拔流程</span>
        <span class="muted">共 {{ stages.length }} 个环节</span>
      </div>
      <div v-if="loading" class="sys-skeleton">
        <div class="sys-skeleton-row" />
        <div class="sys-skeleton-row" />
        <div class="sys-skeleton-row" />
      </div>
      <div v-else-if="!stages.length" class="sys-empty">
        <div class="sys-empty-icon">
          <el-icon><Refresh /></el-icon>
        </div>
        <div class="sys-empty-text">尚未配置环节时间窗</div>
        <div class="sys-empty-hint">环节由管理员统一配置，配置完成后会自动出现在此处</div>
        <el-button size="small" :icon="Refresh" @click="loadAll">刷新环节</el-button>
      </div>
      <div v-else class="timeline">
        <div
          v-for="item in stages"
          :key="item.code"
          class="tl-item"
          :class="{
            active: item.code === currentCode,
            done: item.phase === 'CLOSED'
          }"
        >
          <div class="tl-axis">
            <span class="tl-dot">{{ item.no }}</span>
          </div>
          <div class="tl-body">
            <div class="tl-head">
              <span class="tl-name">{{ item.name }}</span>
              <el-tag size="small" :type="phaseTag(item.phase).type" effect="plain">
                {{ phaseTag(item.phase).text }}
              </el-tag>
              <el-tag v-if="item.code === currentCode" size="small" effect="dark">
                当前环节
              </el-tag>
            </div>
            <div class="tl-time">时间窗：{{ item.startTime }} ~ {{ item.endTime }}</div>
            <div v-if="item.remark" class="tl-remark">{{ item.remark }}</div>
            <el-button
              v-if="item.code === currentCode && currentAction"
              size="small"
              type="primary"
              class="tl-action"
              @click="go(currentAction.path)"
            >
              {{ currentAction.label }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="grid-2">
      <div class="card sys-card--flow">
        <div class="card-title">
          <span>我的意向</span>
          <el-tag v-if="myIntent" size="small" type="info" effect="plain">
            第 {{ myIntent.round }} 轮
          </el-tag>
        </div>

        <div v-if="loading" class="sys-skeleton">
          <div class="sys-skeleton-row" />
          <div class="sys-skeleton-row" />
          <div class="sys-skeleton-row" />
        </div>

        <el-empty
          v-else-if="!myIntent || !myIntent.items || !myIntent.items.length"
          description="尚未填报导师意向"
          :image-size="72"
        >
          <el-button
            type="primary"
            size="small"
            class="sys-btn-primary"
            @click="go('/student/intent')"
          >
            开始填报意向
          </el-button>
        </el-empty>

        <template v-else>
          <div class="intent-list">
            <div v-for="(item, index) in myIntent.items" :key="item.id" class="intent-chip">
              <span class="intent-index">{{ index + 1 }}</span>
              <div class="intent-info">
                <span class="intent-mentor">{{ item.mentorName }}</span>
                <span class="intent-lab">{{ item.labName }}</span>
              </div>
              <el-tag size="small" type="info" effect="plain">{{ intentStatusText(item.status) }}</el-tag>
            </div>
          </div>
          <div class="intent-flags">
            <span class="flag-item">
              服从实训室内部调剂：
              <b>{{ myIntent.acceptInner ? '是' : '否' }}</b>
            </span>
            <span class="flag-item">
              服从全院跨实训室调剂：
              <b>{{ myIntent.acceptCross ? '是' : '否' }}</b>
            </span>
          </div>
        </template>

        <el-button link type="primary" class="card-link" @click="go('/student/intent')">
          前往意向填报
        </el-button>
      </div>

      <div class="card sys-card--flow">
        <div class="card-title">
          <span>我的待办</span>
          <span class="muted">实时汇总</span>
        </div>
        <div v-if="loading" class="sys-skeleton">
          <div class="sys-skeleton-row" />
          <div class="sys-skeleton-row" />
          <div class="sys-skeleton-row" />
        </div>
        <div v-else class="todo-list">
          <div class="todo-item" v-for="todo in todos" :key="todo.label">
            <div>
              <div class="todo-label">{{ todo.label }}</div>
              <div class="todo-hint">{{ todo.hint }}</div>
            </div>
            <div class="todo-right">
              <span
                v-if="!Number.isNaN(parseInt(todo.value))"
                :key="todo.value"
                class="todo-value"
                v-count-up="parseInt(todo.value)"
                :data-suffix="todo.value.replace(/^\d+/, '')"
              >
                {{ todo.value }}
              </span>
              <span v-else class="todo-value">{{ todo.value }}</span>
              <el-button link type="primary" @click="go(todo.path)">查看</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useStageStore } from '@/stores/stage'
import {
  fetchMyIntent,
  fetchMyInterviews,
  fetchMyOffers,
  fetchPublicList,
  fetchStageList,
  fetchSupplementRounds
} from '@/api'
import { PHASE_MAP, PUBLIC_STATUS_MAP, formatRemain, tagOf } from '@/utils/format'

const router = useRouter()
const userStore = useUserStore()
const stage = useStageStore()

const loading = ref(false)
const stages = ref([])
const myIntent = ref(null)
const offersData = ref({ offers: [], canConfirm: false, confirmDeadline: null })
const interviews = ref([])
const publicList = ref([])
const currentRound = ref(1)

let timer = null

const currentStage = computed(() => {
  if (stage.current) return stage.current
  if (!stages.value.length) return null
  const open = stages.value.find((item) => item.phase === 'OPEN')
  if (open) return open
  const closed = stages.value.filter((item) => item.phase === 'CLOSED')
  if (closed.length) return closed[closed.length - 1]
  return stages.value[0]
})

const currentCode = computed(() => (currentStage.value ? currentStage.value.code : ''))
const phaseInfo = computed(() =>
  tagOf(PHASE_MAP, currentStage.value ? currentStage.value.phase : '')
)

const pendingOfferCount = computed(
  () => offersData.value.offers.filter((o) => o.type === 'OFFER' && o.status === 'PENDING').length
)

const myPublicRow = computed(() =>
  publicList.value.find((row) => Number(row.studentId) === Number(userStore.user?.id))
)

const todos = computed(() => [
  {
    label: '待确认拟录取',
    value: pendingOfferCount.value ? `${pendingOfferCount.value} 个` : '暂无',
    hint: pendingOfferCount.value ? '请在确认时间窗内完成确认，逾期将释放名额' : '当前没有待确认的拟录取记录',
    path: '/student/result'
  },
  {
    label: '面试安排',
    value: interviews.value.length ? `${interviews.value.length} 场` : '暂无',
    hint: interviews.value.some((i) => i.status === 'PENDING')
      ? '请按时间与地点准时参加面试'
      : '面试结果与评分可在面试安排页查看',
    path: '/student/interview'
  },
  {
    label: '公示结果',
    value: myPublicRow.value ? tagOf(PUBLIC_STATUS_MAP, myPublicRow.value.status).text : '暂无',
    hint: myPublicRow.value ? '如有异议可在公示期内提交' : '公示名单发布后可在此查看',
    path: '/student/result'
  }
])

const currentAction = computed(() => {
  const current = currentStage.value
  if (!current) return null
  if (current.code === 'INTENT') {
    return { label: current.phase === 'OPEN' ? '去填报' : '去查看', path: '/student/intent' }
  }
  if (current.code === 'INTERVIEW') {
    return { label: '去查看', path: '/student/interview' }
  }
  if (['CONFIRM', 'SUPPLEMENT', 'TRANSFER', 'PUBLICITY'].includes(current.code)) {
    return { label: '去查看', path: '/student/result' }
  }
  return null
})

function phaseTag(phase) {
  return tagOf(PHASE_MAP, phase)
}

function intentStatusText(status) {
  const map = {
    SUBMITTED: '已提交',
    OFFERED: '已获拟录取',
    CONFIRMED: '已确认',
    RELEASED: '已释放'
  }
  return map[status] || status || '未知'
}

function go(path) {
  router.push(path)
}

async function loadStages() {
  try {
    stages.value = await fetchStageList()
  } catch (e) {
    stages.value = []
  }
}

async function loadRound() {
  try {
    const rounds = (await fetchSupplementRounds()) || []
    const openRounds = rounds.filter((item) => item.openFlag === 1)
    if (openRounds.length) {
      currentRound.value = Math.max(...openRounds.map((item) => item.roundNo))
    }
  } catch (e) {
    currentRound.value = 1
  }
}

async function loadAll() {
  loading.value = true
  await loadStages()
  await loadRound()
  await Promise.allSettled([
    fetchMyIntent(currentRound.value).then((data) => {
      myIntent.value = data
    }),
    fetchMyOffers().then((data) => {
      offersData.value = data || { offers: [], canConfirm: false, confirmDeadline: null }
    }),
    fetchMyInterviews().then((data) => {
      interviews.value = data || []
    }),
    fetchPublicList().then((data) => {
      publicList.value = data || []
    })
  ])
  loading.value = false
}

onMounted(() => {
  loadAll()
  timer = setInterval(loadStages, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.timeline {
  position: relative;
  padding-left: 4px;
}

.tl-item {
  display: flex;
  gap: 16px;
  padding: 14px 16px 14px 10px;
  border-radius: var(--sys-radius-sm);
  border: 1px solid transparent;
  transition: background 0.2s var(--sys-ease), border-color 0.2s ease;
  position: relative;
}

.tl-item:hover {
  background: rgba(15, 23, 42, 0.03);
}

.tl-item.active {
  background: var(--brand-light);
  border-color: rgba(59, 130, 246, 0.35);
}

.tl-axis {
  width: 30px;
  flex: 0 0 30px;
  display: flex;
  justify-content: center;
  position: relative;
}

.tl-axis::after {
  content: "";
  position: absolute;
  top: 30px;
  bottom: -32px;
  width: 1px;
  background: rgba(15, 23, 42, 0.1);
}

.tl-item:last-child .tl-axis::after {
  display: none;
}

.tl-dot {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.05);
  color: var(--sys-text-sub);
  border: 1px solid var(--sys-border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  z-index: 1;
  transition: background 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease;
}

.tl-item.active .tl-dot {
  background: linear-gradient(135deg, var(--sys-primary), var(--sys-cyan));
  border-color: transparent;
  color: #fff;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.16), 0 0 18px rgba(59, 130, 246, 0.45);
}

.tl-item.done .tl-dot {
  background: rgba(52, 211, 153, 0.14);
  color: var(--sys-success);
  border-color: rgba(52, 211, 153, 0.35);
}

.tl-body {
  flex: 1;
  min-width: 0;
}

.tl-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tl-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
}

.tl-time {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-sub);
  font-variant-numeric: tabular-nums;
}

.tl-remark {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-muted);
}

.tl-action {
  margin-top: 10px;
}

.intent-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.intent-chip {
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid var(--sys-border);
  border-radius: var(--sys-radius-sm);
  padding: 10px 12px;
  background: rgba(15, 23, 42, 0.04);
  transition: background 0.2s ease, border-color 0.2s ease;
}

.intent-chip:hover {
  background: rgba(15, 23, 42, 0.06);
  border-color: var(--sys-border-strong);
}

.intent-index {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--sys-primary), var(--sys-cyan));
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 22px;
  font-variant-numeric: tabular-nums;
}

.intent-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.intent-mentor {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
}

.intent-lab {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

.intent-flags {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed rgba(15, 23, 42, 0.12);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.flag-item {
  font-size: 13px;
  color: var(--text-sub);
}

.flag-item b {
  color: var(--text-main);
}

.card-link {
  margin-top: 6px;
}

.todo-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.todo-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid var(--sys-border);
  border-radius: var(--sys-radius-sm);
  padding: 12px 14px;
  background: rgba(15, 23, 42, 0.04);
  transition: background 0.2s ease, border-color 0.2s ease;
}

.todo-item:hover {
  background: rgba(15, 23, 42, 0.06);
  border-color: var(--sys-border-strong);
}

.todo-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
}

.todo-hint {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
  line-height: 1.6;
}

.todo-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
}

.todo-value {
  font-size: 15px;
  font-weight: 600;
  color: var(--sys-primary);
  font-variant-numeric: tabular-nums;
}

@media (max-width: 1100px) {
  .grid-2 {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
