<template>
  <div class="page">
    <div class="page-head">
      <div>
        <div class="page-title">面试评审</div>
        <div class="page-sub">为本实训室候选人评分，并编排拟录取、递补与内部调剂名单</div>
      </div>
      <div class="head-actions">
        <el-button :icon="User" @click="openProfile">个人信息</el-button>
        <el-select v-model="round" style="width: 120px" @change="reload">
          <el-option :value="1" label="第 1 轮" />
          <el-option :value="2" label="第 2 轮" />
          <el-option :value="3" label="第 3 轮" />
        </el-select>
        <el-button :icon="Refresh" @click="reload">刷新</el-button>
      </div>
    </div>

    <div class="grid-4">
      <div class="stat-card">
        <div class="stat-label">本人名额</div>
        <div class="stat-value" v-count-up="quota.quota" :key="`quota-${quota.quota}`">{{ quota.quota }}</div>
        <div class="stat-hint">{{ quota.labName || '未绑定导师信息' }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">已用名额</div>
        <div class="stat-value accent" v-count-up="quota.used" :key="`used-${quota.used}`">{{ quota.used }}</div>
        <div class="stat-hint">第 {{ round }} 轮已提交拟录取（含已确认）</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">剩余名额</div>
        <div class="stat-value" v-count-up="quota.remain" :key="`remain-${quota.remain}`">{{ quota.remain }}</div>
        <div class="stat-hint">提交批量名单时不可超出名额</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">候选人</div>
        <div class="stat-value" v-count-up="candidates.length" :key="`cand-${candidates.length}`">{{ candidates.length }}</div>
        <div class="stat-hint">已评分 {{ scoredCount }} 人</div>
      </div>
    </div>

    <div class="card sys-card--flow">
      <div class="card-title">
        <span>候选人评审</span>
        <span class="muted">四维各 0-25 分，总分由系统汇总</span>
      </div>

      <div v-if="loading" class="sys-skeleton">
        <div class="sys-skeleton-row" />
        <div class="sys-skeleton-row" />
        <div class="sys-skeleton-row" />
      </div>

      <template v-else>
        <el-empty v-if="!candidates.length" :image-size="92" description="暂无本室候选人">
          <div class="sys-empty-hint">候选人来自学生填报意向与实训室管理员生成的面试安排。</div>
          <el-button size="small" @click="reload">刷新候选人</el-button>
        </el-empty>

        <el-table v-else :data="candidates" border stripe size="small">
          <el-table-column label="学生" min-width="150">
            <template #default="{ row }">
              <div class="student-cell">
                <span class="student-name">{{ row.studentName }}</span>
                <span class="muted">{{ row.classNo }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="studentNo" label="学号" width="120" />
          <el-table-column prop="labName" label="实训室" min-width="150" />
          <el-table-column prop="slotStart" label="面试时间" min-width="150" />
          <el-table-column label="学习态度" width="88" align="center">
            <template #default="{ row }">{{ dash(row.attitude) }}</template>
          </el-table-column>
          <el-table-column label="逻辑思维" width="88" align="center">
            <template #default="{ row }">{{ dash(row.logic) }}</template>
          </el-table-column>
          <el-table-column label="编程基础" width="88" align="center">
            <template #default="{ row }">{{ dash(row.coding) }}</template>
          </el-table-column>
          <el-table-column label="时间投入" width="88" align="center">
            <template #default="{ row }">{{ dash(row.commitment) }}</template>
          </el-table-column>
          <el-table-column label="总分" width="76" align="center">
            <template #default="{ row }">
              <b v-if="row.total !== null && row.total !== undefined">{{ row.total }}</b>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="结论" width="104" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.result" size="small" :type="resultTag(row.result).type" effect="light">
                {{ resultTag(row.result).text }}
              </el-tag>
              <span v-else class="muted">未评分</span>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ row.remark || '—' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openScore(row)">
                {{ row.result ? '修改评分' : '评分' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </div>

    <div class="card sys-card--flow">
      <div class="card-title">
        <span>录取名单编排</span>
        <div class="batch-actions">
          <span class="muted">共 {{ candidates.length }} 名候选人</span>
          <el-button type="primary" class="sys-btn-primary" :loading="submitting" @click="submitBatch">提交名单</el-button>
        </div>
      </div>

      <div v-if="loading" class="sys-skeleton">
        <div class="sys-skeleton-row" />
        <div class="sys-skeleton-row" />
        <div class="sys-skeleton-row" />
      </div>

      <template v-else>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="请在左侧勾选候选人并加入右侧列表"
          description="拟录取人数不可超出本人名额；递补顺序即补位顺序，可在下方列表中调整。"
          class="mb-16"
        />

        <div class="batch-grid">
          <div v-for="panel in panels" :key="panel.key" class="batch-panel">
            <div class="panel-head">
              <span class="panel-title">{{ panel.title }}</span>
              <span class="muted">{{ targets[panel.key].length }} 人</span>
            </div>
            <el-transfer
              v-model="targets[panel.key]"
              :data="candidateDataFor(panel.key)"
              :titles="['候选人', '已选']"
              filterable
              filter-placeholder="搜索姓名 / 学号"
            />
            <div v-if="panel.key === 'waitlist' && targets.waitlist.length" class="order-list">
              <div class="order-head">递补顺位</div>
              <div v-for="(sid, index) in targets.waitlist" :key="sid" class="order-item">
                <span class="order-no">{{ index + 1 }}</span>
                <span class="order-name">{{ nameOf(sid) }}</span>
                <div class="order-ops">
                  <el-button link :disabled="index === 0" @click="move('waitlist', index, -1)">上移</el-button>
                  <el-button
                    link
                    :disabled="index === targets.waitlist.length - 1"
                    @click="move('waitlist', index, 1)"
                  >
                    下移
                  </el-button>
                </div>
              </div>
            </div>
            <div v-if="panel.key !== 'waitlist'" class="panel-tip">{{ panel.tip }}</div>
          </div>
        </div>
      </template>
    </div>

    <el-dialog v-model="dialogVisible" title="面试评分" width="620px" destroy-on-close>
      <div class="dialog-student">
        <span class="student-name">{{ currentRow.studentName }}</span>
        <span class="muted">{{ currentRow.studentNo }} · {{ currentRow.labName }}</span>
        <el-tag size="small" effect="plain">{{ currentRow.mentorName }}</el-tag>
      </div>

      <div class="score-list">
        <div v-for="dim in dimensions" :key="dim.key" class="score-row">
          <span class="score-label">{{ dim.label }}</span>
          <el-slider v-model="scoreForm[dim.key]" :min="0" :max="25" class="score-slider" />
          <el-input-number
            v-model="scoreForm[dim.key]"
            :min="0"
            :max="25"
            size="small"
            controls-position="right"
            class="score-input"
          />
        </div>
      </div>

      <div class="score-total">
        总分：<b>{{ scoreTotal }}</b> / 100
      </div>

      <el-form label-width="84px" class="score-extra">
        <el-form-item label="面试结论">
          <el-select v-model="scoreForm.result" style="width: 220px">
            <el-option value="OFFER" label="拟录取" />
            <el-option value="WAITLIST" label="递补" />
            <el-option value="INNER" label="内部调剂" />
            <el-option value="REJECT" label="不予录取" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="scoreForm.remark"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="可选，填写面试表现说明"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" class="sys-btn-primary" :loading="scoring" @click="submitScore">提交评分</el-button>
      </template>
    </el-dialog>

    <!-- 导师个人信息编辑弹窗 -->
    <el-dialog
      v-model="profileVisible"
      title="个人信息"
      width="560px"
      destroy-on-close
      @open="loadProfile"
    >
      <el-form :model="profileForm" label-width="96px" class="profile-form">
        <el-form-item label="姓名">
          <el-input v-model="profileForm.name" maxlength="32" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="职位">
          <el-input v-model="profileForm.title" maxlength="32" placeholder="副教授 / 讲师 / 工程师 等" />
        </el-form-item>
        <el-form-item label="所在实验室">
          <el-select
            v-model="profileForm.labId"
            placeholder="请选择实验室"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="lab in labOptions"
              :key="lab.id"
              :value="lab.id"
              :label="lab.name + (lab.code ? '（' + lab.code + '）' : '')"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="所需名额">
          <el-input-number
            v-model="profileForm.quota"
            :min="0"
            :max="99"
            controls-position="right"
            style="width: 140px"
          />
          <span class="profile-tip">人 / 轮</span>
        </el-form-item>
        <el-form-item label="研究方向">
          <el-input
            v-model="profileForm.direction"
            maxlength="64"
            placeholder="如：视觉检测算法、图像分割等"
          />
        </el-form-item>
        <el-form-item label="个人简介">
          <el-input
            v-model="profileForm.intro"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请输入个人简介，学生填报意向时可看到"
          />
        </el-form-item>
        <el-form-item label="跨室调剂">
          <el-switch
            v-model="profileForm.acceptCrossTransfer"
            active-text="接受"
            inactive-text="不接受"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="profileVisible = false">取消</el-button>
        <el-button
          type="primary"
          class="sys-btn-primary"
          :loading="savingProfile"
          @click="saveProfile"
        >
          保存修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  fetchMentorQuota,
  fetchLabInterviews,
  fetchMentorOffers,
  fetchSupplementRounds,
  scoreInterview,
  submitOfferBatch,
  fetchMentorMe,
  updateMentorMe,
  fetchMentorLabs
} from '@/api'
import { RESULT_MAP, tagOf } from '@/utils/format'

const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const scoring = ref(false)
const round = ref(1)
const candidates = ref([])
const quota = reactive({ quota: 0, used: 0, remain: 0, labName: '' })

/* ---------- 个人信息弹窗 ---------- */
const profileVisible = ref(false)
const savingProfile = ref(false)
const labOptions = ref([])
const profileForm = reactive({
  name: '',
  title: '',
  labId: null,
  quota: 0,
  direction: '',
  intro: '',
  acceptCrossTransfer: true
})

async function openProfile() {
  profileVisible.value = true
}

async function loadProfile() {
  try {
    const [me, labs] = await Promise.all([
      fetchMentorMe().catch(() => null),
      fetchMentorLabs().catch(() => [])
    ])
    labOptions.value = labs || []
    if (me) {
      profileForm.name = me.name || ''
      profileForm.title = me.title || ''
      profileForm.labId = me.labId ?? null
      profileForm.quota = Number(me.quota) || 0
      profileForm.direction = me.direction || ''
      profileForm.intro = me.intro || ''
      profileForm.acceptCrossTransfer = Boolean(me.acceptCrossTransfer)
    }
  } catch (e) {
    /* 错误由拦截器处理 */
  }
}

async function saveProfile() {
  if (!profileForm.name.trim()) {
    ElMessage.warning('姓名不能为空')
    return
  }
  savingProfile.value = true
  try {
    await updateMentorMe({ ...profileForm })
    ElMessage.success('个人信息已更新')
    profileVisible.value = false
    // 刷新名额统计（可能 labId 变了）
    await loadQuota()
  } catch (e) {
    /* 错误由拦截器处理 */
  } finally {
    savingProfile.value = false
  }
}

const dialogVisible = ref(false)
const currentRow = ref({})
const scoreForm = reactive({
  attitude: 0,
  logic: 0,
  coding: 0,
  commitment: 0,
  result: 'OFFER',
  remark: ''
})

const targets = reactive({ offers: [], waitlist: [], inner: [] })

const dimensions = [
  { key: 'attitude', label: '学习态度' },
  { key: 'logic', label: '逻辑思维' },
  { key: 'coding', label: '编程基础' },
  { key: 'commitment', label: '时间投入' }
]

const panels = [
  { key: 'offers', title: '拟录取名单', tip: '建议按本人名额 1:1 选择' },
  { key: 'waitlist', title: '递补名单', tip: '' },
  { key: 'inner', title: '内部调剂候选', tip: '本室内其他导师名额未满时可内部调剂' }
]

const scoredCount = computed(
  () => candidates.value.filter((row) => row.result !== null && row.result !== undefined).length
)

const scoreTotal = computed(
  () =>
    (Number(scoreForm.attitude) || 0) +
    (Number(scoreForm.logic) || 0) +
    (Number(scoreForm.coding) || 0) +
    (Number(scoreForm.commitment) || 0)
)

const allCandidateData = computed(() =>
  candidates.value.map((row) => ({
    key: row.studentId,
    label: `${row.studentName}（${row.studentNo}）`
  }))
)

// 三名单互斥：某 panel 的候选池 = 全部候选人 - 已被其他两份名单占用的学生
function candidateDataFor(panelKey) {
  const otherKeys = panels.filter((p) => p.key !== panelKey).map((p) => p.key)
  const blocked = new Set()
  for (const k of otherKeys) {
    for (const sid of targets[k]) blocked.add(sid)
  }
  return allCandidateData.value.filter((item) => !blocked.has(item.key))
}

function nameOf(studentId) {
  const row = candidates.value.find((item) => item.studentId === studentId)
  return row ? `${row.studentName}（${row.studentNo}）` : String(studentId)
}

function dash(value) {
  return value === null || value === undefined ? '—' : value
}

function resultTag(result) {
  return tagOf(RESULT_MAP, result)
}

async function detectRound() {
  try {
    const rounds = await fetchSupplementRounds()
    const openRounds = (rounds || []).filter((item) => item.openFlag === 1)
    if (openRounds.length) {
      round.value = Math.max(...openRounds.map((item) => item.roundNo))
    }
  } catch (e) {
    /* 保持默认第 1 轮 */
  }
}

async function loadQuota() {
  try {
    const data = (await fetchMentorQuota(round.value)) || {}
    quota.quota = Number(data.quota) || 0
    quota.used = Number(data.used) || 0
    quota.remain = Number(data.remain) || 0
    quota.labName = [data.labName, data.mentorName].filter(Boolean).join(' · ')
  } catch (e) {
    /* 未绑定导师信息时保持 0 */
  }
}

async function loadCandidates() {
  try {
    candidates.value = (await fetchLabInterviews(round.value)) || []
  } catch (e) {
    candidates.value = []
  }
}

async function loadExisting() {
  try {
    const rows = (await fetchMentorOffers(round.value)) || []
    targets.offers = rows
      .filter((row) => row.type === 'OFFER' && ['PENDING', 'CONFIRMED'].includes(row.status))
      .map((row) => row.studentId)
    targets.waitlist = rows
      .filter((row) => row.type === 'WAITLIST' && row.status === 'PENDING')
      .sort((a, b) => (a.rankNo || 0) - (b.rankNo || 0))
      .map((row) => row.studentId)
    targets.inner = rows
      .filter((row) => row.type === 'INNER' && row.status === 'PENDING')
      .map((row) => row.studentId)
    quota.used = targets.offers.length
    quota.remain = Math.max(0, quota.quota - quota.used)
  } catch (e) {
    /* 首次编排时没有历史记录 */
  }
}

async function reload() {
  loading.value = true
  await loadQuota()
  await Promise.allSettled([loadCandidates(), loadExisting()])
  loading.value = false
}

function openScore(row) {
  currentRow.value = row
  scoreForm.attitude = row.attitude === null || row.attitude === undefined ? 0 : row.attitude
  scoreForm.logic = row.logic === null || row.logic === undefined ? 0 : row.logic
  scoreForm.coding = row.coding === null || row.coding === undefined ? 0 : row.coding
  scoreForm.commitment = row.commitment === null || row.commitment === undefined ? 0 : row.commitment
  scoreForm.result = row.result || 'OFFER'
  scoreForm.remark = row.remark || ''
  dialogVisible.value = true
}

async function submitScore() {
  scoring.value = true
  try {
    await scoreInterview(currentRow.value.interviewId, {
      attitude: scoreForm.attitude,
      logic: scoreForm.logic,
      coding: scoreForm.coding,
      commitment: scoreForm.commitment,
      result: scoreForm.result,
      remark: scoreForm.remark
    })
    ElMessage.success('评分已提交')
    dialogVisible.value = false
    await Promise.allSettled([loadCandidates(), loadQuota()])
  } catch (e) {
    /* 错误提示由响应拦截器处理 */
  } finally {
    scoring.value = false
  }
}

function move(key, index, delta) {
  const list = targets[key]
  const target = index + delta
  if (target < 0 || target >= list.length) return
  const [item] = list.splice(index, 1)
  list.splice(target, 0, item)
}

async function submitBatch() {
  if (!targets.offers.length && !targets.waitlist.length && !targets.inner.length) {
    ElMessage.warning('请至少选择一名候选人加入名单')
    return
  }
  submitting.value = true
  try {
    const data = await submitOfferBatch({
      round: round.value,
      offers: targets.offers,
      waitlist: targets.waitlist,
      inner: targets.inner
    })
    ElMessage.success(
      `名单已提交：拟录取 ${data.offerCount} 人，递补 ${data.waitlistCount} 人，内部调剂 ${data.innerCount} 人`
    )
    await Promise.allSettled([loadCandidates(), loadExisting(), loadQuota()])
  } catch (e) {
    /* 错误提示由响应拦截器处理 */
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await detectRound()
  await reload()
})
</script>

<style scoped>
.head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mb-16 {
  margin-bottom: 16px;
}

.student-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.student-name {
  font-weight: 600;
  color: var(--sys-text);
}

.batch-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.batch-grid {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.batch-panel {
  border: 1px solid var(--sys-border);
  border-radius: var(--sys-radius-sm);
  padding: 14px 16px 16px;
  background: rgba(15, 23, 42, 0.04);
  transition: border-color 0.3s ease, background 0.3s ease;
}

.batch-panel:hover {
  border-color: var(--sys-border-strong);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--sys-text);
}

.panel-tip {
  margin-top: 10px;
  font-size: 12px;
  color: var(--sys-text-muted);
}

.order-list {
  margin-top: 14px;
  border-top: 1px dashed var(--sys-border-strong);
  padding-top: 10px;
}

.order-head {
  font-size: 12px;
  color: var(--sys-text-sub);
  margin-bottom: 6px;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px;
  border: 1px solid var(--sys-border);
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.04);
  margin-bottom: 6px;
}

.order-no {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--brand-light);
  color: var(--brand);
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.order-name {
  flex: 1;
  font-size: 13px;
  color: var(--sys-text);
}

.order-ops {
  display: flex;
  align-items: center;
  gap: 4px;
}

.dialog-student {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--sys-border);
  margin-bottom: 14px;
}

.score-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.score-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.score-label {
  width: 76px;
  flex: 0 0 76px;
  font-size: 13px;
  color: var(--sys-text-sub);
}

.score-slider {
  flex: 1;
}

.score-input {
  width: 110px;
  flex: 0 0 110px;
}

.score-total {
  margin: 16px 0 4px;
  padding: 12px 14px;
  background: rgba(15, 23, 42, 0.04);
  border: 1px solid var(--sys-border);
  border-radius: var(--sys-radius-sm);
  font-size: 14px;
  color: var(--sys-text-sub);
}

.score-total b {
  font-size: 22px;
  color: var(--sys-primary);
  margin: 0 2px;
}

.score-extra {
  margin-top: 10px;
}

.score-extra :deep(.el-form-item__label) {
  color: var(--sys-text-sub);
  font-weight: 400;
}

.profile-form :deep(.el-form-item__label) {
  color: var(--sys-text-sub);
  font-weight: 400;
}

.profile-tip {
  font-size: 12px;
  color: #6b7280;
  margin-left: 8px;
}
</style>
