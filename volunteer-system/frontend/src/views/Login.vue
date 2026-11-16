<template>
  <div class="login-page">
    <!-- 左侧 60% 动态视觉区：Canvas 粒子网络铺底 -->
    <section ref="brandRef" class="login-brand">
      <canvas ref="canvasRef" class="login-canvas" aria-hidden="true"></canvas>
      <div class="login-glow login-glow-a" aria-hidden="true"></div>
      <div class="login-glow login-glow-b" aria-hidden="true"></div>

      <div class="brand-head">
        <div class="brand-mark">
          <el-icon :size="24"><School /></el-icon>
        </div>
        <div>
          <div class="brand-name">学生实训室选组选拔系统</div>
          <div class="brand-en">Student Lab Selection &amp; Recruitment</div>
        </div>
      </div>

      <h1 class="brand-slogan">
        双选分组，流程可溯<br />
        让每一次选拔公开、公平、可追踪
      </h1>
      <p class="brand-desc">
        覆盖集中宣讲、实训室开放日、意向填报、导师初选与面试考核、拟录取确认、分轮补录、
        统筹调剂与结果公示共 8 个环节，全流程在线留痕。
      </p>

      <div class="brand-points">
        <div class="point">
          <span class="point-dot"></span>
          <div>
            <div class="point-title">8 环节时间窗驱动</div>
            <div class="point-sub">开放时间由学院统一配置，端口自动开关</div>
          </div>
        </div>
        <div class="point">
          <span class="point-dot"></span>
          <div>
            <div class="point-title">最多 3 个平行导师意向</div>
            <div class="point-sub">支持实训室内调剂与全院跨实训室调剂</div>
          </div>
        </div>
        <div class="point">
          <span class="point-dot"></span>
          <div>
            <div class="point-title">四维评分与递补顺位</div>
            <div class="point-sub">评审结论、递补、公示异议全流程闭环</div>
          </div>
        </div>
      </div>

      <div class="brand-foot">© 2026 高校实训室选组选拔 · 演示环境</div>
    </section>

    <!-- 右侧 40% 毛玻璃登录卡片 -->
    <section class="login-panel">
      <div class="login-card">
        <div class="login-title" style="--i: 1">账号登录</div>
        <div class="login-sub" style="--i: 2">请使用学院分配的账号进入系统</div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="login-form"
          style="--i: 3"
          @keyup.enter="submit"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              size="large"
              placeholder="请输入用户名"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <div class="login-meta" style="--i: 4">
            <el-checkbox v-model="rememberMe" size="default">记住我</el-checkbox>
            <button type="button" class="login-link" @click="onForgot">忘记密码？</button>
          </div>

          <button
            ref="btnRef"
            type="button"
            class="login-btn"
            :class="{ 'is-loading': loading }"
            :disabled="loading"
            @click="onLoginClick"
          >
            <span class="login-btn-text">{{ loading ? '登录中…' : '登 录' }}</span>
          </button>
        </el-form>

        <div class="demo-block" style="--i: 5">
          <div class="demo-title">快捷演示账号</div>

          <!-- 角色胶囊：选中滑块平滑移动并发光 -->
          <div class="login-role-pills" :style="{ '--idx': activeIndex }">
            <span class="login-role-slider" aria-hidden="true"></span>

            <template v-for="(account, index) in demoAccounts" :key="index">
              <!-- 实训室胶囊：点击弹出子菜单 -->
              <el-popover
                v-if="account.labAccounts"
                trigger="click"
                placement="bottom"
                :width="220"
                v-model:visible="labPopoverVisible"
                popper-class="lab-popper"
              >
                <template #reference>
                  <button
                    type="button"
                    class="demo-chip login-role-pill"
                    :class="{ 'is-active': index === activeIndex }"
                    @click="choose(account, index)"
                  >
                    {{ account.label }}
                    <span class="pill-caret">▾</span>
                  </button>
                </template>
                <div class="lab-popper-title">请选择实训室</div>
                <button
                  v-for="(sub, subIdx) in account.labAccounts"
                  :key="sub.username"
                  type="button"
                  class="lab-popper-item"
                  :class="{ 'is-active': index === activeIndex && subIdx === labSubIndex }"
                  @click="chooseLabSub(sub, subIdx)"
                >
                  <span class="lab-popper-name">{{ sub.label }}</span>
                  <span class="lab-popper-user">{{ sub.username }}</span>
                </button>
              </el-popover>

              <!-- 导师胶囊：点击弹出子菜单 -->
              <el-popover
                v-else-if="account.mentorAccounts"
                trigger="click"
                placement="bottom"
                :width="240"
                v-model:visible="mentorPopoverVisible"
                popper-class="lab-popper"
              >
                <template #reference>
                  <button
                    type="button"
                    class="demo-chip login-role-pill"
                    :class="{ 'is-active': index === activeIndex }"
                    @click="choose(account, index)"
                  >
                    {{ account.label }}
                    <span class="pill-caret">▾</span>
                  </button>
                </template>
                <div class="lab-popper-title">请选择导师</div>
                <div class="mentor-popper-grid">
                  <button
                    v-for="(sub, subIdx) in account.mentorAccounts"
                    :key="sub.username"
                    type="button"
                    class="mentor-popper-item"
                    :class="{ 'is-active': index === activeIndex && subIdx === mentorSubIndex }"
                    @click="chooseMentorSub(sub, subIdx)"
                  >
                    <span class="mentor-popper-name">{{ sub.label }}</span>
                    <span class="mentor-popper-user">{{ sub.username }}</span>
                  </button>
                </div>
              </el-popover>

              <!-- 学生胶囊：点击弹出子菜单 -->
              <el-popover
                v-else-if="account.studentAccounts"
                trigger="click"
                placement="bottom"
                :width="320"
                v-model:visible="studentPopoverVisible"
                popper-class="lab-popper"
              >
                <template #reference>
                  <button
                    type="button"
                    class="demo-chip login-role-pill"
                    :class="{ 'is-active': index === activeIndex }"
                    @click="choose(account, index)"
                  >
                    {{ account.label }}
                    <span class="pill-caret">▾</span>
                  </button>
                </template>
                <div class="lab-popper-title">请选择学生（共 {{ account.studentAccounts.length }} 人）</div>
                <div class="student-popper-grid">
                  <button
                    v-for="(sub, subIdx) in account.studentAccounts"
                    :key="sub.username"
                    type="button"
                    class="student-popper-item"
                    :class="{ 'is-active': index === activeIndex && subIdx === studentSubIndex }"
                    @click="chooseStudentSub(sub, subIdx)"
                  >
                    <span class="student-popper-name">{{ sub.label }}</span>
                    <span class="student-popper-sub">{{ sub.sub }}</span>
                  </button>
                </div>
              </el-popover>

              <!-- 普通胶囊 -->
              <button
                v-else
                type="button"
                class="demo-chip login-role-pill"
                :class="{ 'is-active': index === activeIndex }"
                @click="choose(account, index)"
              >
                {{ account.label }}
              </button>
            </template>
          </div>

          <div class="login-credential">
            <span class="login-cred-label">当前账号</span>
            <span class="login-cred-text">
              {{ activeAccount.username }} / {{ activeAccount.password }}
            </span>
          </div>
        </div>

        <div class="login-foot" style="--i: 6">
          © 2026 高校实训室选组选拔系统 · 仅用于教学演示
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, School } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { HOME_BY_ROLE } from '@/utils/menu'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const demoAccounts = [
  { username: 'admin', password: '123456', label: '学院管理员' },
  {
    username: 'labadmin1',
    password: '123456',
    label: '实训室',
    labAccounts: [
      { username: 'labadmin1', password: '123456', label: '实训室 A（LAB-A）' },
      { username: 'labadmin2', password: '123456', label: '实训室 B（LAB-B）' },
      { username: 'labadmin3', password: '123456', label: '实训室 C（LAB-C）' },
      { username: 'labadmin4', password: '123456', label: '实训室 D（LAB-D）' }
    ]
  },
  {
    username: 'm01',
    password: '123456',
    label: '导师',
    mentorAccounts: [
      { username: 'm01', password: '123456', label: '张明（A室）' },
      { username: 'm02', password: '123456', label: '李慧（A室）' },
      { username: 'm03', password: '123456', label: '王磊（A室）' },
      { username: 'm04', password: '123456', label: '赵敏（B室）' },
      { username: 'm05', password: '123456', label: '陈昊（B室）' },
      { username: 'm06', password: '123456', label: '周颖（B室）' },
      { username: 'm07', password: '123456', label: '吴涛（C室）' },
      { username: 'm08', password: '123456', label: '郑璇（C室）' },
      { username: 'm09', password: '123456', label: '孙鹏（C室）' },
      { username: 'm10', password: '123456', label: '钱宁（D室）' },
      { username: 'm11', password: '123456', label: '冯宇（D室）' },
      { username: 'm12', password: '123456', label: '韩雪（D室）' }
    ]
  },
  {
    username: 'stu01',
    password: '123456',
    label: '学生',
    studentAccounts: [
      { username: 'stu01', password: '123456', label: '学生01', sub: 'AI1' },
      { username: 'stu02', password: '123456', label: '学生02', sub: 'AI1' },
      { username: 'stu03', password: '123456', label: '学生03', sub: 'AI1' },
      { username: 'stu04', password: '123456', label: '学生04', sub: 'AI1' },
      { username: 'stu05', password: '123456', label: '学生05', sub: 'AI1' },
      { username: 'stu06', password: '123456', label: '学生06', sub: 'AI1' },
      { username: 'stu07', password: '123456', label: '学生07', sub: 'AI1' },
      { username: 'stu08', password: '123456', label: '学生08', sub: 'AI1' },
      { username: 'stu09', password: '123456', label: '学生09', sub: 'AI1' },
      { username: 'stu10', password: '123456', label: '学生10', sub: 'AI1' },
      { username: 'stu11', password: '123456', label: '学生11', sub: 'AI1' },
      { username: 'stu12', password: '123456', label: '学生12', sub: 'AI1' },
      { username: 'stu13', password: '123456', label: '学生13', sub: 'AI1' },
      { username: 'stu14', password: '123456', label: '学生14', sub: 'AI1' },
      { username: 'stu15', password: '123456', label: '学生15', sub: 'AI1' },
      { username: 'stu16', password: '123456', label: '学生16', sub: 'AI2' },
      { username: 'stu17', password: '123456', label: '学生17', sub: 'AI2' },
      { username: 'stu18', password: '123456', label: '学生18', sub: 'AI2' },
      { username: 'stu19', password: '123456', label: '学生19', sub: 'AI2' },
      { username: 'stu20', password: '123456', label: '学生20', sub: 'AI2' },
      { username: 'stu21', password: '123456', label: '学生21', sub: 'AI2' },
      { username: 'stu22', password: '123456', label: '学生22', sub: 'AI2' },
      { username: 'stu23', password: '123456', label: '学生23', sub: 'AI2' },
      { username: 'stu24', password: '123456', label: '学生24', sub: 'AI2' },
      { username: 'stu25', password: '123456', label: '学生25', sub: 'AI2' },
      { username: 'stu26', password: '123456', label: '学生26', sub: 'AI2' },
      { username: 'stu27', password: '123456', label: '学生27', sub: 'AI2' },
      { username: 'stu28', password: '123456', label: '学生28', sub: 'AI2' },
      { username: 'stu29', password: '123456', label: '学生29', sub: 'AI2' },
      { username: 'stu30', password: '123456', label: '学生30', sub: 'AI2' },
      { username: 'stu31', password: '123456', label: '学生31', sub: 'AI1' },
      { username: 'stu32', password: '123456', label: '学生32', sub: 'AI1' }
    ]
  }
]

/* ---------------- 角色胶囊 + 快捷填充 ---------------- */
const activeIndex = ref(3)
// 实训室 / 导师 / 学生胶囊当前选中的子账号
const labSubIndex = ref(0)
const mentorSubIndex = ref(0)
const studentSubIndex = ref(0)
const labPopoverVisible = ref(false)
const mentorPopoverVisible = ref(false)
const studentPopoverVisible = ref(false)

const activeAccount = computed(() => {
  const cur = demoAccounts[activeIndex.value]
  if (cur && cur.labAccounts) {
    return cur.labAccounts[labSubIndex.value] || cur.labAccounts[0]
  }
  if (cur && cur.mentorAccounts) {
    return cur.mentorAccounts[mentorSubIndex.value] || cur.mentorAccounts[0]
  }
  if (cur && cur.studentAccounts) {
    return cur.studentAccounts[studentSubIndex.value] || cur.studentAccounts[0]
  }
  return cur
})

function fill(account) {
  form.username = account.username
  form.password = account.password
}

function choose(account, index) {
  activeIndex.value = index
  if (account.labAccounts) labSubIndex.value = 0
  if (account.mentorAccounts) mentorSubIndex.value = 0
  if (account.studentAccounts) studentSubIndex.value = 0
  fill(activeAccount.value)
  rememberMe.value = false
}

function chooseLabSub(item, subIndex) {
  labSubIndex.value = subIndex
  fill(item)
  labPopoverVisible.value = false
  rememberMe.value = false
}

function chooseMentorSub(item, subIndex) {
  mentorSubIndex.value = subIndex
  fill(item)
  mentorPopoverVisible.value = false
  rememberMe.value = false
}

function chooseStudentSub(item, subIndex) {
  studentSubIndex.value = subIndex
  fill(item)
  studentPopoverVisible.value = false
  rememberMe.value = false
}

/* ---------------- 记住我 / 忘记密码 ---------------- */
const REMEMBER_KEY = 'login_remember_username'
const rememberMe = ref(false)

function onForgot() {
  ElMessage.info('请联系学院教务或实训室管理员重置密码')
}

/* ---------------- 按钮 ripple ---------------- */
const btnRef = ref(null)

function onLoginClick(event) {
  spawnRipple(event)
  submit()
}

function spawnRipple(event) {
  const btn = btnRef.value
  if (!btn) return
  const rect = btn.getBoundingClientRect()
  const size = Math.max(rect.width, rect.height)
  const ripple = document.createElement('span')
  ripple.className = 'login-ripple'
  ripple.style.width = `${size}px`
  ripple.style.height = `${size}px`
  ripple.style.left = `${(event.clientX || rect.left + rect.width / 2) - rect.left - size / 2}px`
  ripple.style.top = `${(event.clientY || rect.top + rect.height / 2) - rect.top - size / 2}px`
  btn.appendChild(ripple)
  ripple.addEventListener('animationend', () => ripple.remove())
}

/* ---------------- 登录逻辑（保持原实现） ---------------- */
function submit() {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const user = await userStore.login(form.username.trim(), form.password)
      if (rememberMe.value) {
        localStorage.setItem(REMEMBER_KEY, form.username.trim())
      } else {
        localStorage.removeItem(REMEMBER_KEY)
      }
      ElMessage.success(`欢迎回来，${user.realName || user.username}`)
      router.replace(HOME_BY_ROLE[user.role] || '/login')
    } catch (e) {
      /* 错误提示由响应拦截器处理 */
    } finally {
      loading.value = false
    }
  })
}

/* ---------------- Canvas 粒子网络 ---------------- */
const brandRef = ref(null)
const canvasRef = ref(null)

const LINK_DIST = 120 // 连线阈值
const MOUSE_DIST = 170 // 鼠标吸引半径
const reduceMotion =
  typeof window !== 'undefined' &&
  window.matchMedia('(prefers-reduced-motion: reduce)').matches

let ctx = null
let dpr = 1
let W = 0
let H = 0
let nodes = []
let rafId = 0
let running = false
let resizeObserver = null
const pointer = { x: -9999, y: -9999, active: false }

function initCanvas() {
  const canvas = canvasRef.value
  const host = brandRef.value
  if (!canvas || !host) return
  ctx = canvas.getContext('2d')
  dpr = Math.min(window.devicePixelRatio || 1, 2)
  resizeCanvas()
  buildNodes()

  canvas.addEventListener('pointermove', onPointerMove)
  canvas.addEventListener('pointerleave', onPointerLeave)

  resizeObserver = new ResizeObserver(() => {
    resizeCanvas()
    buildNodes()
  })
  resizeObserver.observe(host)

  document.addEventListener('visibilitychange', onVisibilityChange)
  window.addEventListener('blur', pause)
  window.addEventListener('focus', resume)

  if (!reduceMotion) start()
  else drawFrame() // 降级：只画一帧静态网络
}

function resizeCanvas() {
  const canvas = canvasRef.value
  const host = brandRef.value
  if (!canvas || !host) return
  W = host.clientWidth
  H = host.clientHeight
  canvas.width = Math.floor(W * dpr)
  canvas.height = Math.floor(H * dpr)
  canvas.style.width = `${W}px`
  canvas.style.height = `${H}px`
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
}

/** 节点数量按面积估算，限制在 60~80 之间，兼顾观感与性能 */
function buildNodes() {
  const count = Math.max(60, Math.min(80, Math.round((W * H) / 14000)))
  nodes = Array.from({ length: count }, (_, i) => ({
    x: Math.random() * W,
    y: Math.random() * H,
    vx: (Math.random() - 0.5) * 0.28,
    vy: (Math.random() - 0.5) * 0.28,
    r: Math.random() * 1.3 + 0.9,
    // 少量节点使用强调色，避免画面杂乱
    accent: i % 23 === 0
  }))
}

function onPointerMove(event) {
  const rect = canvasRef.value.getBoundingClientRect()
  pointer.x = event.clientX - rect.left
  pointer.y = event.clientY - rect.top
  pointer.active = true
}

function onPointerLeave() {
  pointer.active = false
  pointer.x = -9999
  pointer.y = -9999
}

function drawFrame() {
  if (!ctx) return
  ctx.clearRect(0, 0, W, H)

  // 节点位移 + 鼠标轻微吸引
  for (const node of nodes) {
    if (pointer.active) {
      const dx = pointer.x - node.x
      const dy = pointer.y - node.y
      const dist = Math.hypot(dx, dy)
      if (dist < MOUSE_DIST && dist > 0.1) {
        const pull = (1 - dist / MOUSE_DIST) * 0.045
        node.vx += (dx / dist) * pull
        node.vy += (dy / dist) * pull
      }
    }

    node.x += node.vx
    node.y += node.vy

    // 速度衰减，避免被鼠标推得太快
    node.vx *= 0.985
    node.vy *= 0.985
    if (Math.abs(node.vx) < 0.05) node.vx += (Math.random() - 0.5) * 0.04
    if (Math.abs(node.vy) < 0.05) node.vy += (Math.random() - 0.5) * 0.04

    // 边界回弹
    if (node.x < 0 || node.x > W) node.vx *= -1
    if (node.y < 0 || node.y > H) node.vy *= -1
    node.x = Math.max(0, Math.min(W, node.x))
    node.y = Math.max(0, Math.min(H, node.y))
  }

  // 节点间连线：距离越近越亮
  for (let i = 0; i < nodes.length; i++) {
    const a = nodes[i]
    for (let j = i + 1; j < nodes.length; j++) {
      const b = nodes[j]
      const dx = a.x - b.x
      const dy = a.y - b.y
      const dist = Math.hypot(dx, dy)
      if (dist > LINK_DIST) continue
      const alpha = (1 - dist / LINK_DIST) * 0.28
      ctx.strokeStyle = `rgba(59, 130, 246, ${alpha})`
      ctx.lineWidth = 1
      ctx.beginPath()
      ctx.moveTo(a.x, a.y)
      ctx.lineTo(b.x, b.y)
      ctx.stroke()
    }
  }

  // 鼠标附近：高亮连线与节点
  if (pointer.active) {
    ctx.strokeStyle = 'rgba(8, 145, 178, 0.4)'
    ctx.lineWidth = 1
    for (const node of nodes) {
      const dist = Math.hypot(node.x - pointer.x, node.y - pointer.y)
      if (dist > MOUSE_DIST) continue
      ctx.globalAlpha = 1 - dist / MOUSE_DIST
      ctx.beginPath()
      ctx.moveTo(pointer.x, pointer.y)
      ctx.lineTo(node.x, node.y)
      ctx.stroke()
    }
    ctx.globalAlpha = 1

    const halo = ctx.createRadialGradient(pointer.x, pointer.y, 0, pointer.x, pointer.y, MOUSE_DIST)
    halo.addColorStop(0, 'rgba(59, 130, 246, 0.10)')
    halo.addColorStop(1, 'rgba(59, 130, 246, 0)')
    ctx.fillStyle = halo
    ctx.beginPath()
    ctx.arc(pointer.x, pointer.y, MOUSE_DIST, 0, Math.PI * 2)
    ctx.fill()
  }

  // 节点本体
  for (const node of nodes) {
    const nearMouse =
      pointer.active && Math.hypot(node.x - pointer.x, node.y - pointer.y) < MOUSE_DIST
    ctx.beginPath()
    ctx.arc(node.x, node.y, nearMouse ? node.r + 1.2 : node.r, 0, Math.PI * 2)
    if (node.accent) {
      ctx.fillStyle = nearMouse ? 'rgba(245, 158, 11, 0.95)' : 'rgba(245, 158, 11, 0.7)'
    } else {
      ctx.fillStyle = nearMouse ? 'rgba(8, 145, 178, 0.95)' : 'rgba(59, 130, 246, 0.7)'
    }
    ctx.fill()
  }
}

function loop() {
  drawFrame()
  rafId = requestAnimationFrame(loop)
}

function start() {
  if (running || reduceMotion) return
  running = true
  rafId = requestAnimationFrame(loop)
}

function pause() {
  if (!running) return
  running = false
  cancelAnimationFrame(rafId)
}

function resume() {
  if (running || document.hidden) return
  start()
}

/** 页面失焦 / 切到后台时暂停，节省 CPU */
function onVisibilityChange() {
  if (document.hidden) pause()
  else resume()
}

onMounted(() => {
  const remembered = localStorage.getItem(REMEMBER_KEY)
  if (remembered) {
    form.username = remembered
    rememberMe.value = true
    const index = demoAccounts.findIndex((item) => item.username === remembered)
    if (index >= 0) activeIndex.value = index
  }
  nextTick(initCanvas)
})

onBeforeUnmount(() => {
  pause()
  window.removeEventListener('blur', pause)
  window.removeEventListener('focus', resume)
  document.removeEventListener('visibilitychange', onVisibilityChange)
  const canvas = canvasRef.value
  if (canvas) {
    canvas.removeEventListener('pointermove', onPointerMove)
    canvas.removeEventListener('pointerleave', onPointerLeave)
  }
  if (resizeObserver) resizeObserver.disconnect()
})
</script>

<style scoped>
/* ================= 变量 ================= */
.login-page {
  --login-bg-a: #f8fafc;
  --login-bg-b: #ffffff;
  --login-primary: #3b82f6;
  --login-cyan: #0891b2;
  --login-accent: #f59e0b;
  --login-text: #1f2937;
  --login-text-sub: #6b7280;
  --login-line: #e5e7eb;

  min-height: 100vh;
  display: flex;
  background: linear-gradient(135deg, var(--login-bg-a) 0%, var(--login-bg-b) 100%);
  color: var(--login-text);
  overflow: hidden;
}

/* ================= 左侧视觉区 60% ================= */
.login-brand {
  position: relative;
  flex: 1 1 60%;
  padding: 64px 72px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
  isolation: isolate;
}

/* 粒子网络画布，不拦截鼠标事件 */
.login-canvas {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: auto;
  cursor: crosshair;
}

/* 两团品牌色光晕，做背景层次 */
.login-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  z-index: -1;
  pointer-events: none;
}

.login-glow-a {
  width: 460px;
  height: 460px;
  top: -140px;
  left: -120px;
  background: rgba(59, 130, 246, 0.22);
  animation: login-float 14s ease-in-out infinite;
}

.login-glow-b {
  width: 420px;
  height: 420px;
  right: -120px;
  bottom: -160px;
  background: rgba(6, 182, 212, 0.18);
  animation: login-float 18s ease-in-out infinite reverse;
}

.brand-head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-mark {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, var(--login-primary) 0%, var(--login-cyan) 100%);
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.38);
}

.brand-name {
  font-size: 19px;
  font-weight: 700;
  letter-spacing: 1px;
  color: var(--login-text);
}

.brand-en {
  margin-top: 2px;
  font-size: 11px;
  letter-spacing: 0.6px;
  color: var(--login-text-sub);
}

.brand-slogan {
  position: relative;
  z-index: 1;
  margin: 48px 0 0;
  font-size: 34px;
  line-height: 1.5;
  font-weight: 700;
  letter-spacing: 1px;
  color: var(--login-text);
  background: linear-gradient(120deg, #1f2937 0%, #3b82f6 55%, #0891b2 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.brand-desc {
  position: relative;
  z-index: 1;
  margin-top: 22px;
  max-width: 520px;
  font-size: 14px;
  line-height: 1.9;
  color: var(--login-text-sub);
}

.brand-points {
  position: relative;
  z-index: 1;
  margin-top: 40px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 480px;
}

.point {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.point-dot {
  flex: 0 0 auto;
  width: 8px;
  height: 8px;
  margin-top: 7px;
  border-radius: 50%;
  background: var(--login-cyan);
  box-shadow: 0 0 0 4px rgba(6, 182, 212, 0.16), 0 0 12px rgba(6, 182, 212, 0.6);
}

.point-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--login-text);
}

.point-sub {
  margin-top: 3px;
  font-size: 12px;
  color: var(--login-text-sub);
}

.brand-foot {
  position: relative;
  z-index: 1;
  margin-top: 56px;
  font-size: 12px;
  color: var(--login-text-sub);
}

/* ================= 右侧面板 40% ================= */
.login-panel {
  flex: 0 0 40%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  position: relative;
}

/* 白色卡片 */
.login-card {
  position: relative;
  width: 100%;
  max-width: 400px;
  padding: 36px 34px 26px;
  border-radius: 16px;
  border: 1px solid var(--login-line);
  background: #ffffff;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);
  /* 卡片整体自下向上淡入 */
  animation: login-card-in 600ms cubic-bezier(0.16, 1, 0.3, 1) both;
}

/* 内部元素 stagger 依次淡入，间隔 80ms */
.login-title,
.login-sub,
.login-form,
.demo-block,
.login-foot,
.login-meta {
  animation: login-item-in 520ms cubic-bezier(0.16, 1, 0.3, 1) both;
  animation-delay: calc(var(--i, 0) * 80ms + 120ms);
}

.login-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--login-text);
  letter-spacing: 0.5px;
}

.login-sub {
  margin-top: 8px;
  font-size: 13px;
  color: var(--login-text-sub);
}

.login-form {
  margin-top: 24px;
}

/* ---------- Element Plus 深色输入框 ---------- */
.login-form :deep(.el-form-item__label) {
  color: var(--login-text-sub);
  font-size: 13px;
  padding-bottom: 6px;
}

.login-form :deep(.el-input__wrapper) {
  background: #f9fafb;
  border-radius: 10px;
  box-shadow: inset 0 0 0 1px #e5e7eb;
  transition: box-shadow 0.25s ease, background 0.25s ease;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: inset 0 0 0 1px #d1d5db;
}

/* 聚焦：品牌色流光 + 3px 外发光环 */
.login-form :deep(.el-input__wrapper.is-focus) {
  background: #ffffff;
  background-image: linear-gradient(
    90deg,
    rgba(59, 130, 246, 0.08),
    rgba(8, 145, 178, 0.08),
    rgba(59, 130, 246, 0.08)
  );
  background-size: 200% 100%;
  box-shadow: inset 0 0 0 1px rgba(59, 130, 246, 0.9), 0 0 0 3px rgba(59, 130, 246, 0.15);
  animation: login-input-flow 2.4s linear infinite;
}

.login-form :deep(.el-input__inner) {
  color: var(--login-text);
  caret-color: var(--login-cyan);
}

.login-form :deep(.el-input__inner::placeholder) {
  color: #9ca3af;
}

/* 聚焦时前缀图标变色 */
.login-form :deep(.el-input__prefix .el-icon) {
  color: #9ca3af;
  transition: color 0.25s ease;
}

.login-form :deep(.el-input__wrapper.is-focus .el-input__prefix .el-icon) {
  color: var(--login-cyan);
}

/* 密码可见 / 清空图标跟随浅色主题 */
.login-form :deep(.el-input__suffix .el-icon) {
  color: #9ca3af;
  transition: color 0.2s ease;
}

.login-form :deep(.el-input__suffix .el-icon:hover) {
  color: var(--login-text);
}

/* 校验失败文案颜色，保证在浅色卡片上可读 */
.login-form :deep(.el-form-item__error) {
  color: #ef4444;
}

/* 记住我 / 忘记密码 */
.login-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -4px 0 18px;
}

.login-meta :deep(.el-checkbox__label) {
  color: var(--login-text-sub);
  font-size: 13px;
}

.login-meta :deep(.el-checkbox__inner) {
  background: #ffffff;
  border-color: #d1d5db;
}

.login-link {
  border: 0;
  background: none;
  padding: 0;
  font-size: 13px;
  font-family: inherit;
  color: var(--login-cyan);
  cursor: pointer;
  transition: color 0.2s ease;
}

.login-link:hover {
  color: #67e8f9;
  text-decoration: underline;
}

/* ---------- 登录按钮：渐变 + 斜向扫光 + 悬浮 + ripple ---------- */
.login-btn {
  position: relative;
  width: 100%;
  height: 46px;
  border: 0;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  font-family: inherit;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 2px;
  color: #fff;
  background: linear-gradient(120deg, var(--login-primary) 0%, var(--login-cyan) 100%);
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.32);
  transition: transform 0.2s ease, box-shadow 0.25s ease;
}

/* 斜向白光扫过，3s 循环 */
.login-btn::after {
  content: "";
  position: absolute;
  top: -20%;
  left: -60%;
  width: 45%;
  height: 140%;
  background: linear-gradient(
    100deg,
    rgba(255, 255, 255, 0) 0%,
    rgba(255, 255, 255, 0.55) 50%,
    rgba(255, 255, 255, 0) 100%
  );
  transform: skewX(-20deg);
  animation: login-shine 3s ease-in-out infinite;
  pointer-events: none;
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 14px 32px rgba(59, 130, 246, 0.5), 0 0 0 3px rgba(59, 130, 246, 0.16);
}

.login-btn:active:not(:disabled) {
  transform: translateY(0);
}

/* 键盘聚焦时给出明确的可见焦点环 */
.login-btn:focus-visible {
  outline: 2px solid var(--login-cyan);
  outline-offset: 3px;
}

.login-btn:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.login-btn-text {
  position: relative;
  z-index: 1;
}

/* 点击扩散水波 */
.login-ripple {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.45);
  transform: scale(0);
  animation: login-ripple 560ms ease-out forwards;
  pointer-events: none;
}

/* ---------- 演示账号 ---------- */
.demo-block {
  margin-top: 26px;
  padding-top: 20px;
  border-top: 1px solid var(--login-line);
}

.demo-title {
  margin-bottom: 12px;
  font-size: 12px;
  color: var(--login-text-sub);
}

/* 角色胶囊容器 */
.login-role-pills {
  position: relative;
  display: flex;
  padding: 4px;
  border-radius: 12px;
  background: #f3f4f6;
  border: 1px solid var(--login-line);
}

/* 选中滑块：宽度为 1/4，按 --idx 平移 */
.login-role-slider {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc((100% - 8px) / 4);
  height: calc(100% - 8px);
  border-radius: 9px;
  background: linear-gradient(120deg, rgba(59, 130, 246, 0.9), rgba(6, 182, 212, 0.9));
  box-shadow: 0 0 16px rgba(59, 130, 246, 0.6);
  transform: translateX(calc(var(--idx, 0) * 100%));
  transition: transform 380ms cubic-bezier(0.16, 1, 0.3, 1);
  pointer-events: none;
}

/* 胶囊按钮（保留原 demo-chip 类名） */
.demo-chip {
  font-family: inherit;
}

.login-role-pill {
  position: relative;
  z-index: 1;
  flex: 1 1 0;
  padding: 8px 4px;
  border: 0;
  border-radius: 9px;
  background: transparent;
  color: var(--login-text-sub);
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  cursor: pointer;
  transition: color 0.25s ease;
}

.login-role-pill.is-active {
  color: #fff;
  font-weight: 600;
}

.pill-caret {
  margin-left: 2px;
  font-size: 10px;
  opacity: 0.75;
}

/* 实训室下拉菜单样式 */
:deep(.lab-popper) {
  padding: 8px;
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.14);
}

.lab-popper-title {
  font-size: 12px;
  color: #6b7280;
  padding: 4px 10px 8px;
  border-bottom: 1px solid #f1f5f9;
  margin-bottom: 6px;
}

.lab-popper-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 8px 10px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  font-family: inherit;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
  transition: background 0.15s ease;
  margin-bottom: 2px;
}

.lab-popper-item:hover {
  background: #eff6ff;
}

.lab-popper-item.is-active {
  background: linear-gradient(120deg, rgba(59, 130, 246, 0.14), rgba(6, 182, 212, 0.14));
  color: #0369a1;
  font-weight: 600;
}

.lab-popper-name {
  flex: 1;
  text-align: left;
}

.lab-popper-user {
  font-size: 11px;
  color: #9ca3af;
  margin-left: 8px;
  font-variant-numeric: tabular-nums;
}

/* 导师下拉：两列网格 */
.mentor-popper-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
}

.mentor-popper-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 7px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 7px;
  background: #f9fafb;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.15s ease;
}

.mentor-popper-item:hover {
  background: #eff6ff;
  border-color: #93c5fd;
}

.mentor-popper-item.is-active {
  background: linear-gradient(120deg, rgba(59, 130, 246, 0.14), rgba(6, 182, 212, 0.14));
  border-color: #0ea5e9;
}

.mentor-popper-name {
  font-size: 12px;
  color: #374151;
  font-weight: 500;
}

.mentor-popper-user {
  font-size: 11px;
  color: #9ca3af;
  font-variant-numeric: tabular-nums;
}

/* 学生下拉：4 列网格 */
.student-popper-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 4px;
  max-height: 300px;
  overflow-y: auto;
}

.student-popper-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 6px 4px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f9fafb;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.15s ease;
}

.student-popper-item:hover {
  background: #eff6ff;
  border-color: #93c5fd;
}

.student-popper-item.is-active {
  background: linear-gradient(120deg, rgba(59, 130, 246, 0.14), rgba(6, 182, 212, 0.14));
  border-color: #0ea5e9;
}

.student-popper-name {
  font-size: 11px;
  color: #374151;
  font-weight: 500;
}

.student-popper-sub {
  font-size: 10px;
  color: #9ca3af;
  margin-top: 1px;
}

.login-credential {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding: 8px 12px;
  border-radius: 9px;
  background: #f9fafb;
  border: 1px dashed #e5e7eb;
}

.login-cred-label {
  font-size: 12px;
  color: var(--login-text-sub);
}

.login-cred-text {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.4px;
  color: var(--login-text);
  font-variant-numeric: tabular-nums;
}

.login-foot {
  margin-top: 20px;
  text-align: center;
  font-size: 11px;
  color: #64748b;
}

/* ================= keyframes ================= */
@keyframes login-card-in {
  from {
    opacity: 0;
    transform: translateY(28px) scale(0.985);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes login-item-in {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes login-input-flow {
  from {
    background-position: 0% 50%;
  }
  to {
    background-position: 200% 50%;
  }
}

@keyframes login-shine {
  0% {
    left: -60%;
  }
  55%,
  100% {
    left: 130%;
  }
}

@keyframes login-ripple {
  to {
    transform: scale(2.6);
    opacity: 0;
  }
}

@keyframes login-float {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(24px, 18px, 0);
  }
}

/* ================= 移动端：视觉区收为顶部 30vh Banner ================= */
@media (max-width: 900px) {
  .login-page {
    flex-direction: column;
    overflow-y: auto;
  }

  .login-brand {
    flex: 0 0 30vh;
    min-height: 200px;
    padding: 24px 20px;
    justify-content: flex-start;
  }

  .brand-slogan,
  .brand-desc,
  .brand-points,
  .brand-foot {
    display: none;
  }

  .login-panel {
    flex: 1 1 auto;
    padding: 24px 20px 40px;
    align-items: flex-start;
  }

  .login-card {
    max-width: 460px;
  }
}

/* ================= 无障碍：降级关闭粒子与扫光 ================= */
@media (prefers-reduced-motion: reduce) {
  .login-btn::after {
    animation: none;
    display: none;
  }

  .login-card,
  .login-title,
  .login-sub,
  .login-form,
  .demo-block,
  .login-foot,
  .login-meta {
    animation: none;
  }

  .login-glow {
    animation: none;
  }

  .login-role-slider {
    transition: none;
  }

  .login-form :deep(.el-input__wrapper.is-focus) {
    animation: none;
  }

  .login-ripple {
    animation-duration: 200ms;
  }
}
</style>
