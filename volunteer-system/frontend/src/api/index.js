import { get, post, put, del, download, upload } from './http'

/* ---------- 认证 ---------- */
export const login = (username, password) =>
  post('/auth/login', { username, password })
export const fetchMe = () => get('/auth/me')

/* ---------- 环节 ---------- */
export const fetchCurrentStage = () => get('/stage/current')
export const fetchStageList = () => get('/stage/list')

/* ---------- 目录 ---------- */
export const fetchLabs = () => get('/catalog/labs')

/* ---------- 实训室管理员本人 ---------- */
export const fetchLabMe = () => get('/lab/me')
export const updateLabMe = (payload) => put('/lab/me', payload)

/* ---------- 意向 ---------- */
export const fetchMyIntent = (round) => get('/intent/my', { round })
export const submitIntent = (payload) => post('/intent/submit', payload)

/* ---------- 面试 ---------- */
export const fetchMyInterviews = () => get('/interview/my')
export const fetchLabInterviews = (round) => get('/interview/lab', { round })
export const generateInterviews = (round) =>
  post('/interview/generate', { round })
export const scoreInterview = (id, payload) =>
  post(`/interview/${id}/score`, payload)

/* ---------- 拟录取 ---------- */
export const fetchMyOffers = () => get('/offer/my')
export const confirmOffer = (id) => post(`/offer/${id}/confirm`)
export const fetchMentorOffers = (round) => get('/offer/mentor', { round })
export const submitOfferBatch = (payload) => post('/offer/batch', payload)

/* ---------- 导师本人 ---------- */
export const fetchMentorQuota = (round) => get('/mentor/me/quota', { round })
export const fetchMentorMe = () => get('/mentor/me')
export const updateMentorMe = (payload) => put('/mentor/me', payload)
export const fetchMentorLabs = () => get('/mentor/labs')

/* ---------- 补录 ---------- */
export const fetchSupplementRounds = () => get('/supplement/rounds')
export const openSupplementRound = (roundNo) =>
  post('/supplement/open', { roundNo })

/* ---------- 统筹调剂 ---------- */
export const fetchTransferPool = () => get('/transfer/pool')
export const autoTransfer = () => post('/transfer/auto')
export const assignTransfer = (id, payload) =>
  post(`/transfer/${id}/assign`, payload)
export const refuseTransfer = (id, payload) =>
  post(`/transfer/${id}/refuse`, payload)

/* ---------- 公示 ---------- */
export const fetchPublicList = () => get('/public/list')
export const publishPublicList = () => post('/public/publish')
export const submitObjection = (content) =>
  post('/public/objection', { content })
export const downloadPublicExport = () => download('/public/export')

/* ---------- 学院管理 ---------- */
export const fetchOverview = () => get('/admin/overview')
export const fetchAuditLogs = () => get('/admin/audit')
export const updateStage = (code, payload) =>
  put(`/admin/stage/${code}`, payload)
export const advanceStage = () => post('/admin/stage/advance')
export const resetStageToIntent = () => post('/admin/stage/reset-intent')

/* ---------- 个人简介 ---------- */
export const fetchBio = () => get('/bio')
export const uploadBio = (file, onProgress) => upload('/bio/upload', file, onProgress)
export const deleteBio = () => del('/bio')
