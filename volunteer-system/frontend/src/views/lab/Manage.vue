<template>
  <div class="page">
    <div class="page-head">
      <div>
        <div class="page-title">实训室管理</div>
        <div class="page-sub">
          {{ lab ? lab.name : '未绑定实训室' }} · 名额排程、候选人评分与录取汇总
        </div>
      </div>
      <div class="head-actions">
        <el-button :icon="OfficeBuilding" @click="openLabInfo">实训室信息</el-button>
        <el-select v-model="round" style="width: 120px" @change="reload">
          <el-option :value="1" label="第 1 轮" />
          <el-option :value="2" label="第 2 轮" />
          <el-option :value="3" label="第 3 轮" />
        </el-select>
        <el-button :icon="Refresh" @click="reload">刷新</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- 名额与排程 -->
      <el-tab-pane name="quota" label="名额与排程">
        <div class="tab-body">
          <div class="grid-4">
            <div class="stat-card">
              <div class="stat-label">全室名额</div>
              <div class="stat-value" v-count-up="lab ? lab.quota : 0" :key="`quota-${lab ? lab.quota : 0}`">
                {{ lab ? lab.quota : 0 }}
              </div>
              <div class="stat-hint">{{ lab ? lab.code : '—' }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">剩余名额</div>
              <div class="stat-value accent" v-count-up="lab ? lab.remain : 0" :key="`remain-${lab ? lab.remain : 0}`">
                {{ lab ? lab.remain : 0 }}
              </div>
              <div class="stat-hint">第 {{ round }} 轮实时剩余</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">导师人数</div>
              <div class="stat-value" v-count-up="mentors.length" :key="`mentor-${mentors.length}`">{{ mentors.length }}</div>
              <div class="stat-hint">本室在编导师</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">已生成面试</div>
              <div class="stat-value" v-count-up="candidates.length" :key="`cand-${candidates.length}`">{{ candidates.length }}</div>
              <div class="stat-hint">第 {{ round }} 轮面试安排</div>
            </div>
          </div>

          <div class="card sys-card--flow">
            <div class="card-title">
              <span>导师名额</span>
              <span class="muted">{{ lab ? lab.direction : '' }}</span>
            </div>

            <div v-if="loading" class="sys-skeleton">
              <div class="sys-skeleton-row" />
              <div class="sys-skeleton-row" />
              <div class="sys-skeleton-row" />
            </div>

            <template v-else>
              <el-empty v-if="!mentors.length" :image-size="92" description="暂无本室导师信息">
                <div class="sys-empty-hint">导师与名额由学院管理员维护，可稍后刷新查看。</div>
                <el-button size="small" @click="reload">刷新</el-button>
              </el-empty>

              <el-table v-else :data="mentors" border stripe size="small">
                <el-table-column prop="name" label="导师" width="110" />
                <el-table-column prop="title" label="职称" width="100" />
                <el-table-column prop="direction" label="研究方向" min-width="180" />
                <el-table-column prop="quota" label="名额" width="90" align="center" />
                <el-table-column label="剩余" width="90" align="center">
                  <template #default="{ row }">
                    <b :class="{ 'zero-text': row.remain <= 0 }">{{ row.remain }}</b>
                  </template>
                </el-table-column>
                <el-table-column label="跨室调剂" width="110" align="center">
                  <template #default="{ row }">
                    <el-tag size="small" :type="row.acceptCrossTransfer === 1 ? 'success' : 'info'" effect="plain">
                      {{ row.acceptCrossTransfer === 1 ? '接受' : '不接受' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="intro" label="导师简介" min-width="220" show-overflow-tooltip />
              </el-table>
            </template>
          </div>

          <div class="card sys-card--flow">
            <div class="card-title">
              <span>面试排程</span>
              <span class="muted">以本室该轮意向学生为基础生成</span>
            </div>
            <div class="schedule-row">
              <div class="schedule-info">
                <div class="sub-text">一键生成面试安排</div>
                <div class="section-tip">
                  系统将按本室第 {{ round }} 轮意向学生逐人安排 20 分钟面试时段，面试地点默认为本室会议室；
                  已存在面试安排的学生不会重复生成。
                </div>
              </div>
              <el-button type="primary" class="sys-btn-primary" :loading="generating" @click="handleGenerate">
                一键生成面试安排
              </el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 本室候选人 -->
      <el-tab-pane name="candidates" label="本室候选人">
        <div class="card">
          <div class="card-title">
            <span>候选人名单与评分状态</span>
            <div class="filter-row">
              <el-select v-model="labFilter" placeholder="实训室" clearable style="width: 180px">
                <el-option v-for="name in labOptions" :key="name" :value="name" :label="name" />
              </el-select>
              <el-select v-model="mentorFilter" placeholder="全部导师" clearable style="width: 160px">
                <el-option v-for="name in mentorOptions" :key="name" :value="name" :label="name" />
              </el-select>
            </div>
          </div>

          <div v-if="loading" class="sys-skeleton">
            <div class="sys-skeleton-row" />
            <div class="sys-skeleton-row" />
            <div class="sys-skeleton-row" />
          </div>

          <template v-else>
            <el-empty v-if="!filteredCandidates.length" :image-size="92" description="暂无候选人">
              <div class="sys-empty-hint">可先在「名额与排程」一键生成面试安排，或等待学生填报意向。</div>
              <el-button size="small" @click="activeTab = 'quota'">去生成面试安排</el-button>
            </el-empty>

            <el-table v-else :data="filteredCandidates" border stripe size="small">
              <el-table-column label="学生" min-width="140">
                <template #default="{ row }">
                  <div class="student-cell">
                    <span class="student-name">{{ row.studentName }}</span>
                    <span class="muted">{{ row.classNo }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="studentNo" label="学号" width="120" />
              <el-table-column prop="labName" label="实训室" min-width="160" />
              <el-table-column prop="mentorName" label="意向导师" width="100" />
              <el-table-column prop="slotStart" label="面试时间" min-width="150" />
              <el-table-column prop="place" label="面试地点" min-width="150" />
              <el-table-column prop="status" label="状态" width="96" align="center">
                <template #default="{ row }">
                  <el-tag size="small" :type="statusTag(row.status).type" effect="light">
                    {{ statusTag(row.status).text }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="学习态度" width="86" align="center">
                <template #default="{ row }">{{ dash(row.attitude) }}</template>
              </el-table-column>
              <el-table-column label="逻辑思维" width="86" align="center">
                <template #default="{ row }">{{ dash(row.logic) }}</template>
              </el-table-column>
              <el-table-column label="编程基础" width="86" align="center">
                <template #default="{ row }">{{ dash(row.coding) }}</template>
              </el-table-column>
              <el-table-column label="时间投入" width="86" align="center">
                <template #default="{ row }">{{ dash(row.commitment) }}</template>
              </el-table-column>
              <el-table-column label="总分" width="76" align="center">
                <template #default="{ row }">{{ dash(row.total) }}</template>
              </el-table-column>
              <el-table-column label="结论" width="104" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.result" size="small" :type="resultTag(row.result).type" effect="light">
                    {{ resultTag(row.result).text }}
                  </el-tag>
                  <span v-else class="muted">未评分</span>
                </template>
              </el-table-column>
            </el-table>
          </template>
        </div>
      </el-tab-pane>

      <!-- 录取结果汇总 -->
      <el-tab-pane name="summary" label="录取结果汇总">
        <div class="card sys-card--flow">
          <div class="card-title">
            <span>本室录取结果汇总</span>
            <span class="muted">按导师分组统计面试评审结论</span>
          </div>
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="统计口径"
            description="按本室候选人面试评审结论汇总；最终确认状态以学生端与学院管理员数据为准。"
            class="mb-16"
          />

          <div v-if="loading" class="sys-skeleton">
            <div class="sys-skeleton-row" />
            <div class="sys-skeleton-row" />
            <div class="sys-skeleton-row" />
          </div>

          <template v-else>
            <el-empty v-if="!summaryRows.length" :image-size="92" description="暂无录取汇总数据">
              <div class="sys-empty-hint">导师完成评分并提交名单后，这里会按导师汇总录取结果。</div>
              <el-button size="small" @click="reload">刷新</el-button>
            </el-empty>

            <el-table v-else :data="summaryRows" border stripe size="small" show-summary :summary-method="summaryMethod">
              <el-table-column prop="mentorName" label="导师" min-width="130" />
              <el-table-column prop="total" label="候选人" width="90" align="center" />
              <el-table-column prop="scored" label="已评分" width="90" align="center" />
              <el-table-column label="拟录取" width="90" align="center">
                <template #default="{ row }"><b class="num-offer">{{ row.offer }}</b></template>
              </el-table-column>
              <el-table-column label="递补" width="90" align="center">
                <template #default="{ row }">{{ row.waitlist }}</template>
              </el-table-column>
              <el-table-column label="内部调剂" width="100" align="center">
                <template #default="{ row }">{{ row.inner }}</template>
              </el-table-column>
              <el-table-column label="不予录取" width="100" align="center">
                <template #default="{ row }">{{ row.reject }}</template>
              </el-table-column>
              <el-table-column label="未评分" width="90" align="center">
                <template #default="{ row }">{{ row.total - row.scored }}</template>
              </el-table-column>
            </el-table>
          </template>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 实训室信息编辑弹窗 -->
    <el-dialog
      v-model="labInfoVisible"
      title="实训室基本信息"
      width="560px"
      destroy-on-close
      @open="loadLabInfo"
    >
      <el-form :model="labInfoForm" label-width="104px" class="lab-info-form">
        <el-form-item label="实训室编号">
          <el-input v-model="labInfoForm.code" disabled />
        </el-form-item>
        <el-form-item label="实训室名称">
          <el-input v-model="labInfoForm.name" maxlength="64" placeholder="请输入实训室名称" />
        </el-form-item>
        <el-form-item label="研究方向">
          <el-input
            v-model="labInfoForm.direction"
            maxlength="64"
            placeholder="如：计算机视觉 / 图像算法"
          />
        </el-form-item>
        <el-form-item label="实训室简介">
          <el-input
            v-model="labInfoForm.intro"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请输入实训室简介"
          />
        </el-form-item>
        <el-form-item label="全室名额">
          <el-input-number
            v-model="labInfoForm.quotaTotal"
            :min="0"
            :max="999"
            controls-position="right"
            style="width: 140px"
          />
          <span class="lab-quota-tip">人 / 轮</span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="labInfoVisible = false">取消</el-button>
        <el-button
          type="primary"
          class="sys-btn-primary"
          :loading="savingLabInfo"
          @click="saveLabInfo"
        >
          保存修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, OfficeBuilding } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  fetchLabs,
  fetchLabInterviews,
  fetchSupplementRounds,
  generateInterviews,
  fetchLabMe,
  updateLabMe
} from '@/api'
import { INTERVIEW_STATUS_MAP, RESULT_MAP, tagOf } from '@/utils/format'

const userStore = useUserStore()

const activeTab = ref('quota')
const loading = ref(false)
const generating = ref(false)
const round = ref(1)
const labs = ref([])
const candidates = ref([])
const labFilter = ref('')
const mentorFilter = ref('')

/* ---------- 实训室信息弹窗 ---------- */
const labInfoVisible = ref(false)
const savingLabInfo = ref(false)
const labInfoForm = reactive({
  code: '',
  name: '',
  direction: '',
  intro: '',
  quotaTotal: 0
})

async function openLabInfo() {
  labInfoVisible.value = true
}

async function loadLabInfo() {
  try {
    const data = await fetchLabMe()
    Object.assign(labInfoForm, {
      code: data.code || '',
      name: data.name || '',
      direction: data.direction || '',
      intro: data.intro || '',
      quotaTotal: Number(data.quotaTotal) || 0
    })
  } catch (e) {
    /* 错误由拦截器处理 */
  }
}

async function saveLabInfo() {
  if (!labInfoForm.name.trim()) {
    ElMessage.warning('实训室名称不能为空')
    return
  }
  savingLabInfo.value = true
  try {
    await updateLabMe({
      name: labInfoForm.name.trim(),
      direction: labInfoForm.direction,
      intro: labInfoForm.intro,
      quotaTotal: labInfoForm.quotaTotal
    })
    ElMessage.success('实训室信息已更新')
    labInfoVisible.value = false
    // 刷新本室数据
    await reload()
  } catch (e) {
    /* 错误由拦截器处理 */
  } finally {
    savingLabInfo.value = false
  }
}

const lab = computed(() => labs.value.find((item) => Number(item.id) === Number(userStore.user?.labId)) || null)
const mentors = computed(() => (lab.value ? lab.value.mentors || [] : []))

const labOptions = computed(() => {
  const names = new Set()
  candidates.value.forEach((row) => row.labName && names.add(row.labName))
  return [...names]
})

const mentorOptions = computed(() => {
  const names = new Set()
  candidates.value.forEach((row) => row.mentorName && names.add(row.mentorName))
  return [...names]
})

const filteredCandidates = computed(() =>
  candidates.value.filter((row) => {
    if (labFilter.value && row.labName !== labFilter.value) return false
    if (mentorFilter.value && row.mentorName !== mentorFilter.value) return false
    return true
  })
)

const summaryRows = computed(() => {
  const map = new Map()
  const ensured = (name) => {
    const key = name || '未指派导师'
    if (!map.has(key)) {
      map.set(key, { mentorName: key, total: 0, scored: 0, offer: 0, waitlist: 0, inner: 0, reject: 0 })
    }
    return map.get(key)
  }
  candidates.value.forEach((row) => {
    const item = ensured(row.mentorName)
    item.total += 1
    if (row.result) item.scored += 1
    if (row.result === 'OFFER') item.offer += 1
    if (row.result === 'WAITLIST') item.waitlist += 1
    if (row.result === 'INNER') item.inner += 1
    if (row.result === 'REJECT') item.reject += 1
  })
  return [...map.values()]
})

function summaryMethod({ columns, data }) {
  return columns.map((column, index) => {
    if (index === 0) return '合计'
    if (['mentorName'].includes(column.property)) return ''
    if (column.property === 'total' || column.property === 'scored') {
      return data.reduce((sum, row) => sum + Number(row[column.property] || 0), 0)
    }
    return ''
  })
}

function statusTag(status) {
  return tagOf(INTERVIEW_STATUS_MAP, status)
}

function resultTag(result) {
  return tagOf(RESULT_MAP, result)
}

function dash(value) {
  return value === null || value === undefined ? '—' : value
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

async function loadLabs() {
  try {
    labs.value = (await fetchLabs()) || []
  } catch (e) {
    labs.value = []
  }
}

async function loadCandidates() {
  try {
    candidates.value = (await fetchLabInterviews(round.value)) || []
  } catch (e) {
    candidates.value = []
  }
}

async function reload() {
  loading.value = true
  await Promise.allSettled([loadLabs(), loadCandidates()])
  loading.value = false
}

function onTabChange() {
  if (activeTab.value === 'candidates' || activeTab.value === 'summary') {
    loadCandidates()
  }
}

async function handleGenerate() {
  try {
    await ElMessageBox.confirm(
      `将为第 ${round.value} 轮本室意向学生生成面试安排，是否继续？`,
      '生成面试安排',
      { type: 'warning', confirmButtonText: '生成', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  generating.value = true
  try {
    const data = await generateInterviews(round.value)
    if (data.created > 0) {
      ElMessage.success(`已生成 ${data.created} 场面试（第 ${data.round} 轮共 ${data.total} 名意向学生）`)
    } else {
      ElMessage.info(`本轮共 ${data.total} 名意向学生，面试安排已存在，无需重复生成`)
    }
    await Promise.allSettled([loadCandidates(), loadLabs()])
  } catch (e) {
    /* 错误提示由响应拦截器处理 */
  } finally {
    generating.value = false
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

.tab-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mb-16 {
  margin-bottom: 16px;
}

.zero-text {
  color: var(--sys-danger);
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

.schedule-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
}

.schedule-info {
  max-width: 720px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.num-offer {
  color: var(--sys-success);
}

.lab-info-form :deep(.el-form-item__label) {
  color: var(--sys-text-sub);
  font-weight: 400;
}

.lab-quota-tip {
  font-size: 12px;
  color: #6b7280;
  margin-left: 8px;
}
</style>
