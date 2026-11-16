// 检查 .vue 模板里使用但未在 script 中引入的组件（防止按需引入后图标/组件丢失）
const fs = require('fs')
const path = require('path')

const root = process.argv[2]
const problems = []

function walk(dir) {
  for (const name of fs.readdirSync(dir)) {
    const full = path.join(dir, name)
    const stat = fs.statSync(full)
    if (stat.isDirectory()) walk(full)
    else if (full.endsWith('.vue')) check(full)
  }
}

function check(file) {
  const text = fs.readFileSync(file, 'utf8')
  const tplMatch = text.match(/<template>[\s\S]*<\/template>/)
  const scriptMatch = text.match(/<script setup>[\s\S]*?<\/script>/)
  if (!tplMatch || !scriptMatch) return
  const tags = new Set([...tplMatch[0].matchAll(/<([A-Z][A-Za-z0-9]+)/g)].map((m) => m[1]))
  const script = scriptMatch[0]
  const missing = [...tags].filter((tag) => !script.includes(tag))
  if (missing.length) problems.push(`${path.relative(root, file)} -> ${missing.join(', ')}`)
}

walk(root)
console.log(problems.length ? problems.join('\n') : '全部组件均已在 script 中引入')
