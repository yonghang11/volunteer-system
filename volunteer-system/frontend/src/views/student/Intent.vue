<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <div>
        <div class="page-title">意向填报</div>
        <div class="page-sub">
          <template v-if="intentStage">
            意向填报时间窗：{{ intentStage.startTime }} ~ {{ intentStage.endTime }}
          </template>
          <template v-else>正在获取环节信息</template>
        </div>
      </div>
      <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
    </div>

    <el-alert
      v-if="!editable"
      type="info"
      :closable="false"
      show-icon
      :title="readonlyTitle"
    />

    <div class="card sys-card--flow">
      <div class="submit-bar">
        <div class="select-info">
          <span class="select-label">已选</span>
          <span class="select-count" :class="{ complete: form.mentorIds.length === 3 }">
            {{ form.mentorIds.length }}
          </span>
          <span class="select-total">/ 3</span>
          <span class="muted">（最多可选 3 个平行导师意向，点击卡片选择或取消）</span>
        </div>
        <div class="switch-group">
          <div class="switch-item">
            <span>服从实训室内部调剂</span>
            <el-switch v-model="form.acceptInner" :disabled="!editable" />
          </div>
          <div class="switch-item">
            <span>服从全院跨实训室调剂</span>
            <el-switch v-model="form.acceptCross" :disabled="!editable" />
          </div>
        </div>
      </div>

      <el-divider />

      <!-- 个人简介上传区 -->
      <div class="bio-section">
        <div class="bio-header">
          <div class="bio-title">个人简介 <span class="bio-tag">可选但建议填写</span></div>
          <div class="bio-hint">
            导师在初选阶段会查看你的个人简介（PDF / Word / 图片 / TXT，最大 15MB）
          </div>
        </div>

        <!-- 已有简介：显示文件信息 + 预览/更换/删除 -->
        <div v-if="bio.hasBio" class="bio-current">
          <div class="bio-file">
            <div class="bio-icon">📄</div>
            <div class="bio-file-info">
              <div class="bio-filename">{{ bio.name }}</div>
              <div class="bio-meta">
                上传时间：{{ bio.updatedAt || '—' }}
              </div>
            </div>
            <div class="bio-actions">
              <el-button
                v-if="bio.url"
                link
                type="primary"
                :href="bioFullUrl"
                target="_blank"
              >
                查看/下载
              </el-button>
              <el-button
                link
                type="primary"
                @click="triggerBioUpload"
                :disabled="!editable"
              >
                更换
              </el-button>
              <el-button
                link
                type="danger"
                @click="removeBio"
                :disabled="!editable"
              >
                删除
              </el-button>
            </div>
          </div>
          <el-progress
            v-if="bioProgress > 0 && bioProgress < 100"
            :percentage="bioProgress"
            :stroke-width="6"
            style="margin-top: 10px"
          />
        </div>

        <!-- 无简介：显示上传入口 -->
        <div v-else class="bio-empty">
          <el-upload
            :show-file-list="false"
            :before-upload="beforeBioUpload"
            :on-change="handleBioChange"
            :auto-upload="false"
            :disabled="!editable || uploadingBio"
          >
            <el-button type="primary" plain :icon="Upload">
              {{ uploadingBio ? `上传中 ${bioProgress}%` : '上传个人简介' }}
            </el-button>
          </el-upload>
          <span class="bio-hint-inline">
            支持 pdf / doc / docx / txt / png / jpg，最大 15MB
          </span>
        </div>

        <!-- 隐藏的 input，用于更换场景 -->
        <input
          ref="bioInputRef"
          type="file"
          style="display: none"
          @change="handleBioReplace"
          accept=".pdf,.doc,.docx,.txt,.png,.jpg,.jpeg"
        />
      </div>

      <el-divider />

      <MentorPicker v-model="form.mentorIds" :readonly="!editable" :max="3" />

      <div class="action-bar">
        <span class="section-tip">
          提交后将覆盖本轮此前的导师意向（已进入拟录取的记录不受影响）。
        </span>
        <el-button
          v-if="editable"
          type="primary"
          class="sys-btn-primary"
          :loading="submitting"
          @click="submit"
        >
          提交意向
        </el-button>
        <el-tooltip v-else content="当前不在意向填报时间窗内，暂不可提交" placement="top">
          <span>
            <el-button type="primary" disabled>提交意向</el-button>
          </span>
        </el-tooltip>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Upload } from '@element-plus/icons-vue'
import MentorPicker from '@/components/MentorPicker.vue'
import { fetchBio, fetchMyIntent, fetchStageList, submitIntent, uploadBio, deleteBio as removeBioApi } from '@/api'

const loading = ref(false)
const submitting = ref(false)
const stages = ref([])
const myIntent = ref(null)

/* ---------- 个人简介状态 ---------- */
const bio = reactive({ url: null, name: null, updatedAt: null, hasBio: false })
const uploadingBio = ref(false)
const bioProgress = ref(0)
const bioInputRef = ref(null)

const bioFullUrl = computed(() => {
  if (!bio.url) return ''
  // Vite dev server 已代理 /api，上传文件直接走同源即可
  return bio.url
})

const ALLOWED_EXT = ['pdf', 'doc', 'docx', 'txt', 'png', 'jpg', 'jpeg']
const MAX_SIZE = 15 * 1024 * 1024

function validateBioFile(file) {
  const name = file.name || ''
  const ext = name.includes('.') ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : ''
  if (!ALLOWED_EXT.includes(ext)) {
    ElMessage.error('仅支持 pdf / doc / docx / txt / png / jpg')
    return false
  }
  if (file.size > MAX_SIZE) {
    ElMessage.error('文件过大，最大 15MB')
    return false
  }
  return true
}

function beforeBioUpload(file) {
  return validateBioFile(file.raw || file)
}

function handleBioChange(uploadFile) {
  const raw = uploadFile.raw || uploadFile
  if (!validateBioFile(raw)) return
  doUploadBio(raw)
}

function triggerBioUpload() {
  nextTick(() => bioInputRef.value?.click())
}

function handleBioReplace(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (!validateBioFile(file)) return
  doUploadBio(file)
  // 清空，确保下次选同一个文件也能触发 change
  e.target.value = ''
}

async function doUploadBio(file) {
  uploadingBio.value = true
  bioProgress.value = 0
  try {
    const data = await uploadBio(file, (p) => { bioProgress.value = p })
    bio.url = data.url
    bio.name = data.name
    bio.updatedAt = data.updatedAt
    bio.hasBio = true
    ElMessage.success('个人简介上传成功')
  } catch (e) {
    /* 错误已由拦截器处理 */
  } finally {
    uploadingBio.value = false
    bioProgress.value = 0
  }
}

async function removeBio() {
  try {
    await removeBioApi()
    bio.url = null
    bio.name = null
    bio.updatedAt = null
    bio.hasBio = false
    ElMessage.success('已删除个人简介')
  } catch (e) {
    /* 错误已由拦截器处理 */
  }
}

async function loadBio() {
  try {
    const data = await fetchBio()
    Object.assign(bio, {
      url: data.url,
      name: data.name,
      updatedAt: data.updatedAt,
      hasBio: !!data.hasBio
    })
  } catch (e) {
    /* 非关键路径，静默失败 */
  }
}

const form = reactive({
  mentorIds: [],
  acceptInner: true,
  acceptCross: true
})

const intentStage = computed(() =>
  stages.value.find((item) => item.code === 'INTENT') || null
)

const editable = computed(() => Boolean(intentStage.value && intentStage.value.phase === 'OPEN'))

const readonlyTitle = computed(() => {
  if (!intentStage.value) return '当前无法获取意向填报环节信息，页面内容为只读'
  if (intentStage.value.phase === 'PENDING') {
    return `意向填报尚未开始（${intentStage.value.startTime} 开放），以下为已提交内容，暂不可修改`
  }
  if (intentStage.value.phase === 'CLOSED') {
    return '意向填报已结束，以下为已提交内容，暂不可修改'
  }
  return '当前不在意向填报时间窗内，暂不可修改'
})

async function loadStages() {
  try {
    stages.value = await fetchStageList()
  } catch (e) {
    stages.value = []
  }
}

async function loadIntent() {
  try {
    const data = await fetchMyIntent(1)
    myIntent.value = data
    form.mentorIds = (data.items || []).map((item) => item.mentorId)
    form.acceptInner = Boolean(data.acceptInner)
    form.acceptCross = Boolean(data.acceptCross)
  } catch (e) {
    myIntent.value = null
  }
}

async function loadAll() {
  loading.value = true
  await loadStages()
  await loadIntent()
  await loadBio()
  loading.value = false
}

async function submit() {
  if (form.mentorIds.length === 0) {
    ElMessage.warning('请至少选择 1 个导师意向')
    return
  }
  submitting.value = true
  try {
    await submitIntent({
      round: 1,
      mentorIds: form.mentorIds,
      acceptInner: form.acceptInner,
      acceptCross: form.acceptCross
    })
    ElMessage.success('意向提交成功')
    await loadIntent()
  } catch (e) {
    /* 错误提示由响应拦截器处理 */
  } finally {
    submitting.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.submit-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.select-info {
  display: flex;
  align-items: baseline;
  gap: 6px;
  flex-wrap: wrap;
}

.select-label {
  font-size: 14px;
  color: #4b5563;
}

.select-count {
  font-size: 26px;
  font-weight: 700;
  color: var(--sys-primary);
  line-height: 1;
  font-variant-numeric: tabular-nums;
  transition: color 0.2s ease, text-shadow 0.2s ease;
  text-shadow: 0 0 16px rgba(59, 130, 246, 0.35);
}

.select-count.complete {
  color: var(--sys-success);
  text-shadow: 0 0 16px rgba(52, 211, 153, 0.35);
}

.select-total {
  font-size: 14px;
  color: #4b5563;
  font-variant-numeric: tabular-nums;
}

.switch-group {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.switch-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #4b5563;
}

.action-bar {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

/* 提示类文字加深到可读灰色，保证浅底上清晰 */
.submit-bar .muted {
  font-size: 13px;
  color: #4b5563;
}

.action-bar .section-tip {
  color: #4b5563;
}

/* ---------- 个人简介上传区 ---------- */
.bio-section {
  padding: 4px 0 8px;
}

.bio-header {
  margin-bottom: 12px;
}

.bio-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.bio-tag {
  display: inline-block;
  font-size: 12px;
  font-weight: 400;
  color: #0891b2;
  background: #ecfeff;
  border-radius: 10px;
  padding: 1px 8px;
  margin-left: 6px;
  vertical-align: middle;
}

.bio-hint {
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
}

.bio-empty {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.bio-hint-inline {
  font-size: 12px;
  color: #4b5563;
}

.bio-current {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 14px 16px;
}

.bio-file {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.bio-icon {
  font-size: 32px;
  line-height: 1;
}

.bio-file-info {
  flex: 1;
  min-width: 200px;
}

.bio-filename {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
  word-break: break-all;
}

.bio-meta {
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
}

.bio-actions {
  display: flex;
  gap: 14px;
  align-items: center;
}
</style>
