<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <div>
        <div class="page-title">面试安排</div>
        <div class="page-sub">面试由实训室统一组织，评分由导师在面试结束后录入</div>
      </div>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div class="grid-3">
      <div class="stat-card">
        <div class="stat-label">面试场次</div>
        <div class="stat-value" :key="rows.length" v-count-up="rows.length">{{ rows.length }}</div>
        <div class="stat-hint">本轮全部面试安排</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">待面试</div>
        <div class="stat-value accent" :key="pendingCount" v-count-up="pendingCount">{{ pendingCount }}</div>
        <div class="stat-hint">请提前 10 分钟到达面试地点</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">已出分</div>
        <div class="stat-value" :key="scoredCount" v-count-up="scoredCount">{{ scoredCount }}</div>
        <div class="stat-hint">四维评分与面试结论</div>
      </div>
    </div>

    <div class="card sys-card--flow">
      <div class="card-title">
        <span>我的面试安排</span>
        <span class="muted">共 {{ rows.length }} 条记录</span>
      </div>

      <div v-if="loading" class="sys-skeleton">
        <div class="sys-skeleton-row" />
        <div class="sys-skeleton-row" />
        <div class="sys-skeleton-row" />
      </div>

      <div v-else-if="!rows.length" class="sys-empty">
        <div class="sys-empty-icon">
          <el-icon><Refresh /></el-icon>
        </div>
        <div class="sys-empty-text">暂无面试安排，请留意环节通知</div>
        <div class="sys-empty-hint">面试由实训室统一排期，排期完成后会自动出现在此处</div>
        <el-button size="small" :icon="Refresh" @click="load">刷新面试安排</el-button>
      </div>

      <el-table v-else :data="rows" border stripe size="small">
        <el-table-column prop="roundNo" label="轮次" width="72" align="center">
          <template #default="{ row }">第 {{ row.roundNo }} 轮</template>
        </el-table-column>
        <el-table-column prop="slotStart" label="面试时间" min-width="160" />
        <el-table-column prop="place" label="面试地点" min-width="140" />
        <el-table-column prop="labName" label="实训室" min-width="160" />
        <el-table-column prop="mentorName" label="意向导师" width="100" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.status).type" effect="light">
              {{ statusTag(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="学习态度" width="90" align="center">
          <template #default="{ row }">{{ scoreText(row.attitude) }}</template>
        </el-table-column>
        <el-table-column label="逻辑思维" width="90" align="center">
          <template #default="{ row }">{{ scoreText(row.logic) }}</template>
        </el-table-column>
        <el-table-column label="编程基础" width="90" align="center">
          <template #default="{ row }">{{ scoreText(row.coding) }}</template>
        </el-table-column>
        <el-table-column label="时间投入" width="90" align="center">
          <template #default="{ row }">{{ scoreText(row.commitment) }}</template>
        </el-table-column>
        <el-table-column label="总分" width="80" align="center">
          <template #default="{ row }">
            <b v-if="row.total !== null && row.total !== undefined">{{ row.total }}</b>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="面试结论" width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.result" size="small" :type="resultTag(row.result).type" effect="light">
              {{ resultTag(row.result).text }}
            </el-tag>
            <span v-else class="muted">尚未评分</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="导师备注" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '—' }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { fetchMyInterviews } from '@/api'
import { INTERVIEW_STATUS_MAP, RESULT_MAP, tagOf } from '@/utils/format'

const loading = ref(false)
const rows = ref([])

const pendingCount = computed(() => rows.value.filter((row) => row.status === 'PENDING').length)
const scoredCount = computed(() => rows.value.filter((row) => row.total !== null && row.total !== undefined).length)

function statusTag(status) {
  return tagOf(INTERVIEW_STATUS_MAP, status)
}

function resultTag(result) {
  return tagOf(RESULT_MAP, result)
}

function scoreText(value) {
  return value === null || value === undefined ? '—' : value
}

async function load() {
  loading.value = true
  try {
    rows.value = (await fetchMyInterviews()) || []
  } catch (e) {
    rows.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
