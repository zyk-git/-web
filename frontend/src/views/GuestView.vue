<template>
  <div class="page">
    <van-nav-bar title="电子礼簿（H5 网页）" fixed placeholder />

    <van-card>
      <template #title>{{ activity.title || '活动标题' }}</template>
      <template #desc>
        <div>日期：{{ activity.date || '-' }}</div>
        <div>地点：{{ activity.location || '-' }}</div>
    <van-nav-bar title="电子礼簿" />

    <van-card>
      <template #title>{{ activity.title }}</template>
      <template #desc>
        <div>日期：{{ activity.date }}</div>
        <div>地点：{{ activity.location }}</div>
      </template>
    </van-card>

    <van-row gutter="10" class="qr-wrap">
      <van-col span="12">
        <div class="qr-title">微信收款码</div>
        <van-image v-if="activity.wxQrcodePath" width="100%" height="140" fit="contain" :src="fullImg(activity.wxQrcodePath)" />
        <van-empty v-else image="search" description="暂未上传" />
      </van-col>
      <van-col span="12">
        <div class="qr-title">支付宝收款码</div>
        <van-image v-if="activity.aliQrcodePath" width="100%" height="140" fit="contain" :src="fullImg(activity.aliQrcodePath)" />
        <van-empty v-else image="search" description="暂未上传" />
        <van-image width="100%" height="140" fit="contain" :src="fullImg(activity.wxQrcodePath)" />
      </van-col>
      <van-col span="12">
        <div class="qr-title">支付宝收款码</div>
        <van-image width="100%" height="140" fit="contain" :src="fullImg(activity.aliQrcodePath)" />
      </van-col>
    </van-row>

    <van-form @submit="submitForm">
      <div v-for="(item, index) in items" :key="index" class="line-box">
        <div class="line-head">人员 {{ index + 1 }}</div>
        <van-field v-model="item.name" label="姓名" placeholder="必填" required />
        <van-field v-model="item.amount" label="金额" type="number" placeholder="必填" required />
        <van-field v-model="item.relation" label="关系" readonly is-link @click="openRelation(index)" />
        <van-field v-model="item.blessing" label="祝福语" placeholder="可选" />
      </div>

      <div class="actions">
        <van-button block type="primary" plain @click="addItem" :disabled="items.length >= 10">+ 添加一人</van-button>
      </div>

      <van-field v-model="payerName" label="代付人姓名" placeholder="必填" required />
      <van-field v-model="payerPhone" label="代付人手机号" placeholder="可选" type="tel" />

      <van-cell title="总金额" :value="`${total} 元`" class="total" />

      <div class="actions">
        <van-button round block type="success" native-type="submit" :loading="submitting">提交记录</van-button>
      </div>
    </van-form>

    <van-action-sheet v-model:show="showRelationPicker" :actions="relationActions" cancel-text="取消" close-on-click-action @select="onSelectRelation" />
      <van-button block type="primary" plain @click="addItem" :disabled="items.length >= 10">+ 添加一人</van-button>

      <van-field v-model="payerName" label="代付人姓名" placeholder="必填" required />
      <van-field v-model="payerPhone" label="代付人手机号" placeholder="可选" />

      <van-cell title="总金额" :value="total + ' 元'" class="total" />

      <div style="margin: 16px">
        <van-button round block type="success" native-type="submit">提交记录</van-button>
      </div>
    </van-form>

    <van-popup v-model:show="showPicker" position="bottom">
      <van-picker :columns="relations" @confirm="confirmRelation" @cancel="showPicker = false" />
    </van-popup>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { showFailToast, showSuccessToast } from 'vant'
import axios from 'axios'

const apiBase = import.meta.env.VITE_API_BASE || 'http://localhost:8080'
const activity = ref({})
const payerName = ref('')
const payerPhone = ref('')
const submitting = ref(false)
const items = ref([{ name: '', amount: '', relation: '亲戚', blessing: '' }])

const showRelationPicker = ref(false)
const currentIndex = ref(0)
const relationActions = [
  { name: '亲戚' },
  { name: '同学' },
  { name: '同事' },
  { name: '朋友' },
  { name: '其他' }
]

const total = computed(() => items.value.reduce((sum, x) => sum + (Number(x.amount) || 0), 0).toFixed(2))
const fullImg = (path) => (path ? `${apiBase}${path}` : '')

const loadActivity = async () => {
  try {
    const { data } = await axios.get(`${apiBase}/api/activity`)
    activity.value = data || {}
  } catch {
    showFailToast('活动信息加载失败')
  }
const items = ref([{ name: '', amount: '', relation: '亲戚', blessing: '' }])
const relations = ['亲戚', '同学', '同事', '朋友', '其他']
const showPicker = ref(false)
const currentIndex = ref(0)

const total = computed(() => items.value.reduce((sum, x) => sum + (Number(x.amount) || 0), 0).toFixed(2))

const fullImg = (path) => (path ? `${apiBase}${path}` : '')

const loadActivity = async () => {
  const { data } = await axios.get(`${apiBase}/api/activity`)
  activity.value = data
}

const openRelation = (index) => {
  currentIndex.value = index
  showRelationPicker.value = true
}

const onSelectRelation = (action) => {
  items.value[currentIndex.value].relation = action.name
  showRelationPicker.value = false
}

const addItem = () => {
  if (items.value.length >= 10) return showFailToast('最多添加 10 人')
  items.value.push({ name: '', amount: '', relation: '亲戚', blessing: '' })
}

const submitForm = async () => {
  if (!payerName.value.trim()) return showFailToast('请填写代付人姓名')
  if (items.value.some((x) => !x.name.trim() || !x.amount || Number(x.amount) <= 0)) {
    return showFailToast('请填写完整且正确的姓名和金额')
  }

  submitting.value = true
  try {
    const payload = {
      payerName: payerName.value.trim(),
      payerPhone: payerPhone.value.trim(),
      items: items.value.map((x) => ({
        ...x,
        name: x.name.trim(),
        blessing: (x.blessing || '').trim(),
        amount: Number(x.amount)
      }))
    }
    const { data } = await axios.post(`${apiBase}/api/records`, payload)
    showSuccessToast(`记录成功！请扫码转账总金额 ${data.total} 元给主人`)
    payerName.value = ''
    payerPhone.value = ''
    items.value = [{ name: '', amount: '', relation: '亲戚', blessing: '' }]
  } catch (e) {
    showFailToast(e?.response?.data?.message || '提交失败，请稍后再试')
  } finally {
    submitting.value = false
  }
  showPicker.value = true
}

const confirmRelation = ({ selectedValues }) => {
  items.value[currentIndex.value].relation = selectedValues[0]
  showPicker.value = false
}

const addItem = () => {
  if (items.value.length < 10) items.value.push({ name: '', amount: '', relation: '亲戚', blessing: '' })
}

const submitForm = async () => {
  if (!payerName.value) return showFailToast('请填写代付人姓名')
  if (items.value.some((x) => !x.name || !x.amount)) return showFailToast('请填写完整姓名和金额')

  const payload = {
    payerName: payerName.value,
    payerPhone: payerPhone.value,
    items: items.value.map((x) => ({ ...x, amount: Number(x.amount) }))
  }
  const { data } = await axios.post(`${apiBase}/api/records`, payload)
  showSuccessToast(`记录成功！请扫码转账总金额 ${data.total} 元给主人`)
  payerName.value = ''
  payerPhone.value = ''
  items.value = [{ name: '', amount: '', relation: '亲戚', blessing: '' }]
}

onMounted(loadActivity)
</script>

<style scoped>
.page { padding-bottom: 20px; background: #f7f8fa; min-height: 100vh; }
.qr-wrap { padding: 12px; }
.qr-title { text-align: center; margin-bottom: 4px; font-size: 13px; color: #666; }
.line-box { margin: 10px; border-radius: 8px; overflow: hidden; background: #fff; }
.line-head { padding: 10px 12px; color: #666; font-size: 12px; background: #fafafa; }
.total { font-size: 18px; color: #07c160; font-weight: 700; }
.actions { margin: 12px; }
.total { font-size: 18px; color: #07c160; font-weight: 700; }
</style>
