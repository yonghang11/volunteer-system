<template>
  <div class="page">
    <div class="page-head">
      <div>
        <div class="page-title">学院管理</div>
        <div class="page-sub">环节配置、全局总览、分轮补录、统筹调剂、公示与操作日志</div>
      </div>
      <el-button :icon="Refresh" @click="reloadCurrent">刷新当前页</el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- 环节配置 -->
      <el-tab-pane name="stages" label="环节配置">
        <div class="card sys-card--flow">
          <div class="card-title">
            <span>8 个环节时间窗</span>
            <div>
              <el-button style="margin-right: 8px" @click="handleResetIntent">重置到意向填报</el-button>
              <el-button type="primary" class="sys-btn-primary" @click="handleAdvance">一键推进到下一环节</el-button>
            </div>
          </div>
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="环节由时间窗驱动"
            description="系统按开始/结束时间自动判定环节状态（未开始 / 进行中 / 已结束），修改后立即生效。"
            class="mb-16"
          />
          <div v-if="loading" class="sys-skeleton">
            <div class="sys-skeleton-row" />
            <div class="sys-skeleton-row" />
            <div class="sys-skeleton-row" />
          </div>

          <el-table v-else :data="stages" border stripe size="small">
            <el-table-column prop="no" label="序号" width="70" align="center" />
            <el-table-column prop="name" label="环节名称" min-width="150" />
            <el-table-column label="状态" width="96" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="phaseTag(row.phase).type" effect="light">
                  {{ phaseTag(row.phase).text }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="开始时间" width="230">
              <template #default="{ row }">
                <el-date-picker
                  v-model="row.startTime"
                  type="datetime"
                  size="small"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  format="YYYY-MM-DD HH:mm"
                  placeholder="开始时间"
                  style="width: 200px"
                />
              </template>
            </el-table-column>
            <el-table-column label="结束时间" width="230">
              <template #default="{ row }">
                <el-date-picker
                  v-model="row.endTime"
                  type="datetime"
                  size="small"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  format="YYYY-MM-DD HH:mm"
                  placeholder="结束时间"
                  style="width: 200px"
                />
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="170" show-overflow-tooltip>
              <template #default="{ row }">{{ row.remark || '—' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="96" align="center" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="saveStage(row)">保存</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 总览 -->
      <el-tab-pane name="overview" label="总览">
        <div class="card sys-card--flow">
          <div class="card-title">
            <span>全站指标总览</span>
            <span class="muted">当前轮次：第 {{ overview.round || 1 }} 轮</span>
          </div>

          <div v-if="loading" class="sys-skeleton">
            <div class="sys-skeleton-row" />
            <div class="sys-skeleton-row" />
            <div class="sys-skeleton-row" />
          </div>

          <div v-else class="grid-4">
            <div v-for="metric in metrics" :key="metric.key" class="stat-card">
              <div class="stat-label">{{ metric.label }}</div>
              <div
                class="stat-value"
                :class="{ accent: metric.accent }"
                v-count-up="overview[metric.key] ?? 0"
                :key="`${metric.key}-${overview[metric.key] ?? 0}`"
              >
                {{ overview[metric.key] ?? 0 }}
              </div>
              <div class="stat-hint">{{ metric.hint }}</div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 补录 -->
      <el-tab-pane name="supplement" label="补录">
        <div class="tab-body">
          <div v-if="loading" class="sys-skeleton">
            <div class="sys-skeleton-row" />
            <div class="sys-skeleton-row" />
            <div class="sys-skeleton-row" />
          </div>

          <template v-else>
            <el-empty v-if="!supplementRounds.length" :image-size="92" description="暂无补录轮次">
              <div class="sys-empty-hint">补录轮次依赖「环节配置」中的补录时间窗，请先确认时间窗已保存。</div>
              <el-button size="small" @click="activeTab = 'stages'">去配置时间窗</el-button>
            </el-empty>

            <div v-for="round in supplementRounds" :key="round.roundNo" class="card sys-card--flow">
              <div class="card-title">
                <span>第 {{ round.roundNo }} 轮补录</span>
                <div class="supplement-head">
                  <el-tag size="small" :type="round.openFlag === 1 ? 'success' : 'info'" effect="light">
                    {{ round.openFlag === 1 ? '补录中' : '未开放' }}
                  </el-tag>
                  <el-button
                    type="primary"
                    plain
                    :disabled="round.openFlag === 1"
                    :loading="opening === round.roundNo"
                    @click="handleOpenRound(round)"
                  >
                    {{ round.openFlag === 1 ? '已开启' : '开启本轮补录' }}
                  </el-button>
                </div>
              </div>

              <div class="grid-4 mb-16">
                <div class="stat-card">
                  <div class="stat-label">开放状态</div>
                  <div class="stat-value">{{ round.openFlag === 1 ? '开放中' : '未开放' }}</div>
                  <div class="stat-hint">第 {{ round.roundNo }} 轮补录</div>
                </div>
                <div class="stat-card">
                  <div class="stat-label">剩余名额合计</div>
                  <div
                    class="stat-value accent"
                    v-count-up="totalRemain(round.slots)"
                    :key="`remain-${round.roundNo}-${totalRemain(round.slots)}`"
                  >
                    {{ totalRemain(round.slots) }}
                  </div>
                  <div class="stat-hint">实时计算，含全部导师</div>
                </div>
                <div class="stat-card">
                  <div class="stat-label">开放时间</div>
                  <div class="stat-value stat-date">{{ round.startTime || '—' }}</div>
                  <div class="stat-hint">开启后自动生成名额快照</div>
                </div>
                <div class="stat-card">
                  <div class="stat-label">结束时间</div>
                  <div class="stat-value stat-date">{{ round.endTime || '—' }}</div>
                  <div class="stat-hint">与补录环节时间窗一致</div>
                </div>
              </div>

              <el-table :data="round.slots" border stripe size="small" max-height="320">
                <el-table-column prop="labName" label="实训室" min-width="180" />
                <el-table-column prop="mentorName" label="导师" width="110" />
                <el-table-column prop="quota" label="名额" width="90" align="center" />
                <el-table-column label="剩余" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag size="small" :type="row.remain > 0 ? 'success' : 'danger'" effect="plain">
                      {{ row.remain }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </div>
      </el-tab-pane>

      <!-- 统筹调剂 -->
      <el-tab-pane name="transfer" label="统筹调剂">
        <div class="tab-body">
          <div v-if="loading" class="sys-skeleton">
            <div class="sys-skeleton-row" />
            <div class="sys-skeleton-row" />
            <div class="sys-skeleton-row" />
          </div>

          <template v-else>
            <div class="card sys-card--flow">
              <div class="card-title">
                <span>调剂工作台</span>
                <div class="supplement-head">
                  <span class="muted">当前轮次：第 {{ pool.round || 1 }} 轮</span>
                  <el-button type="primary" plain :loading="autoing" @click="handleAuto">自动分级</el-button>
                </div>
              </div>
              <el-alert
                type="info"
                :closable="false"
                show-icon
                title="自动分级规则"
                description="优先级 1：意向实训室仍有剩余名额；优先级 2：服从全院跨实训室调剂且仍有空余名额；优先级 3：约谈后强制安置。重复执行会清空旧待处理任务。"
              />
            </div>

            <div class="card">
              <div class="card-title">
                <span>未录取学生</span>
                <span class="muted">{{ pool.unplaced ? pool.unplaced.length : 0 }} 人</span>
              </div>
              <el-empty v-if="!pool.unplaced || !pool.unplaced.length" :image-size="92" description="当前没有未录取学生">
                <div class="sys-empty-hint">全部学生已完成录取，或仍处于意向填报阶段。</div>
                <el-button size="small" @click="reloadCurrent">刷新</el-button>
              </el-empty>
              <el-table v-else :data="pool.unplaced" border stripe size="small" max-height="260">
                <el-table-column prop="name" label="姓名" width="110" />
                <el-table-column prop="studentNo" label="学号" width="140" />
                <el-table-column prop="classNo" label="班级" min-width="120" />
              </el-table>
            </div>

            <div class="card">
              <div class="card-title">
                <span>剩余名额</span>
                <span class="muted">仅展示仍有剩余名额的导师</span>
              </div>
              <el-empty v-if="!remainSlots.length" :image-size="92" description="全部导师名额已满">
                <div class="sys-empty-hint">当前无需继续调剂，可在后续轮次开放后再次查看。</div>
                <el-button size="small" @click="reloadCurrent">刷新</el-button>
              </el-empty>
              <el-table v-else :data="remainSlots" border stripe size="small" max-height="260">
                <el-table-column prop="labName" label="实训室" min-width="180" />
                <el-table-column prop="mentorName" label="导师" width="110" />
                <el-table-column prop="quota" label="名额" width="90" align="center" />
                <el-table-column label="剩余" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag size="small" type="success" effect="plain">{{ row.remain }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <div class="card sys-card--flow">
              <div class="card-title">
                <span>调剂任务</span>
                <span class="muted">{{ pool.tasks ? pool.tasks.length : 0 }} 条</span>
              </div>
              <el-empty v-if="!pool.tasks || !pool.tasks.length" :image-size="92" description="暂无调剂任务">
                <div class="sys-empty-hint">点击「自动分级」按优先级为未录取学生生成待处理任务。</div>
                <el-button size="small" type="primary" plain @click="handleAuto">自动分级</el-button>
              </el-empty>
              <el-table v-else :data="pool.tasks" border stripe size="small">
                <el-table-column prop="studentName" label="学生" width="100" />
                <el-table-column prop="studentNo" label="学号" width="130" />
                <el-table-column label="优先级" width="190" align="center">
                  <template #default="{ row }">
                    <el-tag size="small" :type="priorityTag(row.priority).type" effect="light">
                      {{ priorityTag(row.priority).text }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="fromLabName" label="原意向实训室" min-width="160">
                  <template #default="{ row }">{{ row.fromLabName || '—' }}</template>
                </el-table-column>
                <el-table-column label="指派导师" min-width="170">
                  <template #default="{ row }">
                    <span v-if="row.toMentorName">{{ row.toLabName }} · {{ row.toMentorName }}</span>
                    <span v-else class="muted">未指派</span>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="96" align="center">
                  <template #default="{ row }">
                    <el-tag size="small" :type="taskTag(row.status).type" effect="light">
                      {{ taskTag(row.status).text }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="note" label="约谈备注" min-width="150" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.note || '—' }}</template>
                </el-table-column>
                <el-table-column label="操作" width="130" align="center" fixed="right">
                  <template #default="{ row }">
                    <el-button
                      link
                      type="primary"
                      :disabled="row.status === 'ASSIGNED' || row.status === 'REFUSED'"
                      @click="openAssign(row)"
                    >
                      指派
                    </el-button>
                    <el-button
                      link
                      type="danger"
                      :disabled="row.status === 'ASSIGNED' || row.status === 'REFUSED'"
                      @click="handleRefuse(row)"
                    >
                      拒绝
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </div>
      </el-tab-pane>

      <!-- 公示 -->
      <el-tab-pane name="public" label="公示">
        <div class="card sys-card--flow">
          <div class="card-title">
            <span>公示名单</span>
            <div class="supplement-head">
              <span class="muted">共 {{ publicList.length }} 人</span>
              <el-button type="primary" class="sys-btn-primary" :loading="publishing" @click="handlePublish">发布公示</el-button>
              <el-button :disabled="!publicList.length" @click="handleExport">导出名单</el-button>
            </div>
          </div>
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="公示说明"
            description="发布公示将汇总全部已确认录取的学生，公示期 1 天，期间学生可提交异议。"
            class="mb-16"
          />

          <div v-if="loading" class="sys-skeleton">
            <div class="sys-skeleton-row" />
            <div class="sys-skeleton-row" />
            <div class="sys-skeleton-row" />
          </div>

          <template v-else>
            <el-empty v-if="!publicList.length" :image-size="92" description="暂无公示名单">
              <div class="sys-empty-hint">学生确认录取后即可发布公示，公示期为 1 天。</div>
              <el-button size="small" type="primary" plain @click="handlePublish">发布公示</el-button>
            </el-empty>

            <el-table v-else :data="publicList" border stripe size="small">
              <el-table-column type="index" label="序号" width="64" align="center" />
              <el-table-column prop="studentName" label="姓名" width="100" />
              <el-table-column prop="studentNo" label="学号" width="130" />
              <el-table-column prop="classNo" label="班级" width="100" />
              <el-table-column prop="labName" label="实训室" min-width="170" />
              <el-table-column prop="mentorName" label="导师" width="100" />
              <el-table-column prop="publishedAt" label="公示时间" min-width="160" />
              <el-table-column prop="objectUntil" label="异议截止" min-width="160" />
              <el-table-column label="状态" width="96" align="center">
                <template #default="{ row }">
                  <el-tag size="small" :type="publicTag(row.status).type" effect="light">
                    {{ publicTag(row.status).text }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="objection" label="异议内容" min-width="180" show-overflow-tooltip>
                <template #default="{ row }">{{ row.objection || '—' }}</template>
              </el-table-column>
            </el-table>
          </template>
        </div>
      </el-tab-pane>

      <!-- 日志 -->
      <el-tab-pane name="audit" label="日志">
        <div class="card sys-card--flow">
          <div class="card-title">
            <span>操作日志</span>
            <span class="muted">最近 50 条审计记录</span>
          </div>

          <div v-if="loading" class="sys-skeleton">
            <div class="sys-skeleton-row" />
            <div class="sys-skeleton-row" />
            <div class="sys-skeleton-row" />
          </div>

          <template v-else>
            <el-empty v-if="!auditLogs.length" :image-size="92" description="暂无操作日志">
              <div class="sys-empty-hint">环节推进、名额配置与公示发布等操作会在此留痕。</div>
              <el-button size="small" @click="reloadCurrent">刷新</el-button>
            </el-empty>

            <el-table v-else :data="auditLogs" border stripe size="small">
              <el-table-column prop="createTime" label="时间" width="170" />
              <el-table-column prop="actor" label="操作人" width="150" />
              <el-table-column prop="action" label="动作" width="170" />
              <el-table-column prop="target" label="对象" min-width="150" />
              <el-table-column prop="detail" label="详情" min-width="320" show-overflow-tooltip />
            </el-table>
          </template>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="assignVisible" title="指派接收导师" width="540px">
      <el-form label-width="96px">
        <el-form-item label="调剂学生">
          <span>{{ currentTask.studentName }}（{{ currentTask.studentNo }}）</span>
        </el-form-item>
        <el-form-item label="优先级">
          <el-tag size="small" :type="priorityTag(currentTask.priority).type" effect="light">
            {{ priorityTag(currentTask.priority).text }}
          </el-tag>
        </el-form-item>
        <el-form-item label="接收导师">
          <el-select v-model="assignForm.mentorId" placeholder="请选择仍有剩余名额的导师" style="width: 100%">
            <el-option
              v-for="slot in assignableSlots"
              :key="slot.mentorId"
              :value="slot.mentorId"
              :label="`${slot.labName} · ${slot.mentorName}（剩余 ${slot.remain}）`"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="约谈备注">
          <el-input
            v-model="assignForm.note"
            type="textarea"
            :rows="3"
            maxlength="120"
            show-word-limit
            placeholder="可填写约谈情况与安置说明"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" class="sys-btn-primary" :loading="assigning" @click="submitAssign">确认指派</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { useStageStore } from '@/stores/stage'
import {
  advanceStage,
  assignTransfer,
  autoTransfer,
  downloadPublicExport,
  fetchAuditLogs,
  fetchOverview,
  fetchPublicList,
  fetchStageList,
  fetchSupplementRounds,
  fetchTransferPool,
  openSupplementRound,
  publishPublicList,
  refuseTransfer,
  resetStageToIntent,
  updateStage
} from '@/api'
import {
  PHASE_MAP,
  PRIORITY_MAP,
  PUBLIC_STATUS_MAP,
  TASK_STATUS_MAP,
  totalRemain,
  tagOf
} from '@/utils/format'

const stageStore = useStageStore()

const activeTab = ref('stages')
const loading = ref(false)
const stages = ref([])
const overview = ref({})
const supplementRounds = ref([])
const pool = ref({ round: 1, unplaced: [], slots: [], tasks: [] })
const publicList = ref([])
const auditLogs = ref([])

const opening = ref(null)
const autoing = ref(false)
const publishing = ref(false)
const assigning = ref(false)

const assignVisible = ref(false)
const currentTask = ref({})
const assignForm = reactive({ mentorId: null, note: '' })

const metrics = [
  { key: 'studentCount', label: '学生总数', hint: '全部在册学生' },
  { key: 'intentCount', label: '意向总数', hint: '各轮意向记录' },
  { key: 'interviewCount', label: '面试场次', hint: '已生成面试安排' },
  { key: 'scoredCount', label: '已评分', hint: '导师已完成评分' },
  { key: 'offerCount', label: '拟录取数', hint: '拟录取类型记录' },
  { key: 'confirmedCount', label: '已确认', hint: '学生已确认录取', accent: true },
  { key: 'unplacedCount', label: '未录取', hint: '尚未确认录取学生' },
  { key: 'remainQuota', label: '剩余名额', hint: '当前轮次全部导师剩余', accent: true }
]

const remainSlots = computed(() => (pool.value.slots || []).filter((slot) => Number(slot.remain) > 0))
const assignableSlots = computed(() => remainSlots.value)

function phaseTag(phase) {
  return tagOf(PHASE_MAP, phase)
}

function priorityTag(priority) {
  return tagOf(PRIORITY_MAP, priority)
}

function taskTag(status) {
  return tagOf(TASK_STATUS_MAP, status)
}

function publicTag(status) {
  return tagOf(PUBLIC_STATUS_MAP, status)
}

async function loadStages() {
  loading.value = true
  try {
    stages.value = (await fetchStageList()) || []
  } catch (e) {
    stages.value = []
  } finally {
    loading.value = false
  }
}

async function loadOverview() {
  loading.value = true
  try {
    overview.value = (await fetchOverview()) || {}
  } catch (e) {
    overview.value = {}
  } finally {
    loading.value = false
  }
}

async function loadSupplement() {
  loading.value = true
  try {
    supplementRounds.value = (await fetchSupplementRounds()) || []
  } catch (e) {
    supplementRounds.value = []
  } finally {
    loading.value = false
  }
}

async function loadPool() {
  loading.value = true
  try {
    pool.value = (await fetchTransferPool()) || { round: 1, unplaced: [], slots: [], tasks: [] }
  } catch (e) {
    pool.value = { round: 1, unplaced: [], slots: [], tasks: [] }
  } finally {
    loading.value = false
  }
}

async function loadPublic() {
  loading.value = true
  try {
    publicList.value = (await fetchPublicList()) || []
  } catch (e) {
    publicList.value = []
  } finally {
    loading.value = false
  }
}

async function loadAudit() {
  loading.value = true
  try {
    auditLogs.value = (await fetchAuditLogs()) || []
  } catch (e) {
    auditLogs.value = []
  } finally {
    loading.value = false
  }
}

function reloadCurrent() {
  if (activeTab.value === 'stages') return loadStages()
  if (activeTab.value === 'overview') return loadOverview()
  if (activeTab.value === 'supplement') return loadSupplement()
  if (activeTab.value === 'transfer') return loadPool()
  if (activeTab.value === 'public') return loadPublic()
  if (activeTab.value === 'audit') return loadAudit()
}

function onTabChange(name) {
  if (name === 'stages') loadStages()
  if (name === 'overview') loadOverview()
  if (name === 'supplement') loadSupplement()
  if (name === 'transfer') loadPool()
  if (name === 'public') loadPublic()
  if (name === 'audit') loadAudit()
}

async function saveStage(row) {
  if (!row.startTime || !row.endTime) {
    ElMessage.warning('开始时间与结束时间均不能为空')
    return
  }
  try {
    await updateStage(row.code, { startTime: row.startTime, endTime: row.endTime })
    ElMessage.success(`「${row.name}」时间窗已更新`)
    await loadStages()
    stageStore.fetchCurrent()
  } catch (e) {
    /* 错误提示由响应拦截器处理 */
  }
}

async function handleAdvance() {
  try {
    await ElMessageBox.confirm(
      '将立即结束当前环节并开启下一环节（演示用），是否继续？',
      '推进环节',
      { type: 'warning', confirmButtonText: '立即推进', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    const data = await advanceStage()
    ElMessage.success(`已推进至：第 ${data.no} 环节 · ${data.name}`)
    await loadStages()
    stageStore.fetchCurrent()
  } catch (e) {
    /* 错误提示由响应拦截器处理 */
  }
}

async function handleResetIntent() {
  try {
    await ElMessageBox.confirm(
      '将把全部环节时间窗整体平移到「意向填报」阶段，仅调整时间、不影响已有数据，是否继续？',
      '重置到意向填报',
      { type: 'warning', confirmButtonText: '立即重置', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    const data = await resetStageToIntent()
    ElMessage.success(`已重置：第 ${data.no} 环节 · ${data.name} 进行中`)
    await loadStages()
    stageStore.fetchCurrent()
  } catch (e) {
    /* 错误提示由响应拦截器处理 */
  }
}

async function handleOpenRound(round) {
  try {
    await ElMessageBox.confirm(
      `开启第 ${round.roundNo} 轮补录后将自动关闭上一轮，并快照当前剩余名额，是否继续？`,
      '开启补录',
      { type: 'warning', confirmButtonText: '开启', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  opening.value = round.roundNo
  try {
    await openSupplementRound(round.roundNo)
    ElMessage.success(`第 ${round.roundNo} 轮补录已开启`)
    await loadSupplement()
  } catch (e) {
    /* 错误提示由响应拦截器处理 */
  } finally {
    opening.value = null
  }
}

async function handleAuto() {
  try {
    await ElMessageBox.confirm(
      '将根据未录取学生的意向与实时名额自动生成调剂任务（覆盖旧的待处理任务），是否继续？',
      '自动分级',
      { type: 'warning', confirmButtonText: '生成', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  autoing.value = true
  try {
    const data = await autoTransfer()
    ElMessage.success(`已生成 ${data.count} 条调剂任务（第 ${data.round} 轮）`)
    await loadPool()
  } catch (e) {
    /* 错误提示由响应拦截器处理 */
  } finally {
    autoing.value = false
  }
}

function openAssign(row) {
  currentTask.value = row
  assignForm.mentorId = null
  assignForm.note = ''
  assignVisible.value = true
}

async function submitAssign() {
  if (!assignForm.mentorId) {
    ElMessage.warning('请选择接收导师')
    return
  }
  assigning.value = true
  try {
    await assignTransfer(currentTask.value.id, {
      mentorId: assignForm.mentorId,
      note: assignForm.note
    })
    ElMessage.success('指派成功，已生成确认录取记录')
    assignVisible.value = false
    await loadPool()
  } catch (e) {
    /* 错误提示由响应拦截器处理 */
  } finally {
    assigning.value = false
  }
}

async function handleRefuse(row) {
  let note = ''
  try {
    const result = await ElMessageBox.prompt(
      `请填写约谈备注（可留空）。确认将「${row.studentName}」标记为拒绝安置？`,
      '标记拒绝安置',
      {
        confirmButtonText: '确认拒绝',
        cancelButtonText: '取消',
        inputPlaceholder: '约谈情况说明'
      }
    )
    note = result.value || ''
  } catch (e) {
    return
  }
  try {
    await refuseTransfer(row.id, { note })
    ElMessage.success('已标记拒绝安置，等待学院强制安置')
    await loadPool()
  } catch (e) {
    /* 错误提示由响应拦截器处理 */
  }
}

async function handlePublish() {
  try {
    await ElMessageBox.confirm(
      '将汇总全部已确认录取的学生并生成公示名单（公示期 1 天），是否继续？',
      '发布公示',
      { type: 'warning', confirmButtonText: '发布', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  publishing.value = true
  try {
    const data = await publishPublicList()
    ElMessage.success(`已发布公示，共 ${data.count} 人，公示至 ${data.objectUntil}`)
    await loadPublic()
  } catch (e) {
    /* 错误提示由响应拦截器处理 */
  } finally {
    publishing.value = false
  }
}

async function handleExport() {
  try {
    const res = await downloadPublicExport()
    const data = res.data
    if (data && data.type && data.type.indexOf('json') >= 0) {
      const text = await data.text()
      let msg = '导出失败'
      try {
        msg = JSON.parse(text).msg || msg
      } catch (e) {
        /* 忽略解析错误 */
      }
      ElMessage.error(msg)
      return
    }
    const blob = new Blob([data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '录取名单.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
    ElMessage.success('名单已导出')
  } catch (e) {
    /* 错误提示由响应拦截器处理 */
  }
}

onMounted(() => {
  loadStages()
})
</script>

<style scoped>
.tab-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mb-16 {
  margin-bottom: 16px;
}

.supplement-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stat-date {
  font-size: 15px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

:deep(.el-tab-pane) {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

:deep(.el-form-item__label) {
  color: var(--sys-text-sub);
}
</style>
