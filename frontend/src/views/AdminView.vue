<template>
  <div class="page">
    <van-nav-bar title="后台管理" />

    <div v-if="!token" class="card">
      <van-field v-model="loginForm.username" label="用户名" placeholder="admin" />
      <van-field v-model="loginForm.password" label="密码" type="password" placeholder="admin" />
      <van-button block type="primary" @click="login">登录</van-button>
    </div>

    <div v-else>
      <div class="card">
        <van-field v-model="activity.title" label="活动标题" />
        <van-field v-model="activity.date" label="活动日期" />
        <van-field v-model="activity.location" label="活动地点" />

        <div class="upload-title">微信收款码</div>
        <van-uploader :after-read="(file) => (wxFile = file.file)" :max-count="1" />
        <van-image v-if="activity.wxQrcodePath" width="100" height="100" :src="fullImg(activity.wxQrcodePath)" />

        <div class="upload-title">支付宝收款码</div>
        <van-uploader :after-read="(file) => (aliFile = file.file)" :max-count="1" />
        <van-image v-if="activity.aliQrcodePath" width="100" height="100" :src="fullImg(activity.aliQrcodePath)" />

        <van-button block type="success" @click="saveActivity">保存活动信息</van-button>
      </div>

      <div class="card">
        <div class="upload-title">宾客页面二维码</div>
        <div id="qrcode"></div>
      </div>

      <div class="card">
        <van-search v-model="keyword" placeholder="按姓名或代付人搜索" @search="loadRecords" />
        <van-cell title="总金额统计" :value="sum + ' 元'" />
        <van-space>
          <van-button size="small" type="primary" @click="loadRecords">刷新</van-button>
          <van-button size="small" type="warning" @click="exportExcel">导出 Excel</van-button>
          <van-button size="small" type="danger" @click="clearAll">清空全部</van-button>
        </van-space>

        <van-cell-group inset>
          <van-swipe-cell v-for="r in records" :key="r.id">
            <van-cell :title="`${r.name} ¥${r.amount}`" :label="`${r.submitTime || ''} | ${r.payerName}(${r.payerPhone || '-'}) | ${r.relation || ''} | ${r.blessing || ''}`" />
            <template #right>
              <van-button square type="danger" text="删除" @click="deleteOne(r.id)" />
            </template>
          </van-swipe-cell>
        </van-cell-group>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { showConfirmDialog, showFailToast, showSuccessToast } from 'vant'
import axios from 'axios'
import QRCode from 'qrcodejs2-fix'

const apiBase = import.meta.env.VITE_API_BASE || 'http://localhost:8080'
const token = ref(localStorage.getItem('token') || '')
const loginForm = ref({ username: 'admin', password: 'admin' })
const activity = ref({ title: '', date: '', location: '', wxQrcodePath: '', aliQrcodePath: '' })
const records = ref([])
const keyword = ref('')
const sum = ref(0)
let wxFile = null
let aliFile = null

const authHeader = () => ({ Authorization: `Bearer ${token.value}` })
const fullImg = (path) => (path ? `${apiBase}${path}` : '')

const buildQr = async () => {
  await nextTick()
  const el = document.getElementById('qrcode')
  if (!el) return
  el.innerHTML = ''
  new QRCode(el, {
    text: `${window.location.origin}/`,
    width: 220,
    height: 220
  })
}

const login = async () => {
  try {
    const { data } = await axios.post(`${apiBase}/api/login`, loginForm.value)
    token.value = data.token
    localStorage.setItem('token', data.token)
    showSuccessToast('登录成功')
    await initAdmin()
  } catch {
    showFailToast('登录失败')
  }
}

const loadActivity = async () => {
  const { data } = await axios.get(`${apiBase}/api/activity`)
  activity.value = data
}

const saveActivity = async () => {
  const formData = new FormData()
  formData.append('title', activity.value.title)
  formData.append('date', activity.value.date)
  formData.append('location', activity.value.location)
  if (wxFile) formData.append('wxFile', wxFile)
  if (aliFile) formData.append('aliFile', aliFile)
  await axios.post(`${apiBase}/api/activity/update`, formData, { headers: { ...authHeader() } })
  showSuccessToast('保存成功')
  wxFile = null
  aliFile = null
  await loadActivity()
}

const loadRecords = async () => {
  const { data } = await axios.get(`${apiBase}/api/records`, {
    headers: authHeader(),
    params: { keyword: keyword.value }
  })
  records.value = data.records
  sum.value = data.sum
}

const clearAll = async () => {
  await showConfirmDialog({ title: '确认', message: '确定清空所有记录？' })
  await axios.delete(`${apiBase}/api/records/clear`, { headers: authHeader() })
  showSuccessToast('已清空')
  await loadRecords()
}

const deleteOne = async (id) => {
  await axios.delete(`${apiBase}/api/records/${id}`, { headers: authHeader() })
  showSuccessToast('已删除')
  await loadRecords()
}

const exportExcel = async () => {
  const res = await axios.get(`${apiBase}/api/export`, {
    headers: authHeader(),
    responseType: 'blob'
  })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(res.data)
  link.download = 'gift_records.xlsx'
  link.click()
}

const initAdmin = async () => {
  await loadActivity()
  await loadRecords()
  await buildQr()
}

onMounted(async () => {
  await loadActivity()
  if (token.value) await initAdmin()
})
</script>

<style scoped>
.page { min-height: 100vh; background: #f7f8fa; }
.card { margin: 12px; padding: 12px; background: #fff; border-radius: 10px; }
.upload-title { margin: 10px 0 6px; color: #666; font-size: 14px; }
</style>
