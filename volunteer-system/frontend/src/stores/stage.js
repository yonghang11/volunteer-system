import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchCurrentStage } from '@/api'
import { parseTime } from '@/utils/format'

export const useStageStore = defineStore('stage', () => {
  const current = ref(null)
  const remainingMs = ref(0)

  let refreshTimer = null
  let tickTimer = null

  async function fetchCurrent() {
    try {
      const data = await fetchCurrentStage()
      current.value = data
      if (data && data.phase === 'OPEN') {
        const end = parseTime(data.endTime)
        remainingMs.value = end ? Math.max(0, end.getTime() - Date.now()) : 0
      } else {
        remainingMs.value = 0
      }
    } catch (e) {
      /* 提示已由响应拦截器统一处理 */
    }
  }

  function start() {
    if (refreshTimer) return
    fetchCurrent()
    refreshTimer = setInterval(fetchCurrent, 30000)
    tickTimer = setInterval(() => {
      if (remainingMs.value > 0) {
        remainingMs.value = Math.max(0, remainingMs.value - 1000)
      }
    }, 1000)
  }

  function stop() {
    if (refreshTimer) clearInterval(refreshTimer)
    if (tickTimer) clearInterval(tickTimer)
    refreshTimer = null
    tickTimer = null
  }

  return { current, remainingMs, fetchCurrent, start, stop }
})
