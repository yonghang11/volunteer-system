<template>
  <div v-loading="loading" class="mentor-picker">
    <el-empty v-if="!loading && !labs.length" description="暂无可选导师信息" />

    <div v-for="lab in labs" :key="lab.id" class="lab-block">
      <div class="lab-head">
        <div class="lab-title-wrap">
          <span class="lab-name">{{ lab.name }}</span>
          <el-tag size="small" effect="plain">{{ lab.direction }}</el-tag>
        </div>
        <span class="lab-quota">
          全室名额 {{ lab.quota }} 人 · 剩余 {{ lab.remain }} 人
        </span>
      </div>
      <p v-if="lab.intro" class="lab-intro">{{ lab.intro }}</p>

      <div class="mentor-grid">
        <div
          v-for="mentor in lab.mentors"
          :key="mentor.id"
          class="mentor-card"
          :class="{
            selected: isSelected(mentor.id),
            disabled: isFull(mentor) && !isSelected(mentor.id),
            readonly: readonly
          }"
          @click="toggle(mentor)"
        >
          <div class="mc-head">
            <span class="mc-name">{{ mentor.name }}</span>
            <el-tag size="small" effect="plain">{{ mentor.title }}</el-tag>
          </div>
          <div class="mc-dir">{{ mentor.direction }}</div>
          <div class="mc-intro" v-if="mentor.intro">{{ mentor.intro }}</div>
          <div class="mc-foot">
            <span class="mc-quota">
              名额 {{ mentor.quota }} · 剩余
              <b :class="{ zero: mentor.remain <= 0 }">{{ mentor.remain }}</b>
            </span>
            <el-tag v-if="mentor.acceptCrossTransfer === 1" size="small" type="info" effect="plain">
              接受跨室调剂
            </el-tag>
            <el-tag v-else size="small" type="info" effect="plain">仅本室</el-tag>
          </div>
          <span v-if="isSelected(mentor.id)" class="mc-check">
            <el-icon :size="14"><Check /></el-icon>
          </span>
          <span v-if="isFull(mentor) && !isSelected(mentor.id)" class="mc-full">名额已满</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import { fetchLabs } from '@/api'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  readonly: { type: Boolean, default: false },
  max: { type: Number, default: 3 }
})

const emit = defineEmits(['update:modelValue'])

const labs = ref([])
const loading = ref(false)

onMounted(loadLabs)

async function loadLabs() {
  loading.value = true
  try {
    labs.value = await fetchLabs()
  } catch (e) {
    labs.value = []
  } finally {
    loading.value = false
  }
}

function isSelected(id) {
  return props.modelValue.includes(id)
}

function isFull(mentor) {
  return (Number(mentor.remain) || 0) <= 0
}

function toggle(mentor) {
  if (props.readonly) {
    return
  }
  if (isSelected(mentor.id)) {
    emit(
      'update:modelValue',
      props.modelValue.filter((id) => id !== mentor.id)
    )
    return
  }
  if (isFull(mentor)) {
    ElMessage.warning(`${mentor.name} 的导师名额已满，暂不可选择`)
    return
  }
  if (props.modelValue.length >= props.max) {
    ElMessage.warning(`最多填报 ${props.max} 个平行导师意向`)
    return
  }
  emit('update:modelValue', [...props.modelValue, mentor.id])
}
</script>

<style scoped>
.mentor-picker {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.lab-block {
  border: 1px solid var(--sys-border);
  border-radius: 12px;
  padding: 16px 18px 18px;
  /* 实训室分组：比外层卡片略深一档，形成层次而不是白底 */
  background: rgba(15, 23, 42, 0.03);
}

.lab-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.lab-title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}

.lab-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--sys-text);
}

.lab-quota {
  font-size: 12px;
  color: #4b5563;
  background: rgba(15, 23, 42, 0.05);
  border: 1px solid var(--sys-border);
  border-radius: 999px;
  padding: 4px 12px;
}

.lab-intro {
  margin: 8px 0 0;
  font-size: 12px;
  color: #4b5563;
  line-height: 1.7;
}

.mentor-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 12px;
}

.mentor-card {
  position: relative;
  border: 1px solid rgba(15, 23, 42, 0.1);
  border-radius: 10px;
  padding: 14px 14px 12px;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease, box-shadow 0.18s ease,
    transform 0.18s ease;
  background: rgba(15, 23, 42, 0.045);
}

.mentor-card:hover {
  transform: translateY(-2px);
  border-color: rgba(59, 130, 246, 0.55);
  background: rgba(59, 130, 246, 0.08);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.4), 0 0 0 1px rgba(59, 130, 246, 0.18);
}

/* 选中态：品牌渐变底 + 发光边框，文字提亮为纯白保证可读 */
.mentor-card.selected {
  border-color: var(--sys-primary);
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.92) 0%, rgba(8, 145, 178, 0.88) 100%);
  box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.45), 0 8px 20px rgba(59, 130, 246, 0.25);
}

.mentor-card.selected .mc-name {
  color: #ffffff;
}

/* 选中卡片上所有次级文字提亮为白色，保证在蓝青渐变底上可读 */
.mentor-card.selected .mc-dir,
.mentor-card.selected .mc-intro,
.mentor-card.selected .mc-quota {
  color: rgba(255, 255, 255, 0.9);
}

.mentor-card.selected .mc-quota b {
  color: #ffffff;
}

/* 选中卡片的虚线分隔线提亮 */
.mentor-card.selected .mc-foot {
  border-top-color: rgba(255, 255, 255, 0.3);
}

/* 选中卡片上的标签：白底深字，避免在渐变底上发灰 */
.mentor-card.selected :deep(.el-tag) {
  color: #1f2937;
  border-color: rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.92);
}

.mentor-card.selected :deep(.el-tag.el-tag--info) {
  color: #374151;
  border-color: rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.85);
}

.mentor-card.disabled {
  background: rgba(15, 23, 42, 0.02);
  cursor: not-allowed;
  opacity: 0.68;
}

.mentor-card.disabled:hover {
  transform: none;
  border-color: rgba(15, 23, 42, 0.1);
  background: rgba(15, 23, 42, 0.02);
  box-shadow: none;
}

.mentor-card.readonly {
  cursor: default;
}

.mentor-card.readonly:hover {
  transform: none;
  border-color: rgba(15, 23, 42, 0.1);
  background: rgba(15, 23, 42, 0.045);
  box-shadow: none;
}

.mentor-card.readonly.selected:hover {
  border-color: var(--sys-primary);
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.92) 0%, rgba(8, 145, 178, 0.88) 100%);
}

.mc-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mc-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--sys-text);
}

.mc-dir {
  margin-top: 6px;
  font-size: 12px;
  color: #4b5563;
}

.mc-intro {
  margin-top: 4px;
  font-size: 12px;
  color: #4b5563;
  line-height: 1.6;
  min-height: 34px;
}

.mc-foot {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed rgba(15, 23, 42, 0.12);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.mc-quota {
  font-size: 12px;
  color: #4b5563;
}

.mc-quota b {
  color: var(--sys-cyan);
  font-weight: 600;
}

.mc-quota b.zero {
  color: var(--sys-danger);
}

.mc-check {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--sys-primary), var(--sys-cyan));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 12px rgba(6, 182, 212, 0.6);
}

.mc-full {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 11px;
  color: var(--sys-danger);
  background: rgba(248, 113, 113, 0.14);
  border: 1px solid rgba(248, 113, 113, 0.3);
}

/* 标签在浅色卡片上使用深色文字，避免发灰 */
.lab-block :deep(.el-tag) {
  color: #0e7490;
  border-color: rgba(8, 145, 178, 0.32);
  background: rgba(8, 145, 178, 0.08);
}

.lab-block :deep(.el-tag.el-tag--info) {
  color: #4b5563;
  border-color: rgba(15, 23, 42, 0.15);
  background: rgba(15, 23, 42, 0.05);
}
</style>
