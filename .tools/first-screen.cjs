// 计算首屏（登录页）实际加载的资源体积：从 index.html 入口出发，递归解析 chunk 依赖
const fs = require('fs')
const path = require('path')
const zlib = require('zlib')

const dist = process.argv[2]
const html = fs.readFileSync(path.join(dist, 'index.html'), 'utf8')

const entry = new Set()
for (const m of html.matchAll(/(?:src|href)="\/assets\/([^"]+)"/g)) entry.add(m[1])
// 登录路由是懒加载 chunk，但它属于首屏必然加载的资源，一并作为根节点统计
for (const extra of process.argv.slice(3)) entry.add(extra)

const seen = new Set()
const queue = [...entry]

while (queue.length) {
  const file = queue.shift()
  if (seen.has(file) || file.endsWith('.html')) continue
  seen.add(file)
  const full = path.join(dist, 'assets', file)
  if (!fs.existsSync(full) || !file.endsWith('.js')) continue
  const code = fs.readFileSync(full, 'utf8')
  // 只统计静态依赖（from "./x.js" 或 import "./x.js"），排除 import("./x.js") 这类路由懒加载
  for (const m of code.matchAll(/(?:from|import)\s*["']\.\/([A-Za-z0-9_\-.]+\.(?:js|css))["']/g)) {
    if (!seen.has(m[1])) queue.push(m[1])
  }
}

let raw = 0
let gz = 0
const rows = []
for (const file of seen) {
  const full = path.join(dist, 'assets', file)
  if (!fs.existsSync(full)) continue
  const buf = fs.readFileSync(full)
  raw += buf.length
  gz += zlib.gzipSync(buf, { level: 9 }).length
  rows.push([file, buf.length])
}

rows.sort((a, b) => b[1] - a[1])
for (const [f, n] of rows) console.log(`${(n / 1024).toFixed(1).padStart(8)} KB  ${f}`)
console.log('---')
console.log(`首个路由实际加载文件数: ${rows.length}`)
console.log(`原始体积: ${(raw / 1024).toFixed(1)} KB`)
console.log(`gzip 体积: ${(gz / 1024).toFixed(1)} KB`)
