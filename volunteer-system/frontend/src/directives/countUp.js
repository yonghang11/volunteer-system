/**
 * v-count-up —— 数字滚动计数指令
 * 用法：<span class="stat-value" v-count-up="12">12</span>
 *      <span v-count-up:2="3.5">3.50</span>   // 参数为小数位数
 *      <span v-count-up="12" data-suffix=" 人">  // data-suffix 追加单位
 * 进入视口才播放；系统开启「减少动态效果」时直接显示终值。
 */
const reduceMotion =
  typeof window !== 'undefined' &&
  window.matchMedia('(prefers-reduced-motion: reduce)').matches

const DURATION = 900

function render(el, value, decimals, suffix) {
  el.textContent = `${Number(value).toFixed(decimals)}${suffix}`
}

function animate(el, target, decimals, suffix) {
  const start = performance.now()
  const from = 0
  function step(now) {
    const progress = Math.min(1, (now - start) / DURATION)
    // easeOutCubic，收尾更自然
    const eased = 1 - Math.pow(1 - progress, 3)
    render(el, from + (target - from) * eased, decimals, suffix)
    if (progress < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}

export default {
  mounted(el, binding) {
    const target = Number(binding.value) || 0
    const decimals = Number(binding.arg) || 0
    const suffix = el.dataset.suffix || ''

    if (reduceMotion || typeof IntersectionObserver === 'undefined') {
      render(el, target, decimals, suffix)
      return
    }

    render(el, 0, decimals, suffix)

    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (!entry.isIntersecting) continue
          animate(el, target, decimals, suffix)
          observer.disconnect()
        }
      },
      { threshold: 0.3 }
    )

    observer.observe(el)
    el.__countUpObserver = observer
  },
  unmounted(el) {
    if (el.__countUpObserver) {
      el.__countUpObserver.disconnect()
      delete el.__countUpObserver
    }
  }
}
