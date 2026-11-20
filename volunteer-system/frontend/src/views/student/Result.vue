<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <div>
        <div class="page-title">录取结果</div>
        <div class="page-sub">拟录取确认、结果查询、分轮补录与公示异议</div>
      </div>
      <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
    </div>

    <el-tabs v-model="activeTab" class="result-tabs">
      <!-- 1. 拟录取确认 -->
      <el-tab-pane name="confirm" label="拟录取确认">
        <div class="card sys-card--flow">
          <div class="card-title">
            <span>待确认的拟录取</span>
            <span class="muted">确认后不可更改，请谨慎选择</span>
          </div>

          <el-alert
            v-if="offerRecords.length === 1 && offerRecords[0].status === 'PENDING'"
            type="info"
            :closable="false"
            show-icon
            title="系统已自动确认"
            description="你仅收到 1 条拟录取记录，系统将在确认环节开启后自动为你完成确认，无需手动操作。"
            class="mb-12"
          />

          <el-alert
            v-if="offerData.confirmDeadline"
            type="warning"
            :closable="false"
            show-icon
            :title="`确认截止时间：${offerData.confirmDeadline}`"
            description="逾期未确认的拟录取将自动释放名额，并按递补顺位补位。"
            class="mb-16"
          />

          <div v-if="loading" class="sys-skeleton">
            <div class="sys-skeleton-row" />
            <div class="sys-skeleton-row" />
            <div class="sys-skeleton-row" />
          </div>

          <div v-else-if="!offerRecords.length" class="sys-empty">
            <div class="sys-empty-icon">
              <el-icon><Refresh /></el-icon>
            </div>
            <div class="sys-empty-text">暂无拟录取信息，请耐心等待导师评审结果</div>
            <div class="sys-empty-hint">评审完成后拟录取记录会出现在此处，也可先去完善导师意向</div>
            <el-button size="small" @click="$router.push('/student/intent')">前往意向填报</el-button>
          </div>

          <template v-else>
            <el-radio-group v-model="selectedOfferId" class="offer-group">
              <div
                v-for="offer in offerRecords"
                :key="offer.id"
                class="offer-item"
                :class="{ active: selectedOfferId === offer.id }"
              >
                <el-radio
                  :value="offer.id"
                  :disabled="offer.status !== 'PENDING' || !offerData.canConfirm"
                  class="offer-radio"
                >
                  <div class="offer-info">
                    <div class="offer-title">
                      导师 {{ offer.mentorName }}
                      <el-tag size="small" effect="plain">{{ offer.labName }}</el-tag>
                    </div>
                    <div class="muted">
                      第 {{ offer.round }} 轮 · 拟录取
                      <template v-if="offer.rankNo"> · 递补顺位 {{ offer.rankNo }}</template>
                    </div>
                  </div>
                </el-radio>
                <el-tag size="small" :type="statusTag(offer.status).type" effect="light">
                  {{ statusTag(offer.status).text }}
                </el-tag>
              </div>
            </el-radio-group>

            <div class="confirm-bar">
              <span v-if="!offerData.canConfirm" class="section-tip">
                当前不在拟录取确认时间窗内，暂不可确认；如仅 1 条拟录取，系统将在确认环节自动确认。
              </span>
              <span v-else-if="!selectedOfferId" class="section-tip">
                请选择一条待确认的拟录取记录后点击确认。
              </span>
              <span v-else class="section-tip">
                已选择「{{ selectedOffer && selectedOffer.mentorName }}」导师的拟录取，确认后不可更改。
              </span>
              <el-button
                type="primary"
                class="sys-btn-primary"
                :disabled="!selectedOfferId || !offerData.canConfirm"
                @click="handleConfirm"
              >
                确认拟录取
              </el-button>
            </div>
          </template>
        </div>
      </el-tab-pane>

      <!-- 2. 我的结果 -->
      <el-tab-pane name="result" label="我的结果">
        <div class="card sys-card--flow">
          <div class="card-title">
            <span>录取结果明细</span>
            <span class="muted">含拟录取、递补、内部调剂记录</span>
          </div>

          <el-alert
            v-if="confirmedOffer"
            type="success"
            :closable="false"
            show-icon
            :title="`已确认录取：导师 ${confirmedOffer.mentorName}（${confirmedOffer.labName}）`"
            description="如需变更请联系学院管理员，按统筹调剂流程处理。"
            class="mb-16"
          />
          <el-alert
            v-else-if="offerRecords.length"
            type="info"
            :closable="false"
            show-icon
            title="尚未确认录取"
            description="请在拟录取确认时间窗内完成确认，逾期未确认的记录将置为过期。"
            class="mb-16"
          />

          <div v-if="loading" class="sys-skeleton">
            <div class="sys-skeleton-row" />
            <div class="sys-skeleton-row" />
            <div class="sys-skeleton-row" />
          </div>

          <div v-else-if="!offerData.offers.length" class="sys-empty">
            <div class="sys-empty-icon">
              <el-icon><Refresh /></el-icon>
            </div>
            <div class="sys-empty-text">暂无录取相关记录</div>
            <div class="sys-empty-hint">拟录取、递补与调剂记录会在结果发布后显示</div>
            <el-button size="small" @click="loadAll">刷新结果</el-button>
          </div>

          <el-table v-else :data="offerData.offers" border stripe size="small">
            <el-table-column prop="round" label="轮次" width="76" align="center">
              <template #default="{ row }">第 {{ row.round }} 轮</template>
            </el-table-column>
            <el-table-column prop="type" label="类型" width="110" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="typeTag(row.type).type" effect="light">
                  {{ typeTag(row.type).text }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="mentorName" label="导师" width="100" />
            <el-table-column prop="labName" label="实训室" min-width="170" />
            <el-table-column label="递补顺位" width="96" align="center">
              <template #default="{ row }">{{ row.rankNo || '—' }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="statusTag(row.status).type" effect="light">
                  {{ statusTag(row.status).text }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 3. 分轮补录 -->
      <el-tab-pane name="supplement" label="分轮补录">
        <div v-if="loading" class="card sys-card--flow">
          <div class="sys-skeleton">
            <div class="sys-skeleton-row" />
            <div class="sys-skeleton-row" />
            <div class="sys-skeleton-row" />
          </div>
        </div>

        <template v-else>
          <div v-if="!supplementRounds.length" class="card sys-card--flow">
            <div class="sys-empty">
              <div class="sys-empty-icon">
                <el-icon><Refresh /></el-icon>
              </div>
              <div class="sys-empty-text">暂无补录轮次信息</div>
              <div class="sys-empty-hint">补录轮次由管理员开启，开启后可在此重新填报导师意向</div>
              <el-button size="small" @click="loadAll">刷新补录信息</el-button>
            </div>
          </div>

          <div
            v-for="round in supplementRounds"
            :key="round.roundNo"
            class="card supplement-card sys-card--flow"
          >
            <div class="card-title">
              <span>第 {{ round.roundNo }} 轮补录</span>
              <div class="supplement-head-right">
                <el-tag size="small" :type="round.openFlag === 1 ? 'success' : 'info'" effect="light">
                  {{ round.openFlag === 1 ? '补录中' : '未开放' }}
                </el-tag>
                <span class="muted">
                  剩余名额合计
                  <span
                    class="sys-num"
                    :key="totalRemain(round.slots)"
                    v-count-up="totalRemain(round.slots)"
                  >{{ totalRemain(round.slots) }}</span>
                  人
                </span>
              </div>
            </div>

            <div class="muted supplement-time">
              <template v-if="round.startTime && round.endTime">
                开放时间：{{ round.startTime }} ~ {{ round.endTime }}
              </template>
              <template v-else>尚未开启，开启后显示开放时间</template>
            </div>

            <template v-if="round.openFlag === 1 && !hasConfirmed">
              <el-alert
                type="info"
                :closable="false"
                show-icon
                title="本轮补录开放中"
                description="可重新填报导师意向（最多 3 个），提交后覆盖本轮此前意向。"
                class="mb-16"
              />
              <div class="switch-group mb-16">
                <div class="switch-item">
                  <span>服从实训室内部调剂</span>
                  <el-switch v-model="supplementForm(round.roundNo).acceptInner" />
                </div>
                <div class="switch-item">
                  <span>服从全院跨实训室调剂</span>
                  <el-switch v-model="supplementForm(round.roundNo).acceptCross" />
                </div>
                <span class="muted">
                  已选 {{ supplementForm(round.roundNo).mentorIds.length }} / 3
                </span>
              </div>
              <MentorPicker v-model="supplementForm(round.roundNo).mentorIds" :max="3" />
              <div class="action-bar">
                <span class="section-tip">提交后可在本页与「我的意向」查看最新填报内容。</span>
                <el-button
                  type="primary"
                  class="sys-btn-primary"
                  :loading="supplementForm(round.roundNo).submitting"
                  @click="submitSupplement(round)"
                >
                  提交第 {{ round.roundNo }} 轮意向
                </el-button>
              </div>
            </template>

            <el-alert
              v-else-if="hasConfirmed"
              type="success"
              :closable="false"
              show-icon
              title="你已完成录取确认，无需参与补录"
            />
            <el-alert
              v-else
              type="info"
              :closable="false"
              show-icon
              title="本轮补录尚未开放"
              description="请留意环节通知，开放后可在本页重新填报意向。"
            />
          </div>
        </template>
      </el-tab-pane>

      <!-- 4. 调剂与公示 -->
      <el-tab-pane name="public" label="调剂与公示">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="统筹调剂说明"
          description="统筹调剂由学院管理员操作。你可在下方公示名单中查看本人结果，并在公示期内提交异议。"
          class="mb-16"
        />

        <div class="card sys-card--flow">
          <div class="card-title">
            <span>公示名单</span>
            <span class="muted">
              共
              <span
                class="sys-num"
                :key="publicList.length"
                v-count-up="publicList.length"
              >{{ publicList.length }}</span>
              人
            </span>
          </div>

          <div v-if="loading" class="sys-skeleton">
            <div class="sys-skeleton-row" />
            <div class="sys-skeleton-row" />
            <div class="sys-skeleton-row" />
          </div>

          <div v-else-if="!publicList.length" class="sys-empty">
            <div class="sys-empty-icon">
              <el-icon><Refresh /></el-icon>
            </div>
            <div class="sys-empty-text">公示名单尚未发布</div>
            <div class="sys-empty-hint">公示发布后可在此查看本人结果，并在公示期内提交异议</div>
            <el-button size="small" @click="loadAll">刷新公示名单</el-button>
          </div>

          <el-table v-else :data="publicList" border stripe size="small">
            <el-table-column type="index" label="序号" width="64" align="center" />
            <el-table-column prop="studentName" label="姓名" width="100" />
            <el-table-column prop="studentNo" label="学号" width="130" />
            <el-table-column prop="classNo" label="班级" width="100" />
            <el-table-column prop="labName" label="实训室" min-width="170" />
            <el-table-column prop="mentorName" label="导师" width="100" />
            <el-table-column prop="publishedAt" label="公示时间" min-width="160" />
            <el-table-column prop="objectUntil" label="异议截止" min-width="160" />
            <el-table-column prop="status" label="状态" width="96" align="center">
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
        </div>

        <div class="card sys-card--flow">
          <div class="card-title">
            <span>提交异议</span>
            <span class="muted">异议内容将记录在公示名单中</span>
          </div>

          <template v-if="myPublicRow && myPublicRow.status === 'PUBLISHED'">
            <div class="objection-form">
              <el-input
                v-model="objectionContent"
                type="textarea"
                :rows="4"
                maxlength="255"
                show-word-limit
                placeholder="请描述你的异议内容，将提交至学院管理员处理"
              />
              <div class="action-bar">
                <span class="section-tip">
                  异议截止时间：{{ myPublicRow.objectUntil }}
                </span>
                <el-button type="primary" class="sys-btn-primary" :loading="objecting" @click="submitObjectionAction">
                  提交异议
                </el-button>
              </div>
            </div>
          </template>

          <el-alert
            v-else-if="myPublicRow && myPublicRow.status === 'OBJECTED'"
            type="warning"
            :closable="false"
            show-icon
            title="你已提交异议"
            :description="`异议内容：${myPublicRow.objection || ''}`"
          />
          <el-alert
            v-else
            type="info"
            :closable="false"
            show-icon
            title="暂无法提交异议"
            description="你当前不在公示名单中，或公示尚未发布 / 公示已结束。"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import MentorPicker from '@/components/MentorPicker.vue'
import { useUserStore } from '@/stores/user'
import {
  confirmOffer,
  fetchMyIntent,
  fetchMyOffers,
  fetchPublicList,
  fetchSupplementRounds,
  submitIntent,
  submitObjection
} from '@/api'
import {
  OFFER_STATUS_MAP,
  OFFER_TYPE_MAP,
  PUBLIC_STATUS_MAP,
  totalRemain,
  tagOf
} from '@/utils/format'

const userStore = useUserStore()
const route = useRoute()

const loading = ref(false)
const activeTab = ref('confirm')
const offerData = ref({ offers: [], canConfirm: false, confirmDeadline: null })
const selectedOfferId = ref(null)
const supplementRounds = ref([])
const supplementForms = reactive({})
const publicList = ref([])
const objectionContent = ref('')
const objecting = ref(false)

const offerRecords = computed(() =>
  offerData.value.offers.filter((offer) => offer.type === 'OFFER')
)

const selectedOffer = computed(() =>
  offerRecords.value.find((offer) => offer.id === selectedOfferId.value) || null
)

const confirmedOffer = computed(() =>
  offerData.value.offers.find((offer) => offer.type === 'OFFER' && offer.status === 'CONFIRMED') || null
)

const hasConfirmed = computed(() => Boolean(confirmedOffer.value))

const myPublicRow = computed(() =>
  publicList.value.find(
    (row) => Number(row.studentId) === Number(userStore.user?.id)
  )
)

function statusTag(status) {
  return tagOf(OFFER_STATUS_MAP, status)
}

function typeTag(type) {
  return tagOf(OFFER_TYPE_MAP, type)
}

function publicTag(status) {
  return tagOf(PUBLIC_STATUS_MAP, status)
}

function supplementForm(roundNo) {
  if (!supplementForms[roundNo]) {
    supplementForms[roundNo] = {
      mentorIds: [],
      acceptInner: true,
      acceptCross: true,
      submitting: false
    }
  }
  return supplementForms[roundNo]
}

async function loadOffers() {
  try {
    offerData.value = (await fetchMyOffers()) || {
      offers: [],
      canConfirm: false,
      confirmDeadline: null
    }
    const firstPending = offerRecords.value.find((offer) => offer.status === 'PENDING')
    selectedOfferId.value = firstPending ? firstPending.id : null
  } catch (e) {
    offerData.value = { offers: [], canConfirm: false, confirmDeadline: null }
  }
}

async function loadSupplement() {
  try {
    supplementRounds.value = (await fetchSupplementRounds()) || []
    for (const round of supplementRounds.value) {
      supplementForm(round.roundNo)
      if (round.openFlag === 1 && !hasConfirmed.value) {
        prefillIntent(round.roundNo)
      }
    }
  } catch (e) {
    supplementRounds.value = []
  }
}

async function prefillIntent(roundNo) {
  try {
    const data = await fetchMyIntent(roundNo)
    const form = supplementForm(roundNo)
    form.mentorIds = (data.items || []).map((item) => item.mentorId)
    form.acceptInner = Boolean(data.acceptInner)
    form.acceptCross = Boolean(data.acceptCross)
  } catch (e) {
    /* 无历史填报时忽略 */
  }
}

async function loadPublic() {
  try {
    publicList.value = (await fetchPublicList()) || []
  } catch (e) {
    publicList.value = []
  }
}

async function loadAll() {
  loading.value = true
  await loadOffers()
  await Promise.allSettled([loadSupplement(), loadPublic()])
  loading.value = false
}

async function handleConfirm() {
  const offer = selectedOffer.value
  if (!offer) return
  try {
    await ElMessageBox.confirm(
      `确认后不可更改。确认选择「${offer.mentorName}」导师（${offer.labName}）的拟录取吗？`,
      '确认拟录取',
      {
        type: 'warning',
        confirmButtonText: '确认录取',
        cancelButtonText: '再想想'
      }
    )
  } catch (e) {
    return
  }
  try {
    await confirmOffer(offer.id)
    ElMessage.success('已确认拟录取')
    await loadOffers()
    await loadPublic()
  } catch (e) {
    /* 错误提示由响应拦截器处理 */
  }
}

async function submitSupplement(round) {
  const form = supplementForm(round.roundNo)
  if (!form.mentorIds.length) {
    ElMessage.warning('请至少选择 1 个导师意向')
    return
  }
  try {
    await ElMessageBox.confirm(
      `提交后将覆盖第 ${round.roundNo} 轮此前的导师意向，是否继续？`,
      '提交补录意向',
      { type: 'warning', confirmButtonText: '提交', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  form.submitting = true
  try {
    await submitIntent({
      round: round.roundNo,
      mentorIds: form.mentorIds,
      acceptInner: form.acceptInner,
      acceptCross: form.acceptCross
    })
    ElMessage.success(`第 ${round.roundNo} 轮补录意向提交成功`)
  } catch (e) {
    /* 错误提示由响应拦截器处理 */
  } finally {
    form.submitting = false
  }
}

async function submitObjectionAction() {
  if (!objectionContent.value.trim()) {
    ElMessage.warning('请填写异议内容')
    return
  }
  objecting.value = true
  try {
    await submitObjection(objectionContent.value.trim())
    ElMessage.success('异议已提交，请留意学院处理结果')
    objectionContent.value = ''
    await loadPublic()
  } catch (e) {
    /* 错误提示由响应拦截器处理 */
  } finally {
    objecting.value = false
  }
}

onMounted(() => {
  loadAll()
  if (route.query.tab) {
    activeTab.value = String(route.query.tab)
  }
})
</script>

<style scoped>
.result-tabs :deep(.el-tabs__header) {
  margin-bottom: 18px;
}

.result-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  font-weight: 500;
}

.result-tabs :deep(.el-tab-pane) {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mb-12 {
  margin-bottom: 12px;
}

.mb-16 {
  margin-bottom: 16px;
}

.offer-group {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.offer-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid var(--sys-border);
  border-radius: var(--sys-radius-sm);
  padding: 14px 16px;
  background: rgba(15, 23, 42, 0.04);
  transition: border-color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.offer-item:hover {
  background: rgba(15, 23, 42, 0.06);
  border-color: var(--sys-border-strong);
}

.offer-item.active {
  border-color: var(--sys-primary);
  background: var(--brand-light);
  box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.25), 0 8px 24px rgba(0, 0, 0, 0.32);
}

.offer-radio {
  flex: 1;
  height: auto;
  margin-right: 0;
}

.offer-radio :deep(.el-radio__label) {
  width: 100%;
}

.offer-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.offer-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
}

.confirm-bar {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.supplement-card {
  margin-bottom: 16px;
}

.supplement-head-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.supplement-time {
  margin-top: -6px;
  margin-bottom: 14px;
}

.switch-group {
  display: flex;
  align-items: center;
  gap: 22px;
  flex-wrap: wrap;
}

.switch-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--sys-text-sub);
}

.sys-num {
  font-weight: 600;
  color: var(--sys-text);
  font-variant-numeric: tabular-nums;
}

.action-bar {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.objection-form {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
</style>
